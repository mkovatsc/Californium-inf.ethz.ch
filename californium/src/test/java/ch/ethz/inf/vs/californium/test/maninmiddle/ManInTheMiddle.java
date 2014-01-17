package ch.ethz.inf.vs.californium.test.maninmiddle;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;

/**
 * The man in the middle is between the server and client and monitors the
 * communication. It can drop a packet to simulate packet loss.
 */
public class ManInTheMiddle implements Runnable {

	private SocketAddress clientAddress;
	private SocketAddress serverAddress;
	
	private DatagramSocket socket;
	private DatagramPacket packet;
	
	private volatile boolean running = true;
	
	private int current = 0;
	private int[] drops = new int[0];

	public ManInTheMiddle(SocketAddress clientAddress, SocketAddress serveraAddress) throws Exception {
		this.clientAddress = clientAddress;
		this.serverAddress = serveraAddress;

		this.socket = new DatagramSocket();
		this.packet = new DatagramPacket(new byte[2000], 2000);
		
		new Thread(this).start();
	}

	public void reset() {
		current = 0;
	}
	
	public void drop(int... numbers) {
		System.out.println("Man in the middle will drop packets "+Arrays.toString(numbers));
		drops = numbers;
	}
	
	public void run() {
		try {
			System.out.println("Start man in the middle");
			while (running) {
				socket.receive(packet);
				
//				Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getLength());
				if (contains(drops, current)) {
					System.out.println("Drop packet "+current);
				
				} else {
					if (packet.getSocketAddress().equals(serverAddress))
						packet.setSocketAddress(clientAddress);
					else
						packet.setSocketAddress(serverAddress);
				
					socket.send(packet);
				}
				current++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		running = false;
		socket.close();
	}
	
	public InetSocketAddress getAddress() {
		return new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort());
	}
	
	private boolean contains(int[] array, int value) {
		for (int a:array)
			if (a == value)
				return true;
		return false;
	}
	
}
