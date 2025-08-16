package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;

public class FilterManager {
	ArrayList<SyslogFilter> filters;
	
	public FilterManager() {
		filters = new ArrayList<>();
	}
	
	public boolean evaluateMessage(BSDSyslogMessage message) {
		for(int i = 0; i < filters.size(); i++) {
			if(!filters.get(i).testMessage(message))
				return false;
		}
		
		return true;
	}
	
	public void addFilter(SyslogFilter filter) {
		filters.add(filter);
	}
	
	public void removeFilter(SyslogFilter filter) {
		filters.remove(filter);
	}
	
	public void removeFilter(int index) {
		filters.remove(index);
	}
	
	public void clearFilters() {
		filters.clear();
	}
	
	public SoffitObject serializeFilters() {
		SoffitObject s_filters = new SoffitObject("Filters");
		
		for(int i = 0; i < filters.size(); i++) {
			SoffitObject s_filter = filters.get(i).serialize();
			//This number field that is inserted has no bearing on the logical processing
			//of filters and is only there for labeling purposes for the user
			s_filter.fields.add(0, new SoffitField("number", String.valueOf(i)));
			s_filters.add(s_filter);
		}
		
		return s_filters;
	}
}
