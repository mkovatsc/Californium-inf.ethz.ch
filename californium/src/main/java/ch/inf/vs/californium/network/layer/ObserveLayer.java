package ch.inf.vs.californium.network.layer;

import java.util.logging.Logger;

import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.NetworkConfig;
import ch.inf.vs.californium.network.Exchange.Origin;
import ch.inf.vs.californium.observe.ObserveNotificationOrderer;
import ch.inf.vs.californium.observe.ObserveRelation;

public class ObserveLayer extends AbstractLayer {

	/** The logger. */
	private final static Logger LOGGER = Logger.getLogger(ObserveLayer.class.getName());
	
	private NetworkConfig config;
	
	public ObserveLayer(NetworkConfig config) {
		this.config = config;
	}
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		if (request.getOptions().hasObserve())
			exchange.setObserveOrderer(new ObserveNotificationOrderer());
		super.sendRequest(exchange, request);
	}

	@Override
	public void sendResponse(Exchange exchange, Response response) {
		ObserveNotificationOrderer orderer = exchange.getObserveOrderer();
		if (orderer != null) {
			orderer.orderResponse(response);
		} // else no observe was requested or the resource does not allow it
		super.sendResponse(exchange, response);
	}

	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		if (response.getOptions().hasObserve()) {
			ObserveNotificationOrderer orderer = exchange.getObserveOrderer();
			if (orderer != null) {
				// Multiple responses with different notification numbers might
				// arrive and be processed by different threads. We have to
				// ensure that only the most fresh one is being delivered.
				// We use the notation from the observe draft-08.
				long T1 = orderer.getTimestamp();
				long T2 = System.currentTimeMillis();
				int V1 = orderer.getCurrent();
				int V2 = response.getOptions().getObserve();
				if (V1 < V2 && V2 - V1 < 1<<23
						|| V1 > V2 && V1 - V2 > 1<<23
						|| T2 > T1 + config.getNotificationMaxAge()) {

					// This is a fresh notification. Make sure no newer
					// notification passed us while figuring that out.
					if (orderer.compareAndSet(V1, V2)) {
						// and it still is fresh :-)
						orderer.setTimestamp(T2);

						// FIXME: In theory, older notifications might still pass
						// newer ones before being delivered. To fix this, we
						// had to block all further responses until predecessors
						// have been delivered with certainty.
						super.receiveResponse(exchange, response);
					} else {
						// well, almost did it but there was a newer notification.
						ignore(response);
					}
				} else {
					ignore(response);
				}
				
			} else {
				// We did not ask for an observe relation, yet the server
				// established one. We deliver the response but we might also
				// reject such a misbehavior.
				super.receiveResponse(exchange, response);
			}
		} else {
			// No observe option in response => deliver (even if we had asked for it)
			super.receiveResponse(exchange, response);
		}
	}
	
	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		if (message.getType() == Type.RST && exchange.getOrigin() == Origin.REMOTE) {
			// The response has been rejected
			ObserveRelation relation = exchange.getRelation();
			if (relation != null) {
				relation.cancel();
			} // else there was no observe relation ship and this layer ignores the rst
		}
		super.receiveEmptyMessage(exchange, message);
	}
	
}
