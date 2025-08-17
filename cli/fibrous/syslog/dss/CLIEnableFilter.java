package fibrous.syslog.dss;

import fibrous.ficli.FiCommand;
import fibrous.soffit.SoffitUtil;

public class CLIEnableFilter extends FiCommand {
	FilterManager filterManager;
	GUIIOStream ios;

	public CLIEnableFilter(String commandString, FilterManager filterManager, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.ios = ios;
		
		this.commandDescription = "Enables a configured filter.  Usage: enable [filter number]";
	}

	@Override
	public void execute() {
		if(filterManager.s_filters.objects.isEmpty()) {
			ios.println("There are no filters to enable");
			return;
		}
		
		if(arguments.size() < 1) {
			ios.println("Syntax error: must include a filter number");
			return;
		}
		
		int filterNumber = 0;
		try {
			filterNumber = Integer.parseInt(arguments.get(0));
		} catch (NumberFormatException e) {
			ios.println("Syntax error: argument must be a valid filter number");
			return;
		}
		
		if(filterNumber > filterManager.s_filters.objects.size() - 1 || filterNumber < 0) {
			ios.println("Syntax error: argument must be a valid filter number");
			return;
		}
		
		filterManager.setFilterEnabled(filterNumber, true);
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeAllFilters()));
	}
}
