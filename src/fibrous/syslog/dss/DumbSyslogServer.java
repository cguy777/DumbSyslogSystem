/*
BSD 3-Clause License

Copyright (c) 2025, Noah McLean

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package fibrous.syslog.dss;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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

import fibrous.soffit.SoffitException;
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
		logFileManager = new LogFileManager(localLogStorageTimeHours, logStorageLocation, disregardTimestamp);
		subscriberHandler = new SubscriberHandler(interfacePort, syslogBuffer, logFileManager);
		syslogHandler = new SyslogHandler(syslogBuffer, subscriberHandler, logFileManager);
	}
	
	public void mainLoop() throws IOException {
		syslogHandlerThread = Thread.ofPlatform().start(syslogHandler);
		logFileManagerThread = Thread.ofPlatform().start(logFileManager);
		if(allowSubscribers)
			subscriberHandlerThread = Thread.ofVirtual().start(subscriberHandler);
		
		System.out.println("DumbSyslogServer has started...");
		
		while(true) {
			syslogSocket.receive(syslogPacket);
			SyslogData data = new SyslogData(syslogPacket);
			syslogHandler.dataQueue.push(data);
			syslogHandler.awaken();
		}
	}
	
	public static void main(String[]args) throws IOException {
		//Load config
		FileInputStream configStream = new FileInputStream("config_server");
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
			logFileManager.addLogToWriteQueue(syslogMessage);
			subscriberHandler.pushMessage(syslogMessage);
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
	volatile boolean performingInitialPush = true;
	
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
	LogFileManager fileManager;
	
	public SubscriberHandler(int interfacePort, SyslogMessageBuffer buffer, LogFileManager fileManager) throws IOException {
		ss = new ServerSocket(interfacePort);
		subscribers = new ArrayList<>();
		this.buffer = buffer;
		this.fileManager = fileManager;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Socket socket = ss.accept();
				SyslogSubscriber sub = new SyslogSubscriber(socket, this);
				subscribers.add(sub);
				
				//Relay the current buffer to the subscriber
				Thread.ofVirtual().start(() -> {
					ArrayDeque<BSDSyslogMessage> bufferedMessages = buffer.getCopyOfBuffer();
					while(!bufferedMessages.isEmpty()) {
						sub.pushMessage(bufferedMessages.pollLast());
					}
					sub.performingInitialPush = false;
					
					//Initialize request handler
					SubscriberReceiveHandler rxHandler = new SubscriberReceiveHandler(socket, sub, this, fileManager);
					Thread.ofVirtual().start(rxHandler);
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void pushMessage(BSDSyslogMessage message) {
		for(int i = 0; i < subscribers.size(); i++) {
			SyslogSubscriber sub = subscribers.get(i);
			while(true) {
				if(sub.performingInitialPush) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {}
					continue;
				}
			
				sub.pushMessage(message);
				break;
			}
		}
	}
}

class SubscriberReceiveHandler implements Runnable {
	
	Socket s;
	SyslogSubscriber sub;
	SubscriberHandler subHandler;
	LogFileManager fileManager;
	
	public SubscriberReceiveHandler(Socket s, SyslogSubscriber sub, SubscriberHandler subHandler, LogFileManager fileManager) {
		this.s = s;
		this.sub = sub;
		this.subHandler = subHandler;
		this.fileManager = fileManager;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				SoffitObject s_reqRoot = SoffitUtil.ReadStream(s.getInputStream());
				
				SoffitObject s_req = s_reqRoot.getFirstObject();
				String reqType = s_req.getType();
				if(reqType.equals("GetArchive")) {
					sendLogArchive();
				}
				
			} catch (SoffitException | IOException e) {
				subHandler.subscribers.remove(sub);
				fileManager.archiveLocked = false;
				break;
			}
		}
		
		try {
			s.close();
		} catch (IOException e) {}
	}
	
	private void sendLogArchive() throws IOException {
		SoffitObject s_root = new SoffitObject("root");
		SoffitObject s_logArchive = new SoffitObject("LogArchive");
		s_root.add(s_logArchive);
		ArrayList<BSDSyslogMessage> syslogs = fileManager.compileLogArchive();
		
		for(int i = 0; i < syslogs.size(); i++) {
			s_logArchive.add(syslogs.get(i).serialize());
		}
		
		SoffitUtil.WriteStream(s_root, s.getOutputStream());
	}
}

class LogFileManager implements Runnable {
	volatile ArrayList<LogFile> knownFiles;
	volatile ArrayList<Path> knownDirs;
	volatile ArrayDeque<BSDSyslogMessage> logWriteQueue;
	int logStorageHours;
	long maxStorageTimeMillis;
	String logStorageLocation;
	boolean storeLogs;
	boolean disregardTimestamp;
	
	volatile boolean archiveLocked = false;
	
	public LogFileManager(int logStorageHours, String logStorageLocation, boolean disregardTimestamp) {
		knownFiles = new ArrayList<>();
		knownDirs = new ArrayList<>();
		logWriteQueue = new ArrayDeque<>();
		this.logStorageHours = logStorageHours;
		maxStorageTimeMillis = logStorageHours * 60 * 60 * 1000;
		this.logStorageLocation = logStorageLocation;
		storeLogs = !logStorageLocation.isEmpty();
		this.disregardTimestamp = disregardTimestamp;
		
		scanForLogs();
	}

	/**
	 * Monitors for log file age and deletes if old enough
	 */
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
			if(!archiveLocked) {
				
				//Delete expired logs
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
				
				//Write new logs
				while(!logWriteQueue.isEmpty()) {
					BSDSyslogMessage message = logWriteQueue.pollLast();
					writeLog(message);
				}
			}
			
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {}
		}
	}
	
	public synchronized void addLogToWriteQueue(BSDSyslogMessage syslogMessage) {
		logWriteQueue.push(syslogMessage);
	}
	
	public void writeLog(BSDSyslogMessage syslogMessage) {
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
			fos.write(syslogMessage.getMessageAsFormattedString(!disregardTimestamp).getBytes());
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
	
	public ArrayList<BSDSyslogMessage> compileLogArchive() throws IOException {
		archiveLocked = true;
		
		ArrayList<BSDSyslogMessage> archive = new ArrayList<>();
		FileReader reader;
		BufferedReader br;
		for(int i = 0; i < knownFiles.size(); i++) {
			reader = new FileReader(knownFiles.get(i).fullPath.toFile());
			br = new BufferedReader(reader);
			
			String fallbackHostname = knownFiles.get(i).fullPath.getParent().getFileName().toString();
			
			while(true) {
				String line = br.readLine();
				if(line != null) {
					BSDSyslogMessage message = BSDSyslogMessage.parseMessage(line.getBytes(), fallbackHostname);
					archive.add(message);
				} else
					break;
			}
			
			br.close();
			reader.close();
		}
		
		archiveLocked = false;
		return archive;
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