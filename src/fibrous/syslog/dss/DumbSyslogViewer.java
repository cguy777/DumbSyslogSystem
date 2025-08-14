package fibrous.syslog.dss;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import fibrous.soffit.SoffitException;
import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitUtil;

public class DumbSyslogViewer {
	
	InetAddress serverAddress;
	int serverInterfacePort;
	Socket socket;
	
	public DumbSyslogViewer(InetAddress serverAddress, int serverInterfacePort) {
		this.serverAddress = serverAddress;
		this.serverInterfacePort = serverInterfacePort;
		
		try {
			socket = new Socket(serverAddress, serverInterfacePort);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void receiveData() throws SoffitException, IOException {
		SoffitObject s_data = SoffitUtil.ReadStream(socket.getInputStream());
		
		SoffitObject s_encapedObject = s_data.getFirstObject();
		//BSDSyslogMessage
		if(s_encapedObject.getType().equals("BSDSyslogMessage")) {
			BSDSyslogMessage message = BSDSyslogMessage.deserialize(s_encapedObject);
			System.out.println(message.getMessageAsString(true));
		}
	}
	
	public static void main(String[]args) throws SoffitException, IOException {
		DumbSyslogViewer client = new DumbSyslogViewer(InetAddress.getByName("10.0.50.118"), 54321);
		
		while(true) {
			client.receiveData();
		}
	}
}
