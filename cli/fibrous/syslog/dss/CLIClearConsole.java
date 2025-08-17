package fibrous.syslog.dss;

import fibrous.ficli.FiCommand;
import fibrous.soffit.SoffitUtil;

public class CLIClearConsole extends FiCommand {
	GUIIOStream ios;
	
	public CLIClearConsole(String commandString, GUIIOStream ios) {
		super(commandString);
		this.ios = ios;
		
		this.commandDescription = "Clears the console window.  Usage: clear conole";
	}

	@Override
	public void execute() {
		ios.clearConsole();
	}
}
