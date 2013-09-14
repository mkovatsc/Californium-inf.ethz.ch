package ch.ethz.inf.vs.californium;

import ch.ethz.inf.vs.californium.coap.Request;

public class CoapObserveRelation {

	private Request request;
	
	private boolean canceled;
	
	private CoapResponse current;
	
	protected CoapObserveRelation(Request request) {
		this.request = request;
		this.canceled = false;
	}
	
	public void cancel() {
		request.cancel();
		setCanceled(true);
	}
	
	public boolean isCanceled() {
		return canceled;
	}
	
	public CoapResponse getCurrent() {
		return current;
	}
	
	protected void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}
	
	protected void setCurrent(CoapResponse current) {
		this.current = current;
	}
	
}
