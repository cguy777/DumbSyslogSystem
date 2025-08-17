package fibrous.syslog.dss;

import java.util.ArrayList;

import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitField;

public class InclusionOrFilter extends SyslogFilter {
	
	String filterName = "";
	ArrayList<String> sequences;
	
	private InclusionOrFilter() {
		sequences = new ArrayList<>();
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
		for(int i = 0; i < sequences.size(); i++) {
			if(message.message.contains(sequences.get(i)))
				return true;
		}
		
		return false;
	}

	@Override
	public SoffitObject serialize() {
		SoffitObject s_filter = new SoffitObject("InclusionOr", filterName);
		
		if(disabled)
			s_filter.add(new SoffitField("disabled"));
		
		for(int i = 0; i < sequences.size(); i++) {
			s_filter.add(new SoffitField("sequence", sequences.get(i)));
		}
		
		return s_filter;
	}
	
	public static InclusionOrFilter deserialize(SoffitObject s_filter) throws SoffitException {
		InclusionOrFilter filter = new InclusionOrFilter();
		filter.filterName = s_filter.getName();
		
		if(s_filter.hasField("disabled"))
			filter.disabled = true;
		
		ArrayList<SoffitField> seqs = s_filter.getFieldsByName("sequence");
		for(int i = 0; i < seqs.size(); i++) {
			filter.sequences.add(seqs.get(i).getValue());
		}
		
		return filter;
	}
}
