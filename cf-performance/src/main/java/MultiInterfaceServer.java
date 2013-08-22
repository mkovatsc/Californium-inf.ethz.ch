

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;

public class MultiInterfaceServer implements Runnable {

	public final static int SERVER_PORT = 57777;
	
	private DatagramSocket socket;
	
	public MultiInterfaceServer() throws Exception {
		InetAddress addr = InetAddress.getByName("192.168.1.37");
//		addr = InetAddress.getLoopbackAddress();
		this.socket = new DatagramSocket(SERVER_PORT, addr);
	}
	
	public void run() {
		// Should be listening on 0.0.0.0/0.0.0.0:57777
		System.out.println("Server listening on "+socket.getLocalSocketAddress());
		
		try {
			byte[] buf = new byte[10];
			DatagramPacket p = new DatagramPacket(buf, buf.length);
			
			while (true) {
				socket.receive(p);
				
				System.out.println("Received packet from "+p.getSocketAddress()+" and respond to it");
				
				byte[] resp = "response".getBytes();
				DatagramPacket r = new DatagramPacket(resp, resp.length);
				r.setAddress(p.getAddress());
				r.setPort(p.getPort());
				socket.send(r);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Collection<InetAddress> getNetworkInterfaces() {
		Collection<InetAddress> interfaces = new LinkedList<InetAddress>();
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
	        for (NetworkInterface netint : Collections.list(nets)) {
	        	Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
	        	if (inetAddresses.hasMoreElements())
	        		interfaces.add(inetAddresses.nextElement());
	        }
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return interfaces;
	}
	
	public static void main(String[] args) throws Exception {
		new Thread(new MultiInterfaceServer()).start();
	}
}
