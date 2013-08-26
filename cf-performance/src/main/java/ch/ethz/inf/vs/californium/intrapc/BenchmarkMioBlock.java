package ch.ethz.inf.vs.californium.intrapc;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import ch.ethz.inf.vs.californium.Server;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointAddress;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.network.RawData;
import ch.ethz.inf.vs.californium.network.connector.Connector;
import ch.ethz.inf.vs.californium.network.connector.ConnectorBase;
import ch.ethz.inf.vs.californium.network.dedupl.CropRotation;
import ch.ethz.inf.vs.californium.producer.MessageProducer;
import ch.ethz.inf.vs.californium.resources.ResourceBase;

/**
 * Measures the time to send 1 million requests and receive the responses.
 */
public class BenchmarkMioBlock {
	
	public static int nix;

	public static final int N = 1000 * 1000;
	public static final String TARGET = "benchmark";
	public static final String TARGET_URI = "coap://localhost:5683/" + TARGET;
	public static final String RESPONSE = "huhu";
	public static final EndpointAddress BENCHMARK_ADDRESS = 
			new EndpointAddress(InetAddress.getLoopbackAddress(), 5683);

	private long start_timestamp;
	
	private ScheduledThreadPoolExecutor executor;

	public BenchmarkMioBlock() throws Exception {
		
		System.out.println("Setup benchmark server");

		NetworkConfig config = new NetworkConfig()
			.setInt(NetworkConfigDefaults.MARK_AND_SWEEP_INTERVAL, 1000)
			.setInt(NetworkConfigDefaults.EXCHANGE_LIFECYCLE, 1500);
		CropRotation.PERIOD = 500;

		Connector connector = new BenchmarkConnector(N);
		Endpoint endpoint = new Endpoint(connector, BENCHMARK_ADDRESS, config);

		executor = (ScheduledThreadPoolExecutor)
				Executors.newScheduledThreadPool(4);
		
		Server server = new Server();
		server.setExecutor(executor);
		server.addEndpoint(endpoint);
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

		System.out.println("Initialized Benchmark");
		MessageProducer.printUsesMemory();

		server.start();
	}

	public static void main(String[] args) throws Exception {
		Server.LOG_ENABLED = false;
		new BenchmarkMioBlock();
	}

	class BenchmarkConnector extends ConnectorBase {

		private Iterator<RawData> producer;
		private AtomicInteger counter = new AtomicInteger();

		public BenchmarkConnector(int amount) throws Exception {
			super(new EndpointAddress(null, 0));
//			this.producer = new EcoMessageProducer(TARGET_URI, amount);
			this.producer = new MessageProducer(TARGET_URI, amount);
		}
		
		@Override
		public void prepareReceiving() {
			System.out.println("Started benchmark endpoint");
			MessageProducer.printUsesMemory();
			start_timestamp = System.nanoTime();
		}

		@Override
		public String getName() {
			return "Benchmark";
		}

		@Override
		protected RawData receiveNext() throws Exception {
			if (producer.hasNext()) {
				return producer.next();
			} else {
				System.out.println("All messages in system");
				MessageProducer.printUsesMemory();
				producer = null;
				while (true)
					Thread.sleep(1000);
			}
		}
		
		@Override
		public void send(RawData msg) {
			int d = counter.incrementAndGet();
			if (d == N) {
				long dt = System.nanoTime() - start_timestamp;
				System.out.println("Time elapsed for " + N + ": " + dt
						/ 1000000f + " ms");
				MessageProducer.printUsesMemory();
				
				try {
					while(true) {
						System.gc();
						MessageProducer.printUsesMemory();
						Thread.sleep(1000);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		}
		
		@Override
		protected void sendNext(RawData raw) throws Exception {
			throw new UnsupportedOperationException();
		}
	}
}