package layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import util.Log;
import util.Properties;

import coap.CodeRegistry;
import coap.Message;

/*
 * This class describes the functionality of a CoAP message layer. It provides:
 * 
 * - Reliable transport of Confirmable messages over underlying layers
 *   by making use of retransmissions and exponential backoff
 *   
 * - Matching of Confirmables to their corresponding Acknowledgement/Reset
 *   
 * - Detection and cancellation of duplicate messages 
 * 
 * - Retransmission of Acknowledgements/Reset messages upon receiving duplicate
 *   Confirmable messages
 *   
 *   
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class MessageLayer extends UpperLayer {

	// Nested Classes //////////////////////////////////////////////////////////

	/*
	 * Entity class to keep state of retransmissions
	 */
	private static class TxContext {
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

		RetransmitTask(TxContext ctx) {
			this.context = ctx;
		}

		@Override
		public void run() {
			handleResponseTimeout(context);
		}

		private TxContext context;
	}

	// Constructors ////////////////////////////////////////////////////////////

	public MessageLayer() {

		// initialize members
		// TODO Randomize initial message ID?
		this.messageID = 0x1D00;
	}

	// I/O implementation //////////////////////////////////////////////////////

	@Override
	protected void doSendMessage(Message msg) throws IOException {

		// set message ID
		if (msg.getID() < 0) {
			msg.setID(nextMessageID());
		}

		// check if message needs confirmation, i.e. a reply is expected
		if (msg.isConfirmable()) {

			// create new transmission context
			// to keep track of the Confirmable
			TxContext ctx = addTransmission(msg);

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
			TxContext ctx = getTransmission(msg);

			if (ctx != null) {

				// match reply to corresponding Confirmable
				Message.matchBuddies(ctx.msg, msg);

				// transmission completed
				removeTransmission(ctx);

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

	private void handleResponseTimeout(TxContext ctx) {

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

				removeTransmission(ctx);

				return;
			}

			// schedule next retransmission
			scheduleRetransmission(ctx);

		} else {

			// cancel transmission
			removeTransmission(ctx);

			Log.warning(this, "Transmission of %s cancelled", ctx.msg.key());

			// invoke event handler method
			ctx.msg.handleTimeout();
		}
	}

	private synchronized TxContext addTransmission(Message msg) {

		if (msg != null) {

			// initialize new transmission context
			TxContext ctx = new TxContext();
			ctx.msg = msg;
			ctx.numRetransmit = 0;
			ctx.retransmitTask = null;

			// add context to context table
			txTable.put(msg.getID(), ctx);

			return ctx;
		}

		return null;
	}

	private synchronized TxContext getTransmission(Message msg) {

		// retrieve context from context table
		return msg != null ? txTable.get(msg.getID()) : null;
	}

	private synchronized void removeTransmission(TxContext ctx) {

		if (ctx != null) {

			// cancel any pending retransmission schedule
			ctx.retransmitTask.cancel();
			ctx.retransmitTask = null;

			// remove context from context table
			txTable.remove(ctx.msg.getID());
		}
	}

	private void scheduleRetransmission(TxContext ctx) {

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

	// Attributes //////////////////////////////////////////////////////////////

	// Timer used to schedule retransmissions
	private Timer timer = new Timer(true); // run as daemon

	// Table used to store context for outgoing messages
	private Map<Integer, TxContext> txTable = new HashMap<Integer, TxContext>();

	// Cache used to detect duplicates of incoming messages
	private MessageCache dupCache = new MessageCache();

	// Cache used to retransmit replies to incoming messages
	private MessageCache replyCache = new MessageCache();

	// ID attached to outgoing messages
	private int messageID;

}
