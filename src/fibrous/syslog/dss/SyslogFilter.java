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
import fibrous.soffit.SoffitObject;

public abstract class SyslogFilter {
	FilterDiscriminant discriminant;
	boolean disabled = false;
	public abstract boolean evaluateMessage(BSDSyslogMessage message);
	public abstract SoffitObject serialize();
	
	public static SyslogFilter deserialize(SoffitObject s_filter) {
		SyslogFilter filter = null;
		//INCLUSION
		if(s_filter.getType().equals("Inclusion")) {
			filter = InclusionFilter.deserialize(s_filter);
		}
		//EXCLUSION
		else if(s_filter.getType().equals("Exclusion")) {
			filter = ExclusionFilter.deserialize(s_filter);
		}
		//SEVERITY
		else if(s_filter.getType().equals("Severity")) {
			filter = SeverityFilter.deserialize(s_filter);
		}
		
		return filter;
	}
	
	public static FilterDiscriminant determineDiscriminant(SoffitObject s_filter) {
		if(s_filter.hasField("message"))
			return FilterDiscriminant.MESSAGE;
		else if(s_filter.hasField("hostname"))
			return FilterDiscriminant.HOSTNAME;
		else if(s_filter.hasField("facility"))
			return FilterDiscriminant.FACILITY;
		else
			return FilterDiscriminant.INAVLID;
	}
}