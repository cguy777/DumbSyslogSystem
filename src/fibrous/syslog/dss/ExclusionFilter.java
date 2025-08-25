/*
BSD 3-Clause License

Copyright (c) 2025, Noah McLean

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

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
