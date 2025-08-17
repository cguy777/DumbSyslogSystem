package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.ficli.FiCommand;
import fibrous.ficli.FiOutputStream;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitUtil;

public class CLIAddExclusionOrFilter extends FiCommand {
	
	FilterManager filterManager;
	GUIIOStream ios;

	public CLIAddExclusionOrFilter(String commandString, FilterManager filterManager, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.ios = ios;
		
		this.commandDescription = "Adds a log message exclusion-or filter.  Usage: exc-or msg seq [filter name] \"sequence of characters\" \"sequence of characters\" ...";
	}

	@Override
	public void execute() {
		if(arguments.size() < 1) {
			ios.println("Syntax error: must include a filter name");
			return;
		}
		
		if(arguments.size() < 2) {
			ios.println("Syntax error: must include at least one sequence");
			return;
		}
		
		SoffitObject s_filter = new SoffitObject("ExclusionOr", arguments.get(0));
		for(int i = 1; i < arguments.size(); i++)
			s_filter.add(new SoffitField("sequence", arguments.get(i)));
		
		filterManager.addFilter(s_filter);
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeAllFilters()));
		ios.showTab(1);
	}
}
