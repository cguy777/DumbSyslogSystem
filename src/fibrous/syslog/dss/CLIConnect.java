package fibrous.syslog.dss;

import java.net.InetAddress;
import java.net.UnknownHostException;

import fibrous.ficli.FiCommand;

public class CLIConnect extends FiCommand {
	
	DataReceiveHandler drh;
	GUIIOStream ios;

	public CLIConnect(String commandString, DataReceiveHandler drh, GUIIOStream ios) {
		super(commandString);
		this.drh = drh;
		this.ios = ios;
		
		this.commandDescription = "Attempts to establish a connection to the configured server.  Usage: connect";
	}

	@Override
	public void execute() {
		drh.resetConnection();
		drh.connect = true;
	}
}
