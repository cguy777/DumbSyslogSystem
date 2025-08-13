package fibrous.syslog.dss;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitUtil;

public class SyslogSubscriber {
	
	Socket socket;
	ArrayList<SyslogSubscriber> subscribers;
	
	public SyslogSubscriber(Socket socket) {
		this.socket = socket;
	}
	
	public void pushMessage(BSDSyslogMessage message) {
		SoffitObject s_message = new SoffitObject("BSDSyslogMessage");
		s_message.add(message.serialize());
		
		try {
			SoffitUtil.WriteStream(s_message, socket.getOutputStream());
		} catch (IOException e) {
			//Remove from the subscriber list if there are any issues
			subscribers.remove(this);
		}
	}
}
