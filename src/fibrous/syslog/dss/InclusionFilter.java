package fibrous.syslog.dss;

import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitField;

public class InclusionFilter implements SyslogFilter {
	String sequence;
	
	private InclusionFilter() {
		
	}
	
	public InclusionFilter(String sequence) {
		this.sequence = sequence;
	}

	@Override
	public boolean testMessage(BSDSyslogMessage message) {
		return message.message.contains(sequence);
	}

	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("Inclusion");
		s_filter.add(new SoffitField("sequence", sequence));
		
		return s_filter;
	}
	
	public static InclusionFilter deserialize(SoffitObject s_filter) throws SoffitException {
		InclusionFilter filter = new InclusionFilter();
		filter.sequence = s_filter.getField("sequence").getValue();
		
		return filter;
	}
}
