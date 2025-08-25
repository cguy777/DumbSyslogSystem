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

import java.net.InetAddress;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitField;

public class BSDSyslogMessage {
	public int pri;
	/**
	 * The timestamp that was reported by the device that sent the message
	 */
	public String originalTimestamp;
	/**
	 * A generated timestamp that is based on when the message was actually received
	 */
	public String receivedTimestamp;
	public String hostname;
	public String message;

	public BSDSyslogMessage() {
		
	}
	
	public void setPRI(int facility, int severity) {
		pri = facility * 8;
		pri += severity;
	}
	
	public int getFacility() {
		return (int) ((float) pri / (float) 8);
	}
	
	public int getSeverity() {
		return (int) ((float) pri % (float) 8);
	}
	
	//Fills the value of parsedPRI with the pri.  Returns the position in the byte array immediately after the pri section
	private static int parsePri(byte[] bytes, EncapsulatedInt parsedPRI) {
		int startPos = 0;
		int endPos = 0;
		String priS = "";
		
		//Find left bracket
		for(int i = 0; i < bytes.length; i++) {
			if(bytes[i] == (int) '<') {
				startPos = i + 1;
				break;
			}
		}
		
		//Find right bracket
		for(int i = 0; i < bytes.length; i++) {
			if(bytes[i] == (int) '>') {
				endPos = i - 1;
				break;
			}
		}
		
		//Combine bytes into string
		for(int i = startPos; i <= endPos; i++) {
			priS += (char) bytes[i];
		}
		
		parsedPRI.integer = Integer.parseInt(priS);
		//Account for right bracket
		return endPos + 2;
	}
	
	private static boolean isMonth(byte[] bytes, int startPoint) {
        if(startPoint + 3 > bytes.length)
            return false;

        String month = "";
        month += (char) bytes[startPoint];
        month += (char) bytes[startPoint + 1];
        month += (char) bytes[startPoint + 2];
        switch(month) {
            //Fall through down to true;
            case "Jan":
            case "Feb":
            case "Mar":
            case "Apr":
            case "May":
            case "Jun":
            case "Jul":
            case "Aug":
            case "Sep":
            case "Oct":
            case "Nov":
            case "Dec":
                return true;

            default:
                return false;
        }
    }
	
	//Returns the location in the byte array that is the beginning of the timestamp.
	//a returned -1 means no timestamp was found.
	private static int findTimestampStart(byte[] bytes, int startPos) {
		for(int i = startPos; i < bytes.length; i++) {
			if(isMonth(bytes, i)) {
				return i;
			}
		}
		
		return -1;
	}
	
	private static String generateTimestamp() {
		Instant instant = Instant.now();
		ZonedDateTime currentDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
		
		String generatedTimeStamp = getMonthFromNumber(currentDateTime.getMonthValue()) + " " +
				conditionalDayPad(currentDateTime.getDayOfMonth()) + " " +
				formatTime(currentDateTime);
		
		return generatedTimeStamp;
	}
	
	/**
	 * 
	 * @param bytes
	 * @param startPos
	 * @param timestamp
	 * @return the position immediately after the timestamp, or -1 if there was an error.
	 */
	private static int parseTimestamp(byte[] bytes, int startPos, EncapsulatedString timestamp, String receivedTimestamp) {
		String month = "";
		String day = "";
		String time = "";
		
		int pos = startPos;
		
		//fill month
		if(pos + 3 >= bytes.length) {
			timestamp.string = receivedTimestamp;
			return -1;
		}
			
		for(int i = 0; i < 3; i++) {
			month += (char) bytes[pos + i];
		}
		pos += 3;
		
		//fill day
		int dayStart = 0;
		int dayEnd = 0;
		//Find start
		for(int i = pos; i < bytes.length; i++) {
			if(bytes[i] != (int) ' ') {
				dayStart = i;
				break;
			}
		}
		//Find end
		for(int i = dayStart; i < bytes.length; i++) {
			if(bytes[i] == (int) ' ') {
				dayEnd = i - 1;
				break;
			}
		}
		
		//Check for errors
		if(dayStart == 0 || dayEnd == 0) {
			timestamp.string = receivedTimestamp;
			return -1;
		}
		
		for(int i = dayStart; i <= dayEnd; i++) {
			day += (char) bytes[i];
		}
		pos = dayEnd + 1;
		
		//Fill time
		int timeStart = 0;
		int timeEnd = 0;
		//Find start
		for(int i = pos; i < bytes.length; i++) {
			if(bytes[i] != (int) ' ') {
				timeStart = i;
				break;
			}
		}
		//Find end
		for(int i = timeStart; i < bytes.length; i++) {
			if(bytes[i] == (int) ' ') {
				timeEnd = i - 1;
				break;
			}
		}
		
		//Check for errors
		if(timeStart == 0 || timeEnd == 0) {
			timestamp.string = receivedTimestamp;
			return -1;
		}
		
		for(int i = timeStart; i <= timeEnd; i++) {
			time += (char) bytes[i];
		}
		timestamp.string = month + " " + day + " " + time;
		
		//Takes care of Cisco devices appending ":" to the timestamp
		if(timestamp.string.charAt(timestamp.string.length() - 1) == ':') {
			timestamp.string = timestamp.string.substring(0, timestamp.string.length() - 1);
		}
		
		//Return the final timestamp position, plus 1.
		return timeEnd + 1;
	}
	
	private static int parseWhitespacedToken(byte[] bytes, int startPos, EncapsulatedString returnedToken) {
		String token = "";
		
		//Fill time
		int tokenStart = 0;
		int tokenEnd = 0;
		//Find start
		for(int i = startPos; i < bytes.length; i++) {
			if(bytes[i] != (int) ' ') {
				tokenStart = i;
				break;
			}
		}
		//Find end
		for(int i = tokenStart; i < bytes.length; i++) {
			if(bytes[i] == (int) ' ') {
				tokenEnd = i - 1;
				break;
			}
			
			//Break of end of byte array as well.
			if(i == bytes.length - 1) {
				tokenEnd = 1;
				break;
			}
		}
		
		if(tokenStart == 0 || tokenEnd == 0) {
			returnedToken.string = token;
			return -1;
		}
		
		for(int i = tokenStart; i <= tokenEnd; i++) {
			token += (char) bytes[i];
		}
		
		returnedToken.string = token;
		return tokenEnd + 1;
	}
	
	private static int parseHostname(byte[] bytes, int startPos, InetAddress remoteAddress, EncapsulatedString returnedHostname) {
		EncapsulatedString hostname = new EncapsulatedString();
		
		int pos = parseWhitespacedToken(bytes, startPos, hostname);
		if(pos == -1) {
			returnedHostname.string = remoteAddress.getHostAddress();
			return -1;
		}
		
		if(!SyslogUtils.isValidHostName(hostname.string)) {
			returnedHostname.string = remoteAddress.getHostAddress();
			return -1;
		}
			
		returnedHostname.string = hostname.string;
		return pos;
	}
	
	private static String getMonthFromNumber(int number) {
		switch(number) {
		case 1: return "Jan";
		case 2: return "Feb";
		case 3: return "Mar";
		case 4: return "Apr";
		case 5: return "May";
		case 6: return "Jun";
		case 7: return "Jul";
		case 8: return "Aug";
		case 9: return "Sep";
		case 10: return "Oct";
		case 11: return "Nov";
		case 12: return "Dec";
		
		default: return "";
		}
	}
	
	private static String conditionalDayPad(int day) {
		if(day < 10)
			return "0" + day;
		else
			return String.valueOf(day);
	}
	
	private static String formatTime(ZonedDateTime zdt) {
		String hour = "";
		String minute = "";
		String second = "";
		
		if(zdt.getHour() < 10)
			hour = "0" + zdt.getHour();
		else
			hour = String.valueOf(zdt.getHour());
		
		if(zdt.getMinute() < 10)
			minute = "0" + zdt.getMinute();
		else
			minute = String.valueOf(zdt.getMinute());
		
		if(zdt.getSecond() < 10)
			second = "0" + zdt.getSecond();
		else
			second = String.valueOf(zdt.getSecond());
		
		return hour + ":" + minute + ":" + second;
	}
	
	public static BSDSyslogMessage parseMessage(byte[] bytes, InetAddress remoteAddress) {
		BSDSyslogMessage bsdMessage = new BSDSyslogMessage();
		
		int bytePos = 0;
		
		//PRI
		EncapsulatedInt parsedPRI = new EncapsulatedInt();
		bytePos = parsePri(bytes, parsedPRI);
		bsdMessage.pri = parsedPRI.integer;
		
		//Timestamp
		//Start with the generated one first
		bsdMessage.receivedTimestamp = generateTimestamp();
		boolean hasTimestamp = false;
		int timestampStart = findTimestampStart(bytes, bytePos);
		if(timestampStart != -1) {
			bytePos = timestampStart;
			hasTimestamp = true;
		}
		EncapsulatedString timestamp = new EncapsulatedString();
		if(hasTimestamp) {
			
			int afterTimePosition = parseTimestamp(bytes, timestampStart, timestamp, bsdMessage.receivedTimestamp);
			if(afterTimePosition != -1)
				bytePos = afterTimePosition;
			
			bsdMessage.originalTimestamp = timestamp.string;
		} else {
			//Use the time of reception in lieu of a self-reported timestamp
			bsdMessage.originalTimestamp = bsdMessage.receivedTimestamp;
		}
		
		//Hostname
		EncapsulatedString hostname = new EncapsulatedString();
		int hostnamePos = parseHostname(bytes, bytePos, remoteAddress, hostname);
		bsdMessage.hostname = hostname.string;
		if(hostnamePos != -1)
			bytePos = hostnamePos;
		
		//Message
		String message = "";
		for(int i = bytePos; i < bytes.length; i++) {
			message += (char) bytes[i];
		}
		bsdMessage.message = message.strip();
		
		return bsdMessage;
	}
	
	public String getMessageAsFormattedString(boolean useOriginalTimestamp) {
		if(useOriginalTimestamp) {
			return "<" +
				pri +
				">" +
				originalTimestamp + " " +
				hostname + " " +
				message;
		} else {
			return "<" +
					pri +
					">" +
					receivedTimestamp + " " +
					hostname + " " +
					message;
		}
	}
	
	public SoffitObject serialize() {
		SoffitObject s_message = new SoffitObject("BSDSyslogMessage");
		s_message.add(new SoffitField("PRI", String.valueOf(pri)));
		s_message.add(new SoffitField("OriginalTimestamp", originalTimestamp));
		s_message.add(new SoffitField("ReceivedTimestamp", receivedTimestamp));
		s_message.add(new SoffitField("Hostname", hostname));
		s_message.add(new SoffitField("Message", message));
		
		return s_message;
	}
	
	public static BSDSyslogMessage deserialize(SoffitObject s_message) {
		BSDSyslogMessage message = new BSDSyslogMessage();
		
		message.pri = Integer.parseInt(s_message.getField("PRI").getValue());
		message.originalTimestamp = s_message.getField("OriginalTimestamp").getValue();
		message.receivedTimestamp = s_message.getField("ReceivedTimestamp").getValue();
		message.hostname = s_message.getField("Hostname").getValue();
		message.message = s_message.getField("Message").getValue();
		
		return message;
	}
}

class EncapsulatedString {
	public String string = "";
}

class EncapsulatedInt {
	public int integer;
}