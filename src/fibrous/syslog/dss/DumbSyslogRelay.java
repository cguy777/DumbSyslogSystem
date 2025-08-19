package fibrous.syslog.dss;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DumbSyslogRelay {
    DatagramSocket inputSocket;
    DatagramSocket outputSocket;

    DatagramPacket inPacket;
    DatagramPacket outPacket;
    InetAddress remoteAddress;
    
    int forwardedPort;

    public DumbSyslogRelay(InetAddress toAddress, int port) throws IOException {
        inputSocket = new DatagramSocket(port);
        outputSocket = new DatagramSocket();
        remoteAddress = toAddress;
        forwardedPort = port;
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
        if(args.length < 1) {
            System.out.println("Usage: [destination IP address/Hostname] [destination port]");
        }

        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt((args[1]));

        DumbSyslogRelay forwarder = new DumbSyslogRelay(address, port);

        while(true) {
            forwarder.processTraffic();
        }
    }
}