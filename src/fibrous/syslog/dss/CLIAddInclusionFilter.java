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
		
		this.commandDescription = "Adds a host/message/facility inclusion filter.  Usage: inc [host | msg | fac] \"phrase/facility # to match\" \"OR another phrase/facility # to match\" ...";
	}

	@Override
	public void execute() {
		if(arguments.size() < 1) {
			ios.println("Syntax error: must include type (host, msg, or fac)");
			return;
		}
		
		InclusionFilter filter = null;
		if(arguments.get(0).equals("host")) {
			filter = new InclusionFilter(FilterDiscriminant.HOSTNAME);
		} else if(arguments.get(0).equals("msg")) {
			filter = new InclusionFilter(FilterDiscriminant.MESSAGE);
		} else if(arguments.get(0).equals("fac")) {
			filter = new InclusionFilter(FilterDiscriminant.FACILITY);
		} else {
			ios.println("Syntax error: must include valid type (host, msg, or fac)");
			return;
		}
		
		if(arguments.size() < 2) {
			ios.println("Syntax error: must include at least one argument to match against");
			return;
		}
		
		if(filter.discriminant == FilterDiscriminant.FACILITY) {
			for(int i = 1; i < arguments.size(); i++) {
				int f = 0;
				try {
					f = Integer.parseInt(arguments.get(i));
				} catch (NumberFormatException e) {
					ios.println("Error: facilities must be a number between 0-23");
					return;
				}
				
				if(f < 0 || f > 23) {
					ios.println("Error: facilities must be a number between 0-23");
					return;
				}
				
				filter.addFacility(f);
			}
		} else {
			for(int i = 1; i < arguments.size(); i++) {
				filter.addSequence(arguments.get(i));
			}
		}
		
		filterManager.addFilter(filter.serialize());
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(filterManager.serializeAllFilters()));
		ios.showTab(1);
	}
}
