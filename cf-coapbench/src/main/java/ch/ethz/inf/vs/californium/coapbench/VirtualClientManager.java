package ch.ethz.inf.vs.californium.coapbench;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The VirtualClient manager creates the virtual clients for the benchmarks.
 * Each virtual client sends request to the server as fast as the server can
 * handle them.
 */
public class VirtualClientManager {

	public static final String LOG_FILE = "coapbench";
	
	private Timer timer;

	private URI uri;
	private InetSocketAddress bindAddr;

	private long timestamp;
	private int count;
	private int time;
	private ArrayList<VirtualClient> clients;
	
	private LogFile log;

	public VirtualClientManager() throws Exception {
		this(null);
	}
	
	public VirtualClientManager(URI uri) throws Exception {
		this(uri, null);
	}
	
	public VirtualClientManager(URI uri, InetSocketAddress bindAddr) throws Exception {
		this.uri = uri;
		this.bindAddr = bindAddr;
		this.log = new LogFile(LOG_FILE);

		this.clients = new ArrayList<VirtualClient>();
		this.timer = new Timer();
		log.format("Concurrency, Time, Completed, Timeouted, Throughput | 50%%, 66%%, 75%%, 80%%, 90%%, 95%%, 98%%, 99%%, 100%%, stdev (ms)\n");
	}
	
	public void runConcurrencySeries(int[] cs, int time) throws Exception {
		int n = cs.length;
		log("Run series: "+Arrays.toString(cs).replace("[","").replace("]", ""));
		
		for (int i=0;i<n;i++) {
			start(cs[i], time);
			Thread.sleep(time + 5*1000);
		}
	}
	
	public void log(String entry) throws Exception {
		ensurelog();
		log.println(entry);
	}
	
	public void lognew(String name) throws Exception {
		this.log = new LogFile(LOG_FILE + "_" + name);
	}
	
	private void ensurelog() throws Exception {
		if (log==null) {
			log = new LogFile(LOG_FILE);
			log.format("Concurrency, Time, Completed, Timeouted, Throughput | 50%%, 66%%, 75%%, 80%%, 90%%, 95%%, 98%%, 99%%, 100%%, stdev (ms)\n");
		}
	}
	
	public void setClientCount(int c) throws Exception {
		if (c < clients.size()) {
			for (int i=clients.size()-1; i>=c; i--)
				clients.remove(i);
		} else {
			for (int i=clients.size(); i<c; i++)
				clients.add(new VirtualClient(uri, bindAddr));
		}
		this.count = c;
	}
	
	public void setURI(URI uri) throws UnknownHostException {
		this.uri = uri;
		for (VirtualClient vc:clients)
			vc.setURI(uri);
	}
	
	public void start(int count, int time) throws Exception {
		this.time = time;
		setClientCount(count);
		Thread[] threads = new Thread[count];
		for (int i=0;i<count;i++) {
			VirtualClient c = clients.get(i);
			c.reset();
			threads[i] = new Thread(c);
		}
		System.out.println("\nStart "+count+" virtual clients for "+time+" ms");
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
		System.out.println("Stop virtual clients and collect results");
		for (VirtualClient vc:clients)
			vc.stop();
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
				count, time, sum, sumTimeout, throughput,
				q50, q66, q75, q80, q90, q95, q98, q99, q100, var);
	}
}

