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
