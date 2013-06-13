package ch.inf.vs.californium.coap;

public abstract class MessageObserverAdapter implements MessageObserver {

	@Override
	public void responded(Response response) {
		// empty implementation
	}

	@Override
	public void acknowledged() {
		// empty implementation
	}

	@Override
	public void rejected() {
		// empty implementation
	}

	@Override
	public void timeouted() {
		// empty implementation
	}

	@Override
	public void canceled() {
		// empty implementation
	}

}
