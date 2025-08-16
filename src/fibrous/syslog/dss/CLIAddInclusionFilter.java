package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.ficli.FiCommand;
import fibrous.ficli.FiOutputStream;

public class CLIAddInclusionFilter extends FiCommand {
	
	FilterManager filterManager;
	FiOutputStream fos;

	public CLIAddInclusionFilter(String commandString, FilterManager filterManager, FiOutputStream fos) {
		super(commandString);
		this.filterManager = filterManager;
		this.fos = fos;
		
		this.commandDescription = "Adds a log message inclusion filter.  Usage: inc msg seq \"sequence of characters in quotations or a single word without\"";
	}

	@Override
	public void execute() {
		if(arguments.size() < 1)
			fos.println("Syntax error: must include a sequence to include");
		
		String sequence = arguments.get(0);
		filterManager.addFilter(new InclusionFilter(sequence));
	}
}
