package fibrous.syslog.dss;

import fibrous.soffit.SoffitObject;

public interface SyslogFilter {
	public boolean testMessage(BSDSyslogMessage message);
	public SoffitObject serialize();
	
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
		
		return filter;
	}
}