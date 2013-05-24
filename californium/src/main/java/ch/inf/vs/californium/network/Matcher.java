package ch.inf.vs.californium.network;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Message;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

public class Matcher {

	private final static Logger LOGGER = Logger.getLogger(Matcher.class.getName());
	
	private HandlerBrokerChannelIrgendwas handler;
	
	// Why do we give NCON messages a MID?
	// TODO: Make per endpoint
	private static AtomicInteger currendMID = new AtomicInteger(1); 
	
	private ConcurrentHashMap<String, Exchange> exchangesByMID; // Outgoing
	private ConcurrentHashMap<String, Exchange> exchangesByToken;
	// TODO: Multicast Exchanges
	
	private ConcurrentHashMap<String, Exchange> incommingMessages; // for deduplication
	private ConcurrentHashMap<String, Exchange> ongoingExchanges; // for blockwise
	
	public Matcher(HandlerBrokerChannelIrgendwas handler) {
		this.handler = handler;
		this.exchangesByMID = new ConcurrentHashMap<>();
		this.exchangesByToken = new ConcurrentHashMap<>();
		this.incommingMessages = new ConcurrentHashMap<>();
		this.ongoingExchanges = new ConcurrentHashMap<>();
	}
	
	public void sendRequest(Exchange exchange, Request request) {
		LOGGER.info("send request");
		assert(exchange != null && request != null);
		if (request.getMid() == Message.NONE)
			request.setMid(currendMID.getAndIncrement());

		/*
		 * The request is a CON or NCON and must be prepared for these responses
		 * - CON  => ACK/RST/ACK+response/CON+response/NCON+response
		 * - NCON => RST/CON+response/NCON+response
		 * If this request goes lost, we do not get anything back.
		 */
		
		String idByMID = getExchangeByMIDIdentifier(
				request.getDestination(), request.getDestinationPort(), request.getMid());
		String idByTok = getExchangeByTokenIdentifier(
				request.getDestination(), request.getDestinationPort(), request.getToken());

		exchangesByMID.put(idByMID, exchange);
		exchangesByToken.put(idByTok, exchange);
	}

	public void sendResponse(Exchange exchange, Response response) {
		assert(exchange != null && response != null);
		if (response.getMid() == Message.NONE)
			response.setMid(currendMID.getAndIncrement());
		
		/*
		 * The response is a CON or NCON or ACK and must be prepared for these
		 * - CON  => ACK/RST // we only care to to stop retransmission
		 * - NCON => RST // we don't care
		 * - ACK  => nothing!
		 * If this response goes lost, we must be prepared to get the same 
		 * CON/NCON request with same MID again. We then find the corresponding
		 * exchange and the retransmissionlayer resends this response.
		 */
		
		if (response.getType() == Type.CON) {
			String idByMID = getExchangeByMIDIdentifier(
					response.getDestination(), response.getDestinationPort(), response.getMid());
			exchangesByMID.put(idByMID, exchange);
		}
		
	}

	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		assert(message != null); // exchange might be null (for sending RSTs)
		/*
		 * We do not expect any response for an empty message TODO: Should we
		 * set the MID? The exchange might be null if we receive a response with
		 * a token for which no exchange is to be found.
		 */
		if (message.getMid() == Message.NONE)
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
		
		String idByTok = getExchangeByTokenIdentifier(
				request.getSource(), request.getSourcePort(), request.getToken());

		String idByMID = getExchangeByMIDIdentifier(
				request.getSource(), request.getSourcePort(), request.getMid());

		if (!request.getOptions().hasBlock1() || request.getOptions().getBlock1().getNum()==0) {
			// This request starts a new exchange
			Exchange exchange = new Exchange(request, false);
			
			Exchange previous = incommingMessages.putIfAbsent(idByMID, exchange);
			if (previous == null) {
				ongoingExchanges.put(idByTok, exchange);
				return exchange;
			} else {
				request.setDuplicate(true);
				return previous;
			}
			
		} else {
			// This is a block of an ongoing request
			Exchange ongoing = ongoingExchanges.get(idByTok);
			if (ongoing != null) {
				return ongoing;
			} else {
				// We have no ongoing exchange for that block. (This only
				// happens if the client is broken)
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

		String idByTok = getExchangeByTokenIdentifier(
				response.getSource(), response.getSourcePort(), response.getToken());
		Exchange exchange = exchangesByToken.get(idByTok);
		LOGGER.info("Searched for exchange with idByTok="+idByTok+" and found "+exchange);
		
		if (exchange != null) {
			// There is an exchange with the given token. But is it a duplicate?
			String idByMID = getExchangeByMIDIdentifier(
					response.getSource(), response.getSourcePort(), response.getMid());
			Exchange prev = incommingMessages.putIfAbsent(idByMID, exchange);
			if (prev != null) { // (and prev is the same as exchange)
				response.setDuplicate(true);
			}
			
			if (response.getType() == Type.ACK) { 
				// this is a piggy-backed response and the MID must match
				if (exchange.getCurrentRequest().getMid() == response.getMid()) {
					// The token and MID match. This is a response for this exchange
					return exchange;
					
				} else {
					// The token matches but not the MID. This is a response for an older exchange
					// TODO ignore or reject?
					LOGGER.info("Token matches but not MID: wants "+exchange.getCurrentRequest().getMid()+" but gets "+response.getMid());
					EmptyMessage rst = EmptyMessage.newRST(response);
					sendEmptyMessage(exchange, rst);
					response.setIgnored(true);
					return null;
				}
				
			} else {
				// this is a seperate response that we can deliver
				return exchange;
			}
			
		} else {
			// There is no exchange with the given token. We have no use for a
			// response we have not requested.
			EmptyMessage rst = EmptyMessage.newRST(response);
			sendEmptyMessage(exchange, rst);
			response.setIgnored(true);
			return null;
		}
	}

	public Exchange receiveEmptyMessage(EmptyMessage message) {
		assert(message != null);
		
		String idByMID = getExchangeByMIDIdentifier(
				message.getSource(), message.getSourcePort(), message.getMid());
		Exchange exchange = exchangesByMID.get(idByMID);
		
		if (exchange != null) {
			return exchange;
		} else {
			message.setIgnored(true);
			return null;
		}
		// else, this is an ACK for unknown exchange and we ignore it
	}
	
	private String getExchangeByMIDIdentifier(InetAddress remoteAddr, int remotePort, int mid) {
		return remoteAddr.getHostAddress()+":"+remotePort+":"+mid;
	}
	
	private String getExchangeByTokenIdentifier(InetAddress remoteAddr, int remotePort, byte[] token) {
		if (token == null) throw new NullPointerException("Token must not be null");
		if (remoteAddr == null) throw new NullPointerException("Remote address must not be null");
		return remoteAddr.getHostAddress()+":"+remotePort+":"+new String(token);
	}
}
