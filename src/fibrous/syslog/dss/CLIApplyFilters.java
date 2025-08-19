package fibrous.syslog.dss;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import fibrous.ficli.FiCommand;
import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitUtil;
import fibrous.syslog.dss.DataReceiveHandler;

public class CLIApplyFilters extends FiCommand {
	
	FilterManager filterManager;
	DataReceiveHandler messageHandler;
	GUIIOStream ios;

	public CLIApplyFilters(String commandString, FilterManager filterManager, DataReceiveHandler messageHandler, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.messageHandler = messageHandler;
		this.ios = ios;
		
		this.commandDescription = "Reloads the logs with the current filters applied.  Usage: apply";
	}

	@Override
	public void execute() {
		try {
			filterManager.setFiltersFromText(ios.console.filterEditor.getText());
		} catch (SoffitException e) {
			ios.println("Problem parsing filters:");
			ios.println(e.getMessage());
			return;
		}
		
		try {
			filterManager.applyFilters();
		} catch (SoffitException e) {
			ios.println(e.getMessage());
			return;
		}
		
		SoffitObject s_activeFilters = filterManager.serializeActiveFilters();
		
		ios.clearFilterEditor();
		ios.printToFilterEditor(SoffitUtil.WriteStreamToString(s_activeFilters));
		
		try {
			FileOutputStream fos = new FileOutputStream("ActiveFilters.soffit");
			SoffitUtil.WriteStream(s_activeFilters, fos);
			fos.close();
		} catch (IOException e) {
			ios.print("Filters are applied, but they could not be saved to disk.");
		}
		
		ios.clearLogs();
		messageHandler.resetConnection();
		
		ios.showTab(0);
	}
}
