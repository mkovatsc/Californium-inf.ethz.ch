package layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import util.Log;
import util.Properties;
import coap.BlockOption;
import coap.CodeRegistry;
import coap.Message;
import coap.Option;
import coap.OptionNumberRegistry;
import coap.Request;
import coap.Response;
import coap.TokenManager;

/*
 * This class describes the functionality of a CoAP transfer layer. It provides:
 * 
 * - Support for block-wise transfers using BLOCK1 and BLOCK2 options
 *
 *   
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
//TODO ETag matching

public class TransferLayer extends UpperLayer {

	private Map<String, Message> incomplete = new HashMap<String, Message>();
	private Map<String, Integer> awaiting = new HashMap<String, Integer>();

	private TokenManager tokenManager;
	
	// default block size used for the transfer
	private int defaultSZX;
	
	// Constructors ////////////////////////////////////////////////////////////
	
	/*
	 * Constructor for a new TransferLayer
	 * 
	 * @param defaultBlockSize The default block size used for block-wise transfers
	 *                         or -1 to disable outgoing block-wise transfers
	 */
	public TransferLayer(TokenManager tokenManager, int defaultBlockSize) {
		
		this.tokenManager = tokenManager;
		
		if (defaultBlockSize > 0) {
		
			defaultSZX = BlockOption.encodeSZX(defaultBlockSize);
			if (defaultSZX < 0 || defaultSZX > 8) {
				
				Log.warning(this, "Unsupported block size %d, using %d instead", 
					defaultBlockSize, Properties.std.getInt("DEFAULT_BLOCK_SIZE"));
				
				defaultSZX = BlockOption.encodeSZX(Properties.std.getInt("DEFAULT_BLOCK_SIZE"));
			}
			
		} else {
			// disable outgoing blockwise transfers
			defaultSZX = -1;
		}
	}
	
	public TransferLayer(TokenManager tokenManager) {
		this(tokenManager, Properties.std.getInt("DEFAULT_BLOCK_SIZE"));
	}

	// I/O implementation //////////////////////////////////////////////////////
	
	@Override
	protected void doSendMessage(Message msg) throws IOException {
		
		int sendSZX = defaultSZX;
		int sendNUM = 0;
		
		// block size negotiation
		if (msg.isResponse() && ((Response)msg).getRequest()!=null) {
			BlockOption buddyBlock = (BlockOption) ((Response)msg).getRequest().getFirstOption(OptionNumberRegistry.BLOCK2);
			if (buddyBlock!=null) {
				if (buddyBlock.getSZX()<defaultSZX) {
					sendSZX = buddyBlock.getSZX();
				}
				sendNUM = buddyBlock.getNUM();
			}
		}
		
		// check if message needs to be split up
		if (
			BlockOption.validSZX(sendSZX) && 
			msg.payloadSize() > BlockOption.decodeSZX(sendSZX)
		) {			
			// split message up using block1 for requests and block2 for responses
			
			if (msg.requiresToken()) {
				msg.setToken(tokenManager.acquireToken(false));
			}
			
			Message block = getBlock(msg, sendNUM, sendSZX);
			
			if (block!=null) {
				// send block and wait for reply
				sendMessageOverLowerLayer(block);
				
				// store if not complete
				if (((BlockOption)block.getFirstOption(OptionNumberRegistry.BLOCK2)).getM()) {
					incomplete.put(msg.transferID(), msg); //TODO timeout to clean up incomplete Map after a while
					Log.info(this, "Transfer cached for %s", msg.transferID());
				} else {
					Log.info(this, "Blockwise transfer complete | %s", msg.transferID());
				}
				
				// update timestamp
				msg.setTimestamp(block.getTimestamp());
			} else {
				handleOutOfScopeError(msg);
			}
			
		} else {
			// send complete message
			sendMessageOverLowerLayer(msg);
		}
	}
	
	@Override
	protected void doReceiveMessage(Message msg) {
		
		BlockOption block1 = 
			(BlockOption) msg.getFirstOption(OptionNumberRegistry.BLOCK1);
		BlockOption block2 = 
			(BlockOption) msg.getFirstOption(OptionNumberRegistry.BLOCK2);
		
		Message first = incomplete.get(msg.transferID());
		
		if (block1 == null && block2 == null && !msg.requiresBlockwise()) {
			if (first instanceof Request && msg instanceof Response) {
				if (((Response)msg).getRequest()==null) {
					Log.error(this, "Received unmatched response | %s", msg.key());
				}
			}
			
			deliverMessage(msg);
		
		} else if (msg.isRequest() && (block1 != null || msg.requiresBlockwise())) {
			// handle incoming payload using block1
			
			if (msg.requiresBlockwise()) {
				Log.info(this, "Requesting blockwise transfer | %s", msg.key());
				
				if (first!=null) {
					incomplete.remove(msg.transferID());
					Log.error(this, "Resetting incomplete transfer | %s", msg.key());
				}
				
				block1 = new BlockOption(msg.isRequest() ? OptionNumberRegistry.BLOCK1 : OptionNumberRegistry.BLOCK2, 0, BlockOption.encodeSZX(Properties.std.getInt("DEFAULT_BLOCK_SIZE")), true);
			}
			
			Log.info(this, "Incoming payload, block1");
			
			handleIncomingPayload(msg, block1);
				
		} else if (msg.isRequest() && block2 != null) {
			// send blockwise response
				
			Log.info(this, "Block request received : %s | %s", block2.getDisplayValue(), msg.key());

			if (first == null) {
				// get current representation
				Log.info(this, "New blockwise transfer | %s", msg.transferID());
				deliverMessage(msg);
				
			} else {
				// use cached representation
				
				Message resp = getBlock(first, block2.getNUM(), block2.getSZX());
				
				if (resp!=null) {

					// update message ID
					resp.setID(msg.getID());
					
					BlockOption respBlock = (BlockOption)resp.getFirstOption(OptionNumberRegistry.BLOCK2);
					
					try {
						sendMessageOverLowerLayer(resp);
						Log.info(this, "Block request responded: %s | %s", respBlock.getDisplayValue(), resp.key());
						
					} catch (IOException e) {
						Log.error(this, "Failed to send block response: %s", e.getMessage());
					}
					
					
					// remove transfer context if completed
					if (!respBlock.getM()) {
						incomplete.remove(msg.transferID());
						Log.info(this, "Blockwise transfer complete | %s", resp.transferID());
					}
				} else {
					handleOutOfScopeError(msg.newReply(true));
				}
			}
			
		} else if (msg.isResponse() && block1 != null) {
			// handle blockwise acknowledgement
			
			if (first != null) {
				
				if (!msg.isReset()) {
				
					// send next block
					Message block = getBlock(first, block1.getNUM() + 1, block1.getSZX());
					try {
						sendMessageOverLowerLayer(block);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					return;
					
				} else {
					// cancel transfer
					
					Log.info(this, "Block-wise transfer cancelled by peer (RST): %s", msg.transferID());
					//partialOut.remove(msg.transferID());
					incomplete.remove(msg.transferID());
					
					deliverMessage(msg);
				}
			} else {
				Log.warning(this, "Unexpected reply in blockwise transfer dropped: %s", msg.key());
				//return;
			}
			
		} else if (msg.isResponse() && block2 != null) {
		
			// handle incoming payload using block2
			Log.info(this, "Incoming payload, block2");
			handleIncomingPayload(msg, block2);
			
		}
	}
	
	private void handleIncomingPayload(Message msg, BlockOption blockOpt) {
		
		Message initial = incomplete.get(msg.transferID());
		
		if (initial != null) {
			
			// compare block offsets
			if (blockOpt.getNUM()*blockOpt.getSize()==awaiting.get(msg.transferID())*((BlockOption) initial.getFirstOption(OptionNumberRegistry.BLOCK1)).getSize() ) {
								
				// append received payload to first response and update message ID
				initial.appendPayload(msg.getPayload());
				initial.setID(msg.getID());
				
				awaiting.put(msg.transferID(), blockOpt.getNUM()+1);
				
				// update info
				initial.setID(msg.getID());
				initial.setOption(blockOpt);
				
				Log.info(this, "Block received : %s", blockOpt.getDisplayValue());
			} else {
				Log.warning(this, "Wrong block received : %s", blockOpt.getDisplayValue());
			}
			
		} else if (blockOpt.getNUM()==0 && msg.payloadSize()>0) {
			
			//System.out.println("New transfer: NUM " + blockOpt.getNUM() + " DIV " + msg.payloadSize()/BlockOption.decodeSZX(blockOpt.getSZX()));
			
			// calculate next block num from received payload length
			int size = BlockOption.decodeSZX(blockOpt.getSZX());
			int num = (msg.payloadSize() / size) - 1;
			blockOpt.setNUM(num);
			msg.setOption(blockOpt);
			
			// crop payload
			byte[] newPayload = new byte[(num+1)*size];
	        System.arraycopy(msg.getPayload(), 0, newPayload, 0, newPayload.length);
			msg.setPayload(newPayload);
			
			System.out.println("New transfer: NEW " + msg.payloadSize());
			
			// create new transfer context
			initial = msg;
			incomplete.put(msg.transferID(), initial);
			awaiting.put(msg.transferID(), blockOpt.getNUM()+1);
			
			Log.info(this, "Transfer initiated for %s", msg.transferID());
		} else {
			Log.error(this, "Transfer started out of order: %s", msg.key());
			handleIncompleteError(msg.newReply(true));
			return;
		}
		
		if (blockOpt.getM()) {
			Message reply = null;

			if (msg instanceof Response) {

				reply = new Request(CodeRegistry.METHOD_GET, msg.isConfirmable());
				reply.setURI(msg.getURI());

				// TODO set Accept

				reply.setOption(msg.getFirstOption(OptionNumberRegistry.TOKEN));
				reply.requiresToken(msg.requiresToken());
				reply.setOption(new BlockOption(OptionNumberRegistry.BLOCK2, awaiting.get(msg.transferID()), blockOpt.getSZX(), false));

			} else if (msg instanceof Request) {

				reply = msg.newReply(true);
				// picked arbitrarily, cannot decide if created or changed without putting resource logic here
				reply.setCode(CodeRegistry.RESP_CREATED);
				
				// echo block option
				reply.addOption(blockOpt);
			} else {
				Log.error(this, "Unsupported message type: %s", msg.key());
				return;
			}

			try {
				sendMessageOverLowerLayer(reply);
	
				BlockOption replyBlock = (BlockOption)reply.getFirstOption(blockOpt.getOptionNumber());
				Log.info(this, "Block replied: %s, %s", reply.key(), replyBlock.getDisplayValue());

			} catch (IOException e) {
				Log.error(this, "Failed to request block: %s", e.getMessage());
			}
		} else {
			deliverMessage(initial);
			incomplete.remove(msg.transferID());
			awaiting.remove(msg.transferID());
		}
	}
	
	private void handleOutOfScopeError(Message resp) {
		
		resp.setCode(CodeRegistry.RESP_BAD_REQUEST);
		resp.setPayload("BlockOutOfScope");
		
		try {
			sendMessageOverLowerLayer(resp);
			Log.info(this, "Out-of-scope block request rejected | %s", resp.key());
			
		} catch (IOException e) {
			Log.error(this, "Failed to send error message: %s", e.getMessage());
		}
	}
	
	private void handleIncompleteError(Message resp) {
		
		resp.setCode(CodeRegistry.RESP_REQUEST_ENTITY_INCOMPLETE);
		resp.setPayload("Start with block num 0");
		
		try {
			sendMessageOverLowerLayer(resp);
			Log.info(this, "Incomplete request rejected | %s", resp.key());
			
		} catch (IOException e) {
			Log.error(this, "Failed to send error message: %s", e.getMessage());
		}
	}
	
	
	// Static Methods //////////////////////////////////////////////////////////

	private static Message getBlock(Message msg, int num, int szx) {
		
		int blockSize = 1 << (szx + 4);
		int payloadOffset = num * blockSize;
		int payloadLeft = msg.payloadSize() - payloadOffset;
		
		if (payloadLeft > 0) {
			Message block = null;
			try {
				block = msg.getClass().newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			
			block.setID(msg.getID());
			block.setType(msg.getType());
			block.setCode(msg.getCode());
			
			// use same options
			for (Option opt : msg.getOptionList()) {
				block.addOption(opt);
			}
			
			block.setURI(msg.getURI());
			
			block.requiresToken(msg.requiresToken());
			
			// calculate 'more' bit 
			boolean m = blockSize < payloadLeft;
			
			// limit block size to size of payload left
			if (!m) {
				blockSize = payloadLeft;
			}
			
			// copy payload block
			
			byte[] blockPayload = new byte[blockSize];
			System.arraycopy(msg.getPayload(), payloadOffset, 
				blockPayload, 0, blockSize);
			
			block.setPayload(blockPayload);
			
			Option blockOpt = null;
			if (msg.isRequest()) {
				blockOpt = new BlockOption(OptionNumberRegistry.BLOCK1, num, szx, m);
			} else {
				blockOpt = new BlockOption(OptionNumberRegistry.BLOCK2, num, szx, m);
			}
			block.setOption(blockOpt);
			
			return block;
			
		} else {
			return null;
		}
	}
}
