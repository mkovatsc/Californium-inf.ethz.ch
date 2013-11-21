package ch.ethz.inf.vs.californium.examples.plugtest2;

import java.net.URI;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.Utils;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.examples.PlugtestClient.TestClientAbstract;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;

/**
 * TD_COAP_CORE_16: Perform GET transaction (CON mode, delayed response) in
 * a lossy context
 * 
 * @author Matthias Kovatsch
 */
public class CC16 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/separate";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

    private static final long wait = NetworkConfig.getStandard().getInt(NetworkConfigDefaults.EXCHANGE_LIFECYCLE);

	public CC16(String serverURI) {
		super(CC16.class.getSimpleName());

		// create the request
		Request request = new Request(Code.GET, Type.CON);
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);

	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkType(Type.CON, response.getType());
		success &= checkInt(EXPECTED_RESPONSE_CODE.value,
				response.getCode().value, "code");
		success &= hasContentType(response);
		success &= hasNonEmptyPalyoad(response);

		return success;
	}
	
	@Override
	protected synchronized void executeRequest(Request request,
			String serverURI, String resourceUri) {
		if (serverURI == null || serverURI.isEmpty()) {
			System.err.println("serverURI == null || serverURI.isEmpty()");
			throw new IllegalArgumentException(
					"serverURI == null || serverURI.isEmpty()");
		}

		// defensive check for slash
		if (!serverURI.endsWith("/") && !resourceUri.startsWith("/")) {
			resourceUri = "/" + resourceUri;
		}

		URI uri = null;
		try {
			uri = new URI(serverURI + resourceUri);
		} catch (URISyntaxException use) {
			System.err.println("Invalid URI: " + use.getMessage());
		}

		request.setURI(uri);

		request.addMessageObserver(new TestResponseHandler(request));

		// print request info
		if (verbose) {
			System.out.println("Request for test " + this.testName
					+ " sent");
			Utils.prettyPrint(request);
		}

		// execute the request
		try {
			request.send();
			if (sync) {
				request.waitForResponse(wait);
			}
			
			request.waitForResponse(wait/2);
			
		} catch (InterruptedException e) {
			System.err.println("Interupted during receive: "
					+ e.getMessage());
			System.exit(-1);
		}
	}
}