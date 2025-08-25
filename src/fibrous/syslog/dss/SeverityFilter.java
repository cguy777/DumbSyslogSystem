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

import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;

/**
 * Filter that mandates a minimum severity level.
 * In the BSD standard, values range from 0 - 7.
 * 7 being least severe and 0 being most severe.
 */
public class SeverityFilter extends SyslogFilter {
	
	int severity;
	
	private SeverityFilter() {
		
	}
	
	public SeverityFilter(int severity) {
		this.severity = severity;
	}

	@Override
	public boolean evaluateMessage(BSDSyslogMessage message) {
		return message.getSeverity() <= severity;
	}

	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("Severity");
		
		if(disabled)
			s_filter.add(new SoffitField("disabled"));
		
		s_filter.add(new SoffitField("minimum", String.valueOf(severity)));
		
		return s_filter;
	}
	
	public static SeverityFilter deserialize(SoffitObject s_filter) throws SoffitException {
		SeverityFilter filter = new SeverityFilter();
		
		if(s_filter.hasField("disabled"))
			filter.disabled = true;
		
		try {
			filter.severity = Integer.parseInt(s_filter.getField("minimum").getValue());
		} catch (NumberFormatException e) {
			throw new SoffitException("Error: field \"mimimum\" in a severity filter must be a number between 0 and 7");
		}
		
		if(filter.severity < 0 || filter.severity > 7)
			throw new SoffitException("Error: field \"mimimum\" in a severity filter must be a number between 0 and 7");
		
		return filter;
	}
}
