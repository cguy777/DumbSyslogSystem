package fibrous.syslog.dss;

import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;

public class ExclusionFilter implements SyslogFilter {
	String sequence;
	
	private ExclusionFilter() {
		
	}
	
	public ExclusionFilter(String sequence) {
		this.sequence = sequence;
	}
	
	@Override
	public boolean evaluateMessage(BSDSyslogMessage message) {
		return !message.message.contains(sequence);
	}
	
	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("Exclusion");
		s_filter.add(new SoffitField("sequence", sequence));
		
		return s_filter;
	}

	public static SyslogFilter deserialize(SoffitObject s_filter) {
		ExclusionFilter filter = new ExclusionFilter();
		filter.sequence = s_filter.getField("sequence").getValue();
		
		return filter;
	}
}
