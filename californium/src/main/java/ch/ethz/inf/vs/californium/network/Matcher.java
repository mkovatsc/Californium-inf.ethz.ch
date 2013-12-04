package ch.ethz.inf.vs.californium.network;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange.KeyMID;
import ch.ethz.inf.vs.californium.network.Exchange.KeyToken;
import ch.ethz.inf.vs.californium.network.Exchange.KeyUri;
import ch.ethz.inf.vs.californium.network.Exchange.Origin;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.network.dedupl.Deduplicator;
import ch.ethz.inf.vs.californium.network.dedupl.DeduplicatorFactory;
import ch.ethz.inf.vs.californium.network.layer.ExchangeForwarder;

public class Matcher {

	private final static Logger LOGGER = Logger.getLogger(Matcher.class.getCanonicalName());
	
	private boolean started;
	private ExchangeObserver exchangeObserver = new ExchangeObserverImpl();
	
	private ExchangeForwarder forwarder; // TODO: still necessary?
	
	/** The executor. */
	private ScheduledExecutorService executor;
	
	// TODO: Make per endpoint
	private AtomicInteger currendMID; 
	
	private ConcurrentHashMap<KeyMID, Exchange> exchangesByMID; // Outgoing
	private ConcurrentHashMap<KeyToken, Exchange> exchangesByToken;
	
	private ConcurrentHashMap<KeyUri, Exchange> ongoingExchanges; // for blockwise
	
	// TODO: Multicast Exchanges: should not be removed from deduplicator
	private Deduplicator deduplicator;
	// Idea: Only store acks/rsts and not the whole exchange. Responses should be sent CON.
	
	public Matcher(ExchangeForwarder forwarder, NetworkConfig config) {
		this.forwarder = forwarder;
		this.started = false;
		this.exchangesByMID = new ConcurrentHashMap<KeyMID, Exchange>();
		this.exchangesByToken = new ConcurrentHashMap<KeyToken, Exchange>();
		this.ongoingExchanges = new ConcurrentHashMap<KeyUri, Exchange>();

		DeduplicatorFactory factory = DeduplicatorFactory.getDeduplicatorFactory();
		this.deduplicator = factory.createDeduplicator(config);
		
		if (config.getBoolean(NetworkConfigDefaults.USE_RANDOM_MID_START))
			currendMID = new AtomicInteger(new Random().nextInt(1<<16));
		else currendMID = new AtomicInteger(0);
	}
	
	public synchronized void start() {
		if (started) return;
		else started = true;
		if (executor == null)
			throw new IllegalStateException("Matcher has no executor to schedule exchnage removal");
		deduplicator.start();
	}
	
	public synchronized void stop() {
		if (!started) return;
		else started = false;
		deduplicator.stop();
		clear();
	}
	
	public synchronized void setExecutor(ScheduledExecutorService executor) {
		deduplicator.setExecutor(executor);
		this.executor = executor;
	}
	
	public void sendRequest(Exchange exchange, Request request) {
		if (request.getMID() == Message.NONE)
			request.setMID(currendMID.getAndIncrement()%(1<<16));

		/*
		 * The request is a CON or NCON and must be prepared for these responses
		 * - CON  => ACK/RST/ACK+response/CON+response/NCON+response
		 * - NCON => RST/CON+response/NCON+response
		 * If this request goes lost, we do not get anything back.
		 */
		
		KeyMID idByMID = new KeyMID(request.getMID(), 
				request.getDestination().getAddress(), request.getDestinationPort());
		KeyToken idByTok = new KeyToken(request.getToken(),
				request.getDestination().getAddress(), request.getDestinationPort());
		
		exchange.setObserver(exchangeObserver);
		
		// TODO: remove this statement to save computation?
		LOGGER.fine("Remember by MID "+idByMID+" and by Token "+idByTok);
		
		exchangesByMID.put(idByMID, exchange);
		exchangesByToken.put(idByTok, exchange);
	}

	public void sendResponse(Exchange exchange, Response response) {
		if (response.getMID() == Message.NONE)
			response.setMID(currendMID.getAndIncrement()%(1<<16));
		
		/*
		 * The response is a CON or NON or ACK and must be prepared for these
		 * - CON  => ACK/RST // we only care to stop retransmission
		 * - NCON => RST // we don't care
		 * - ACK  => nothing!
		 * If this response goes lost, we must be prepared to get the same 
		 * CON/NCON request with same MID again. We then find the corresponding
		 * exchange and the retransmissionlayer resends this response.
		 */
		
		if (response.getDestination() == null)
			throw new NullPointerException("Response has no destination address set");
		if (response.getDestinationPort() == 0)
			throw new NullPointerException("Response hsa no destination port set");
		
		// Insert CON and NON to match ACKs and RSTs to the exchange
		KeyMID idByMID = new KeyMID(response.getMID(), 
				response.getDestination().getAddress(), response.getDestinationPort());
		exchangesByMID.put(idByMID, exchange);
		
		if (/*exchange.getCurrentRequest().getCode() == Code.GET
				&&*/ response.getOptions().hasBlock2()) {
			// Remember ongoing blockwise GET requests
			Request request = exchange.getRequest();
//			KeyToken idByTok = new KeyToken(request.getToken(),
//					request.getSource().getAddress(), request.getSourcePort());
			KeyUri keyUri = new KeyUri(request.getURI(),
					response.getDestination().getAddress(), response.getDestinationPort());
			LOGGER.fine("Add request to ongoing exchanges with key "+keyUri);
			ongoingExchanges.put(keyUri, exchange);
		}
		
		if (response.getType() == Type.ACK || response.getType() == Type.NON) {
			// Since this is an ACK or NON, the exchange is over with sending this response.
			if (response.isLast()) {
				exchange.setComplete(true);
				exchangeObserver.completed(exchange);
			}
		} // else this is a CON and we need to wait for the ACK or RST
	}

	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		
		if (message.getType() == Type.RST && exchange != null) {
			// We have rejected the request or response
			exchange.setComplete(true);
			exchangeObserver.completed(exchange);
		}
		
		/*
		 * We do not expect any response for an empty message
		 */
		if (message.getMID() == Message.NONE)
			LOGGER.warning("Empy message "+ message+" has MID NONE // debugging");
	}

	public Exchange receiveRequest(Request request) {
		/*
		 * This request could be
		 *  - Complete origin request => deliver with new exchange
		 *  - One origin block        => deliver with ongoing exchange
		 *  - Complete duplicate request or one duplicate block (because client got no ACK) 
		 *      =>
		 * 		if ACK got lost => resend ACK
		 * 		if ACK+response got lost => resend ACK+response
		 * 		if nothing has been sent yet => do nothing
		 * (Retransmission is supposed to be done by the retransm. layer)
		 */
		
		KeyMID idByMID = new KeyMID(request.getMID(),
				request.getSource().getAddress(), request.getSourcePort());
		
//		KeyToken idByTok = new KeyToken(request.getToken(),
//				request.getSource().getAddress(), request.getSourcePort());
		
		/*
		 * The differentiation between the case where there is a Block1 or
		 * Block2 option and the case where there is none has the advantage that
		 * all exchanges that do not need blockwise transfer have simpler and
		 * faster code than exchanges with blockwise transfer.
		 */
		
		if (!request.getOptions().hasBlock1() && !request.getOptions().hasBlock2()) {
			LOGGER.fine("Create new exchange for remote request");

			Exchange exchange = new Exchange(request, Origin.REMOTE);
			Exchange previous = deduplicator.findPrevious(idByMID, exchange);
			if (previous == null) {
				return exchange;
				
			} else {
				LOGGER.info("Message is a duplicate, ignore: "+request);
				request.setDuplicate(true);
				return previous;
			}
			
		} else {
			
			KeyUri idByUri = new KeyUri(request.getURI(),
					request.getSource().getAddress(), request.getSourcePort());
			
			LOGGER.fine("Lookup ongoing exchange for "+idByUri);
			Exchange ongoing = ongoingExchanges.get(idByUri);
			if (ongoing != null) {
				LOGGER.fine("Found exchange"); // TODO: remove this line
				// This is a block of an ongoing request
				
				Exchange prev = deduplicator.findPrevious(idByMID, ongoing);
				if (prev != null) {
					LOGGER.info("Message is a duplicate: "+request);
					request.setDuplicate(true);
				}
				return ongoing;
		
			} else {
				// We have no ongoing exchange for that block. 
				/*
				 * Note the difficulty of the following code: The first message
				 * of a blockwise transfer might arrive twice due to a
				 * retransmission. The new Exchange must be inserted in both the
				 * hash map 'ongoing' and the deduplicator. They must agree on
				 * which exchange they store!
				 */
				
				LOGGER.fine("Create new exchange for remote request with blockwise transfer");
				Exchange exchange = new Exchange(request, Origin.REMOTE);
				Exchange previous = deduplicator.findPrevious(idByMID, exchange);
				if (previous == null) {
					ongoingExchanges.put(idByUri, exchange);
					return exchange;
					
				} else {
					LOGGER.info("Message is a duplicate: "+request);
					request.setDuplicate(true);
					return previous;
				}
			} // if ongoing
		} // if blockwise
	}

	public Exchange receiveResponse(Response response) {
		
		/*
		 * This response could be
		 * - The first CON/NCON/ACK+response => deliver
		 * - Retransmitted CON (because client got no ACK)
		 * 		=> resend ACK
		 */

		KeyMID idByMID = new KeyMID(response.getMID(), 
				response.getSource().getAddress(), response.getSourcePort());
		
		KeyToken idByTok = new KeyToken(response.getToken(), 
				response.getSource().getAddress(), response.getSourcePort());
		
		Exchange exchange = exchangesByToken.get(idByTok);
		
		if (exchange != null) {
			// There is an exchange with the given token
			
			if (response.getType() != Type.ACK) {
				// Need deduplication for CON and NON but not for ACK (because MID defined by server)
				Exchange prev = deduplicator.findPrevious(idByMID, exchange);
				if (prev != null) { // (and thus it holds: prev == exchange)
					LOGGER.fine("Response is a duplicate "+response);
					response.setDuplicate(true);
				}
			} else {
				/*
				 * In the draft coap-18, section 4.5, there is nothing written
				 * about deduplication of ACKs. Deduplicating ACKs might lead to
				 * a problem, when a server sends requests to itself:
				 * [5683] CON [MID=1234] GET --->  [5683] // => remember MID=1234
				 * [5683] <--- ACK [MID=1234] 2.00 [5683] // => MID=1234 is a duplicate 
				 */ 
			}
			
			if (response.getType() == Type.ACK) { 
				// this is a piggy-backed response and the MID must match
				if (exchange.getCurrentRequest().getMID() == response.getMID()) {
					// The token and MID match. This is a response for this exchange
					return exchange;
					
				} else {
					// The token matches but not the MID. This is a response for an older exchange
					LOGGER.info("Token matches but not MID: wants "+exchange.getCurrentRequest().getMID()+" but gets "+response.getMID());
					EmptyMessage rst = EmptyMessage.newRST(response);
					sendEmptyMessage(exchange, rst);
					// ignore response
					return null;
				}
				
			} else {
				// this is a separate response that we can deliver
				return exchange;
			}
			
		} else {
			// There is no exchange with the given token.
			

			// This might be a duplicate response to an exchanges that is already completed
			if (response.getType() != Type.ACK) {
				// Need deduplication for CON and NON but not for ACK (because MID defined by server)
				Exchange prev = deduplicator.find(idByMID);
				if (prev != null) { // (and thus it holds: prev == exchange)
					LOGGER.info("Message is a duplicate, ignore: "+response);
					response.setDuplicate(true);
					return prev;
				}
			}
			
			LOGGER.info("Received response with unknown token "+idByTok+" and MID "+idByMID+". Reject "+response);
			// This is a totally unexpected response.
			EmptyMessage rst = EmptyMessage.newRST(response);
			sendEmptyMessage(exchange, rst);
			// ignore response
			return null;
		}
	}

	public Exchange receiveEmptyMessage(EmptyMessage message) {
		
		KeyMID idByMID = new KeyMID(message.getMID(),
				message.getSource().getAddress(), message.getSourcePort());
		
		Exchange exchange = exchangesByMID.get(idByMID);
		
		if (exchange != null) {
			return exchange;
		} else {
			LOGGER.info("Matcher received empty message that does not match any exchange: "+message);
			// ignore message;
			return null;
		} // else, this is an ACK for an unknown exchange and we ignore it
	}
	
	public void clear() {
		this.exchangesByMID.clear();
		this.exchangesByToken.clear();
		this.ongoingExchanges.clear();
		deduplicator.clear();
	}
	
	private class ExchangeObserverImpl implements ExchangeObserver {

		@Override
		public void completed(Exchange exchange) {
			if (exchange.getOrigin() == Origin.LOCAL) {
				// TODO: Observe+Blockwise use multiple tokens and we have to
				//       remove all of them
				Request request = exchange.getRequest();
				KeyToken tokKey = new KeyToken(exchange.getCurrentRequest().getToken(),
						request.getDestination().getAddress(), request.getDestinationPort());
				LOGGER.fine("Exchange completed, forget token "+tokKey);
				exchangesByToken.remove(tokKey);
				// TODO: What if the request is only a block?
				
				KeyMID midKey = new KeyMID(request.getMID(), 
						request.getDestination().getAddress(), request.getDestinationPort());
				exchangesByMID.remove(midKey);
			}
			if (exchange.getOrigin() == Origin.REMOTE) {
				Request request = exchange.getCurrentRequest();
				if (request != null) {
					KeyUri uriKey = new KeyUri(request.getURI(),
							request.getSource().getAddress(), request.getSourcePort());
					LOGGER.fine("Exchange completed, forget blockwise transfer to URI "+uriKey);
					ongoingExchanges.remove(uriKey);
				}
				// TODO: What if the request is only a block?
				// TODO: This should only happen if the transfer was blockwise

				Response response = exchange.getResponse();
				if (response != null) {
					KeyMID midKey = new KeyMID(response.getMID(), 
							response.getDestination().getAddress(), response.getDestinationPort());
					exchangesByMID.remove(midKey);
				}
				
			}
		}
		
	}
	
}
