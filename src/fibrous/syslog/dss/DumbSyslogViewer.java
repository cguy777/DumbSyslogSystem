package fibrous.syslog.dss;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.ArrayList;

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
	
	MessageHandler messageHandler;
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
		
		messageHandler = new DumbSyslogViewer.MessageHandler();
		
		GUIIOStream ios = new GUIIOStream(console);
		
		FiCLI cli = new FiCLI(ios, ios, "?");
		cli.setCaret(" > ");
		
		cli.addCommand(new CLIAddInclusionFilter("inc msg seq", filterManager, ios));
		cli.addCommand(new CLIAddExclusionFilter("exc msg seq", filterManager, ios));
		cli.addCommand(new CLIRemoveFilter("rem", filterManager, ios));
		cli.addCommand(new CLIApplyFilters("apply", filterManager, messageHandler, ios));
		cli.addCommand(new CLIClearConsole("clear console", ios));
		
		console.enterButton.addActionListener(new DoCLIAction(cli, socket));
		console.input.addKeyListener(new DoCLIAction(cli, socket));
	}
	
	public void receiveData() {
		messageHandlerThread = Thread.ofVirtual().start(messageHandler);
	}
	
	public static void main(String[]args) throws SoffitException, IOException {
		DumbSyslogViewer client = new DumbSyslogViewer(InetAddress.getByName("10.0.50.118"), 54321);
		
		client.receiveData();
	}
	
	class MessageHandler implements Runnable {

		Socket socket;
		
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
						break;
					} catch (SocketException e) {
						break;
					} catch (IOException e) {
						e.printStackTrace();
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
	
	static class DoCLIAction implements ActionListener, KeyListener {
		
		private FiCLI cli;
		private Socket connection;
		
		public DoCLIAction(FiCLI cli, Socket connection) {
			this.cli = cli;
			this.connection = connection;
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
			
			if(cliState.state == FiState.EXIT) {
				try {
					connection.close();
					System.exit(0);
				} catch (IOException ex) {
					
				}
			}
			
			//Just ignore empty input
			if(cliState.input.length() == 0)
				return;
			
			if(cliState.state == FiState.INVALID) {
				cli.getOutputStream().println("INVALID INPUT: " + cliState.input);
			}
		}
	}
}

