package fibrous.syslog.dss;

import fibrous.ficli.FiCommand;
import fibrous.soffit.SoffitUtil;
import fibrous.syslog.dss.DumbSyslogViewer.MessageHandler;

public class CLIApplyFilters extends FiCommand {
	
	FilterManager filterManager;
	MessageHandler messageHandler;
	GUIIOStream ios;

	public CLIApplyFilters(String commandString, FilterManager filterManager, MessageHandler messageHandler, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.messageHandler = messageHandler;
		this.ios = ios;
		
		this.commandDescription = "Reloads the logs with the current filters applied.  Usage: apply";
	}

	@Override
	public void execute() {
		filterManager.setFiltersFromText(ios.console.filterEditor.getText());
		filterManager.applyFilters();
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeActiveFilters()));
		
		ios.clearLogs();
		messageHandler.resetConnection();
		
		ios.showTab(0);
	}
}
