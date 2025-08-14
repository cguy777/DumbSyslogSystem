package fibrous.syslog.dss;

public class SyslogUtils {
	/**
	 * Verifies if a String is a valid hostname.
	 * Valid in this case means that characters are only ascii 0-9, aA-zZ, ".", "-", and "_"
	 * @return
	 */
	public static boolean isValidHostName(String hostName) {
		for(int i = 0; i < hostName.length(); i++) {
			char c = hostName.charAt(i);
			//Numbers
			if(c >= '0' && c <= '9')
				continue;
			//Upper case letters
			if(c >= 'A' && c <= 'Z')
				continue;
			//Lower case letters
			if(c >= 'a' && c <= 'z')
				continue;
			
			//Dot, hyphen, and underscore, and the default of disallowed character
			switch(c) {
			case '.':
			case '-':
			case '_':
				continue;
				
			default:
				return false;
			}
		}
		
		return true;
	}
	
	public static String getFileFriendlyTimestamp(String timestamp) {
		String friendlyTimestamp = timestamp.replace(':', '-');
		return friendlyTimestamp;
	}
}
