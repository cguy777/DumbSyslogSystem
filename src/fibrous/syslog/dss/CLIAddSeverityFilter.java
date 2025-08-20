package fibrous.syslog.dss;

import fibrous.ficli.FiCommand;
import fibrous.soffit.SoffitUtil;

public class CLIAddSeverityFilter extends FiCommand {
	
	FilterManager filterManager;
	GUIIOStream ios;

	public CLIAddSeverityFilter(String commandString, FilterManager filterManager, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.ios = ios;
		
		this.commandDescription = "Adds a mimimum severity filter.  Usage: severity [severity level 0-7]";
	}

	@Override
	public void execute() {
		if(arguments.size() < 1) {
			ios.println("Syntax error: must include the minimum severity level (0-7)");
			return;
		}
		
		int severity = 0;
		try {
			severity = Integer.parseInt(arguments.get(0));
		} catch (NumberFormatException e) {
			ios.println("Syntax error: a severity of 0 and 7 must be specified");
			return;
		}
		
		if(severity < 0 || severity > 7) {
			ios.println("Syntax error: a severity of 0 and 7 must be specified");
			return;
		}
		
		SeverityFilter filter = new SeverityFilter(severity);
		filterManager.addFilter(filter.serialize());
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeAllFilters()));
		ios.showTab(1);
	}
}
