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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.JFrame;

import fibrous.ficli.FiState;
import fibrous.ficli.FiCLI;
import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitUtil;

public class DumbSyslogViewer {
	
	Socket socket;
	
	FilterManager filterManager;
	FiGUIConsole console;
	CommandHistoryBuffer chb;
	
	DataReceiveHandler receiveHandler;
	Thread messageHandlerThread;
	
	boolean configured = false;
	
	public DumbSyslogViewer() throws IOException {
		
		JFrame frame = new JFrame("DumbSyslogViewer");
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		console = new FiGUIConsole();
		console.caret.setText(" > ");
		frame.add(console);
		frame.setVisible(true);
		chb = new CommandHistoryBuffer();
		
		filterManager = new FilterManager();
		try {
			FileInputStream fis = new FileInputStream("ActiveFilters.soffit");
			filterManager.s_filters = SoffitUtil.ReadStream(fis);
			filterManager.applyFilters();
			fis.close();
		} catch (SoffitException e) {
		} catch (IOException e) {
		}
		console.filterEditor.setText(SoffitUtil.WriteStreamToString(filterManager.serializeActiveFilters()));
		
		receiveHandler = new DataReceiveHandler(null, -1, filterManager, console);
		
		GUIIOStream ios = new GUIIOStream(console);
		
		FiCLI cli = new FiCLI(ios, ios, "?");
		cli.setCaret(" > ");
		
		cli.addCommand(new CLIAddInclusionFilter("inc", filterManager, ios));
		cli.addCommand(new CLIAddExclusionFilter("exc", filterManager, ios));
		cli.addCommand(new CLIAddSeverityFilter("severity", filterManager, ios));
		cli.addCommand(new CLIRemoveFilter("rem", filterManager, ios));
		cli.addCommand(new CLIEnableFilter("enable", filterManager, ios));
		cli.addCommand(new CLIDisableFilter("disable", filterManager, ios));
		cli.addCommand(new CLIApplyFilters("apply", filterManager, receiveHandler, ios));
		cli.addCommand(new CLIClearConsole("clear console", ios));
		cli.addCommand(new CLIServerSet("server set", receiveHandler, ios, this));
		cli.addCommand(new CLIServerShow("server show", receiveHandler, ios, this));
		cli.addCommand(new CLIConnect("connect", receiveHandler, ios));
		cli.addCommand(new CLIGetLogArchive("get archive", filterManager, receiveHandler, ios));
		
		console.enterButton.addActionListener(new DoCLIAction(cli, console, chb));
		console.input.addKeyListener(new DoCLIAction(cli, console, chb));
		console.input.addKeyListener(new CycleCommandHistoryAction(console, chb));
		
		//Write initial messages to console
		console.printLineToConsole("DumbSyslogViewer");
		console.printLineToConsole("Copyright (c) 2025, Noah McLean");
		console.printLineToConsole("Type ? for available commands");
		
		loadSettings();
		
		if(!filterManager.filters.isEmpty())
			console.printLineToConsole("THERE ARE CURRENTLY ACTIVE FILTERS");
	}
	
	public void writeSettings() {
		SoffitObject s_settings = new SoffitObject("root");
		s_settings.add(new SoffitField("ServerHostname", receiveHandler.serverAddress.getHostAddress()));
		s_settings.add(new SoffitField("ServerInterfacePort", String.valueOf(receiveHandler.serverInterfacePort)));
		
		try {
			FileOutputStream fos = new FileOutputStream("ViewerSettings.soffit");
			SoffitUtil.WriteStream(s_settings, fos);
			fos.close();
		} catch (IOException e) {
			console.printLineToLog(e.getMessage());
		}
	}
	
	public void loadSettings() {
		try {
			FileInputStream fis = new FileInputStream("ViewerSettings.soffit");
			SoffitObject s_settings = SoffitUtil.ReadStream(fis);
			fis.close();
			
			receiveHandler.serverAddress = InetAddress.getByName(s_settings.getField("ServerHostname").getValue());
			receiveHandler.serverInterfacePort = Integer.parseInt(s_settings.getField("ServerInterfacePort").getValue());
			
			configured = true;
		} catch(FileNotFoundException e) {
			console.printLineToConsole("No configuration found.  Please call \"server set\"");
		} catch (IOException e) {
			console.printLineToLog(e.getMessage());
		}
	}
	
	public void receiveData() {
		messageHandlerThread = Thread.ofVirtual().start(receiveHandler);
	}
	
	public static void main(String[]args) throws SoffitException, IOException {
		DumbSyslogViewer client = new DumbSyslogViewer();
		
		client.receiveData();
	}
}

class DataReceiveHandler implements Runnable {

	Socket socket;
	InetAddress serverAddress;
	int serverInterfacePort;
	FilterManager filterManager;
	FiGUIConsole console;
	
	volatile boolean connect = true;
	
	volatile boolean receivingArchive = false;
	
	boolean archive_filter = false;
	boolean archive_consolodate = false;
	
	public DataReceiveHandler(InetAddress serverAddress, int serverInterfacePort, FilterManager filterManager, FiGUIConsole console) {
		this.serverAddress = serverAddress;
		this.serverInterfacePort = serverInterfacePort;
		this.filterManager = filterManager;
		this.console = console;
	}
	
	@Override
	public void run() {
		long lastConnectionAttemptTime = 0;
		while(true) {
			if(serverAddress == null)
				connect = false;
			
			if(!connect) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
				continue;
			}
			
			try {
				console.clearLog();
				console.printLineToConsole("Connecting to server...");
				
				//Annoying stuff to not spam the console during certain connection problems 
				long currentConnectionAttemptTime = System.currentTimeMillis();
				long connectionAttemptDelta = currentConnectionAttemptTime - lastConnectionAttemptTime;
				if(connectionAttemptDelta < 5000) {
					Thread.sleep(5000 - connectionAttemptDelta);
				}
				
				socket = new Socket(serverAddress, serverInterfacePort);
				console.printLineToConsole("Connected to server.  Will begin receiving logs");
			} catch (IOException e) {
				lastConnectionAttemptTime = System.currentTimeMillis();
				console.printToConsole("Could not connect to server.");
				if(connect)
					console.printToConsole("  Will try again.");
				console.printToConsole("\n");
				continue;
			} catch (InterruptedException e) {}
			
			while(true) {
				if(!connect) {
					console.printLineToConsole("Disconnected");
					break;
				}
				
				try {
					receiveData();
				} catch (SoffitException e) {
					console.printToConsole("Disconnected.");
					if(connect)
						console.printToConsole("  Will attempt re-connection and refresh displayed logs");
					console.printToConsole("\n");
					break;
				} catch (SocketException e) {
					console.printToConsole("Disconnected.");
					if(connect)
						console.printToConsole("  Will attempt re-connection and refresh displayed logs");
					console.printToConsole("\n");
					break;
				} catch (Exception e) {
					console.printLineToConsole(e.getMessage());
					break;
				}
			}
		}
	}
	
	public void resetConnection() {
		try {
			if(socket != null)
				socket.close();
		} catch (IOException e) {}
	}

	public void receiveData() throws SoffitException, SocketException, IOException {
		SoffitObject s_data = SoffitUtil.ReadStream(socket.getInputStream());
		
		SoffitObject s_encapedObject = s_data.getFirstObject();
		String type = s_encapedObject.getType();
		//Regular live message push
		if(type.equals("BSDSyslogMessage")) {
			BSDSyslogMessage message = BSDSyslogMessage.deserialize(s_encapedObject);
			if(filterManager.evaluateMessage(message))
				console.printLineToLog(message.getMessageAsFormattedString(false));
		}
		//Entire log archive
		else if(type.equals("LogArchive")) {
			//Re-writing timestamp
			handleLogArchive(s_encapedObject, false, archive_filter, archive_consolodate);
		}
	}
	
	private void handleLogArchive(SoffitObject s_logArchive, boolean disregardOriginalTimestamp, boolean filter, boolean consolodate) {
		String logStorageLocation = "./logs/";
		ArrayList<SoffitObject> logs = s_logArchive.getAllObjects();
		
		if(consolodate) {
			writeConsolodatedLog(logs, logStorageLocation, filter);
		} else {
			for(int i = 0; i < logs.size(); i++) {
				BSDSyslogMessage message = BSDSyslogMessage.deserialize(logs.get(i));
				
				//If we are filtering AND fail a filter evaluation, then skip this message
				if(filter && !filterManager.evaluateMessage(message))
					continue;
			
				writeLog(message, logStorageLocation, disregardOriginalTimestamp);
			}
		}
		
		receivingArchive = false;
	}
	
	public void writeLog(BSDSyslogMessage syslogMessage, String logStorageLocation, boolean disregardTimestamp) {

		Path logDirPath = Path.of(logStorageLocation + syslogMessage.hostname);
		try {
			if(!java.nio.file.Files.exists(logDirPath))
				java.nio.file.Files.createDirectories(logDirPath);
		} catch (IOException e) {
			e.printStackTrace();
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeConsolodatedLog(ArrayList<SoffitObject> logs, String logStorageLocation, boolean filter) {
		Path logDirPath = Path.of(logStorageLocation);
		try {
			if(!java.nio.file.Files.exists(logDirPath))
				java.nio.file.Files.createDirectories(logDirPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ArrayList<BSDSyslogMessage> syslogs = new ArrayList<>(logs.size());
		ArrayList<Long> times = new ArrayList<>(logs.size());
		for(int i = 0; i < logs.size(); i++) {
			BSDSyslogMessage syslog = BSDSyslogMessage.deserialize(logs.get(i));
			
			//Filter evaluation
			if(filter && !filterManager.evaluateMessage(syslog))
				continue;
			
			if(syslogs.isEmpty()) {
				syslogs.add(syslog);
				times.add(timestampToLong(syslog.originalTimestamp));
			} else {
				insertSortSyslog(syslogs, times, syslog);
			}
		}
		
		try {
			FileWriter fw = new FileWriter(logStorageLocation + "ConsolidatedLog");
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(int i = 0; i < syslogs.size(); i++) {
				bw.write(syslogs.get(i).getMessageAsFormattedString(true));
				bw.newLine();
			}
			
			bw.flush();
			bw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void insertSortSyslog(ArrayList<BSDSyslogMessage> syslogs, ArrayList<Long> times, BSDSyslogMessage syslog) {
		long time = timestampToLong(syslog.originalTimestamp);
		for(int i = 0; i < syslogs.size(); i++) {
			if(time < times.get(i)) {
				syslogs.add(i, syslog);
				times.add(i, time);
				return;
			}
		}

		syslogs.add(syslog);
		times.add(time);
	}
	
	//Returns a derived long that is simply for comparison between other values generated ONLY from this function.  
	public long timestampToLong(String timestamp) {
		String monthStr = "";
		String dayStr = "";
		String hourStr = "";
		String minuteStr = "";
		String secondStr = "";
		
		//fill month
		monthStr = timestamp.substring(0, 3);
		
		//fill day
		dayStr = timestamp.substring(4, 6);
		
		//fill time
		hourStr = timestamp.substring(7, 9);
		minuteStr = timestamp.substring(10, 12);
		secondStr = timestamp.substring(13);
		
		int month = getNumFromMonth(monthStr);
		int day = Integer.parseInt(dayStr);
		int hour = Integer.parseInt(hourStr);
		int minute = Integer.parseInt(minuteStr);
		int second = Integer.parseInt(secondStr);
		
		long comboValue = second;
		comboValue += minute * 100;
		comboValue += hour * 10000;
		comboValue += day * 1000000;
		comboValue += month * 100000000;
		
		return comboValue;
	}
	
	public int getNumFromMonth(String month) {
		switch(month) {
		case "Jan": return 1;
		case "Feb": return 2;
		case "Mar": return 3;
		case "Apr": return 4;
		case "May": return 5;
		case "Jun": return 6;
		case "Jul": return 7;
		case "Aug": return 8;
		case "Sep": return 9;
		case "Oct": return 10;
		case "Nov": return 11;
		case "Dec": return 12;
		default: return -1;
		}
	}
}

class DoCLIAction implements ActionListener, KeyListener {
	
	private FiCLI cli;
	FiGUIConsole console;
	CommandHistoryBuffer chb;
	
	public DoCLIAction(FiCLI cli, FiGUIConsole console, CommandHistoryBuffer chb) {
		this.cli = cli;
		this.console = console;
		this.chb = chb;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_ENTER) {
			process();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		process();
	}
	
	private void process() {
		
		FiState cliState = cli.processCommand();
		chb.pushCommand(cliState.input);
		
		if(cliState.state == FiState.EXIT) {
			System.exit(0);
		}
		
		//Just ignore empty input
		if(cliState.input.length() == 0)
			return;
		
		if(cliState.state == FiState.INVALID) {
			cli.getOutputStream().println("INVALID INPUT: " + cliState.input);
		}
	}
}

class CycleCommandHistoryAction implements KeyListener {
	
	CommandHistoryBuffer chb;
	FiGUIConsole console;
	
	public CycleCommandHistoryAction(FiGUIConsole console, CommandHistoryBuffer chb) {
		this.chb = chb;
		this.console = console;
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_UP) {
			chb.commandHistoryIndex--;
			console.input.setText(chb.getCommandHistory());
		} else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
			chb.commandHistoryIndex++;
			console.input.setText(chb.getCommandHistory());
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {}
	
}

class CommandHistoryBuffer {
	Stack<String> commandHistory;
	int commandHistoryIndex = 0;
	
	public CommandHistoryBuffer() {
		commandHistory = new Stack<>();
	}
	
	public void pushCommand(String command) {
		if(commandHistory.size() > 1) {
			if(!commandHistory.peek().equals(command))
				commandHistory.push(command);
		} else {
			commandHistory.push(command);
		}
		
		commandHistoryIndex = commandHistory.size();
	}
	
	public String getCommandHistory() {
		commandHistoryIndex = Math.clamp(commandHistoryIndex, 0, commandHistory.size());
		if(commandHistoryIndex == commandHistory.size())
			return "";
		else
			return commandHistory.get(commandHistoryIndex);
	}
}