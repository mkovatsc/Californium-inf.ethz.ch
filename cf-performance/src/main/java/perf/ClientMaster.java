package perf;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class ClientMaster implements Runnable {

	public static final int PORT = 8888;

	public static final String CMD_EXIT = "exit";
	public static final String CMD_STATUS = "status";
	public static final String CMD_PING = "ping";
	public static final String CMD_STRESS = "stress";
	public static final String CMD_BENCH = "bench";
	public static final String CMD_WAIT = "wait";
	public static final String CMD_APACHE_BENCH = "ab";
	
	private ServerSocket masterSocket;
	
	private List<Slave> slaves;
	
	private String last = "";
	
	public ClientMaster() throws Exception {
		this.masterSocket = new ServerSocket(PORT);
		this.slaves = new LinkedList<>();
	}
	
	public void start() {
		System.out.println("Start client master");
		new Thread(this).start();
		try (Scanner in = new Scanner(System.in)) {
			while (true) {
				try {
					String line = in.nextLine();
					if (line.equals("-"))
						line = last;
					else last = line;
					String[] commands = line.split(";");
					for (String cmd:commands) {
						Command command = new Command(cmd.trim());
						String body = command.getBody();
						if (body.isEmpty()) {
							continue;
						} else if (body.startsWith(CMD_EXIT)) {
							exit(command);
						} else if (body.startsWith(CMD_STATUS)) {
							status();
						} else if (body.startsWith(CMD_PING)) {
							ping(command);
						} else if (body.startsWith(CMD_STRESS)) {
							command(command);
						} else if (body.startsWith(CMD_BENCH)) {
							command(command);
						} else if (body.startsWith(CMD_APACHE_BENCH)) {
							command(command);
						} else if (body.startsWith(CMD_WAIT)) {
							wait(command);
							
						} else {
							System.out.println("Unknown command: "+command);
						}
					}
					System.out.println();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void status() {
		System.out.println("Connected to "+slaves.size()+" slaves");
		for (Slave s:slaves)
			System.out.println(s);
	}
	
	public void ping(Command command) {
		System.out.println("Ping to slaves");
		for (Slave s:getSlaves(command.getAt())) {
			System.out.println(" - "+s+": "+s.ping()+" ms");
		}
	}
	
	private void command(Command command) {
		for (Slave s:getSlaves(command.getAt())) {
			System.out.println("Send to \""+command.getBody()+"\" to "+s);
			s.send(command.getBody());
		}
	}
	
	public void exit(Command command) throws Exception {
		if (command.has("-all")) {
			for (Slave s:getSlaves(command.getAt())) {
				System.out.println("exit "+s);
				s.send(CMD_EXIT);
			}
			Thread.sleep(100);
		} else {
			System.out.println("Only master exits");
		}
		System.out.println("exit");
		System.exit(0);
	}
	
	public void wait(Command command) throws InterruptedException {
		if (command.has("-t")) {
			int time = command.get("-t");
			System.out.println("Wait "+(time / 1000f) +" s" );
			Thread.sleep(time);
		} else {
			System.out.println("no time option \"-t X\" found");
		}
	}
	
	public void run() {
		System.out.println("Start masterSocket "+masterSocket.getLocalSocketAddress());
		while (true) {
			try {
				Socket connection = masterSocket.accept();
				System.out.println("Connected to new slave "+connection);
				Slave slave = new Slave(connection);
				slaves.add(slave);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private ArrayList<Slave> getSlaves(int at) {
		if (at == Command.ALL)
			return new ArrayList<>(slaves);
		else {
			ArrayList<Slave> s = new ArrayList<>();
			s.add(slaves.get(at-1));
			return s;
		}
	}
	
	public void remove(Slave slave) {
		System.out.println("Remove slave "+slave);
		slaves.remove(slave);
	}
	
	private class Slave {
		
		private Socket socket;
		private Scanner in;
		
		public Slave(Socket socket) throws Exception {
			this.socket = socket;
			this.in = new Scanner(socket.getInputStream());
		}
		
		public boolean send(String command) {
			try {
				socket.getOutputStream().write(command.getBytes());
				socket.getOutputStream().write("\n".getBytes());
				socket.getOutputStream().flush();
				return true;
				
			} catch (SocketException e) {
				// When slave is shutdown, we arrive here
				System.out.println("Exception while sending to "+this+": \""+e.getMessage()+"\"");
				remove(this);
			} catch (IOException e) {
				e.printStackTrace();
				remove(this);
			}
			return false;
		}
		
		public int ping() {
			try {
				long t0 = System.nanoTime();
				boolean succ = send(CMD_PING);
				if (!succ) return -1;
				in.nextLine(); // wait for response
				long dt = System.nanoTime() - t0;
				return (int) (dt / 1000000);
			} catch (Exception e) {
				e.printStackTrace();
				return -1;
			}
		}
		
		@Override
		public String toString() {
			return socket.getRemoteSocketAddress().toString();
		}
	}
	
	public static void main(String[] args) throws Exception {
		new ClientMaster().start();
	}
}
