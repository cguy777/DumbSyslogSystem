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
		//INCLUSION-OR
		else if(s_filter.getType().equals("InclusionOr")) {
			filter = InclusionOrFilter.deserialize(s_filter);
		}
		//EXCLUSION
		else if(s_filter.getType().equals("Exclusion")) {
			filter = ExclusionFilter.deserialize(s_filter);
		}
		//EXCLUSION-OR
		else if(s_filter.getType().equals("ExclusionOr")) {
			filter = ExclusionOrFilter.deserialize(s_filter);
		}
		
		return filter;
	}
	
	public static FilterDiscriminant determineDiscriminant(SoffitObject s_filter) {
		if(s_filter.hasField("message"))
			return FilterDiscriminant.MESSAGE;
		else if(s_filter.hasField("hostname"))
			return FilterDiscriminant.HOSTNAME;
		else
			return FilterDiscriminant.INAVLID;
	}
}