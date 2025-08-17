package fibrous.syslog.dss;

import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;

public class ExclusionFilter extends SyslogFilter {
	String sequence;
	
	private ExclusionFilter() {
		
	}
	
	@Override
	public boolean evaluateMessage(BSDSyslogMessage message) {		
		return !message.message.contains(sequence);
	}
	
	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("Exclusion");
		
		if(s_filter.hasField("disabled"))
			disabled = true;
		
		s_filter.add(new SoffitField("sequence", sequence));
		
		return s_filter;
	}

	public static SyslogFilter deserialize(SoffitObject s_filter) {
		ExclusionFilter filter = new ExclusionFilter();
		
		if(s_filter.hasField("diabled"))
			filter.disabled = true;
		
		filter.sequence = s_filter.getField("sequence").getValue();
		
		return filter;
	}
}
