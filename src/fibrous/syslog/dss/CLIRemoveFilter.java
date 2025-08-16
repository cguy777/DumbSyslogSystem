package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.ficli.FiCommand;
import fibrous.ficli.FiOutputStream;

public class CLIRemoveFilter extends FiCommand {

	FilterManager filterManager;
	FiOutputStream fos;
	
	public CLIRemoveFilter(String commandString, FilterManager filterManager, FiOutputStream fos) {
		super(commandString);
		this.filterManager = filterManager;
		this.fos = fos;
		
		this.commandDescription = "Removes a filter.  Usage: remove [filter number or \"all\"]";
	}

	@Override
	public void execute() {
		if(arguments.size() < 1) {
			fos.println("Syntax error: must include the filter number to remove");
			return;
		}
		
		int i = 0;
		try {
			i = Integer.parseInt(arguments.get(0));
		} catch (NumberFormatException e) {
			
			if(arguments.get(0).equals("all")) {
				filterManager.clearFilters();
				return;
			}
			
			fos.println("Syntax error: argument was not a number");
			return;
		}
		
		if(i >= filterManager.filters.size())
			fos.println("Error: rule number does not exist");
		else
			filterManager.removeFilter(i);
	}
}
