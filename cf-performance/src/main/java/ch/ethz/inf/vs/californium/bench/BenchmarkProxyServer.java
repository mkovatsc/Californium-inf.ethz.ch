package ch.ethz.inf.vs.californium.bench;

import java.io.IOException;
import java.util.logging.Level;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.proxy.ProxyCoAPResolver;
import ch.ethz.inf.vs.californium.proxy.ProxyHttpServer;

/**
 * This benchmark measures the throughput for the {@link ProxyHttpServer}. The
 * HTTP server receives an HTTP-GET request and translates it into a CoAP
 * request. The server then immediately responds with a 2048 bytes long response
 * and translates the CoAP response back into an HTTP response to send back to
 * the client. This server does not use the CoAP2CoAP proxy.
 * <p>
 * Http2CoAP: Insert in browser: URI:
 * http://localhost:8080/proxy/coap://localhost:5683/benchmark
 */
public class BenchmarkProxyServer {

	public static final int PORT = 8080;
	
	// Creates a byte array of length 8 x 8 x 8 x 4 = 2048
	private byte[] resp = "11111111".replace("1", "22222222")
			.replace("2", "33333333").replace("3", "ABCD").getBytes();
	
	/**
	 * Starts an HTTP-to-CoAP proxy server that listens on port 8080
	 * @throws IOException
	 */
	public BenchmarkProxyServer() throws IOException {
		ProxyHttpServer httpServer = new ProxyHttpServer(PORT);
		httpServer.setProxyCoapResolver(new ProxyCoAPResolver() {
			@Override public void forwardRequest(Exchange exchange) {
				Response response = new Response(ResponseCode.CONTENT);
				response.setPayload(resp);
				exchange.respond(response);
			}
		});
	}
	
	public static void main(String[] args) throws Exception {
		CalifonriumLogger.setLoggerLevel(Level.ALL);
		NetworkConfig.createStandardWithoutFile();
		new BenchmarkProxyServer();
	}
}
