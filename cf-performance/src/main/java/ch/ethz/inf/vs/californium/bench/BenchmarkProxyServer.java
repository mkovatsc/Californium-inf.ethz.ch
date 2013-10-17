package ch.ethz.inf.vs.californium.bench;

import java.io.IOException;
import java.util.logging.Level;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.proxy.ProxyCoAPResolver;
import ch.ethz.inf.vs.californium.proxy.ProxyHttpServer;

/**
 * Http2CoAP: Insert in browser:
 *     URI: http://localhost:8080/proxy/coap://localhost:5683/hello
 */
public class BenchmarkProxyServer {

	private byte[] resp = "11111111".replace("1", "22222222")
			.replace("2", "3333").replace("3", "ABCDEFGH").getBytes();
	
	public BenchmarkProxyServer() throws IOException {
		
		ProxyHttpServer httpServer = new ProxyHttpServer(8080);
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
		new BenchmarkProxyServer();
	}
}
