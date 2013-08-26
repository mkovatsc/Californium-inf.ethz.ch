package ch.ethz.inf.vs.californium.interpc;
import java.net.InetAddress;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * The client slave keeps a TCP connection to the master. The master sends
 * commands to the slave.
 */
public class ClientSlave implements Runnable {

	public static final String MASTER_ADDRESS = "192.168.1.37";
	public static final int MASTER_PORT = 8888;

	public static final String CMD_PING = "ping";
	public static final String CMD_EXIT = "exit";
	public static final String CMD_STRESS = "stress";
	public static final String CMD_BENCH = "bench";
	public static final String CMD_APACHE_BENCH = "ab";
	
//	private final SocketAddress address;
	
	private Socket socket;
	
	private VirtualClientManager vcm;
	private ApacheBench ab;
	
	public ClientSlave() throws Exception {
		System.out.println("Start client slave");
	}
	
	public void run() {
		try {
			while (true) {
				connect();
				runrun();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Something went wrong. We should not leave the method run()
			System.err.println("SEVERE: Client has stopped working");
		}
	}
	
	private void connect() {
		// Try to connect until it worked
		while (true) {
			try {
				socket = new Socket(
						InetAddress.getByName(MASTER_ADDRESS), MASTER_PORT);
				System.out.println("Connected to "+socket.getRemoteSocketAddress());
				return; // return if successful
			} catch (Exception e) {
				System.err.println("Failed to connect to "+MASTER_ADDRESS+":"+MASTER_PORT);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void runrun() {
		System.out.println("Waiting for commands");
		try (Scanner in = new Scanner(socket.getInputStream())) {
			while (true) {
				String command = in.nextLine();
				System.out.println("Received command: "+command);
				
				if (command.startsWith(CMD_PING)) {
					send(CMD_PING); // respond with ping

				} else if (command.startsWith(CMD_STRESS)) {
					stress(new Command(command));
					
				} else if (command.startsWith(CMD_BENCH)) {
					bench(new Command(command));
					
				} else if (command.startsWith(CMD_APACHE_BENCH)) {
					ab(new Command(command));
					
				} else if (command.startsWith(CMD_EXIT)) {
					System.exit(0);
					
				} else {
					System.out.println("Unknown command: "+command);
				}
			}
		} catch (NoSuchElementException e) {
			// When master is shutdown, we arrive here
			System.out.println("Exception when reading from master: \""+e.getMessage()+"\"");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void send(String response) {
		try {
//			System.out.println("Send "+response);
			socket.getOutputStream().write(response.getBytes());
			socket.getOutputStream().write("\n".getBytes());
			socket.getOutputStream().flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void stress(Command command) throws Exception {
		if (command.has("start"))
			StressClient.main(null);
		if (command.has("stop"))
			StressClient.stop();
	}

	private void bench(Command command) throws Exception {
		if (this.vcm == null)
			this.vcm = new VirtualClientManager();
		if (command.has("-start")) {
			int time = command.has("-t") ? command.get("-t") : VirtualClientManager.DEFAULT_TIME;
			vcm.start(command.get("-start"), time);
		}
	}
	
	private void ab(Command command) throws Exception {
		if (this.ab == null)
			this.ab = new ApacheBench();
		ab.start(command);
	}
	
	public static void main(String[] args) throws Exception {
		new ClientSlave().run();
	}
}
