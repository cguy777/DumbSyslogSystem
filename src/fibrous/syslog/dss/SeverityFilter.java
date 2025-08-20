package fibrous.syslog.dss;

import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;

/**
 * Filter that mandates a minimum severity level.
 * In the BSD standard, values range from 0 - 7.
 * 7 being least severe and 0 being most severe.
 */
public class SeverityFilter extends SyslogFilter {
	
	int severity;
	
	private SeverityFilter() {
		
	}
	
	public SeverityFilter(int severity) {
		this.severity = severity;
	}

	@Override
	public boolean evaluateMessage(BSDSyslogMessage message) {
		return message.getSeverity() <= severity;
	}

	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("Severity");
		
		if(disabled)
			s_filter.add(new SoffitField("disabled"));
		
		s_filter.add(new SoffitField("minimum", String.valueOf(severity)));
		
		return s_filter;
	}
	
	public static SeverityFilter deserialize(SoffitObject s_filter) throws SoffitException {
		SeverityFilter filter = new SeverityFilter();
		
		if(s_filter.hasField("disabled"))
			filter.disabled = true;
		
		try {
			filter.severity = Integer.parseInt(s_filter.getField("minimum").getValue());
		} catch (NumberFormatException e) {
			throw new SoffitException("Error: field \"mimimum\" in a severity filter must be a number between 0 and 7");
		}
		
		if(filter.severity < 0 || filter.severity > 7)
			throw new SoffitException("Error: field \"mimimum\" in a severity filter must be a number between 0 and 7");
		
		return filter;
	}
}
