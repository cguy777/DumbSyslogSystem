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
	
	public void applyFilters() throws SoffitException {
		filters.clear();
		for(int i = 0; i < s_filters.objects.size(); i++) {
			filters.add(SyslogFilter.deserialize(s_filters.objects.get(i)));
		}
	}
	
	public SoffitObject serializeAllFilters() {
		SoffitObject s_allFilters = new SoffitObject("Filters");
		
		for(int i = 0; i < s_filters.objects.size(); i++) {
			//Performing a deserialize/serialize cycle ends up being the easiest way to do a full copy.
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
