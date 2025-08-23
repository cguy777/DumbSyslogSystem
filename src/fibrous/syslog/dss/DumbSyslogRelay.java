package fibrous.syslog.dss;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import fibrous.soffit.SoffitObject;
import fibrous.soffit.SoffitUtil;

public class DumbSyslogRelay {
    DatagramSocket inputSocket;
    DatagramSocket outputSocket;

    DatagramPacket inPacket;
    DatagramPacket outPacket;
    InetAddress remoteAddress;
    
    int forwardedPort;

    public DumbSyslogRelay() throws IOException {
    	
    	FileInputStream fis = new FileInputStream("config_relay");
    	SoffitObject s_config = SoffitUtil.ReadStream(fis);
    	fis.close();
    	
    	if(s_config.hasField("SyslogPort"))
    		forwardedPort = Integer.parseInt(s_config.getField("SyslogPort").getValue());
    	else {
    		System.err.println("\"SyslogPort\" not found in config");
    		System.exit(-1);
    	}
    	
    	if(s_config.hasField("ServerAddress"))
    		remoteAddress = InetAddress.getByName(s_config.getField("ServerAddress").getValue());
    	else {
    		System.err.println("\"ServerAddress\" not found in config");
    		System.exit(-1);
    	}
    	
        inputSocket = new DatagramSocket(forwardedPort);
        outputSocket = new DatagramSocket();
    }

    public void processTraffic() throws IOException {
        byte[] buffer = new byte[65536];
        inPacket = new DatagramPacket(buffer, buffer.length);
        inputSocket.receive(inPacket);
        
        byte[] trimmedBuffer = new byte[inPacket.getLength()];
        System.arraycopy(inPacket.getData(), inPacket.getOffset(), trimmedBuffer, 0, inPacket.getLength());
        
        BSDSyslogMessage message = BSDSyslogMessage.parseMessage(trimmedBuffer, inPacket.getAddress());
        trimmedBuffer = message.getMessageAsFormattedString(true).getBytes();
        
        outPacket = new DatagramPacket(trimmedBuffer, trimmedBuffer.length);
        outPacket.setAddress(remoteAddress);
        outPacket.setPort(forwardedPort);
        outputSocket.send(outPacket);
    }

    public static void main(String[]args) throws IOException {
    	DumbSyslogRelay forwarder = null;
    	try {
    		forwarder = new DumbSyslogRelay();
    		System.out.println("DumbSyslogRelay has started");
    		System.out.println("Listening on port " + forwarder.forwardedPort + " And forwarding to " + forwarder.remoteAddress.getHostAddress());
    	} catch (IOException e) {
    		System.err.println("Error loading config or starting DumbSyslogRelay: " + e.getMessage());
    		e.printStackTrace();
    		System.exit(-1);
    	}

        while(true) {
            forwarder.processTraffic();
        }
    }
}