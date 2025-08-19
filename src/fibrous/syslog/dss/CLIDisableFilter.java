package fibrous.syslog.dss;

import fibrous.ficli.FiCommand;
import fibrous.soffit.SoffitUtil;

public class CLIDisableFilter extends FiCommand {
	FilterManager filterManager;
	GUIIOStream ios;

	public CLIDisableFilter(String commandString, FilterManager filterManager, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.ios = ios;
		
		this.commandDescription = "Disables a configured filter.  Usage: disable [filter number]";
	}

	@Override
	public void execute() {
		if(filterManager.s_filters.objects.isEmpty()) {
			ios.println("There are no filters to disable");
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
		
		filterManager.setFilterEnabled(filterNumber, false);
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeAllFilters()));
	}
}
