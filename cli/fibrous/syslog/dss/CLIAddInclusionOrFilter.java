package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.ficli.FiCommand;
import fibrous.ficli.FiOutputStream;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitUtil;

public class CLIAddInclusionOrFilter extends FiCommand {
	
	FilterManager filterManager;
	GUIIOStream ios;

	public CLIAddInclusionOrFilter(String commandString, FilterManager filterManager, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.ios = ios;
		
		this.commandDescription = "Adds a log message inclusion-or filter.  Usage: inc-or msg seq \"sequence of characters\" \"sequence of characters\" ...";
	}

	@Override
	public void execute() {
		
		if(arguments.size() < 1) {
			ios.println("Syntax error: must include at least one sequence");
			return;
		}
		
		InclusionOrFilter filter = new InclusionOrFilter(FilterDiscriminant.MESSAGE);
		for(int i = 0; i < arguments.size(); i++) {
			filter.addSequence(arguments.get(i));
		}
		
		filterManager.addFilter(filter.serialize());
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeAllFilters()));
		ios.showTab(1);
	}
}
