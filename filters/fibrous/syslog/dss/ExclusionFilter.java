package fibrous.syslog.dss;

import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;

public class ExclusionFilter extends SyslogFilter {
	String msgSequence;
	
	private ExclusionFilter() {
		
	}
	
	@Override
	public boolean evaluateMessage(BSDSyslogMessage message) {		
		return !message.message.contains(msgSequence);
	}
	
	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("Exclusion");
		
		if(disabled)
			s_filter.add(new SoffitField("disabled"));
		
		s_filter.add(new SoffitField("sequence", msgSequence));
		
		return s_filter;
	}

	public static ExclusionFilter deserialize(SoffitObject s_filter) {
		ExclusionFilter filter = new ExclusionFilter();
		
		if(s_filter.hasField("disabled"))
			filter.disabled = true;
		
		filter.msgSequence = s_filter.getField("sequence").getValue();
		
		return filter;
	}
}
