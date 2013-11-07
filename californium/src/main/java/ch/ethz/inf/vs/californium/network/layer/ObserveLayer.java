package ch.ethz.inf.vs.californium.network.layer;

import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.MessageObserverAdapter;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.Exchange.Origin;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.observe.ObserveRelation;

public class ObserveLayer extends AbstractLayer {

	/** The logger. */
	private final static Logger LOGGER = Logger.getLogger(ObserveLayer.class.getName());
	
	public ObserveLayer(NetworkConfig config) { }
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		super.sendRequest(exchange, request);
	}

	@Override
	public void sendResponse(Exchange exchange, Response response) {
		final ObserveRelation relation = exchange.getRelation();
		if (relation != null && relation.isEstablished()) {
			if (exchange.getRequest().getType() == Type.CON
					&& !exchange.getRequest().isAcknowledged()) {
				// Make sure that first response to CON request is ACK
				exchange.getRequest().setAcknowledged(true);
				response.setType(Type.ACK);
			} else if (response.getType() == null) {
				if (relation.check()) {
					response.setType(Type.CON);
					relation.resetCheck();
				} else {
					response.setType(Type.NON);
				}
			}
			
			// This is a notification
			response.setLast(false);

			// NOTE: possible optimization? Try to not always create a new object.
			// We might store something inside relations
			// How about: Reliability=>exchange.settimeout=>relation=>cancel
			response.addMessageObserver(new MessageObserverAdapter() {
				@Override
				public void timeouted() {
					LOGGER.info("Notification timed out. Cancel all relations with source "+relation.getSource());
					relation.cancelAll();
				}
			});
		} // else no observe was requested or the resource does not allow it
		super.sendResponse(exchange, response);
	}

	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		if (response.getOptions().hasObserve()) {
			// Check that request is not already canceled
			if (exchange.getRequest().isCanceled()) {
				// The request was canceled and we no longer want notifications
				EmptyMessage rst = EmptyMessage.newRST(response);
				sendEmptyMessage(exchange, rst);
				return;
			}
			
			super.receiveResponse(exchange, response);
		} else {
			// No observe option in response => deliver (even if we had asked for it)
			super.receiveResponse(exchange, response);
		}
	}
	
	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		// NOTE: We could also move this into the MessageObserverAdapter from
		// sendResponse into the method rejected().
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
