package fibrous.syslog.dss;

import fibrous.ficli.FiInputStream;
import fibrous.ficli.FiOutputStream;

public class GUIIOStream implements FiInputStream, FiOutputStream {
	
	FiGUIConsole console;
	
	public GUIIOStream(FiGUIConsole console) {
		this.console = console;
	}

	@Override
	public void print(String s) {
		console.printToConsole(s);
	}

	@Override
	public void println(String s) {
		console.printLineToConsole(s);
	}

	@Override
	public String readLine() {
		String line = console.getConsoleInputText();
		
		if(line.length() != 0)
			console.printLineToConsole(line);
		
		console.clearConsoleInput();
		
		return line;
	}
	
	public void clearConsole() {
		console.clearConsole();
	}

	public void printToFilterEditor(String text) {
		console.filterEditor.append(text);
	}
	
	public void printLineToFilterEditor(String text) {
		console.filterEditor.append(text + "\n");
	}
	
	public void clearFilterEditor() {
		console.filterEditor.setText("");
	}
	
	public void clearLogs() {
		console.logOutput.setText("");
	}
	
	public void showTab(int tabIndex) {
		if(tabIndex < 0 || tabIndex > console.tabbedPane.getTabRunCount())
			return;
		
		console.tabbedPane.setSelectedIndex(tabIndex);
	}
}
