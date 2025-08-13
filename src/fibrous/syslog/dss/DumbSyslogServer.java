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
	String logStorageLocation;
	int logStorageHours;
	int messageBufferCount;
	
	LogFileManager logFileManager;
	volatile SyslogMessageBuffer syslogBuffer;
	SyslogHandler handler;
	Thread syslogHandlerThread;
	
	volatile ArrayList<SyslogSubscriber> subscribers;
	
	public DumbSyslogServer(int syslogPort, int interfacePort, String logStorageLocation, int localLogStorageTimeHours, int messageBufferCount) throws IOException {
		syslogSocket = new DatagramSocket(syslogPort);
		packetBuffer = new byte[1024 * 1024];
		syslogPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
		this.logStorageLocation = logStorageLocation;
		this.logStorageHours = localLogStorageTimeHours;
		this.messageBufferCount = messageBufferCount;
		
		if(!logStorageLocation.isEmpty()) {
			//Add trailing path delimiter, if needed.
			if(logStorageLocation.charAt(logStorageLocation.length() - 1) != '/' || logStorageLocation.charAt(logStorageLocation.length() - 1) != '\\')
				logStorageLocation += '/';
		}
		
		if(interfacePort != 0) {
			allowSubscribers = true;
			interfaceSocket = new ServerSocket(interfacePort);
		}
		
		syslogBuffer = new SyslogMessageBuffer(messageBufferCount);
		subscribers = new ArrayList<>();
		logFileManager = new LogFileManager(localLogStorageTimeHours, logStorageLocation);
		handler = new SyslogHandler(syslogBuffer, subscribers, logFileManager);
	}
	
	public void mainLoop() throws IOException {
		syslogHandlerThread = Thread.ofPlatform().start(handler);
		
		while(true) {
			syslogSocket.receive(syslogPacket);
			SyslogData data = new SyslogData(syslogPacket);
			handler.dataQueue.push(data);
			handler.awaken();
		}
	}
	
	public static void main(String[]args) throws IOException {
		/*
		FileInputStream fis = new FileInputStream("save_cisco.dump");
		
		byte[] bytes = fis.readAllBytes();
		BSDSyslogMessage syslogMessage = BSDSyslogMessage.parseMessage(bytes, InetAddress.getLoopbackAddress());
		System.out.println(syslogMessage.pri);
		System.out.println(syslogMessage.timestamp);
		System.out.println(syslogMessage.hostname);
		System.out.println(syslogMessage.message);
		
		fis.close();
		*/
		
		FileInputStream configStream = new FileInputStream("config");
		SoffitObject s_config = SoffitUtil.ReadStream(configStream);
		configStream.close();
		
		int syslogPort = Integer.parseInt(s_config.getField("SyslogPort").getValue());
		
		int interfacePort = 0;
		if(s_config.hasField("InterfacePort"))
			interfacePort = Integer.parseInt(s_config.getField("InterfacePort").getValue());
		
		String logStorageLocation = "";
		if(s_config.hasField("LogStorageLocation"))
			logStorageLocation = s_config.getField("LogStorageLocation").getValue();
		
		int logStorageHours = Integer.parseInt(s_config.getField("LogStorageHours").getValue());
		int messageBufferCount = Integer.parseInt(s_config.getField("MessageBufferCount").getValue());
		
		DumbSyslogServer server = new DumbSyslogServer(syslogPort, interfacePort, logStorageLocation, logStorageHours, messageBufferCount);
		server.mainLoop();
	}
}

class SyslogHandler implements Runnable {
	
	int fileCount = 0;
	volatile ArrayDeque<SyslogData> dataQueue;
	volatile SyslogMessageBuffer syslogBuffer;
	volatile ArrayList<SyslogSubscriber> subscribers;
	boolean storeLogs;
	LogFileManager logFileManager;
	
	public SyslogHandler(SyslogMessageBuffer syslogBuffer, ArrayList<SyslogSubscriber> subscribers, LogFileManager logFileManager) throws FileNotFoundException {
		this.syslogBuffer = syslogBuffer;
		dataQueue = new ArrayDeque<>();
		this.subscribers = subscribers;
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
			
			for(int i = 0; i < subscribers.size(); i++) {
				//What happens when this hangs?
				//Extra assurance if a subscribers gets removed mid push
				if(i < subscribers.size())
					subscribers.get(i).pushMessage(syslogMessage);
			}
			
			logFileManager.writeLog(syslogMessage, data);
			
			System.out.println(syslogMessage.pri);
			System.out.println(syslogMessage.timestamp);
			System.out.println(syslogMessage.hostname);
			System.out.println(syslogMessage.message);
			System.out.println();
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
}

class SubscriberHandler implements Runnable {

	volatile ArrayList<SyslogSubscriber> subscribers;
	ServerSocket ss;
	
	public SubscriberHandler(int interfacePort, ArrayList<SyslogSubscriber> subscribers) throws IOException {
		ss = new ServerSocket(interfacePort);
		this.subscribers = subscribers;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Socket socket = ss.accept();
				SyslogSubscriber subscriber = new SyslogSubscriber(socket);
				subscribers.add(subscriber);
			} catch (IOException e) {}
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
	
	public LogFileManager(int logStorageHours, String logStorageLocation) {
		knownFiles = new ArrayList<>();
		knownDirs = new ArrayList<>();
		this.logStorageHours = logStorageHours;
		maxStorageTimeMillis = logStorageHours * 60 * 60 * 1000;
		this.logStorageLocation = logStorageLocation;
		storeLogs = !logStorageLocation.isEmpty();
		
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
		if(!storeLogs)
			return;

		if(!hasDir(Path.of(logStorageLocation + syslogMessage.hostname))) {
			try {
				if(!java.nio.file.Files.exists(Path.of(logStorageLocation + syslogMessage.hostname)))
					java.nio.file.Files.createDirectories(Path.of(logStorageLocation + syslogMessage.hostname));
				
				knownDirs.add(Path.of(logStorageLocation + syslogMessage.hostname));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			String fileName = logStorageLocation + syslogMessage.hostname + "/" + syslogMessage.timestamp;
			FileOutputStream fos = new FileOutputStream(fileName, true);
			fos.write(data.data);
			fos.write((int) '\n');
			fos.close();
			knownFiles.add(new LogFile(Path.of(fileName), System.currentTimeMillis()));
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