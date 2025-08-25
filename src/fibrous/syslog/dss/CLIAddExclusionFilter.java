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

import java.util.ArrayList;

import fibrous.ficli.FiCommand;
import fibrous.ficli.FiOutputStream;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitUtil;

public class CLIAddExclusionFilter extends FiCommand {
	
	FilterManager filterManager;
	GUIIOStream ios;

	public CLIAddExclusionFilter(String commandString, FilterManager filterManager, GUIIOStream ios) {
		super(commandString);
		this.filterManager = filterManager;
		this.ios = ios;
		
		this.commandDescription = "Adds a host/message/facility exclusion filter.  Usage: inc [host | msg | fac] \"phrase or facility number to match\" \"OR another phrase or facility number to match\" ...";
	}

	@Override
	public void execute() {
		if(arguments.size() < 1) {
			ios.println("Syntax error: must include type (host, msg, or fac)");
			return;
		}
		
		ExclusionFilter filter = null;
		if(arguments.get(0).equals("host")) {
			filter = new ExclusionFilter(FilterDiscriminant.HOSTNAME);
		} else if(arguments.get(0).equals("msg")) {
			filter = new ExclusionFilter(FilterDiscriminant.MESSAGE);
		} else if(arguments.get(0).equals("fac")) {
			filter = new ExclusionFilter(FilterDiscriminant.FACILITY);
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
