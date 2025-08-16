package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.ficli.FiCommand;
import fibrous.ficli.FiOutputStream;
import fibrous.soffit.SoffitUtil;

public class CLIAddInclusionFilter extends FiCommand {
	
	FilterManager filterManager;
	GUIIOStream ios;

	public CLIAddInclusionFilter(String commandString, FilterManager filterManager, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.ios = ios;
		
		this.commandDescription = "Adds a log message inclusion filter.  Usage: inc msg seq \"sequence of characters in quotations or a single word without\"";
	}

	@Override
	public void execute() {
		if(arguments.size() < 1)
			ios.println("Syntax error: must include a sequence to include");
		
		String sequence = arguments.get(0);
		filterManager.addFilter(new InclusionFilter(sequence));
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeFilters()));
		ios.showTab(1);
	}
}
