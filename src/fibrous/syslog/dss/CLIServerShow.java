package fibrous.syslog.dss;

import java.net.InetAddress;
import java.net.UnknownHostException;

import fibrous.ficli.FiCommand;

public class CLIServerShow extends FiCommand {
	
	DataReceiveHandler drh;
	GUIIOStream ios;
	DumbSyslogViewer viewer;

	public CLIServerShow(String commandString, DataReceiveHandler drh, GUIIOStream ios, DumbSyslogViewer viewer) {
		super(commandString);
		this.drh = drh;
		this.ios = ios;
		this.viewer = viewer;
		
		this.commandDescription = "Shows the DumbSyslogServer this viewer is pointed towards.  Usage: server show";
	}

	@Override
	public void execute() {
		if(viewer.configured) {
			ios.println("IP Address: " + drh.serverAddress.getHostAddress());
			ios.println("port: " + drh.serverInterfacePort);
		} else {
			ios.println("Not currently configured to a server.  Please call \"server set\"");
		}
	}
}
