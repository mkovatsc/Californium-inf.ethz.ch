package test;

import static org.junit.Assert.*;

import java.util.Timer;
import java.util.TimerTask;

import org.junit.Test;

import coap.Request;
import coap.GETRequest;
import coap.Response;

public class RequestTest {

	class RespondTask extends TimerTask {

		RespondTask(Request request, Response response) {
			this.request = request;
			this.response = response;
		}

		@Override
		public void run() {
			request.respond(response);
		}

		Request request;
		Response response;

	}

	@Test
	public void testRespond() {

		System.out.println("/b".split("/").length);

		// Client Side /////////////////////////////////////////////////////////

		// create new request with own response handler
		Request request = new GETRequest() {
			@Override
			protected void handleResponse(Response response) {
				// change state of outer object
				handledResponse = response;
			}
		};

		/* (...) send the request to server */

		// Server Side /////////////////////////////////////////////////////////

		/* (...) receive request from client */

		// create new response
		Response response = new Response();

		// respond to the request
		request.respond(response);

		// Validation /////////////////////////////////////////////////////////

		// check if response was handled correctly
		assertSame(response, handledResponse);

	}

	@Test
	public void testReceiveResponse() throws InterruptedException {

		// Client Side /////////////////////////////////////////////////////////

		Request request = new GETRequest();

		// enable response queue in order to perform receiveResponse() calls
		request.enableResponseQueue(true);

		/* (...) send the request to server */

		// Server Side /////////////////////////////////////////////////////////

		/* (...) receive request from client */

		// create new response
		Response response = new Response();

		// schedule delayed response (e.g. take some time for computation etc.)
		timer.schedule(new RespondTask(request, response), 500);

		// Client Side /////////////////////////////////////////////////////////

		// block until response received
		Response receivedResponse = request.receiveResponse();

		// Validation /////////////////////////////////////////////////////////

		// check if response was received correctly
		assertSame(response, receivedResponse);
	}

	Response handledResponse;
	Timer timer = new Timer();
}
