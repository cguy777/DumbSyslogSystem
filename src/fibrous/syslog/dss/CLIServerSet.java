package fibrous.syslog.dss;

import java.net.InetAddress;
import java.net.UnknownHostException;

import fibrous.ficli.FiCommand;

public class CLIServerSet extends FiCommand {
	
	DataReceiveHandler drh;
	GUIIOStream ios;
	DumbSyslogViewer viewer;

	public CLIServerSet(String commandString, DataReceiveHandler drh, GUIIOStream ios, DumbSyslogViewer viewer) {
		super(commandString);
		this.drh = drh;
		this.ios = ios;
		this.viewer = viewer;
		
		this.commandDescription = "Sets the DumbSyslogServer this viewer is pointed towards.  Usage: server set [ip/hostname] [port]";
	}

	@Override
	public void execute() {
		if(arguments.size() < 2) {
			ios.println("Syntax error: you must include both the IP address/hostname and the port of the server");
			return;
		}
		
		try {
			InetAddress serverAddress = InetAddress.getByName(arguments.get(0));
			int serverPort = Integer.parseInt(arguments.get(1));
			
			if(serverPort < 0 || serverPort > 65535) {
				ios.println("Port number must be between 0 and 65535.");
				return;
			}
			
			drh.serverAddress = serverAddress;
			drh.serverInterfacePort = serverPort;
			drh.connect = false;
			drh.resetConnection();
			viewer.writeSettings();
			viewer.configured = true;
			
			ios.println("Configuration changed.  Please call \"connect\" (or just close the program with the big red X button)");
		} catch (UnknownHostException e) {
			ios.println("Error: either an unresolvable hostname or an improperly formatted IP address was provided");
		} catch (NumberFormatException e) {
			ios.println("Error: argument that should be a port number was not a number");
		}
	}
}
