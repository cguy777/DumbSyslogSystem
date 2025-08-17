package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.ficli.FiCommand;
import fibrous.ficli.FiOutputStream;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitUtil;

public class CLIAddExclusionFilter extends FiCommand {
	
	FilterManager filterManager;
	GUIIOStream ios;

	public CLIAddExclusionFilter(String commandString, FilterManager filterManager, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.ios = ios;
		
		this.commandDescription = "Adds a log message exclusion filter.  Usage: exc msg seq \"sequence of characters in quotations or a single word without\"";
	}

	@Override
	public void execute() {
		if(arguments.size() < 1) {
			ios.println("Syntax error: must include a sequence to exclude");
			return;
		}
		
		String sequence = arguments.get(0);
		
		SoffitObject s_filter = new SoffitObject("Exclusion");
		s_filter.add(new SoffitField("sequence", sequence));
		
		filterManager.addFilter(s_filter);
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeAllFilters()));
		ios.showTab(1);
	}
}
