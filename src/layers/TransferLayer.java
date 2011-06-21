package layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import util.Log;

import coap.BlockOption;
import coap.Message;
import coap.Option;
import coap.OptionNumberRegistry;
import coap.Response;

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
	
	// the default block size if not changed by user
	// must be power of two between 16 and 1024
	public static int DEFAULT_BLOCK_SIZE = 512; 
	
	// Constructors ////////////////////////////////////////////////////////////
	
	/*
	 * Constructor for a new TransferLayer
	 * 
	 * @param defaultBlockSize The default block size used for block-wise transfers
	 *                         or -1 to disable outgoing block-wise transfers
	 */
	public TransferLayer(int defaultBlockSize) {
		
		if (defaultBlockSize > 0) {
		
			defaultSZX = BlockOption.encodeSZX(defaultBlockSize);
			if (defaultSZX < 0 || defaultSZX > 8) {
				
				Log.warning(this, "Unsupported block size %d, using %d instead", 
					defaultBlockSize, DEFAULT_BLOCK_SIZE);
				
				defaultSZX = BlockOption.encodeSZX(DEFAULT_BLOCK_SIZE);
			}
			
		} else {
			// disable outgoing blockwise transfers
			defaultSZX = -1;
		}
	}
	
	public TransferLayer() {
		this(DEFAULT_BLOCK_SIZE);
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
			
			Message block = getBlock(msg, 0, defaultSZX);
			
			partialOut.put(msg.transferID(), msg);
			
			Log.info(this, "Transfer initiated for %s", msg.transferID());
			
			// send only first block and wait for reply
			sendMessageOverLowerLayer(block);
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
		
		if (block2 != null && msg.isRequest()) {

			// send blockwise response
			
			Log.info(this, "Block request received : %s | %s", block2.getDisplayValue(), msg.key());

			Message first = partialOut.get(msg.transferID());
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
				return;
				
			} else {
				Log.error(this, "Missing transfer context for %s", msg.transferID());
			}
		}
		
		if 	(block2 != null && msg.isResponse()) {
			// handle incoming payload using block2
			
			Response response = (Response) msg;
			
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
				return;
				
			} else {
				
				// transfer complete
				Log.info(this, "Transfer completed: %s", msg.transferID());
				
				// remove block option
				initial.removeOption(OptionNumberRegistry.BLOCK2);
				deliverMessage(initial);
				
				return;
				
			}
			
		}
		
		if 	(block1 != null && msg.isRequest()) {
			// handle incoming payload using block1
		}
		
		if (block1 != null && msg.isResponse()) {
			// handle blockwise acknowledgement
		}
		
		deliverMessage(msg);
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
			block.setType(msg.getType());
			block.setCode(msg.getCode());
			block.setURI(msg.getURI());
			
			
			// use same options
			// TODO Aliasing?
			for (Option opt : msg.getOptionList()) {
				block.addOption(opt);
			}
			
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
	
/*	private boolean isPayloadMsg(Message msg) {
		return 
			msg instanceof POSTRequest ||
			msg instanceof PUTRequest ||
			(msg instanceof Response && ((Response)msg).getRequest() instanceof GETRequest);
	}*/

	private Map<String, Message> incomplete
		= new HashMap<String, Message>();
	
	private Map<String, Message> partialOut
	= new HashMap<String, Message>();
	
	// default block size used for the transfer
	private int defaultSZX;

}
