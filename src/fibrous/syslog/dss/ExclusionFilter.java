package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitField;
import fibrous.soffit.SoffitObject;

public class ExclusionFilter extends SyslogFilter {

	ArrayList<String> sequences;
	ArrayList<Integer> facilities;
	
	private ExclusionFilter() {
		sequences = new ArrayList<>();
		facilities = new ArrayList<>();
	}
	
	public ExclusionFilter(FilterDiscriminant disc) {
		sequences = new ArrayList<>();
		facilities = new ArrayList<>();
		this.discriminant = disc;
	}
	
	public void addSequence(String sequence) {
		sequences.add(sequence);
	}
	
	public void addFacility(int facility) {
		facilities.add(facility);
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
			
		case FACILITY:
			for(int i = 0; i < facilities.size(); i++) {
				if(message.getFacility() == facilities.get(i))
					return false;
			}
			
			return true;
			
		default:
			return true;
		}
	}

	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("Exclusion");
		
		if(disabled)
			s_filter.add(new SoffitField("disabled"));
		
		String disc = null;
		switch(discriminant) {
		case HOSTNAME: disc = "hostname";
			break;
		case MESSAGE: disc = "message";
			break;
		case FACILITY: disc = "facility";
			break;
		default:
			disc = "invalid";
		}
		
		if(disc.equals("invalid")) {
			s_filter.add(new SoffitField(disc));
		} else {
			if(discriminant == FilterDiscriminant.FACILITY) {
				for(int i = 0; i < facilities.size(); i++)
					s_filter.add(new SoffitField(disc, String.valueOf(facilities.get(i))));
			} else {
				for(int i = 0; i < sequences.size(); i++) {
					s_filter.add(new SoffitField(disc, sequences.get(i)));
				}
			}
		}
		
		return s_filter;
	}
	
	public static ExclusionFilter deserialize(SoffitObject s_filter) throws SoffitException {
		ExclusionFilter filter = new ExclusionFilter();
		
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
		case FACILITY:
			seqs = s_filter.getFieldsByName("facility");
			break;
		case INAVLID:
			throw new SoffitException("neither hostname, message, nor facility present in filter");
		}
		
		for(int i = 0; i < seqs.size(); i++) {
			//Some extra validity tests for facility numbers
			if(filter.discriminant == FilterDiscriminant.FACILITY) {
				int f = 0;
				try {
					f = Integer.parseInt(seqs.get(i).getValue());
				} catch (NumberFormatException e) {
					throw new SoffitException("\"facility\" field must be a number between 0-23");
				}
				
				if(f < 0 || f > 23)
					throw new SoffitException("\"facility\" field must be a number between 0-23");
				
				filter.addFacility(Integer.parseInt(seqs.get(i).getValue()));
			} else {
				filter.addSequence(seqs.get(i).getValue());
			}
		}
		
		return filter;
	}
}
