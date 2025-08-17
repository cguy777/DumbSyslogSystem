package fibrous.syslog.dss;

import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;

public class ExclusionFilter extends SyslogFilter {
	String sequence;
	
	private ExclusionFilter() {
		
	}
	
	public ExclusionFilter(FilterDiscriminant disc, String sequence) {
		this.discriminant = disc;
		this.sequence = sequence;
	}
	
	@Override
	public boolean evaluateMessage(BSDSyslogMessage syslogMessage) {		
		switch(discriminant) {
		case HOSTNAME:
			return !syslogMessage.hostname.contains(sequence);
		case MESSAGE:
			return !syslogMessage.message.contains(sequence);
		default:
			return true;
		}
	}
	
	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("Exclusion");
		
		if(disabled)
			s_filter.add(new SoffitField("disabled"));
		
		switch(discriminant) {
		case HOSTNAME:
			s_filter.add(new SoffitField("hostname", sequence));
			break;
		case MESSAGE:
			s_filter.add(new SoffitField("message", sequence));
			break;
		default:
			s_filter.add(new SoffitField("invalid"));
		}
		
		return s_filter;
	}

	public static ExclusionFilter deserialize(SoffitObject s_filter) {
		ExclusionFilter filter = new ExclusionFilter();
		
		if(s_filter.hasField("disabled"))
			filter.disabled = true;
		
		filter.discriminant = determineDiscriminant(s_filter);
		switch(filter.discriminant) {
		case HOSTNAME:
			filter.sequence = s_filter.getField("hostname").getValue();
			break;
		case MESSAGE:
			filter.sequence = s_filter.getField("message").getValue();
			break;
		case INAVLID:
			throw new SoffitException("hostname, message, or facility not present in filter");
		}
		
		return filter;
	}
}
