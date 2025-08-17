package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;

public class ExclusionOrFilter extends SyslogFilter {

	ArrayList<String> sequences;
	
	private ExclusionOrFilter() {
		sequences = new ArrayList<>();
	}
	
	public ExclusionOrFilter(FilterDiscriminant disc) {
		sequences = new ArrayList<>();
		this.discriminant = disc;
	}
	
	public void addSequence(String sequence) {
		sequences.add(sequence);
	}
	
	public boolean removeSequence(String sequence) {
		return sequences.remove(sequence);
	}
	
	public void removeSequence(int index) {
		sequences.remove(index);
	}
	
	@Override
	public boolean evaluateMessage(BSDSyslogMessage message) {
		switch(discriminant) {
		case HOSTNAME:
			for(int i = 0; i < sequences.size(); i++) {
				if(message.hostname.contains(sequences.get(i)))
					return false;
			}
			
			return true;
			
		case MESSAGE:
			for(int i = 0; i < sequences.size(); i++) {
				if(message.message.contains(sequences.get(i)))
					return false;
			}
			
			return true;
			
		default:
			return true;
		}
	}

	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("ExclusionOr");
		
		if(disabled)
			s_filter.add(new SoffitField("disabled"));
		
		String disc = null;
		switch(discriminant) {
		case HOSTNAME: disc = "hostname";
			break;
		case MESSAGE: disc = "message";
			break;
		default:
			disc = "invalid";
		}
		
		if(disc.equals("invalid")) {
			s_filter.add(new SoffitField(disc));
		} else {
			for(int i = 0; i < sequences.size(); i++) {
				s_filter.add(new SoffitField(disc, sequences.get(i)));
			}
		}
		
		return s_filter;
	}
	
	public static ExclusionOrFilter deserialize(SoffitObject s_filter) throws SoffitException {
		ExclusionOrFilter filter = new ExclusionOrFilter();
		
		if(s_filter.hasField("disabled"))
			filter.disabled = true;
		
		filter.discriminant = determineDiscriminant(s_filter);
		ArrayList<SoffitField> seqs = null;
		switch(filter.discriminant) {
		case HOSTNAME:
			seqs = s_filter.getFieldsByName("hostname");
			break;
		case MESSAGE:
			seqs = s_filter.getFieldsByName("message");
			break;
		case INAVLID:
			throw new SoffitException("hostname or message not present in filter");
		}
		
		for(int i = 0; i < seqs.size(); i++) {
			filter.addSequence(seqs.get(i).getValue());
		}
		
		return filter;
	}
}
