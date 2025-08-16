package fibrous.syslog.dss;

import fibrous.ficli.FiCommand;
import fibrous.syslog.dss.DumbSyslogViewer.MessageHandler;

public class CLIApplyFilters extends FiCommand {
	
	FilterManager filterManager;
	MessageHandler messageHandler;
	FiGUIConsole console;

	public CLIApplyFilters(String commandString, FilterManager filterManager, MessageHandler messageHandler, FiGUIConsole console) {
		super(commandString);
		this.filterManager = filterManager;
		this.messageHandler = messageHandler;
		this.console = console;
		
		this.commandDescription = "Reloads the logs with the current filters applied.  Usage: apply";
	}

	@Override
	public void execute() {
		console.clearConsoleLog();
		messageHandler.resetConnection();
	}
}
