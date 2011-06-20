package layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
 * - Generation of tokens
 *   
 *   
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */

public class TransactionLayer extends UpperLayer {
	
	public TransactionLayer() {
		// member initialization
		// TODO randomize initial token?
		this.currentToken = 0xCAFE;
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
			
			// associate token with request
			tokenMap.put(tokenOpt.getDisplayValue(), request);
			
		}
		sendMessageOverLowerLayer(msg);
	}	
	
	@Override
	protected void doReceiveMessage(Message msg) {

		// retrieve token option
		Option tokenOpt = msg.getFirstOption(OptionNumberRegistry.TOKEN);
		
		if (msg instanceof Response) {

			Response response = (Response) msg;
			
			Request request = null;
			
			if (tokenOpt != null) {
				
				// retrieve request corresponding to token
				//int token = tokenOpt.getIntValue();
				request = tokenMap.get(tokenOpt.getDisplayValue());
				
				/*if (request == null) {
					System.out.printf("[%s] WARNING: Unexpected response, Token=0x%x\n",
						getClass().getName(), token);
				}*/
			} else {
				// no token option present (blame server)
				
				Log.warning(this, "Token missing for matching response to request");
				
				// try to use buddy for matching response to request
				if (response.getBuddy() instanceof Request) {
					
					request = (Request)response.getBuddy();

					Log.info(this, "Falling back to buddy matching for %s", response.key());
				}
			}
			
			// check if received response needs confirmation
			if (response.isConfirmable()) {
				try {
					// reply with ACK if response matched to request,
					// otherwise reply with RST
					
					Message reply = response.newReply(request != null);

					sendMessageOverLowerLayer(reply);

				} catch (IOException e) {
					Log.error(this, "Failed to reply to confirmable response %s: %s",
						response.key(), e.getMessage());
				}
			}

			if (request != null) {
				
				// attach request to response
				response.setRequest(request);
			} else {
				
				// log unsuccessful matching
				/*System.out.printf("[%s] ERROR: Failed to match response to request:\n",
					getClass().getName());
				response.log();*/
			}
			
		} else if (msg instanceof Request) {
			
			// incoming request: 
			if (tokenOpt != null) {
				tokenMap.put(tokenOpt.getDisplayValue(), (Request) msg);
			}
		}

		deliverMessage(msg);
	}
	
	private Map<String, Request> tokenMap
		= new HashMap<String, Request>();

	private int currentToken;
}
