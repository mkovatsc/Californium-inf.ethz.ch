package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_LINK_01: Access to well-known interface for resource discovery.
 * 
 * @author Matthias Kovatsch
 */
public class CL01 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/.well-known/core";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

	public CL01(String serverURI) {
		super(CL01.class.getSimpleName());

		// create the request
		Request request = Request.newGet();
		
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
		success &= checkOption(MediaTypeRegistry.APPLICATION_LINK_FORMAT, response.getOptions().getContentFormat(), "Content-Format");
		success &= hasNonEmptyPalyoad(response);

		return success;
	}
}