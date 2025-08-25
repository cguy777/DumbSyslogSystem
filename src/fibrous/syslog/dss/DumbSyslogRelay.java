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