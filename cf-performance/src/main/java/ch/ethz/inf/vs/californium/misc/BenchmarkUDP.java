package ch.ethz.inf.vs.californium.misc;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Measures how many UDP packets can be sent over the network. In my network I have measured
 * a maximum of 280,000 empty packets when three client keep unabatedly sending them.
 */
public class BenchmarkUDP {

	public static final int OCCUPATION = 100000;
	
	public static void send(InetAddress addr, int port) throws Exception {
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.setSendBufferSize(1000*1000);
			byte[] buf = new byte[0];
			DatagramPacket p = new DatagramPacket(buf, buf.length);
			p.setAddress(addr);
			p.setPort(port);
			System.out.println("Sending to "+addr+":"+port);
			
			int counter = 0;
			long ts = System.nanoTime();
			while (true) {
				socket.send(p);
				
				if (++counter%OCCUPATION == 0) {
					long now = System.nanoTime();
					long dt = now - ts;
					System.out.println(String.format("Sent %d, Throughput: %d s-1", counter, OCCUPATION*1000000000L / dt));
					ts = now;
				}
			}
		}
	}
	
	private static long ts;
	private static AtomicInteger counter;
	
	public static void receive(final int... ports) throws Exception {
		counter = new AtomicInteger();
		
		for (int i=0;i<ports.length;i++) {
			final int port = ports[i];
			new Thread() {
				public void run() {
					try (DatagramSocket socket = new DatagramSocket(port)) {
						socket.setReceiveBufferSize(10*1000*1000);
						byte[] buf = new byte[128];
						DatagramPacket p = new DatagramPacket(buf, buf.length);
						System.out.println("Listening on port "+port);
						
						ts = System.nanoTime();
						while (true) {
							socket.receive(p);
							
							int d = counter.incrementAndGet();
							if (d%OCCUPATION == 0) {
								long now = System.nanoTime();
								long dt = now - ts;
								System.out.println(String.format("Received %d, Throughput: %d s-1", d, OCCUPATION*1000000000L / dt));
								ts = now;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
		
		
		
	}
	
	public static void main(String[] args) throws Exception {
//		args = new String[] {"send", "localhost", "7777"};
		args = new String[] {"receive", "7777"};
		
		if ("send".equals(args[0])) {
			InetAddress addr = InetAddress.getByName(args[1]);
			int port = Integer.parseInt(args[2]);
			send(addr, port);
		} else if ("receive".equals(args[0])) {
			int port = Integer.parseInt(args[1]);
			receive(port);
		}
	}
}
