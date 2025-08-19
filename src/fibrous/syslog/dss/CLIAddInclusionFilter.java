package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.ficli.FiCommand;
import fibrous.ficli.FiOutputStream;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitUtil;

public class CLIAddInclusionFilter extends FiCommand {
	
	FilterManager filterManager;
	GUIIOStream ios;

	public CLIAddInclusionFilter(String commandString, FilterManager filterManager, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.ios = ios;
		
		this.commandDescription = "Adds a log host/message inclusion filter.  Usage: inc [host | msg] \"phrase to match\" \"OR another phrase to match with\" ...";
	}

	@Override
	public void execute() {
		if(arguments.size() < 1) {
			ios.println("Syntax error: must include type (host or msg)");
			return;
		}
		
		InclusionFilter filter = null;
		if(arguments.get(0).equals("host")) {
			filter = new InclusionFilter(FilterDiscriminant.HOSTNAME);
		} else if(arguments.get(0).equals("msg")) {
			filter = new InclusionFilter(FilterDiscriminant.MESSAGE);
		} else {
			ios.println("Syntax error: must include type (host or msg)");
			return;
		}
		
		if(arguments.size() < 2) {
			ios.println("Syntax error: must include at least one phrase to match against");
			return;
		}
		
		for(int i = 1; i < arguments.size(); i++) {
			filter.addSequence(arguments.get(i));
		}
		
		filterManager.addFilter(filter.serialize());
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeAllFilters()));
		ios.showTab(1);
	}
}
