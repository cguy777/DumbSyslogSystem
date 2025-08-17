package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.ficli.FiCommand;
import fibrous.ficli.FiOutputStream;
import fibrous.soffit.SoffitUtil;

public class CLIRemoveFilter extends FiCommand {

	FilterManager filterManager;
	GUIIOStream ios;
	
	public CLIRemoveFilter(String commandString, FilterManager filterManager, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.ios = ios;
		
		this.commandDescription = "Removes a filter.  Usage: rem [filter number or \"all\"]";
	}

	@Override
	public void execute() {
		if(arguments.size() < 1) {
			ios.println("Syntax error: must include the filter number to remove");
			return;
		}
		
		int i = 0;
		try {
			i = Integer.parseInt(arguments.get(0));
		} catch (NumberFormatException e) {
			
			if(arguments.get(0).equals("all")) {
				filterManager.clearFilters();
				ios.clearFilterEditor();
				ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeAllFilters()));
				return;
			}
			
			ios.println("Syntax error: argument was not a number");
			return;
		}
		
		if(i >= filterManager.filters.size())
			ios.println("Error: rule number does not exist");
		else
			filterManager.removeFilter(i);
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeAllFilters()));
	}
}
