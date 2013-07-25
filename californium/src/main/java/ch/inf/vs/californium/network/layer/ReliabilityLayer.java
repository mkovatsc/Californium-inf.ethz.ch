package ch.inf.vs.californium.network.layer;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Message;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.Exchange.Origin;
import ch.inf.vs.californium.network.NetworkConfig;

public class ReliabilityLayer extends AbstractLayer {
	
	private final static Logger LOGGER = Logger.getLogger(ReliabilityLayer.class.getName());
	
	private Random rand = new Random();
	
	private NetworkConfig config;
	
	public ReliabilityLayer(NetworkConfig config) {
		this.config = config;
	}
	
	@Override
	public void sendRequest(final Exchange exchange, final Request request) {
		assert(exchange != null && request != null);

		if (Server.LOG_ENABLED) 
			LOGGER.fine("Send request, failed transmissions: "+exchange.getFailedTransmissionCount());
		
		if (request.getType() == null)
			request.setType(Type.CON);
		
		if (request.getType() == Type.CON) {
			prepareRetransmission(exchange, new RetransmissionTask(exchange, request) {
				public void retransmitt() {
					sendRequest(exchange, request);
				}
			});
		}
		super.sendRequest(exchange, request);
	}

	@Override
	public void sendResponse(final Exchange exchange, final Response response) {
		assert(exchange != null && response != null);

		if (Server.LOG_ENABLED) 
			LOGGER.fine("Send response, failed transmissions: "+exchange.getFailedTransmissionCount());

		// If a response type is set, we do not mess around with it.
		// Only if none is set, we have to decide for one here.
		
		Type respType = response.getType();
		if (respType == null) {
			Type reqType = exchange.getRequest().getType();
			if (reqType == Type.CON) {
				if (exchange.getRequest().isAcknowledged()) {
					// send separate response
					response.setType(Type.CON);
				} else {
					// send piggy-backed response
					response.setType(Type.ACK);
					response.setMID(exchange.getRequest().getMID());
				}
			} else {
				// send NON response
				response.setType(Type.NON);
			}
			if (Server.LOG_ENABLED)
				LOGGER.fine("Switched response type to "+response.getType()+", (req:"+reqType+")");
		} else if (respType == Type.ACK || respType == Type.RST) {
			response.setMID(exchange.getCurrentRequest().getMID()); // Since 24.07.2013
		}
		
		if (response.getType() == Type.CON) {
			prepareRetransmission(exchange, new RetransmissionTask(exchange, response) {
				public void retransmitt() {
					sendResponse(exchange, response);
				}
			});
		}
		super.sendResponse(exchange, response);
	}
	
	
	private void prepareRetransmission(Exchange exchange, RetransmissionTask task) {
		/*
		 * For a new confirmable message, the initial timeout is set to a
		 * random number between ACK_TIMEOUT and (ACK_TIMEOUT *
		 * ACK_RANDOM_FACTOR)
		 */
		int timeout;
		if (exchange.getFailedTransmissionCount() == 0) {
			int ack_timeout = config.getAckTimeout();
			float ack_random_factor = config.getAckRandomFactor();
			timeout = getRandomTimeout(ack_timeout, (int) (ack_timeout*ack_random_factor));
		} else {
			timeout = config.getAckTimeoutScale() * exchange.getCurrentTimeout();
		}
		exchange.setCurrentTimeout(timeout);
		
		ScheduledFuture<?> f = executor.schedule(task , timeout, TimeUnit.MILLISECONDS);
		exchange.setRetransmissionHandle(f);
	}
	
	@Override
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		assert(exchange != null && message != null);
		super.sendEmptyMessage(exchange, message);
	}

	@Override
	public void receiveRequest(Exchange exchange, Request request) {
		assert(exchange != null && request != null);
		
		if (request.isDuplicate()) {
			// Request is a duplicate, so resend ACK, RST or response
			if (exchange.getCurrentResponse() != null) {
				// Do not restart retransmission cycle
				super.sendResponse(exchange, exchange.getCurrentResponse());
				
			} else if (exchange.getCurrentRequest().isAcknowledged()) {
				EmptyMessage ack = EmptyMessage.newACK(request);
				sendEmptyMessage(exchange, ack);
			
			} else if (exchange.getCurrentRequest().isRejected()) { 
				EmptyMessage rst = EmptyMessage.newRST(request);
				sendEmptyMessage(exchange, rst);
			} else {
				// server has not yet decided, whether to ack or reject request
				ignore(request);
			}

		} else {
			// Request is not a duplicate
			exchange.setCurrentRequest(request);
			super.receiveRequest(exchange, request);
		}
	}

	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		assert(exchange != null && response != null);
		
		exchange.getCurrentRequest().setAcknowledged(true);
		cancelRetransmission(exchange);
		
		if (response.getType() == Type.CON) {
			EmptyMessage ack = EmptyMessage.newACK(response);
			sendEmptyMessage(exchange, ack);
		}
		
		if (response.isDuplicate()) {
			LOGGER.info("response is duplicate and we send a new ack");
			ignore(response);
		} else {
			super.receiveResponse(exchange, response);
		}
	}

	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		assert(exchange != null && message != null);
		exchange.setFailedTransmissionCount(0);
		
		if (message.getType() == Type.ACK) {
			if (exchange.getOrigin() == Origin.LOCAL)
				exchange.getCurrentRequest().setAcknowledged(true);
			else
				exchange.getCurrentResponse().setAcknowledged(true);
		} else if (message.getType() == Type.RST) {
			if (exchange.getOrigin() == Origin.LOCAL)
				exchange.getCurrentRequest().setRejected(true);
			else
				exchange.getCurrentResponse().setRejected(true);
		} else {
			LOGGER.warning("Empty messgae was not ACK nor RST: "+message);
		}
		
		cancelRetransmission(exchange);
		
		super.receiveEmptyMessage(exchange, message);
	}
	
	private int getRandomTimeout(int min, int max) {
		if (min == max) return min;
		return min + rand.nextInt(max - min);
	}
	
	private void cancelRetransmission(Exchange exchange) {
		ScheduledFuture<?> retransmissionHandle = exchange.getRetransmissionHandle();
		if (retransmissionHandle != null) {
			LOGGER.fine("Cancel retransmission");
			retransmissionHandle.cancel(false);
		}
	}
	
	/*
	 * The main reason to create this class was to enable the methods
	 * sendRequest and sendResponse to use the same code for sending messages
	 * but where the retransmission method calls sendRequest and sendResponse
	 * respectively.
	 */
	private abstract class RetransmissionTask implements Runnable {
		
		private Exchange exchange;
		private Message message;
		
		public RetransmissionTask(Exchange exchange, Message message) {
			this.exchange = exchange;
			this.message = message;
		}
		
		@Override
		public void run() {
			try {
				if (message.isAcknowledged()) {
					LOGGER.info("Timeout: message already acknowledged, cancel retransmission of "+message);
					return;
					
				} else if (message.isRejected()) {
					LOGGER.info("Timeout: message already rejected, cancel retransmission of "+message);
					return;
					
				} else if (message.isCanceled()) {
					LOGGER.info("Timeout: canceled, do not retransmit");
					return;
				
				} else if (exchange.getFailedTransmissionCount() + 1 <= config.getMaxRetransmit()) {
					LOGGER.info("Timeout: retransmitt message, failed: "+(exchange.getFailedTransmissionCount() + 1)+", message: "+message);
					exchange.setFailedTransmissionCount(exchange.getFailedTransmissionCount() + 1);
					retransmitt();

				} else {
					LOGGER.info("Timeout: retransmission limit reached, exchange failed, message: "+message);
					exchange.setTimeouted();
					message.setTimeouted(true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public abstract void retransmitt();
	}
	
}
