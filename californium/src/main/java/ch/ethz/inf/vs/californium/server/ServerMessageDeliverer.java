
package ch.ethz.inf.vs.californium.server;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.observe.ObserveManager;
import ch.ethz.inf.vs.californium.observe.ObserveRelation;
import ch.ethz.inf.vs.californium.observe.ObservingEndpoint;
import ch.ethz.inf.vs.californium.server.resources.Resource;

/**
 * The ServerMessageDeliverer delivers requests to corresponding resources and
 * responses to corresponding requests.
 */
public class ServerMessageDeliverer implements MessageDeliverer {

	private final static Logger LOGGER = Logger.getLogger(ServerMessageDeliverer.class.getCanonicalName());

	/** The root of all resources */
	private final Resource root;

	/** The manager of the observe mechanism for this server */
	private ObserveManager observeManager = new ObserveManager();

	/**
	 * Constructs a default message deliverer that delivers requests to the
	 * resources rooted at the specified root.
	 */
	public ServerMessageDeliverer(Resource root) {
		this.root = root;
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.MessageDeliverer#deliverRequest(ch.inf.vs.californium.network.Exchange)
	 */
	@Override
	public void deliverRequest(final Exchange exchange) {
		Request request = exchange.getRequest();
		List<String> path = request.getOptions().getURIPaths();
		final Resource resource = findResource(path);
		if (resource != null) {
			checkForObserveOption(exchange, resource);
			
			// Get the executor and let it process the request
			Executor executor = resource.getExecutor();
			if (executor != null) {
				executor.execute(new Runnable() {
					public void run() {
						resource.handleRequest(exchange);
					} });
			} else {
				resource.handleRequest(exchange);
			}
		} else {
			LOGGER.info("Did not find resource " + path.toString());
			exchange.respond(new Response(ResponseCode.NOT_FOUND));
		}
	}

	/**
	 * Checks whether an observe relationship has to be established or canceled.
	 * 
	 * @param exchange
	 *            the exchange of the current request
	 * @param resource
	 *            the target resource
	 * @param path
	 *            the path to the resource
	 */
	private void checkForObserveOption(Exchange exchange, Resource resource) {
		Request request = exchange.getRequest();
		if (request.getCode() != Code.GET) return;

		InetSocketAddress source = new InetSocketAddress(request.getSource(), request.getSourcePort());

		if (request.getOptions().hasObserve()) {
			if (resource.isObservable()) {
				// Requests wants to observe and resource allows it :-)
				LOGGER.info("Initiate an observe relation between " + request.getSource() + ":" + request.getSourcePort() + " and resource " + resource.getURI());
				ObservingEndpoint endpoint = observeManager.findObservingEndpoint(source);
				ObserveRelation relation = new ObserveRelation(endpoint, resource, exchange);
				endpoint.addObserveRelation(relation);
				exchange.setRelation(relation);
				// all that's left is to add the relation to the resource which
				// the resource must do itself if the response is successful 
			}
			/*
			 * else, the request wants to observe but the resource does not
			 * support it. The only consequence to that is that the response
			 * will not contain an observe option.
			 */

		} else {
			/*
			 * Since draft-ietf-core-observe-09 it is no longer possible to
			 * cancel an observe relation by sending a GET request without
			 * observe option. The code in draft-08 looked like this:
			 * 
			 * // There is no observe option. Therefore, we have to remove it from
			 * // the resource (if it is actually there).
			 * ObservingEndpoint endpoint = observeManager.getObservingEndpoint(source);
			 * if (endpoint == null) return; // because no relation can exist
			 * ObserveRelation relation = endpoint.getObserveRelation(path);
			 * if (relation == null) return; // because no relation can exist
			 * // Otherwise, we need to remove it
			 * relation.cancel();
			 */
		}
	}

	/**
	 * Searches in the resource tree for the specified path. A parent resource
	 * may accept requests to subresources, e.g., to allow addresses with
	 * wildcards like <code>coap://example.com:5683/devices/*</code>
	 * 
	 * @param list the path as list of resource names
	 * @return the resource or null if not found
	 */
	private Resource findResource(List<String> list) {
		LinkedList<String> path = new LinkedList<String>(list);
		Resource current = root;
		while (!path.isEmpty() && current != null) {
			String name = path.removeFirst();
			current = current.getChild(name);
		}
		return current;
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.MessageDeliverer#deliverResponse(ch.inf.vs.californium.network.Exchange, ch.inf.vs.californium.coap.Response)
	 */
	@Override
	public void deliverResponse(Exchange exchange, Response response) {
		if (response == null) throw new NullPointerException();
		if (exchange == null) throw new NullPointerException();
		if (exchange.getRequest() == null) throw new NullPointerException();
		exchange.getRequest().setResponse(response);
	}
}
