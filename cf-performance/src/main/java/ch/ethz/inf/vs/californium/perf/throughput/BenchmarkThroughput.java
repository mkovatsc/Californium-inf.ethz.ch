package ch.ethz.inf.vs.californium.perf.throughput;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import ch.ethz.inf.vs.californium.Server;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointAddress;
import ch.ethz.inf.vs.californium.network.EndpointObserver;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.RawData;
import ch.ethz.inf.vs.californium.network.connector.Connector;
import ch.ethz.inf.vs.californium.network.connector.ConnectorBase;
import ch.ethz.inf.vs.californium.network.dedupl.CropRotation;
import ch.ethz.inf.vs.californium.resources.ResourceBase;

public class BenchmarkThroughput {

	public static final EndpointAddress BENCHMARK_ADDRESS = new EndpointAddress(
			InetAddress.getLoopbackAddress(), 5683);
	public static final String TARGET = "benchmark";
	public static final String TARGET_URI = "coap://localhost:5683/" + TARGET;
	public static final String RESPONSE = "huhu";
	public static final int OCCUPATION = 100000;

	private long t0;

	private ScheduledThreadPoolExecutor executor;

	public BenchmarkThroughput() throws Exception {

		NetworkConfig config = new NetworkConfig();
		CropRotation.PERIOD = 500;
		config.setMarkAndSweepInterval(1000);
		config.setExchangeLifecycle(1500);

		Connector connector = new BenchmarkConnector();
		Endpoint endpoint = new Endpoint(connector, BENCHMARK_ADDRESS, config);

		executor = (ScheduledThreadPoolExecutor) Executors
				.newScheduledThreadPool(4);

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

		System.out.println("After init");
		MessageProducer.printUsesMemory();

		endpoint.addObserver(new EndpointObserver() {
			public void stopped(Endpoint endpoint) {
			}

			public void destroyed(Endpoint endpoint) {
			}

			public void started(Endpoint endpoint) {
				System.out.println("start");
				MessageProducer.printUsesMemory();
				t0 = System.nanoTime();
			}
		});
		endpoint.start();
	}

	public static void main(String[] args) throws Exception {
		Server.LOG_ENABLED = false;
		new BenchmarkThroughput();
	}

	class BenchmarkConnector extends ConnectorBase {

		private EcoMessageProducer producer;
		private AtomicInteger counter = new AtomicInteger();
		
		private int cc = 0;
		private BlockingQueue<Object> q = new LinkedBlockingQueue<>();
		private float avg[] = new float[10];
		private int avgp = 0;

		public BenchmarkConnector() {
			super(new EndpointAddress(null, 0));
			this.producer = new EcoMessageProducer(TARGET_URI);
		}

		@Override
		public String getName() {
			return "Benchmark";
		}

		@Override
		protected RawData receiveNext() throws Exception {
			if (++cc % OCCUPATION == 0) {
				pause();
			}
			return producer.next();
		}

		@Override
		public void send(RawData msg) {
			int d = counter.incrementAndGet();
			if (d % OCCUPATION == OCCUPATION / 2) {
				resume();

				long now = System.nanoTime();
				float dt = (now - t0) / 1000000f;
				float through = OCCUPATION * 1000L / dt;
				System.out.format("received %8d. In %4d  ms, %6d per sec, 10avg: %6d\n",
						d, (int) dt, (int) through, (int) nextAvg(through));
				t0 = now;
			}
		}

		@Override
		protected void sendNext(RawData raw) throws Exception {
			throw new UnsupportedOperationException();
		}

		private void pause() throws InterruptedException {
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
	}

}