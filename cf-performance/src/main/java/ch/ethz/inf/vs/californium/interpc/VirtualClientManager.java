package ch.ethz.inf.vs.californium.interpc;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.bench.BenchmarkServer;

/**
 * The VirtualClient manager creates the virtual clients for the benchmarks.
 * Each virtual client sends request to the server as fast as the server can
 * handle them.
 */
public class VirtualClientManager {

	public static String HOST = "localhost";
	public static int[] PORTS = { 5683 };
	
	public static final int DEFAULT_TIME = 30; // in seconds
	public static final int DEFAULT_CONCURRENCY = 1000;

	public static final String LOG_FILE = "coapbench";
	
	private Timer timer;
	
	private InetAddress address;
	private int[] ports;
	private long timestamp;
	
	private int count;
	private int duration;
	private ArrayList<VirtualClient> clients;
	
	private LogFile log;
	
	public VirtualClientManager() throws Exception {
		this.log = new LogFile(LOG_FILE);
		this.address = InetAddress.getByName(HOST);
		this.ports = PORTS;
		this.clients = new ArrayList<VirtualClient>();
		this.timer = new Timer();
		log.format("Concurrency, Time, Completed, Timeouted, Throughput | 50%%, 66%%, 75%%, 80%%, 90%%, 95%%, 98%%, 99%%, 100%%, stdev (ms)\n");
	}
	
	public void setClientCount(int c) throws Exception {
		if (c < clients.size()) {
			for (int i=clients.size()-1; i>=c; i--)
				clients.remove(i);
		} else {
			for (int i=clients.size(); i<c; i++)
				clients.add(new VirtualClient(address, ports));
		}
		this.count = c;
	}
	
	public void start(int count, int time) throws Exception {
		this.duration = time * 1000; // convert to ms here
		setClientCount(count);
		Thread[] threads = new Thread[count];
		for (int i=0;i<count;i++) {
			VirtualClient c = clients.get(i);
			c.reset();
			threads[i] = new Thread(c);
		}
		System.out.println("\nStart "+count+" virtual clients for "+time+"s");
		for (int i=0;i<count;i++)
			threads[i].start();
		timestamp = System.nanoTime();
		timer.schedule(new TimerTask() {
			public void run() {
				stop();
			} }, this.duration);
	}
	
	public void stop() {
		float dt = (System.nanoTime() - timestamp) / 1000000f;
		for (VirtualClient vc:clients)
			vc.stop();
		System.out.println("Stoped virtual clients");
		int sum = 0;
		int sumTimeout = 0;
		IntArray latencies = new IntArray();
		for (int i=0;i<clients.size();i++) {
			VirtualClient client = clients.get(i);
			int count = client.getCount();
			int lost = client.getTimeouted();
			latencies.add(client.getLatencies());
			sum += count;
			sumTimeout += lost;
			System.out.format("Virtual client %2d received %7d, timeout %3d, throughput %d /s\n"
					, i, count, lost, (int) (count * 1000L / dt));
		}
		int throughput = (int) (sum * 1000L / dt);
		
		int[] lats = latencies.getArray();
		long latsum = 0;
		for (int l:lats) latsum += l;
		double mean = (double) latsum / lats.length;
		double temp = 0;
        for(int l :lats) temp += (mean-l)*(mean-l);
        double var = Math.sqrt(temp / lats.length);
            
		Arrays.sort(lats); // TODO: bad if length==0
		int q50 = lats[(int) (lats.length/2)];
		int q66 = lats[(int) (lats.length * 2L/3)];
		int q75 = lats[(int) (lats.length * 3L/4)];
		int q80 = lats[(int) (lats.length * 4L/5)];
		int q90 = lats[(int) (lats.length * 9L/10)];
		int q95 = lats[(int) (lats.length * 19L/20)];
		int q98 = lats[(int) (lats.length * 98L/100)];
		int q99 = lats[(int) (lats.length * 99L/100)];
		int q100 = lats[lats.length - 1];
		
		System.out.format("Total received %8d, timeout %4d, throughput %d /s\n"
				, sum, sumTimeout, throughput);
		log.format("%d, %d, %d, %d, %d | %d, %d, %d, %d, %d, %d, %d, %d, %d, %.1f\n",
				count, duration, sum, sumTimeout, throughput,
				q50, q66, q75, q80, q90, q95, q98, q99, q100, var);
	}
	
	public static void main(String[] args) throws Exception {
		int t = DEFAULT_TIME; // in seconds
		int c = DEFAULT_CONCURRENCY;
		int[] cs = null;
		
		if (args.length > 0) {
			int index = 0;
			while (index < args.length) {
				String arg = args[index];

				if ("-usage".equals(arg) || "-help".equals(arg) || "-?".equals(arg)) { // TODO multiple ports?
					System.out.println();
					System.out.println("SYNOPSIS");
					System.out.println("	" + VirtualClientManager.class.getSimpleName() + " URI [-c CONCURRENCY] [-s STEP] [-t TIMELIMIT] [-nocheck]");
					System.out.println("OPTIONS");
					System.out.println("	URI");
					System.out.println("		The target server URI to benchmark.");
					System.out.println("	-c CONCURRENCY");
					System.out.println("		The concurrency level, i.e., the number of parallel clients (default is " + DEFAULT_CONCURRENCY + ").");
					System.out.println("	-s STEP");
					System.out.println("		Stepwise increase the concurrency level by STEP.");
					System.out.println("	-t TIMELIMIT");
					System.out.println("		Limit the duration of the benchmark to TIMELIMIT seconds (default is " + DEFAULT_TIME + ").");
					System.out.println("	-nocheck");
					System.out.println("		???"); //TODO
					System.out.println("EXAMPLES");
					System.out.println("	java -Xms4096m -Xmx4096m " + BenchmarkServer.class.getSimpleName() + " -port 5684 -pool 16");
					System.out.println("	java -Xms4096m -Xmx4096m -jar " + BenchmarkServer.class.getSimpleName() + ".jar -udp-sender 2 -udp-receiver 2");
					System.exit(0);
				// TODO use URI instead
				} else if ("-host".equals(arg)) {
					HOST = args[index+1];
				} else if ("-port".equals(arg)) {
					PORTS = new int[] {Integer.parseInt(args[index+1])};
				} else if ("-target".equals(arg)) {
					VirtualClient.TARGET = args[index+1];
				} else if ("-ports".equals(arg)) { //TODO do we still need multiple ports? maybe better use multiple VirtClMngrs instead if really required?
					ArrayList<String> vals = new ArrayList<String>();
					for (int i=index+1; i<args.length && !args[i].startsWith("-") ;i++)
						vals.add(args[i]);
					PORTS = new int[vals.size()];
					for (int i=0;i<vals.size();i++)
						PORTS[i] = Integer.parseInt(vals.get(i));
					index = index + vals.size() - 1;
				} else if ("-c".equals(arg)) {
					c = Integer.parseInt(args[index+1]);
				} else if ("-s".equals(arg)) {
					int cn = Integer.parseInt(args[index+1]);
					cs = new int[cn];
					for (int i=0;i<cn;i++) cs[i] = Integer.parseInt(args[index+2+i]);
					index += cn;
				} else if ("-t".equals(arg)) {
					t = Integer.parseInt(args[index+1]);
				} else if ("-nocheck".equals(arg))
					VirtualClient.CHECK_CODE = false;
				index += 2;
			}
		}
		System.out.println("CoapBench sends requests to "+HOST+":"+Arrays.toString(PORTS));
		VirtualClientManager m = new VirtualClientManager();
		
		if (cs != null) {
			for (int i=0;i<cs.length;i++) {
				m.start(cs[i], t);
				Thread.sleep((t+5)*1000);
			}
		} else {
			m.start(c, t);
			Thread.sleep((t+1)*1000);
		}
		System.exit(0);
	}
}

