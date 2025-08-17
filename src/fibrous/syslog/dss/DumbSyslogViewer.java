package fibrous.syslog.dss;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Stack;

import javax.swing.JFrame;

import fibrous.ficli.FiState;
import fibrous.ficli.FiCLI;
import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitUtil;

public class DumbSyslogViewer {
	
	InetAddress serverAddress;
	int serverInterfacePort;
	Socket socket;
	
	FilterManager filterManager;
	FiGUIConsole console;
	CommandHistoryBuffer chb;
	
	DataReceiveHandler messageHandler;
	Thread messageHandlerThread;
	
	public DumbSyslogViewer(InetAddress serverAddress, int serverInterfacePort) throws IOException {
		this.serverAddress = serverAddress;
		this.serverInterfacePort = serverInterfacePort;
		
		filterManager = new FilterManager();
		
		JFrame frame = new JFrame("Dumb Syslog Viewer");
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		console = new FiGUIConsole();
		console.caret.setText(" > ");
		frame.add(console);
		frame.setVisible(true);
		chb = new CommandHistoryBuffer();
		
		messageHandler = new DataReceiveHandler(serverAddress, serverInterfacePort, filterManager, console);
		
		GUIIOStream ios = new GUIIOStream(console);
		
		FiCLI cli = new FiCLI(ios, ios, "?");
		cli.setCaret(" > ");
		
		cli.addCommand(new CLIAddInclusionFilter("inc msg seq", filterManager, ios));
		cli.addCommand(new CLIAddInclusionOrFilter("inc-or msg seq", filterManager, ios));
		cli.addCommand(new CLIAddExclusionFilter("exc msg seq", filterManager, ios));
		cli.addCommand(new CLIAddExclusionOrFilter("exc-or msg seq", filterManager, ios));
		cli.addCommand(new CLIRemoveFilter("rem", filterManager, ios));
		cli.addCommand(new CLIEnableFilter("enable", filterManager, ios));
		cli.addCommand(new CLIDisableFilter("disable", filterManager, ios));
		cli.addCommand(new CLIApplyFilters("apply", filterManager, messageHandler, ios));
		cli.addCommand(new CLIClearConsole("clear console", ios));
		
		console.enterButton.addActionListener(new DoCLIAction(cli, console, chb));
		console.input.addKeyListener(new DoCLIAction(cli, console, chb));
		console.input.addKeyListener(new CycleCommandHistoryAction(console, chb));
	}
	
	public void receiveData() {
		messageHandlerThread = Thread.ofVirtual().start(messageHandler);
	}
	
	public static void main(String[]args) throws SoffitException, IOException {
		DumbSyslogViewer client = new DumbSyslogViewer(InetAddress.getByName("10.0.50.118"), 54321);
		
		client.receiveData();
	}
}

class DataReceiveHandler implements Runnable {

	Socket socket;
	InetAddress serverAddress;
	int serverInterfacePort;
	FilterManager filterManager;
	FiGUIConsole console;
	
	public DataReceiveHandler(InetAddress serverAddress, int serverInterfacePort, FilterManager filterManager, FiGUIConsole console) {
		this.serverAddress = serverAddress;
		this.serverInterfacePort = serverInterfacePort;
		this.filterManager = filterManager;
		this.console = console;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				socket = new Socket(serverAddress, serverInterfacePort);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			while(true) {
				try {
					receiveData();
				} catch (SoffitException e) {
					System.out.println("Resetting (SOFFIT)");
					break;
				} catch (SocketException e) {
					System.out.println("Resetting (Socket)");
					break;
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}
	
	public void resetConnection() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void receiveData() throws SoffitException, SocketException, IOException {
		SoffitObject s_data = SoffitUtil.ReadStream(socket.getInputStream());
		
		SoffitObject s_encapedObject = s_data.getFirstObject();
		if(s_encapedObject.getType().equals("BSDSyslogMessage")) {
			BSDSyslogMessage message = BSDSyslogMessage.deserialize(s_encapedObject);
			if(filterManager.evaluateMessage(message))
				console.printLineToLog(message.getMessageAsFormattedString(false));
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