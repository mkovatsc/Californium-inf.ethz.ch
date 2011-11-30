package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.util.Log;
import ch.ethz.inf.vs.californium.util.Properties;


/*
 * This class describes the functionality of a CoAP transaction layer. It provides:
 * 
 * - Matching of responses to the according requests
 * 
 * - Transaction timeouts, e.g. to limit wait time for separate responses
 *   and responses to non-confirmable requests 
 *   
 *   
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */

public class TransactionLayer extends UpperLayer {
	
	// Nested Classes //////////////////////////////////////////////////////////
	
	/*
	 * Entity class to keep state of transactions
	 */
	private static class Transaction {
		public Option token;
		public Request request;
		public TimerTask timeoutTask;
	}
	
	/*
	 * Utility class to provide transaction timeouts
	 */
	private class TimeoutTask extends TimerTask {

		public TimeoutTask(Transaction transaction) {
			this.transaction = transaction;
		}
		
		@Override
		public void run() {
			transactionTimedOut(transaction);
		}
		
		private Transaction transaction;
	}
	
	// Constructors ////////////////////////////////////////////////////////////
	
	public TransactionLayer(TokenManager tokenManager, int transactionTimeout) {
		// member initialization
		this.tokenManager = tokenManager;
		this.transactionTimeout = transactionTimeout;
	}
	
	public TransactionLayer(TokenManager tokenManager) {
		this(tokenManager, Properties.std.getInt("DEFAULT_TRANSACTION_TIMEOUT"));
	}

	// I/O implementation //////////////////////////////////////////////////////
	
	@Override
	protected void doSendMessage(Message msg) throws IOException { 
		
		// set token option if required
		if (msg.requiresToken()) {
			msg.setToken( tokenManager.acquireToken(true) );
		}
		
		// use overall timeout for clients (e.g., server crash after separate response ACK)
		if (msg instanceof Request) {
			addTransaction((Request) msg);
		}
		
		sendMessageOverLowerLayer(msg);
	}	
	
	@Override
	protected void doReceiveMessage(Message msg) {

		if (msg instanceof Response) {

			Response response = (Response) msg;

			// retrieve token option
			Option token = response.getToken();
			
			Transaction transaction = getTransaction(token);

			// check for missing token
			if (transaction == null && token == null) {
				
				Log.error(this, "Remote endpoint failed to echo token");
				
				/* Not good, must consider IP and port, too.
				 * 
				for (Transaction t : transactions.values()) {
					if (response.getID() == t.request.getID()) {
						transaction = t;
						Log.warning(this, "Falling back to buddy matching");
						break;
					}
				}
				*/
				
				// let timeout handle the problem
				return;
			}
			
			// check if received response needs confirmation
			if (response.isConfirmable()) {
				try {
					// reply with ACK if response matched to transaction,
					// otherwise reply with RST
					
					Message reply = response.newReply(transaction != null);

					sendMessageOverLowerLayer(reply);

				} catch (IOException e) {
					Log.error(this, "Failed to reply to confirmable response %s: %s",
						response.key(), e.getMessage());
				}
			}

			
			if (transaction != null) {
				
				// attach request to response
				response.setRequest(transaction.request);
				
				// cancel timeout
				if (!response.isEmptyACK()) {
					transaction.timeoutTask.cancel();
				}
				
				// TODO separate observe registry
				if (msg.getFirstOption(OptionNumberRegistry.OBSERVE)==null) {
					removeTransaction(token);
				}
				
				deliverMessage(msg);
				
				
			} else {
				//TODO send RST
				Log.warning(this, "Dropping unexpected response: %s", token.getDisplayValue());
			}
			
		} else if (msg instanceof Request) {
			deliverMessage(msg);
		}

		
	}
	
	private Transaction addTransaction(Request request) {
		
		// get token
		Option token = request.getToken();
		
		// create new Transaction
		Transaction transaction = new Transaction();
		transaction.token = token;
		transaction.request = request;
		transaction.timeoutTask = new TimeoutTask(transaction);
		
		// associate token with Transaction
		transactions.put(transaction.token, transaction);
		
		timer.schedule(transaction.timeoutTask, transactionTimeout);
		
		return transaction;
	}
	
	private Transaction getTransaction(Option token) {
		return token != null ? transactions.get(token) : null;
	}
	
	private void removeTransaction(Option token) {
		
		Transaction transaction = transactions.remove(token);
		
		transaction.timeoutTask.cancel();
		transaction.timeoutTask = null;
	}
	
	private void transactionTimedOut(Transaction transaction) {
		
		// cancel transaction
		Log.warning(this, "Transaction timed out: %s", transaction.token.getDisplayValue());
		removeTransaction(transaction.token);
		
		// call event handler
		transaction.request.handleTimeout();
	}
	
	private Map<Option, Transaction> transactions
		= new HashMap<Option, Transaction>();


	
	// timer for scheduling transaction timeouts
	private Timer timer
		= new Timer(true);
	
	// time to wait for transactions to complete, in milliseconds
	private int transactionTimeout;
	
	private TokenManager tokenManager;
}
