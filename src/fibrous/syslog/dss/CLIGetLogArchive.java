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

import java.io.IOException;

import fibrous.ficli.FiCommand;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitUtil;

public class CLIGetLogArchive extends FiCommand {
	
	FilterManager filterManager;
	DataReceiveHandler receiveHandler;
	GUIIOStream ios;

	public CLIGetLogArchive(String commandString, FilterManager filterManager, DataReceiveHandler receiveHandler, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.receiveHandler = receiveHandler;
		this.ios = ios;
		
		this.commandDescription = "Retrieves all logs from the server.  Usage: get archive [(optional) --filter] [(optional) --consolidate]";
	}

	@Override
	public void execute() {
		
		//Set command switches
		boolean filter = false;
		boolean consolodate = false;
		for(int i = 0; i < arguments.size(); i++) {
			switch(arguments.get(i)) {
			case "--filter":
				filter = true;
				break;
			case "--consolidate":
				consolodate = true;
				break;
			default:
				ios.println("Syntax error: invalid argument");
				return;
			}
		}
		
		receiveHandler.archive_filter = filter;
		receiveHandler.archive_consolodate = consolodate;
		
		SoffitObject s_reqRoot = new SoffitObject("root");
		SoffitObject s_archiveRequest = new SoffitObject("GetArchive");
		s_reqRoot.add(s_archiveRequest);
		
		try {
			SoffitUtil.WriteStream(s_reqRoot, receiveHandler.socket.getOutputStream());
		} catch (IOException e) {
			ios.println(e.getMessage());
			return;
		}
		
		Thread.ofVirtual().start(() -> {
			receiveHandler.receivingArchive = true;
			ios.println("Retrieving log archive...");
			do {
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {}
			} while(receiveHandler.receivingArchive);
			ios.println("Log archive has been downloaded in application directory");
		});
	}
}
