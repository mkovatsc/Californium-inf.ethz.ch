/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.ObservingManager;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.UnsupportedRequest;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class TransactionLayer provides the functionality of the CoAP messaging
 * layer as a subclass of {@link UpperLayer}. It introduces reliable transport
 * of confirmable messages over underlying layers by making use of
 * retransmissions and exponential backoff, matching of confirmables to their
 * corresponding ACK/RST, detection and cancellation of duplicate messages,
 * retransmission of ACK/RST messages upon receiving duplicate confirmable
 * messages.
 * 
 * @author Matthias Kovatsch
 */
public class TransactionLayer extends UpperLayer {

// Static attributes ///////////////////////////////////////////////////////////

	/** The message ID used for newly generated messages. */
	private static int currentMID = (int) (Math.random() * 0x10000);

	/**
	 * Returns the next message ID to use out of the consecutive 16-bit range.
	 * 
	 * @return the current message ID
	 */
	public static int nextMessageID() {

		currentMID = ++currentMID % 0x10000;

		return currentMID;
	}
	
// Members /////////////////////////////////////////////////////////////////////

	/** The timer daemon to schedule retransmissions. */
	private Timer timer = new Timer(true); // run as daemon

	/** The Table to store the transactions of outgoing messages. */
	private Map<String, Transaction> transactionTable = new HashMap<String, Transaction>();

	/** The cache for duplicate detection. */
	private MessageCache dupCache = new MessageCache();

	// Cache used to retransmit replies to incoming messages
	private MessageCache replyCache = new MessageCache();

// Nested Classes //////////////////////////////////////////////////////////////

	/**
	 * Entity class to keep state of retransmissions.
	 */
	private static class Transaction {
		Message msg;
		RetransmitTask retransmitTask;
		int numRetransmit;
		int timeout; // to satisfy RESPONSE_RANDOM_FACTOR
	}

	/**
	 * The MessageCache is a utility class used for duplicate detection and
	 * reply retransmissions. It is a ring buffer whose size is configured
	 * through the Californium properties file. 
	 */
	@SuppressWarnings("serial")
	private static class MessageCache extends LinkedHashMap<String, Message> {

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Message> eldest) {
			return size() > Properties.std.getInt("MESSAGE_CACHE_SIZE");
		}

	}

	/**
	 * Utility class to handle timeouts.
	 */
	private class RetransmitTask extends TimerTask {

		private Transaction transaction;

		RetransmitTask(Transaction transaction) {
			this.transaction = transaction;
		}

		@Override
		public void run() {
			handleResponseTimeout(transaction);
		}
	}

// Static methods //////////////////////////////////////////////////////////////

	/**
	 * Calculates the initial timeout for outgoing confirmable messages.
	 * 
	 * @Return the timeout in milliseconds
	 */
	private static int initialTimeout() {
		
		final double min = Properties.std.getDbl("RESPONSE_TIMEOUT");
		final double f = Properties.std.getDbl("RESPONSE_RANDOM_FACTOR");
		
		return (int) (min + (min * (f - 1d) * Math.random()));
	}
	
// Constructors ////////////////////////////////////////////////////////////////

	public TransactionLayer() {
	}

// I/O implementation //////////////////////////////////////////////////////////

	@Override
	protected void doSendMessage(Message msg) throws IOException {

		// set message ID
		if (msg.getMID() < 0) {
			msg.setMID(nextMessageID());
		}
		
		// check if message needs confirmation, i.e., a reply is expected
		if (msg.isConfirmable()) {

			// create new transmission context for retransmissions
			addTransaction(msg);

		} else if (msg.isReply()) {

			// put message into ring buffer in case peer retransmits
			replyCache.put(msg.transactionKey(), msg);
		}

		// send message over unreliable channel
		sendMessageOverLowerLayer(msg);
	}

	@Override
	protected void doReceiveMessage(Message msg) {
		
		// check if supported
		if (msg instanceof UnsupportedRequest) {
			try {
				Message reply = msg.newReply(msg.isConfirmable());
				reply.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
				reply.setPayload(String.format("Method code %d not supported.", msg.getCode()));
				sendMessageOverLowerLayer(reply);
				LOG.info(String.format("Replied to unsupported request code %d: %s", msg.getCode(), msg.key()));
			} catch (IOException e) {
				LOG.severe(String.format("Replying to unsupported request code failed: %s\n%s", msg.key(), e.getMessage()));
			}
			return;
		}

		// check for duplicate
		if (dupCache.containsKey(msg.key())) {

			// check for retransmitted Confirmable
			if (msg.isConfirmable()) {

				// retrieve cached reply
				Message reply = replyCache.get(msg.transactionKey());
				if (reply != null) {

					// retransmit reply
					try {
						sendMessageOverLowerLayer(reply);
						LOG.info(String.format("Replied to duplicate confirmable: %s", msg.key()));
					} catch (IOException e) {
						LOG.severe(String.format("Replying to duplicate confirmable failed: %s\n%s", msg.key(), e.getMessage()));
					}
				} else {
					LOG.info(String.format("Dropped duplicate confirmable without cached reply: %s", msg.key()));
				}

				// drop duplicate anyway
				return;

			} else {

				// ignore duplicate
				LOG.info(String.format("Dropped duplicate: %s", msg.key()));
				return;
			}

		} else {

			// cache received message
			dupCache.put(msg.key(), msg);
		}

		// check for reply to CON and remove transaction
		if (msg.isReply()) {

			// retrieve transaction for the incoming message
			Transaction transaction = getTransaction(msg);

			if (transaction != null) {

				// transmission completed
				removeTransaction(transaction);
				
				if (msg.isEmptyACK()) {
					
					// transaction is complete, no information for higher layers
					return;
					
				} else if (msg.getType()==Message.messageType.RST) {
					
					handleIncomingReset(msg);
					return;
				}
				
			} else if (msg.getType()==Message.messageType.RST) {
				
				handleIncomingReset(msg);
				return;

			} else {
				
				// ignore unexpected reply except RST, which could match to a NON sent by the endpoint
				LOG.warning(String.format("Dropped unexpected reply: %s", msg.key()));
				return;
			}
		}
		
		// Only accept Responses here, Requests must be handled at application level 
		if (msg instanceof Response && msg.isConfirmable()) {
			try {
				LOG.info(String.format("Accepted confirmable response: %s", msg.key()));
				sendMessageOverLowerLayer(msg.newAccept());
			} catch (IOException e) {
				LOG.severe(String.format("Accepting confirmable failed: %s\n%s", msg.key(), e.getMessage()));
			}
		}

		// pass message to registered receivers
		deliverMessage(msg);
	}

	// Internal ////////////////////////////////////////////////////////////////

	private void handleIncomingReset(Message msg) {
		
		// remove possible observers
		ObservingManager.getInstance().removeObserver(msg.getPeerAddress().toString(), msg.getMID());
	}

	private void handleResponseTimeout(Transaction transaction) {

		final int max = Properties.std.getInt("MAX_RETRANSMIT");
		
		// check if limit of retransmissions reached
		if (transaction.numRetransmit < max) {

			// retransmit message
			transaction.msg.setRetransmissioned(++transaction.numRetransmit); 

			LOG.info(String.format("Retransmitting %s (%d of %d)", transaction.msg.key(), transaction.numRetransmit, max));

			try {
				sendMessageOverLowerLayer(transaction.msg);
			} catch (IOException e) {

				LOG.severe(String.format("Retransmission failed: %s", e.getMessage()));
				removeTransaction(transaction);

				return;
			}

			// schedule next retransmission
			scheduleRetransmission(transaction);

		} else {

			// cancel transmission
			removeTransaction(transaction);
			
			// cancel observations
			ObservingManager.getInstance().removeObserver(transaction.msg.getPeerAddress().toString());

			// invoke event handler method
			transaction.msg.handleTimeout();
		}
	}

	private synchronized Transaction addTransaction(Message msg) {

		// initialize new transmission context
		Transaction transaction = new Transaction();
		transaction.msg = msg;
		transaction.numRetransmit = 0;
		transaction.retransmitTask = null;

		transactionTable.put(msg.transactionKey(), transaction);

		// schedule first retransmission
		scheduleRetransmission(transaction);
		
		LOG.finest(String.format("Stored new transaction for %s", msg.key()));

		return transaction;
	}

	private synchronized Transaction getTransaction(Message msg) {
		return transactionTable.get(msg.transactionKey());
	}

	private synchronized void removeTransaction(Transaction transaction) {

		// cancel any pending retransmission schedule
		transaction.retransmitTask.cancel();
		transaction.retransmitTask = null;

		// remove transaction from table
		transactionTable.remove(transaction.msg.transactionKey());
		
		LOG.finest(String.format("Cleared transaction for %s", transaction.msg.key()));
	}

	private void scheduleRetransmission(Transaction transaction) {

		// cancel existing schedule (if any)
		if (transaction.retransmitTask != null) {
			transaction.retransmitTask.cancel();
		}

		// create new retransmission task
		transaction.retransmitTask = new RetransmitTask(transaction);

		// calculate timeout using exponential back-off
		if (transaction.timeout == 0) {
			// use initial timeout
			transaction.timeout = initialTimeout();
		} else {
			// double timeout
			transaction.timeout *= 2;
		}

		// schedule retransmission task
		timer.schedule(transaction.retransmitTask, transaction.timeout);
	}
	
	public String getStats() {
		StringBuilder stats = new StringBuilder();

		stats.append("Current message ID: ");
		stats.append(currentMID);
		stats.append('\n');
		stats.append("Open transactions: ");
		stats.append(transactionTable.size());
		stats.append('\n');
		stats.append("Messages sent:     ");
		stats.append(numMessagesSent);
		stats.append('\n');
		stats.append("Messages received: ");
		stats.append(numMessagesReceived);
		
		return stats.toString();
	}
}
