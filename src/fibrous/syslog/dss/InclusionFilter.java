package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitField;

public class InclusionFilter extends SyslogFilter {
	
	ArrayList<String> sequences;
	
	private InclusionFilter() {
		sequences = new ArrayList<>();
	}
	
	public InclusionFilter(FilterDiscriminant disc) {
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
					return true;
			}
			
			return false;
			
		case MESSAGE:
			for(int i = 0; i < sequences.size(); i++) {
				if(message.message.contains(sequences.get(i)))
					return true;
			}
			
			return false;
			
		default:
			return true;
		}
	}

	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("Inclusion");
		
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
	
	public static InclusionFilter deserialize(SoffitObject s_filter) throws SoffitException {
		InclusionFilter filter = new InclusionFilter();
		
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
