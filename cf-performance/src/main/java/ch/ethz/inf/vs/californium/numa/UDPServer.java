package ch.ethz.inf.vs.californium.numa;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;

public class UDPServer {

	private DatagramSocket socket;
	
	public UDPServer(int port, int threads) throws IOException {
		System.out.println("Start "+threads+" threads listening on port "+port);
		this.socket = new DatagramSocket(port);
		this.socket.setReceiveBufferSize(10*1000*1000);
		this.socket.setSendBufferSize(10*1000*1000);
		for (int i=0;i<threads;i++) {
			new Thread() {
				private DatagramPacket request = new DatagramPacket(new byte[64], 64);
				private DatagramPacket response = new DatagramPacket(new byte[64], 64);
				public void run() {
					try {
						while (true) {
							socket.receive(request);
							// do what a CoAP server does:
							// datagram -> bytes -> request -> response -> bytes
							byte[] bytes = Arrays.copyOfRange(request.getData(), request.getOffset(), request.getLength());
							response.setData(new String(bytes).getBytes());
							response.setAddress(request.getAddress());
							response.setPort(request.getPort());
							socket.send(response);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	public static void main(String[] args) throws Exception {
		args = new String[] {"-ports", "5683", "-threads", "4"};
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
			new UDPServer(ports[i], threads);
		}
	}
	
}
