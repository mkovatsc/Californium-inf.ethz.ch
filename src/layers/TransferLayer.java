package layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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

public class TransferLayer extends UpperLayer {
	
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
		
		// check if message needs to be split up
		if (
			BlockOption.validSZX(defaultSZX) && 
			msg.payloadSize() > BlockOption.decodeSZX(defaultSZX)
		) {
			
			// split message up using block1 for requests and block2 for responses
			
			if (msg.needsToken()) {
				msg.setToken(tokenManager.acquireToken(false));
			}
			
			Message block = getBlock(msg, 0, defaultSZX);
			
			//partialOut.put(msg.transferID(), msg);
			incomplete.put(msg.transferID(), msg);
			
			Log.info(this, "Transfer initiated for %s", msg.transferID());
			
			// send only first block and wait for reply
			sendMessageOverLowerLayer(block);
			
			// update timestamp
			msg.setTimestamp(block.getTimestamp());
			
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
		
		if (block1 == null && block2 == null) {
			if (first instanceof Request && msg instanceof Response) {
				((Response)msg).setRequest((Request)first);
				
				// TODO complete transfer
			}
			deliverMessage(msg);
		}
		
		if (block2 != null && msg.isRequest()) {

			// send blockwise response
			
			Log.info(this, "Block request received : %s | %s", block2.getDisplayValue(), msg.key());

			//Message first = partialOut.get(msg.transferID());
			//Message first = incomplete.get(msg.transferID());
			if (first != null) {
				
				Message resp = getBlock(first, block2.getNUM(), block2.getSZX());
				
				// echo request ID
				resp.setID(msg.getID());
				
				try {
					sendMessageOverLowerLayer(resp);
					
					BlockOption respBlock = (BlockOption)resp.getFirstOption(OptionNumberRegistry.BLOCK2);
					Log.info(this, "Block request responded: %s | %s", respBlock.getDisplayValue(), resp.key());
					
				} catch (IOException e) {
					Log.error(this, "Failed to send block response: %s", e.getMessage());
				}
				
				
				// TODO remove transfer context if completed
				
				// do not propagate blockwise requests
				//return;
				
			} else {
				Log.error(this, "Missing transfer context for %s", msg.transferID());
			}
		}
		
		if 	(block2 != null && msg.isResponse()) {
			// handle incoming payload using block2
			Log.info(this, "Incoming payload, block2");
			handleIncomingPayload(msg, block2);
			
			/*Response response = (Response) msg;
			
			Message initial = incomplete.get(msg.transferID());
			if (initial != null) {
				
				// append received payload to first response
				initial.appendPayload(msg.getPayload());
				
				BlockOption respBlock = (BlockOption)msg.getFirstOption(OptionNumberRegistry.BLOCK2);
				Log.info(this, "Block received : %s", 
					respBlock.getDisplayValue());
				
			} else {
				
				// create new transfer context
				initial = msg;
				incomplete.put(msg.transferID(), initial);
				
				Log.info(this, "Transfer initiated for %s", msg.transferID());
			}
		
			// check "More" bit
			if (block2.getM()) {
			
				// more data available
				
				// request next block
				Message req = split(response.getRequest(), 
					block2.getNUM() + 1, block2.getSZX());
				try {
					sendMessageOverLowerLayer(req);
					
					BlockOption reqBlock = (BlockOption)req.getFirstOption(OptionNumberRegistry.BLOCK2);
					Log.info(this, "Block requested: %s", reqBlock.getDisplayValue());
					
				} catch (IOException e) {
					Log.error(this, "Failed to request block: %s", e.getMessage());
				}
			
				// do not deliver message
				// until transfer complete
				//return;
				
			} else {
				
				// transfer complete
				Log.info(this, "Transfer completed: %s", msg.transferID());
				
				// remove block option
				initial.removeOption(OptionNumberRegistry.BLOCK2);
				deliverMessage(initial);
				
				//return;
				
			}*/
			
		}
		
		if 	(block1 != null && msg.isRequest()) {
			// handle incoming payload using block1
			Log.info(this, "Incoming payload, block1");
			
			handleIncomingPayload(msg, block1);
		}
		
		if (block1 != null && msg.isResponse()) {
			// handle blockwise acknowledgement
			
			//Message first = partialOut.get(msg.transferID());
			//Message first = incomplete.get(msg.transferID());
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
		}
		
		//deliverMessage(msg);
	}

	private void handleIncomingPayload(Message msg, BlockOption blockOpt) {
		
		Message initial = incomplete.get(msg.transferID());
		if (initial != null) {
			
			// append received payload to first response
			initial.appendPayload(msg.getPayload());
			
			Log.info(this, "Block received : %s", 
				blockOpt.getDisplayValue());
			
		} else {
			
			// create new transfer context
			initial = msg;
			incomplete.put(msg.transferID(), initial);
			
			Log.info(this, "Transfer initiated for %s", msg.transferID());
		}
	
		if (blockOpt.getM()) {
			Message reply = null;
			
			if (msg instanceof Response) {
				
				// more data available
				// request next block
				reply = split(((Response)msg).getRequest(), 
						blockOpt.getNUM() + 1, blockOpt.getSZX());
				
			} else if (msg instanceof Request){
				
				reply = msg.newReply(true);
				
				// set provisional code, as final response code not yet known
				reply.setCode(CodeRegistry.RESP_CHANGED);
				
				// echo block option
				// TODO Aliasing?
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
		
		} else  {
			
			// transfer complete
			Log.info(this, "Transfer completed: %s", msg.transferID());
			
			if (msg.isRequest()) {
				//initial.setID(msg.getID());
				msg.setPayload(initial.getPayload());
				initial = msg;
			}
			
			// remove block option
			initial.removeOption(blockOpt.getOptionNumber());
			deliverMessage(initial);
			
			return;
			
		}

	}
	
	
	// Static Methods //////////////////////////////////////////////////////////
	
	private static Message split(Message msg, int num, int szx) {
		
		Message block = null;
		try {
			block = msg.getClass().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		block.setType(msg.getType());
		block.setCode(msg.getCode());
		block.setURI(msg.getURI());

		// TODO set options (Content-Type, Max-Age etc)	
		
		block.setOption(msg.getFirstOption(OptionNumberRegistry.TOKEN));
		block.setNeedsToken(msg.needsToken());
		block.setOption(new BlockOption(OptionNumberRegistry.BLOCK2, num, szx, false));
		
		return block;
	}

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
			// TODO Aliasing?
			for (Option opt : msg.getOptionList()) {
				block.addOption(opt);
			}
			
			block.setURI(msg.getURI());
			
			block.setNeedsToken(msg.needsToken());
			
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
	

	private Map<String, Message> incomplete
		= new HashMap<String, Message>();

	private TokenManager tokenManager;
	
	// default block size used for the transfer
	private int defaultSZX;

}
