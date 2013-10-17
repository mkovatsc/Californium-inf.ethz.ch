package ch.ethz.inf.vs.californium.numa;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;

public class NIOUDPServer {

	private Selector selector;
	private DatagramChannel channel;
	
	public NIOUDPServer(int port, int threads) throws IOException {
		System.out.println("Start "+threads+" threads listening on port "+port);
		
		selector = Selector.open();
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new InetSocketAddress(port));
		channel.register(selector, SelectionKey.OP_READ, new ClientRecord());
		channel.setOption(StandardSocketOptions.SO_SNDBUF, 10*1000*1000);
		channel.setOption(StandardSocketOptions.SO_RCVBUF, 10*1000*1000);
		
		for (int i=0;i<threads;i++) {
			new Thread() {
				private ByteBuffer request = ByteBuffer.allocate(64);
				private ByteBuffer response = ByteBuffer.allocate(64);
				public void run() {
					try {
						while (true) {

							request.clear();
							
							if (selector.selectedKeys() == null)
								continue;
							
							SocketAddress client = channel.receive(request);
							if (client == null) continue;
							response = ByteBuffer.wrap(new String(request.array()).getBytes());
							channel.send(response, client);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	static class ClientRecord {
		public SocketAddress clientAddress;
		public ByteBuffer recv_buffer = ByteBuffer.allocate(64);
		public ByteBuffer send_buffer = ByteBuffer.allocate(64);
	}

	public static void main(String[] args) throws Exception {
//		args = new String[] {"-ports", "5683", "-threads", "8"};
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
			new NIOUDPServer(ports[i], threads);
		}
	}
}
