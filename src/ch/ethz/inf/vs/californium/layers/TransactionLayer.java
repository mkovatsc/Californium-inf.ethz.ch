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
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.endpoint.Endpoint;
import ch.ethz.inf.vs.californium.util.Log;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class TransactionLayer provides the functionality of a CoAP message layer
 * as a subclass of {@link UpperLayer}. It introduces reliable transport of
 * confirmable messages over underlying layers by making use of retransmissions
 * and exponential backoff, matching of confirmables to their corresponding
 * ACK/RST, detection and cancellation of duplicate messages, retransmission of
 * ACK/RST messages upon receiving duplicate confirmable messages.
 * 
 * @author Matthias Kovatsch
 */
public class TransactionLayer extends UpperLayer {

	// Members //////////////////////////////////////////////////////////////

	// Timer used to schedule retransmissions
	private Timer timer = new Timer(true); // run as daemon

	// Table used to store context for outgoing messages
	private Map<Integer, Transaction> transactionTable = new HashMap<Integer, Transaction>();

	// Cache used to detect duplicates of incoming messages
	private MessageCache dupCache = new MessageCache();

	// Cache used to retransmit replies to incoming messages
	private MessageCache replyCache = new MessageCache();

	// ID attached to outgoing messages
	private int messageID;

	// Nested Classes //////////////////////////////////////////////////////////

	/*
	 * Entity class to keep state of retransmissions
	 */
	private static class Transaction {
		Message msg;
		RetransmitTask retransmitTask;
		int numRetransmit;
		int timeout;
	}

	/*
	 * Utility class used for duplicate detection and reply retransmissions
	 */
	@SuppressWarnings("serial")
	private static class MessageCache extends LinkedHashMap<String, Message> {

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Message> eldest) {
			return size() > Properties.std.getInt("MESSAGE_CACHE_SIZE");
		}

	}

	/*
	 * Utility class used to notify the Communicator class about timed-out
	 * replies
	 */
	private class RetransmitTask extends TimerTask {

		private Transaction context;

		RetransmitTask(Transaction ctx) {
			this.context = ctx;
		}

		@Override
		public void run() {
			handleResponseTimeout(context);
		}
	}

	// Constructors ////////////////////////////////////////////////////////////

	public TransactionLayer() {

		// initialize members
		this.messageID = (int) (Math.random() * 0x10000);
	}

	// I/O implementation //////////////////////////////////////////////////////

	@Override
	protected void doSendMessage(Message msg) throws IOException {

		// set message ID
		if (msg.getMID() < 0) {
			msg.setMID(nextMessageID());
		}

		// check if message needs confirmation, i.e. a reply is expected
		if (msg.isConfirmable()) {

			// create new transmission context
			// to keep track of the Confirmable
			Transaction ctx = addTransmission(msg);

			// schedule first retransmission
			scheduleRetransmission(ctx);

		} else if (msg.isReply()) {

			replyCache.put(msg.key(), msg);
		}

		// send message over unreliable channel
		sendMessageOverLowerLayer(msg);
	}

	@Override
	protected void doReceiveMessage(Message msg) {

		// check for duplicate
		if (dupCache.containsKey(msg.key())) {

			// check for retransmitted Confirmable
			if (msg.isConfirmable()) {

				// retrieve cached reply
				Message reply = replyCache.get(msg.key());
				if (reply != null) {

					// retransmit reply
					try {
						sendMessageOverLowerLayer(reply);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// ignore duplicate
					Log.info(this, "Replied to duplicate Confirmable: %s", msg.key());
					return;
				}

			} else {

				// ignore duplicate
				Log.info(this, "Duplicate dropped: %s", msg.key());
				return;
			}

		} else {

			// cache received message
			dupCache.put(msg.key(), msg);
		}

		// check for reply to Confirmable
		if (msg.isReply()) {

			// retrieve context to the incoming message
			Transaction ctx = getTransaction(msg);

			if (ctx != null) {

				// match reply to corresponding Confirmable
				Message.matchBuddies(ctx.msg, msg);

				// transmission completed
				removeTransaction(ctx);

			} else if (msg.getType()==Message.messageType.Reset) {
				
				// TODO check observing relationships for last used MID
				
				
			} else {
				// ignore unexpected reply
				Log.warning(this, "Unexpected reply dropped: %s", msg.key());
				return;
			}
		}

		// pass message to registered receivers
		deliverMessage(msg);
	}

	// Internal ////////////////////////////////////////////////////////////////

	private void handleResponseTimeout(Transaction ctx) {

		// check if limit of retransmissions reached
		int max =  Properties.std.getInt("MAX_RETRANSMIT");
		if (ctx.numRetransmit < max) {

			// retransmit message

			++ctx.numRetransmit;

			Log.info(this, "Retransmitting %s (%d of %d)",
				ctx.msg.key(), ctx.numRetransmit, max);

			try {
				sendMessageOverLowerLayer(ctx.msg);
			} catch (IOException e) {

				Log.error(this, "Retransmission failed: %s", e.getMessage());

				removeTransaction(ctx);

				return;
			}

			// schedule next retransmission
			scheduleRetransmission(ctx);

		} else {

			// cancel transmission
			removeTransaction(ctx);

			Log.warning(this, "Transmission of %s cancelled", ctx.msg.key());

			// invoke event handler method
			ctx.msg.handleTimeout();
		}
	}

	private synchronized Transaction addTransmission(Message msg) {

		if (msg != null) {

			// initialize new transmission context
			Transaction ctx = new Transaction();
			ctx.msg = msg;
			ctx.numRetransmit = 0;
			ctx.retransmitTask = null;

			// add context to context table
			transactionTable.put(msg.getMID(), ctx);

			return ctx;
		}

		return null;
	}

	private synchronized Transaction getTransaction(Message msg) {

		// retrieve context from context table
		return msg != null ? transactionTable.get(msg.getMID()) : null;
	}

	private synchronized void removeTransaction(Transaction ctx) {

		if (ctx != null) {

			// cancel any pending retransmission schedule
			ctx.retransmitTask.cancel();
			ctx.retransmitTask = null;

			// remove context from context table
			transactionTable.remove(ctx.msg.getMID());
		}
	}

	private void scheduleRetransmission(Transaction ctx) {

		// cancel existing schedule (if any)
		if (ctx.retransmitTask != null) {
			ctx.retransmitTask.cancel();
		}

		// create new retransmission task
		ctx.retransmitTask = new RetransmitTask(ctx);

		// calculate timeout using exponential backoff
		if (ctx.timeout == 0) {
			// use initial timeout
			ctx.timeout = initialTimeout();
		} else {
			// double timeout
			ctx.timeout *= 2;
		}

		// schedule retransmission task
		timer.schedule(ctx.retransmitTask, ctx.timeout);
	}

	/*
	 * Returns the next message ID to use out of a consecutive range
	 * 
	 * @return The message ID
	 */
	private int nextMessageID() {

		int ID = messageID;

		++messageID;

		// check for wrap-around
		if (messageID > Message.MAX_ID) {
			messageID = 1;
		}

		return ID;
	}

	/*
	 * Calculates the initial timeout for outgoing Confirmable messages.
	 * 
	 * @Return The timeout in milliseconds
	 */
	private static int initialTimeout() {
		
		final int min = Properties.std.getInt("RESPONSE_TIMEOUT");
		final double f = Properties.std.getDbl("RESPONSE_RANDOM_FACTOR");
		
		return rnd(min,	(int) (min * f));
	}

	/*
	 * Returns a random number within a given range.
	 * 
	 * @param min The lower limit of the range
	 * 
	 * @param max The upper limit of the range, inclusive
	 * 
	 * @return A random number from the range [min, max]
	 */
	private static int rnd(int min, int max) {
		return min + (int) (Math.random() * (max - min + 1));
	}
}
