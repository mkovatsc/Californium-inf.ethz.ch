package ch.ethz.inf.vs.californium.interpc;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The VirtualClient manager creates the virtual clients for the benchmarks.
 * Each virtual client sends request to the server as fast as the server can
 * handle them.
 */
public class VirtualClientManager {

//	public static final String HOST = ClientSlave.MASTER_ADDRESS;
	public static final String HOST = "localhost";
	public static final int PORT = 5683;
	public static final String LOG_FILE = "bench";
	
	public static final int DEFAULT_TIME = 60*1000;
	private Timer timer;
	
	private InetAddress address;
	private int port;
	private long timestamp;
	
	private ArrayList<VirtualClient> clients;
	
	private LogFile log;
	
	public VirtualClientManager() throws Exception {
		this.log = new LogFile(LOG_FILE);
		this.address = InetAddress.getByName(HOST);
		this.port = PORT;
		this.clients = new ArrayList<VirtualClient>();
		this.timer = new Timer();
	}
	
	public void setClientCount(int c) throws Exception {
		if (c < clients.size()) {
			for (int i=clients.size()-1; i>=c; i--)
				clients.remove(i);
		} else {
			for (int i=clients.size(); i<c; i++)
				clients.add(new VirtualClient(address, port));
		}
	}
	
	public void start(int count, int time) throws Exception {
		setClientCount(count);
		Thread[] threads = new Thread[count];
		for (int i=0;i<count;i++) {
			VirtualClient c = clients.get(i);
			c.reset();
			threads[i] = new Thread(c);
		}
		log.println("\nStart "+count+" virtual clients for "+time+" ms");
		for (int i=0;i<count;i++)
			threads[i].start();
		timestamp = System.nanoTime();
		timer.schedule(new TimerTask() {
			public void run() {
				stop();
			} }, time);
	}
	
	public void stop() {
		float dt = (System.nanoTime() - timestamp) / 1000000f;
		for (VirtualClient vc:clients)
			vc.stop();
		log.println("Stoped virtual clients");
		int sum = 0;
		int sumLost = 0;
		for (int i=0;i<clients.size();i++) {
			int count = clients.get(i).getCount();
			int lost = clients.get(i).getLost();
			sum += count;
			sumLost += lost;
			log.format("Virtual client %2d received %7d, lost %3d, throughput %d /s\n"
					, i, count, lost, (int) (count * 1000L / dt));
		}
		log.format("Total received %8d, lost %4d, throughput %d /s\n"
				, sum, sumLost, (int) (sum * 1000L / dt));
	}
	
	public static void main(String[] args) throws Exception {
		VirtualClientManager m = new VirtualClientManager();
		m.start(20,10000);
	}
}

