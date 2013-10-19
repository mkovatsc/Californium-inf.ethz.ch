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

// TODO: Auto-generated Javadoc
/**
 * The blockwise layer supports CoAP's observe mechanism. For instance, when a
 * client rejects a notification, the observe relation must be canceled. If a
 * client is not reachable so that a confirmable notification timeouts, all
 * observe relations between the client and resources of the server will be
 * canceled.
 */
public class ObserveLayer extends AbstractLayer {

	/** The logger. */
	private final static Logger LOGGER = Logger.getLogger(ObserveLayer.class.getName());
	
	/**
	 * Constructs a new ObserveLayer.
	 *
	 * @param config the configuration
	 */
	public ObserveLayer(NetworkConfig config) { }
	
	/**
	 * When we send a notification, we must mark the response as not being
	 * the last one of the exchange so that the matcher does not remove
	 * the exchange. We should also mix in confirmable notifications. If the
	 * client is no longer reachable, the relation should be canceled.
	 */
	@Override
	public void sendResponse(Exchange exchange, Response response) {
		final ObserveRelation relation = exchange.getRelation();
		if (relation != null && relation.isEstablished()) {
			if (response.getType() == null) {
				if (exchange.getRequest().getType() == Type.CON
						&& !exchange.getRequest().isAcknowledged()) {
					// Make sure that first response to CON request is ACK
					exchange.getRequest().setAcknowledged(true);
					response.setType(Type.ACK);
				} else {
					// FIXME: mix in some CONs and DO NOT cancel them when the
					// resource issues another (NON) notification. The client
					// will correctly reorder them anyway.
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
					LOGGER.info("Notification timeouted. Cancel all relations with source "+relation.getSource());
					relation.cancelAll();
				}
			});
		} // else no observe was requested or the resource does not allow it
		super.sendResponse(exchange, response);
	}

	/**
	 * When we receive a notification for an observe relation that we have
	 * canceled, we reject it so that the server cancels it as well.
	 */
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
	
	/**
	 * If a client rejects a notification, we cancel the observe relation.
	 */
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
