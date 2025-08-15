package fibrous.syslog.dss;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;

import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitUtil;

public class DumbSyslogServer {
	
	DatagramSocket syslogSocket;
	DatagramPacket syslogPacket;
	byte[] packetBuffer;
	
	ServerSocket interfaceSocket = null;
	boolean allowSubscribers = false;
	boolean disregardTimestamp = false;
	String logStorageLocation;
	int logStorageHours;
	int messageBufferCount;
	
	LogFileManager logFileManager;
	Thread logFileManagerThread;
	volatile SyslogMessageBuffer syslogBuffer;
	SyslogHandler syslogHandler;
	Thread syslogHandlerThread;
	
	SubscriberHandler subscriberHandler;
	Thread subscriberHandlerThread;
	
	public DumbSyslogServer(int syslogPort, int interfacePort, boolean disregardTimestamp, String logStorageLocation, int localLogStorageTimeHours, int messageBufferCount) throws IOException {
		syslogSocket = new DatagramSocket(syslogPort);
		packetBuffer = new byte[1024 * 1024];
		syslogPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
		this.disregardTimestamp = disregardTimestamp;
		this.logStorageLocation = logStorageLocation;
		this.logStorageHours = localLogStorageTimeHours;
		this.messageBufferCount = messageBufferCount;
		
		if(!logStorageLocation.isEmpty()) {
			//Add trailing path delimiter, if needed.
			if(logStorageLocation.charAt(logStorageLocation.length() - 1) != '/' || logStorageLocation.charAt(logStorageLocation.length() - 1) != '\\')
				logStorageLocation += '/';
		}
		
		if(interfacePort != 0)
			allowSubscribers = true;
		
		syslogBuffer = new SyslogMessageBuffer(messageBufferCount);
		subscriberHandler = new SubscriberHandler(interfacePort, syslogBuffer);
		logFileManager = new LogFileManager(localLogStorageTimeHours, logStorageLocation, disregardTimestamp);
		syslogHandler = new SyslogHandler(syslogBuffer, subscriberHandler, logFileManager);
	}
	
	public void mainLoop() throws IOException {
		syslogHandlerThread = Thread.ofPlatform().start(syslogHandler);
		logFileManagerThread = Thread.ofPlatform().start(logFileManager);
		if(allowSubscribers)
			subscriberHandlerThread = Thread.ofVirtual().start(subscriberHandler);
		
		System.out.println("DumbSyslogServer magic in the form of three platform threads and one virtual thread has started...");
		
		while(true) {
			syslogSocket.receive(syslogPacket);
			SyslogData data = new SyslogData(syslogPacket);
			syslogHandler.dataQueue.push(data);
			syslogHandler.awaken();
		}
	}
	
	public static void main(String[]args) throws IOException {
		//Load config
		FileInputStream configStream = new FileInputStream("config");
		SoffitObject s_config = SoffitUtil.ReadStream(configStream);
		configStream.close();
		
		int syslogPort = Integer.parseInt(s_config.getField("SyslogPort").getValue());
		
		boolean disregardTimestamp = false;
		if(s_config.hasField("DisregardTimestamp"))
			disregardTimestamp = Boolean.parseBoolean(s_config.getField("DisregardTimestamp").getValue());
		
		int interfacePort = 0;
		if(s_config.hasField("InterfacePort"))
			interfacePort = Integer.parseInt(s_config.getField("InterfacePort").getValue());
		
		String logStorageLocation = "";
		if(s_config.hasField("LogStorageLocation"))
			logStorageLocation = s_config.getField("LogStorageLocation").getValue();
		
		int logStorageHours = Integer.parseInt(s_config.getField("LogStorageHours").getValue());
		int messageBufferCount = Integer.parseInt(s_config.getField("MessageBufferCount").getValue());
		
		DumbSyslogServer server = new DumbSyslogServer(syslogPort, interfacePort, disregardTimestamp, logStorageLocation, logStorageHours, messageBufferCount);
		server.mainLoop();
	}
}

class SyslogHandler implements Runnable {
	
	volatile ArrayDeque<SyslogData> dataQueue;
	volatile SyslogMessageBuffer syslogBuffer;
	SubscriberHandler subscriberHandler;
	LogFileManager logFileManager;
	
	public SyslogHandler(SyslogMessageBuffer syslogBuffer, SubscriberHandler subscriberHandler, LogFileManager logFileManager) throws FileNotFoundException {
		this.syslogBuffer = syslogBuffer;
		dataQueue = new ArrayDeque<>();
		this.subscriberHandler = subscriberHandler;
		this.logFileManager = logFileManager;
	}
	
	@Override
	public void run() {
		while(true) {
			processLogs();
		}
	}
	
	private synchronized void processLogs() {
		while(!dataQueue.isEmpty()) {
			SyslogData data = dataQueue.pollLast();
			
			BSDSyslogMessage syslogMessage = BSDSyslogMessage.parseMessage(data.data, data.address);
			syslogBuffer.addMessage(syslogMessage);
			
			subscriberHandler.pushMessage(syslogMessage);
			
			logFileManager.writeLog(syslogMessage, data);
			
			/*
			System.out.println(syslogMessage.pri);
			System.out.println(syslogMessage.originalTimestamp);
			System.out.println(syslogMessage.hostname);
			System.out.println(syslogMessage.message);
			System.out.println();
			*/
		}
		
		try {
			wait();
		} catch (InterruptedException e) {}
	}
	
	public synchronized void awaken() {
		notify();
	}
}

class SyslogData {
	
	public byte[] data;
	public InetAddress address;
	
	public SyslogData(DatagramPacket packet) {
		data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
		this.address = packet.getAddress();
	}
}

class SyslogMessageBuffer {
	public ArrayDeque<BSDSyslogMessage> buffer;
	public int bufferSize;
	
	public SyslogMessageBuffer(int bufferSize) {
		buffer = new ArrayDeque<>();
		this.bufferSize = bufferSize;
	}
	
	public synchronized void addMessage(BSDSyslogMessage message) {
		buffer.push(message);
		
		if(buffer.size() > bufferSize)
			buffer.removeLast();
	}
	
	public ArrayDeque<BSDSyslogMessage> getCopyOfBuffer() {
		return buffer.clone();
	}
}

class SyslogSubscriber {
	
	Socket socket;
	SubscriberHandler handler;
	
	public SyslogSubscriber(Socket socket, SubscriberHandler handler) {
		this.socket = socket;
		this.handler = handler;
	}
	
	public void pushMessage(BSDSyslogMessage message) {
		SoffitObject s_message = new SoffitObject("root");
		s_message.add(message.serialize());
		
		try {
			SoffitUtil.WriteStream(s_message, socket.getOutputStream());
		} catch (IOException e) {
			//Remove from the subscriber list if there are any issues
			handler.subscribers.remove(this);
		}
	}
}

class SubscriberHandler implements Runnable {

	public volatile ArrayList<SyslogSubscriber> subscribers;
	ServerSocket ss;
	SyslogMessageBuffer buffer;
	
	public SubscriberHandler(int interfacePort, SyslogMessageBuffer buffer) throws IOException {
		ss = new ServerSocket(interfacePort);
		subscribers = new ArrayList<>();
		this.buffer = buffer;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Socket socket = ss.accept();
				SyslogSubscriber subscriber = new SyslogSubscriber(socket, this);
				subscribers.add(subscriber);
				
				//Relay the current buffer to the subscriber
				Thread.ofVirtual().start(() -> {
					ArrayDeque<BSDSyslogMessage> bufferedMessages = buffer.getCopyOfBuffer();
					while(!bufferedMessages.isEmpty()) {
						subscriber.pushMessage(bufferedMessages.pollLast());
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void pushMessage(BSDSyslogMessage message) {
		for(int i = 0; i < subscribers.size(); i++) {
			subscribers.get(i).pushMessage(message);
		}
	}
}

class LogFileManager implements Runnable {
	volatile ArrayList<LogFile> knownFiles;
	volatile ArrayList<Path> knownDirs;
	int logStorageHours;
	long maxStorageTimeMillis;
	String logStorageLocation;
	boolean storeLogs;
	boolean disregardTimestamp;
	
	public LogFileManager(int logStorageHours, String logStorageLocation, boolean disregardTimestamp) {
		knownFiles = new ArrayList<>();
		knownDirs = new ArrayList<>();
		this.logStorageHours = logStorageHours;
		maxStorageTimeMillis = logStorageHours * 60 * 60 * 1000;
		//maxStorageTimeMillis = logStorageHours * 1000;
		this.logStorageLocation = logStorageLocation;
		storeLogs = !logStorageLocation.isEmpty();
		this.disregardTimestamp = disregardTimestamp;
		
		scanForLogs();
	}

	@Override
	public void run() {
		//Don't perform any deletion if not configured to delete old logs
		if(logStorageHours == 0)
			return;
		
		//Don't do any log file management if there is no intention to store them in the first place
		if(!storeLogs)
			return;
		
		ArrayList<LogFile> filesToPop = new ArrayList<>();
		
		while(true) {
			for(int i = 0; i < knownFiles.size(); i++) {
				if(knownFiles.get(i).creationTime + maxStorageTimeMillis < System.currentTimeMillis()) {
					try {
						java.nio.file.Files.delete(knownFiles.get(i).fullPath);
						filesToPop.add(knownFiles.get(i));
					} catch (IOException e) {
						//Remove on failure
						filesToPop.add(knownFiles.get(i));
					}
				}
			}
			
			//Pop from known files list
			for(int i = 0; i < filesToPop.size(); i++) {
				knownFiles.remove(filesToPop.get(i));
			}
			
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {}
		}
	}
	
	public void writeLog(BSDSyslogMessage syslogMessage, SyslogData data) {
		//Don't write logs if not storing
		if(!storeLogs)
			return;

		Path logDirPath = Path.of(logStorageLocation + syslogMessage.hostname);
		if(!hasDir(logDirPath)) {
			try {
				if(!java.nio.file.Files.exists(logDirPath))
					java.nio.file.Files.createDirectories(logDirPath);
				
				knownDirs.add(logDirPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			String fileName = null;
			if(disregardTimestamp)
				fileName = logStorageLocation + syslogMessage.hostname + "/" + SyslogUtils.getFileFriendlyTimestamp(syslogMessage.receivedTimestamp);
			else
				fileName = logStorageLocation + syslogMessage.hostname + "/" + SyslogUtils.getFileFriendlyTimestamp(syslogMessage.originalTimestamp);
			
			FileOutputStream fos = new FileOutputStream(fileName, true);
			fos.write(data.data);
			fos.write((int) '\n');
			fos.close();
			knownFiles.add(new LogFile(Path.of(fileName), System.currentTimeMillis()));
		} catch(FileNotFoundException e) {
			//Somebody/something might've cleaned up the directories.
			knownDirs.remove(logDirPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean hasFile(Path file) {
		for(int i = 0; i < knownFiles.size(); i++) {
			if(knownFiles.get(i).fullPath.equals(file))
				return true;
		}
		
		return false;
	}
	
	public boolean hasDir(Path dir) {
		for(int i = 0; i < knownDirs.size(); i++) {
			if(knownDirs.get(i).equals(dir))
				return true;
		}
		
		return false;
	}
	
	private void scanForLogs() {
		try {
			if(!java.nio.file.Files.exists(Path.of(logStorageLocation))) {
				java.nio.file.Files.createDirectories(Path.of(logStorageLocation));
				return;
			}
			
			Iterator<Path> paths = java.nio.file.Files.list(Path.of(logStorageLocation)).iterator();
			recursiveFileScan(paths);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private void recursiveFileScan(Iterator<Path> paths) throws IOException {
		while(paths.hasNext()) {
			Path path = paths.next();
			if(java.nio.file.Files.isDirectory(path)) {
				knownDirs.add(path);
				recursiveFileScan(java.nio.file.Files.list(path).iterator());
			} else {
				knownFiles.add(new LogFile(path, java.nio.file.Files.getLastModifiedTime(path).toMillis()));
			}
		}
	}
}

class LogFile {
	Path fullPath;
	long creationTime;
	
	public LogFile(Path fullPath, long creationTime) {
		this.fullPath = fullPath;
		this.creationTime = creationTime;
	}
}