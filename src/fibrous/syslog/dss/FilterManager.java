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

import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitUtil;

public class FilterManager {
	ArrayList<SyslogFilter> filters;
	SoffitObject s_filters;
	
	public FilterManager() {
		filters = new ArrayList<>();
		s_filters = new SoffitObject("Filters");
	}
	
	public boolean evaluateMessage(BSDSyslogMessage message) {
		for(int i = 0; i < filters.size(); i++) {
			//Check if disabled
			if(filters.get(i).disabled)
				continue;
			
			if(!filters.get(i).evaluateMessage(message))
				return false;
		}
		
		return true;
	}
	
	public void addFilter(SoffitObject s_filter) {
		s_filters.add(s_filter);
	}
	
	public void removeFilter(int index) {
		s_filters.objects.remove(index);
	}
	
	public void clearFilters() {
		s_filters.removeAllObjects();
	}
	
	public void setFilterEnabled(int configuredFilterIndex, boolean enabled) {
		if(enabled) {
			if(s_filters.objects.get(configuredFilterIndex).hasField("disabled"))
				s_filters.objects.get(configuredFilterIndex).removeField("disabled");
		} else {
			if(!s_filters.objects.get(configuredFilterIndex).hasField("disabled"))
				s_filters.objects.get(configuredFilterIndex).add(new SoffitField("disabled"));
		}
	}
	
	public void applyFilters() throws SoffitException {
		filters.clear();
		for(int i = 0; i < s_filters.objects.size(); i++) {
			filters.add(SyslogFilter.deserialize(s_filters.objects.get(i)));
		}
	}
	
	public SoffitObject serializeAllFilters() {
		SoffitObject s_allFilters = new SoffitObject("Filters");
		
		for(int i = 0; i < s_filters.objects.size(); i++) {
			//Performing a deserialize/serialize cycle to make a full copy of the filter SoffitObject.
			s_allFilters.add(SyslogFilter.deserialize(s_filters.objects.get(i)).serialize());
			s_allFilters.objects.get(i).add(new SoffitField("number", String.valueOf(i)), 0);
		}
		
		return s_allFilters;
	}
	
	public SoffitObject serializeActiveFilters() {
		SoffitObject s_filters = new SoffitObject("Filters");
		
		for(int i = 0; i < filters.size(); i++) {
			SoffitObject s_filter = filters.get(i).serialize();
			//This number field that is inserted has no bearing on the logical processing
			//of filters and is only there for labeling purposes for the user
			s_filter.add(new SoffitField("number", String.valueOf(i)), 0);
			s_filters.add(s_filter);
		}
		
		return s_filters;
	}
	
	public void setFiltersFromText(String filtersText) throws SoffitException {
		filters.clear();		
		s_filters = SoffitUtil.ReadStreamFromString(filtersText);
	}
}
