/*
BSD 3-Clause License

Copyright (c) 2025, Noah McLean

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

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
