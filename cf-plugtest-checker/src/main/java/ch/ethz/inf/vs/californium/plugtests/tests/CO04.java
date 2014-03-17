package ch.ethz.inf.vs.californium.plugtests.tests;

import java.net.URI;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.Utils;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.MessageObserverAdapter;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_OBS_04: Client detection of deregistration (Max-Age).
 * 
 * @author Matthias Kovatsch
 */
public class CO04 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/obs";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

	public CO04(String serverURI) {
		super(CO04.class.getSimpleName());

		// create the request
		Request request = new Request(Code.GET, Type.CON);
		// set Observe option
		request.setObserve();
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	@Override
	protected synchronized void executeRequest(Request request, String serverURI, String resourceUri) {

		// defensive check for slash
		if (!serverURI.endsWith("/") && !resourceUri.startsWith("/")) {
			resourceUri = "/" + resourceUri;
		}

		URI uri = null;
		try {
			uri = new URI(serverURI + resourceUri);
		} catch (URISyntaxException use) {
			throw new IllegalArgumentException("Invalid URI: "
					+ use.getMessage());
		}

		request.setURI(uri);

        // for observing
        int observeLoop = 10;

        // print request info
        if (verbose) {
            System.out.println("Request for test " + this.testName + " sent");
			Utils.prettyPrint(request);
        }

        // execute the request
        try {
            Response response = null;
            boolean success = true;
            long time = 5000;
            boolean timedOut = false;

			request.send();
            
            System.out.println();
            System.out.println("**** TEST: " + testName + " ****");
            System.out.println("**** BEGIN CHECK ****");

			response = request.waitForResponse(time);
            if (response != null) {
				success &= checkType(Type.ACK, response.getType());
				success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
				success &= checkToken(request.getToken(), response.getToken());
				success &= hasContentType(response);
				success &= hasNonEmptyPalyoad(response);
				success &= hasObserve(response);
                
                if (success) {

                	time = response.getOptions().getMaxAge() * 1000;
    				System.out.println("+++++ Max-Age: "+time+" +++++");
    				if (time==0) time = 5000;
	            
		            for (int l = 0; success && (l < observeLoop); ++l) {
		
						response = request.waitForResponse(time + 1000);
		                
						// checking the response
						if (response != null) {
							System.out.println("Received notification " + l);
		                	
		                    // print response info
		                    if (verbose) {
		                        System.out.println("Response received");
		                        System.out.println("Time elapsed (ms): " + response.getRTT());
		                        Utils.prettyPrint(response);
		                    }
		
		                    success &= checkResponse(request, response);

							// update timeout
							time = response.getOptions().getMaxAge() * 1000;

							if (!timedOut && l >= 2) {
								System.out.println("+++++++++++++++++++++++");
								System.out.println("++++ REBOOT SERVER ++++");
								System.out.println("+++++++++++++++++++++++");

								System.out.println("++++ obs-reset PUT ++++");
								Request asyncRequest = new Request(Code.POST, Type.CON);
								asyncRequest.setPayload("sesame");
								asyncRequest.setURI(serverURI + "/obs-reset");
								asyncRequest.addMessageObserver(new MessageObserverAdapter() {
										public void onResponse(Response response) {
												if (response != null) {
													System.out.println("Received: " + response.getCode());
													System.out.println("+++++++++++++++++++++++");
												}
											}
										});
								asyncRequest.send();
							}

						} else if (!timedOut) {
							timedOut = true;
							l = observeLoop / 2;
							System.out.println("PASS: Max-Age timed out");
							System.out.println("+++++ Re-registering +++++");
							Request reregister = Request.newGet();
							reregister.setURI(uri);
							reregister.setToken(request.getToken());
							reregister.setObserve();
							request = reregister;
							request.send();
							
							response = request.waitForResponse(time);
				            if (response != null) {
								success &= checkType(Type.ACK, response.getType());
								success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
								success &= checkToken(request.getToken(), response.getToken());
								success &= hasContentType(response);
								success &= hasNonEmptyPalyoad(response);
								success &= hasObserve(response);
				            } else {
				            	System.out.println("FAIL: Re-registration failed");
								success = false;
								break;
				            }
						} else {
							System.out.println("+++++++++++++++++++++++");
							System.out.println("++++ START SERVER +++++");
							System.out.println("+++++++++++++++++++++++");
						} // response != null
					} // observeLoop
		            
		            if (!timedOut) {
		            	System.out.println("FAIL: Server not rebooted");
						success = false;
		            }
					
					if (response!=null) {
		            
			            // RST to cancel
			            System.out.println("+++++++ Cancelling +++++++");
			            
			            request.cancel();
	
						response = request.waitForResponse(time + time/2);
	
						if (response != null) {
				            System.out.println("+++++++ Sent RST +++++++");
						} else {
				            System.out.println("+++++++ No notification +++++++");
						}
					} else {
	                    System.out.println("FAIL: No notification after re-registration");
						success = false;
					}
                }
            } else {
            	System.out.println("FAIL: No notification after registration");
				success = false;
            }
			
            if (success) {
                System.out.println("**** TEST PASSED ****");
                addSummaryEntry(testName + ": PASSED");
            } else {
                System.out.println("**** TEST FAILED ****");
                addSummaryEntry(testName + ": --FAILED--");
            }

            tickOffTest();
			
		} catch (InterruptedException e) {
			System.err.println("Interupted during receive: "
					+ e.getMessage());
			System.exit(-1);
		}
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkType(Type.CON, response.getType());
		success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
		success &= checkToken(request.getToken(), response.getToken());
		success &= hasContentType(response);
		success &= hasNonEmptyPalyoad(response);
		success &= hasObserve(response);

		return success;
	}
}