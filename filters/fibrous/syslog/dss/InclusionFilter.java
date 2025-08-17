package fibrous.syslog.dss;

import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitField;

public class InclusionFilter extends SyslogFilter {
	String msgSequence;
	
	private InclusionFilter() {
		
	}

	@Override
	public boolean evaluateMessage(BSDSyslogMessage message) {
		return message.message.contains(msgSequence);
	}

	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("Inclusion");
		
		if(disabled)
			s_filter.add(new SoffitField("disabled"));
		
		s_filter.add(new SoffitField("sequence", msgSequence));
		
		return s_filter;
	}
	
	public static InclusionFilter deserialize(SoffitObject s_filter) throws SoffitException {
		InclusionFilter filter = new InclusionFilter();
		
		if(s_filter.hasField("disabled"))
			filter.disabled = true;
		
		filter.msgSequence = s_filter.getField("sequence").getValue();
		
		return filter;
	}
}
