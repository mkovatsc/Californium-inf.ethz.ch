package ch.ethz.inf.vs.californium.examples.plugtest2;

import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.Request;

/*
 * Utility class to provide transaction timeouts
 */
public class MaxAgeTask extends TimerTask {

	private Request request;

	public MaxAgeTask(Request request) {
		this.request = request;
	}

	@Override
	public void run() {
		// this.request.handleTimeout();
		request.setResponse(null);
	}
}