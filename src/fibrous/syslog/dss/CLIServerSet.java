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

import java.net.InetAddress;
import java.net.UnknownHostException;

import fibrous.ficli.FiCommand;

public class CLIServerSet extends FiCommand {
	
	DataReceiveHandler drh;
	GUIIOStream ios;
	DumbSyslogViewer viewer;

	public CLIServerSet(String commandString, DataReceiveHandler drh, GUIIOStream ios, DumbSyslogViewer viewer) {
		super(commandString);
		this.drh = drh;
		this.ios = ios;
		this.viewer = viewer;
		
		this.commandDescription = "Sets the DumbSyslogServer this viewer is pointed towards.  Usage: server set [ip/hostname] [port]";
	}

	@Override
	public void execute() {
		if(arguments.size() < 2) {
			ios.println("Syntax error: you must include both the IP address/hostname and the port of the server");
			return;
		}
		
		try {
			InetAddress serverAddress = InetAddress.getByName(arguments.get(0));
			int serverPort = Integer.parseInt(arguments.get(1));
			
			if(serverPort < 0 || serverPort > 65535) {
				ios.println("Port number must be between 0 and 65535.");
				return;
			}
			
			drh.serverAddress = serverAddress;
			drh.serverInterfacePort = serverPort;
			drh.connect = false;
			drh.resetConnection();
			viewer.writeSettings();
			viewer.configured = true;
			
			ios.println("Configuration changed.  Please call \"connect\" (or just close the program with the big red X button)");
		} catch (UnknownHostException e) {
			ios.println("Error: either an unresolvable hostname or an improperly formatted IP address was provided");
		} catch (NumberFormatException e) {
			ios.println("Error: argument that should be a port number was not a number");
		}
	}
}
