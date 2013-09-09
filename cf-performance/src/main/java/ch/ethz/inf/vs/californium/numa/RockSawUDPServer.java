package ch.ethz.inf.vs.californium.numa;

import static com.savarese.rocksaw.net.RawSocket.PF_INET;
import static com.savarese.rocksaw.net.RawSocket.getProtocolByName;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

import com.savarese.rocksaw.net.RawSocket;

public class RockSawUDPServer {

	/**
	 * ================
	 * How do we bind this thing to a port???
	 * Not the first who wonders: https://forums.oracle.com/thread/1149650
	 * 
	 * Even better: How do we send back to a certain port?
	 */
	
	private RawSocket socket;
	
	public RockSawUDPServer(int port, int threads) throws IOException {
		System.out.println("Start "+threads+" threads listening on port "+port);

		this.socket = new RawSocket();
		socket.open(PF_INET, getProtocolByName("udp"));
		
		this.socket.setReceiveBufferSize(10*1000*1000);
		this.socket.setSendBufferSize(10*1000*1000);
		for (int i=0;i<threads;i++) {
			new Thread() {
				private byte[] sendData = new byte[256];
				private byte[] recvData = new byte[256];
				private byte[] srcAddress = new byte[4];
				public void run() {
					try {
						while (true) {
							System.out.println("listening");
							socket.read(recvData, srcAddress);
							
							System.out.println("received something");
							
							// do what a CoAP server does:
							// datagram -> bytes -> request -> response -> bytes
							sendData = new String(recvData).getBytes();
							InetAddress dst = InetAddress.getByAddress(srcAddress);
							System.out.println("send to "+dst+": "+Arrays.toString(sendData));
							socket.write(dst, sendData);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	public static void main(String[] args) throws Exception {
//		-Djava.library.path=rocksaw-1.0.1\lib\
//		args = new String[] {"-ports", "5683", "-threads", "4"};
		int threads = 1;
		int index = 0;
		int[] ports = {5683};
		while (index < args.length) {
			String arg = args[index];
			if ("-threads".equals(arg)) {
				threads = Integer.parseInt(args[index+1]);
			} else if ("-ports".equals(arg)) {
				ArrayList<String> vals = new ArrayList<String>();
				for (int i=index+1;i<args.length;i++) {
					if (args[i].startsWith("-")) break;
					else vals.add(args[i]);
				}
				ports = new int[vals.size()];
				for (int i=0;i<vals.size();i++)
					ports[i] = Integer.parseInt(vals.get(i));
				index = index + vals.size() - 1;
			}
			index += 2;
		}
		
		for (int i=0;i<ports.length;i++) {
			new RockSawUDPServer(ports[i], threads);
		}
	}
	
}
