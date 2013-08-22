

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MultiInterfaceClient {

	public final static int SERVER_PORT = 57777;
	public final static int CLIENT_PORT = 58888;
	
	public static void main(String[] args) throws Exception {
		
		//Send a packet from 192.168.1.37:57777 to localhost:58888
		DatagramSocket socket = new DatagramSocket( 57777,
				InetAddress.getByName("192.168.1.37"));
		
		byte[] request = "request".getBytes();
		DatagramPacket p = new DatagramPacket(request, request.length);
		p.setAddress(InetAddress.getByName("localhost"));
		p.setPort(58888);
		
		System.out.println("Send request to "+p.getSocketAddress());
		socket.send(p); // Exception
		
		
////		InetAddress destination = InetAddress.getByName("192.168.1.48");
//		InetAddress destination = InetAddress.getByName("localhost");
//		
//		
//		DatagramSocket socket = new DatagramSocket(CLIENT_PORT,InetAddress.getByName("192.168.1.48"));
//		System.out.println("Client created socket "+socket.getLocalSocketAddress());
//		
//		byte[] request = "request".getBytes();
//		DatagramPacket p = new DatagramPacket(request, request.length);
//		p.setAddress(destination);
//		p.setPort(SERVER_PORT);
//		
//		System.out.println("Send request to "+p.getSocketAddress());
//		socket.send(p);
		
		byte[] buf = new byte[10];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		System.out.println("Waiting for response...");
		socket.receive(packet);
		
		System.out.println("Received response from "+packet.getSocketAddress());
		socket.close();
		
	}
	
}
