package ch.inf.vs.californium.network;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import ch.inf.vs.californium.CalifonriumLogger;
import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Message;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange.KeyMID;
import ch.inf.vs.californium.network.Exchange.KeyToken;
import ch.inf.vs.californium.network.Exchange.Origin;
import ch.inf.vs.californium.network.dedupl.Deduplicator;
import ch.inf.vs.californium.network.dedupl.MarkAndSweep;

public class Matcher {

	private final static Logger LOGGER = CalifonriumLogger.getLogger(Matcher.class);
	
	private boolean started;
//	private MarkAndSweep markAndSweep;
	private ExchangeObserver exchangeObserver = new ExchangeObserverImpl();
	
	private RawDataChannel handler;
	
	/** The executor. */
	private ScheduledExecutorService executor;
	
	// TODO: Make per endpoint
	private AtomicInteger currendMID = new AtomicInteger(new Random().nextInt(1<<16)); 
//	private AtomicInteger currendMID = new AtomicInteger(100);
	
	private ConcurrentHashMap<KeyMID, Exchange> exchangesByMID; // Outgoing
	private ConcurrentHashMap<KeyToken, Exchange> exchangesByToken;
	// TODO: Multicast Exchanges
	
	private ConcurrentHashMap<KeyToken, Exchange> ongoingExchanges; // for blockwise
//	private ConcurrentHashMap<KeyMID, Exchange> incommingMessages; // for deduplication
	
	private Deduplicator deduplicator;
	// Idea: Only store acks/rsts and not the whole exchange. Responses should be sent CON.
	
	public Matcher(RawDataChannel handler, NetworkConfig config) {
		this.handler = handler;
		this.started = false;
		this.exchangesByMID = new ConcurrentHashMap<>();
		this.exchangesByToken = new ConcurrentHashMap<>();
		this.ongoingExchanges = new ConcurrentHashMap<>();

		this.deduplicator = new MarkAndSweep(config);
//		this.deduplicator = new CropRotation();
//		this.deduplicator = new NoDeduplicator();
	}
	
	public synchronized void start() {
		if (started) return;
		else started = true;
		if (executor == null)
			throw new IllegalStateException("Matcher has no executor to schedule exchnage removal");
//		markAndSweep.schedule();
		deduplicator.start();
	}
	
	public synchronized void stop() {
		if (!started) return;
		else started = false;
//		markAndSweep.cancel();
		deduplicator.stop();
		clear();
	}
	
	public synchronized void setExecutor(ScheduledExecutorService executor) {
//		markAndSweep.cancel();
		deduplicator.setExecutor(executor);
		this.executor = executor;
//		if (started)
//			markAndSweep.schedule();
	}
	
	public void sendRequest(Exchange exchange, Request request) {
		assert(exchange != null && request != null);
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
		
//		exchange.addMIDKey(idByMID);
//		exchange.addTokenKey(idByTok);
		exchange.setObserver(exchangeObserver);
		
		exchangesByMID.put(idByMID, exchange);
		exchangesByToken.put(idByTok, exchange);
	}

	public void sendResponse(Exchange exchange, Response response) {
		assert(exchange != null && response != null);
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
		
		// Insert CON and NON to match ACKs and RSTs to the exchange
		KeyMID idByMID = new KeyMID(response.getMID(), 
				response.getDestination().getAddress(), response.getDestinationPort());
		exchangesByMID.put(idByMID, exchange);
		
		if (exchange.getCurrentRequest().getCode() == Code.GET
				&& response.getOptions().hasBlock2()) {
			// Remember ongoing blockwise GET requests
			Request request = exchange.getRequest();
			KeyToken idByTok = new KeyToken(request.getToken(),
					request.getSource().getAddress(), request.getSourcePort());
			ongoingExchanges.put(idByTok, exchange);
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
		assert(message != null); // exchange might be null (for sending RSTs)
		
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
		assert(request != null); // This is the lowest layer so there is no Exchange yet
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
		
		KeyToken idByTok = new KeyToken(request.getToken(),
				request.getSource().getAddress(), request.getSourcePort());
		
		// until 24.7.2013
//		if (!request.getOptions().hasBlock1() || request.getOptions().getBlock1().getNum()==0) {
		
		if (!(
				(request.getOptions().hasBlock1() && request.getOptions().getBlock1().getNum()!=0)
				|| (request.getOptions().hasBlock2() && request.getOptions().getBlock2().getNum()!=0)
			) ) {
			if (Server.LOG_ENABLED)
				LOGGER.fine("Create new exchange for remote request");
			// This request starts a new exchange
			Exchange exchange = new Exchange(request, Origin.REMOTE);
			
//			Exchange previous = incommingMessages.putIfAbsent(idByMID, exchange);
			Exchange previous = deduplicator.findPrevious(idByMID, exchange);
			if (previous == null) {
//				exchange.addTokenKey(idByTok);
				if (request.getOptions().hasBlock1()) {
					ongoingExchanges.put(idByTok, exchange); // TODO: optimize: Only insert if blockwise
				}
				return exchange;
				
			} else {
				request.setDuplicate(true);
				return previous;
			}
			
		} else {
			if (Server.LOG_ENABLED)
				LOGGER.fine("Lookup ongoing exchange");
			// This is a block of an ongoing request
			Exchange ongoing = ongoingExchanges.get(idByTok);
			if (ongoing != null) {
				
//				Exchange prev = incommingMessages.putIfAbsent(idByMID, ongoing);
				Exchange prev = deduplicator.findPrevious(idByMID, ongoing);
				if (prev != null) {
					request.setDuplicate(true);
				}
				return ongoing;
		
			} else {
				// We have no ongoing exchange for that block. 
				// This might be a duplicate request of an already completed exchange
//				Exchange prev = incommingMessages.get(idByMID);
				Exchange prev = deduplicator.find(idByMID);
				if (prev != null) {
					request.setDuplicate(true);
					return prev;
				}
				
				// This request fits no exchange
				EmptyMessage rst = EmptyMessage.newACK(request);
				handler.sendEmptyMessage(null, rst);
				request.setIgnored(true);
				return null;
			}
		}
	}

	public Exchange receiveResponse(Response response) {
		assert(response != null); // This is the lowest layer so there is no Exchange yet
		
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
			// There is an exchange with the given token. But is it a duplicate?

			if (response.getType() != Type.ACK) {
				// Need deduplication for CON and NON but not for ACK (MID defined by server)
//				Exchange prev = incommingMessages.putIfAbsent(idByMID, exchange);
				Exchange prev = deduplicator.findPrevious(idByMID, exchange);
				if (prev != null) { // (and thus it holds: prev == exchange)
					response.setDuplicate(true);
				}
			}
			
			if (response.getType() == Type.ACK) { 
				// this is a piggy-backed response and the MID must match
				if (exchange.getCurrentRequest().getMID() == response.getMID()) {
					// The token and MID match. This is a response for this exchange
					return exchange;
					
				} else {
					// The token matches but not the MID. This is a response for an older exchange
					// TODO ignore or reject?
					LOGGER.info("Token matches but not MID: wants "+exchange.getCurrentRequest().getMID()+" but gets "+response.getMID());
					EmptyMessage rst = EmptyMessage.newRST(response);
					sendEmptyMessage(exchange, rst);
					response.setIgnored(true);
					return null;
				}
				
			} else {
				// this is a separate response that we can deliver
				return exchange;
			}
			
		} else {
			// There is no exchange with the given token.
			// This might be a duplicate of an exchanges that is already completed
			
			if (response.getType() != Type.ACK) {
				// Need deduplication for CON and NON but not for ACK (MID defined by server)
//				Exchange prev = incommingMessages.get(idByMID);
				Exchange prev = deduplicator.find(idByMID);
				if (prev != null) { // (and thus it holds: prev == exchange)
					response.setDuplicate(true);
					return prev;
				}
			}
			
			// This is a totally unexpected response.
			EmptyMessage rst = EmptyMessage.newRST(response);
			sendEmptyMessage(exchange, rst);
			response.setIgnored(true);
			return null;
		}
	}

	public Exchange receiveEmptyMessage(EmptyMessage message) {
		assert(message != null);
		
		KeyMID idByMID = new KeyMID(message.getMID(),
				message.getSource().getAddress(), message.getSourcePort());
		
		Exchange exchange = exchangesByMID.get(idByMID);
		
		if (exchange != null) {
			return exchange;
		} else {
			LOGGER.info("Matcher received empty message that does not match any exchange: "+message);
			message.setIgnored(true);
			return null;
		} // else, this is an ACK for unknown exchange and we ignore it
	}
	
	public void clear() {
		this.exchangesByMID.clear();
		this.exchangesByToken.clear();
//		this.incommingMessages.clear();
		this.ongoingExchanges.clear();
		deduplicator.clear();
	}
	
	private class ExchangeObserverImpl implements ExchangeObserver {

		@Override
		public void completed(Exchange exchange) {
			if (exchange.getOrigin() == Origin.LOCAL) {
				Request request = exchange.getRequest(); // TODO: What if only block?
				KeyToken tokKey = new KeyToken(exchange.getToken(),
						request.getDestination().getAddress(), request.getDestinationPort());
				exchangesByToken.remove(tokKey);
//				for (KeyToken tokKey:exchange.getTokenKeys())
//					exchangesByToken.remove(tokKey);
				
				KeyMID midKey = new KeyMID(request.getMID(), 
						request.getDestination().getAddress(), request.getDestinationPort());
				exchangesByMID.remove(midKey);
			}
			if (exchange.getOrigin() == Origin.REMOTE) {
				Request request = exchange.getRequest(); // TODO: What if only block?
				KeyToken tokKey = new KeyToken(request.getToken(),
						request.getSource().getAddress(), request.getSourcePort());
				ongoingExchanges.remove(tokKey);
//				for (KeyToken tokKey:exchange.getTokenKeys())
//					ongoingExchanges.remove(tokKey);
				
				Response response = exchange.getResponse();
				KeyMID midKey = new KeyMID(response.getMID(), 
						response.getDestination().getAddress(), response.getDestinationPort());
				exchangesByMID.remove(midKey);
				
				// TODO: Testing!! Exchange is still in deduplictor!
//				OptionSetManager.restore(request.getOptions());
//				request.getOptions().clear();
//				request.setOptions(null);
				
//				exchange.setRequest(null);
//				exchange.setResponse(null);
//				exchange.setCurrentRequest(null);
//				exchange.setCurrentResponse(null);
			}
//			for (KeyMID midKey:exchange.getMIDKeys()) {
//				exchangesByMID.remove(midKey);
//			}
		}
		
	}
	
}
