package ch.inf.vs.californium.perf.throughput;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogManager;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.MessageObserver;
import ch.inf.vs.californium.coap.MessageObserverAdapter;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.EndpointAddress;
import ch.inf.vs.californium.network.EndpointManager;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.NetworkConfig;
import ch.inf.vs.californium.network.dedupl.CropRotation;
import ch.inf.vs.californium.resources.ResourceBase;

public class BenchmarkThroughputViaPort {

	public static final int SERVER_PORT = 7777;
	public static final String TARGET = "benchmark";
	public static final String TARGET_URI = "coap://localhost:"+SERVER_PORT+"/" + TARGET;
	public static final String RESPONSE = "huhu";
	public static final int START_PORT = 61001;
	public static final int OCCUPATION = 50000; // Must not exceed port range of 65000 and remember random MID start

	private ScheduledExecutorService executor;
	
	public BenchmarkThroughputViaPort() throws Exception {

		NetworkConfig config = new NetworkConfig();
		CropRotation.PERIOD = 500;
		config.setMarkAndSweepInterval(2000);
		config.setExchangeLifecycle(1500);
		config.setReceiveBuffer(10*1000*1000);
		config.setSendBuffer(10*100*1000);

//		Connector connector = new BenchmarkConnector();
//		Endpoint endpoint = new Endpoint(connector, BENCHMARK_ADDRESS, config);

		executor = Executors.newScheduledThreadPool(2);

		Server server = new Server();
		Endpoint server_endpoint = new Endpoint(new EndpointAddress(null, SERVER_PORT), config);
		server.addEndpoint(server_endpoint);
		server.setExecutor(executor);
		server.add(new ResourceBase(TARGET) {
			@Override
			public void processRequest(Exchange exchange) {
				try {
					Response response = new Response(ResponseCode.CONTENT);
					response.setPayload(RESPONSE);
					// If we took a NON, we would run out of MIDs immediately.
					// We take an ACK instead to simulate the NON but without
					// the need for new MIDs.
					response.setType(Type.ACK);
					exchange.respond(response);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		System.out.println("After init");
		MessageProducer.printUsesMemory();
		
		server.start();
		
	}
	
	private static class BenchmarkClient {
		
		public Endpoint old, old2;
		private Endpoint endpoint;
		private MessageObserver msgObs;
		private AtomicInteger counter = new AtomicInteger(); // response count
		
		private int cc = 0;
		private BlockingQueue<Object> q = new LinkedBlockingQueue<>();
		private float avg[] = new float[10];
		private int avgp = 0;
		
		private int current_port = START_PORT;
		
		private ScheduledExecutorService clientexecutor;// = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(4);
		
		private long start_timestamp;

		public BenchmarkClient() throws Exception {
			this.msgObs = new MessageObserverAdapter() {
				public void responded(Response response) {
//					System.out.println("recv request "+serverC.incrementAndGet());
					BenchmarkClient.this.responded();
				}
			};
			clientexecutor = Executors.newScheduledThreadPool(4);
			
			createEndpoint();
			
//			new Thread() {
//				public void run() {
//					try {
//						Thread.sleep(18000);
//						System.out.println("\nStatus report");
//						printAllStackTraces();
//						System.out.println("and counter is "+counter);
//						System.out.println("clientC: "+clientC);
//						System.exit(0);
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//			}.start();
		}
		
//		private static void printAllStackTraces() {
//		    Map liveThreads = Thread.getAllStackTraces();
//		    for (Iterator i = liveThreads.keySet().iterator(); i.hasNext(); ) {
//		      Thread key = (Thread)i.next();
//		      System.out.println("Thread " + key.getName());
//		        StackTraceElement[] trace = (StackTraceElement[])liveThreads.get(key);
//		        for (int j = 0; j < trace.length; j++) {
//		            System.out.println("\tat " + trace[j]);
//		        }
//		    }
//		}
		
		private void start() {
			start_timestamp = System.nanoTime();
			try {
				while (true) {
					sendRequest();
	//				if (cc%100 == 0)
	//					Thread.sleep(10);
					if (++cc % OCCUPATION == 0) {
						pause();
						createEndpoint();
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		
		private void sendRequest() {
			Request request = new Request(Code.GET);
			request.setType(Type.NON);
			request.setURI(TARGET_URI);
			request.addMessageObserver(msgObs);
			endpoint.sendRequest(request);
		}
		
		private void responded() {
			int d = counter.incrementAndGet();
			if (d % OCCUPATION == OCCUPATION / 2) {
				resume();

				long now = System.nanoTime();
				float dt = (now - start_timestamp) / 1000000f;
				float through = OCCUPATION * 1000L / dt;
				System.out.format("received %8d. In %4d  ms, %6d per sec, 10avg: %6d\n",
						d, (int) dt, (int) through, (int) nextAvg(through));
				start_timestamp = now;
			}
		}
		
		private void pause() throws Exception {
			q.take();
		}

		private void resume() {
			q.add("nix");
		}

		private float nextAvg(float val) {
			avg[avgp++ % avg.length] = val;
			float sum = 0;
			for (float f : avg)
				sum += f;
			return sum / avg.length;
		}
		
		private void createEndpoint() throws Exception {
			if (old != null)
				old.destroy();
			old = old2;
			old2 = endpoint;
			
			NetworkConfig config = new NetworkConfig();
			config.setMarkAndSweepInterval(1000);
			config.setExchangeLifecycle(1500);
			config.setReceiveBuffer(10*1000*1000);
			config.setSendBuffer(10*1000*1000);
			
			System.out.println("creating endpoint "+current_port);
			endpoint = new Endpoint(new EndpointAddress(current_port), config);
			current_port += 2;
			endpoint.setMessageDeliverer(new EndpointManager.ClientMessageDeliverer());
			endpoint.setExecutor(clientexecutor);
			endpoint.start();
		}
	}

	public static void main(String[] args) throws Exception {
		Server.LOG_ENABLED = false;
		LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
		new BenchmarkThroughputViaPort();
		new BenchmarkClient().start();
	}

}