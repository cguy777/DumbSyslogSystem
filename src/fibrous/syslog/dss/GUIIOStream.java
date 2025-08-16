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
		console.printToLog(s);
	}

	@Override
	public void println(String s) {
		console.printLineToLog(s);
	}

	@Override
	public String readLine() {
		String line = console.getConsoleInputText();
		
		if(line.length() != 0)
			console.printLineToLog(console.caret.getText()  + line);
		
		console.clearConsoleInput();
		
		return line;
	}

}
