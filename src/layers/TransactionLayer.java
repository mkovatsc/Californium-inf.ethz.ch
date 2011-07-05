package layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import util.Log;

import coap.Message;
import coap.Option;
import coap.OptionNumberRegistry;
import coap.Request;
import coap.Response;

/*
 * This class describes the functionality of a CoAP transaction layer. It provides:
 * 
 * - Matching of responses to the according requests
 * 
 * - Transaction timeouts, e.g. to limit wait time for separate responses
 *   and responses to non-confirmable requests 
 *   
 * - Generation of transaction tokens
 *   
 *   
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */

public class TransactionLayer extends UpperLayer {
	
	// Default transaction timeout. This is application-specific and not
	// defined by the CoAP draft
	public static final int DEFAULT_TRANSACTION_TIMEOUT = 5000; // ms
	
	// Nested Classes //////////////////////////////////////////////////////////
	
	/*
	 * Entity class to keep state of transactions
	 */
	private static class Transaction {
		public String token;
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
	
	public TransactionLayer(int transactionTimeout) {
		// member initialization
		// TODO randomize initial token?
		this.transactionTimeout = transactionTimeout;
		this.currentToken = 0xCAFE;
	}
	
	public TransactionLayer() {
		this(DEFAULT_TRANSACTION_TIMEOUT);
	}

	// I/O implementation //////////////////////////////////////////////////////
	
	@Override
	protected void doSendMessage(Message msg) throws IOException {
		
		Option tokenOpt = msg.getFirstOption(OptionNumberRegistry.TOKEN); 
		
		// set token option if required
		if (msg.needsToken()) {
			tokenOpt = new Option(currentToken, OptionNumberRegistry.TOKEN);
			msg.setOption(tokenOpt);
			
			System.err.println("+++ Token set: " + tokenOpt.getDisplayValue());
			
			// compute next token
			++currentToken;
		}
		
		if (msg instanceof Request) {
			Request request = (Request) msg;
			
			addTransaction(request);
			
		}
		sendMessageOverLowerLayer(msg);
	}	
	
	@Override
	protected void doReceiveMessage(Message msg) {

		// retrieve token option
		Option tokenOpt = msg.getFirstOption(OptionNumberRegistry.TOKEN);
		String token = tokenOpt != null ? tokenOpt.getDisplayValue() : null;
		
		if (msg instanceof Response) {

			Response response = (Response) msg;
			
			Transaction transaction = getTransaction(token);

			// check for missing token
			if (transaction == null && token == null) {
				
				Log.warning(this, "Remote endpoint failed to echo token");
				
				for (Transaction t : transactions.values()) {
					if (response.getID() == t.request.getID()) {
						transaction = t;
						Log.warning(this, "Falling back to buddy matching");
						break;
					}
				}
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
				
				// TODO When to remove transaction? Multicasts, observations etc.
				//removeTransaction(token);
				
				deliverMessage(msg);
				
				
			} else {
				Log.warning(this, "Dropping unexpected response: %s", token);
			}
			
		} else if (msg instanceof Request) {
			
			// incoming request: 
			/*if (tokenOpt != null) {
				//tokenMap.put(tokenOpt.getDisplayValue(), (Request) msg);
				addTransaction((Request) msg);
			}*/
			
			deliverMessage(msg);
		}

		
	}
	
	private Transaction addTransaction(Request request) {
		
		// get token
		Option tokenOpt = request.getFirstOption(OptionNumberRegistry.TOKEN);
		String token = tokenOpt != null ? tokenOpt.getDisplayValue() : null;
		
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
	
	private Transaction getTransaction(String token) {
		return transactions.get(token);
	}
	
	private void removeTransaction(String token) {
		
		Transaction transaction = transactions.remove(token);
		
		transaction.timeoutTask.cancel();
		transaction.timeoutTask = null;
	}
	
	private void transactionTimedOut(Transaction transaction) {
		
		// cancel transaction
		Log.warning(this, "Transaction timed out: %s", transaction.token);
		removeTransaction(transaction.token);
		
		// call event handler
		transaction.request.handleTimeout();
	}
	
	private Map<String, Transaction> transactions
		= new HashMap<String, Transaction>();


	
	// timer for scheduling transaction timeouts
	private Timer timer
		= new Timer(true);
	
	// time to wait for transactions to complete, in milliseconds
	private int transactionTimeout;
	
	// token used to initiate the next transaction
	private int currentToken;
	
}
