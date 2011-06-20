package layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import coap.GETRequest;
import coap.Message;
import coap.Option;
import coap.OptionNumberRegistry;
import coap.POSTRequest;
import coap.PUTRequest;
import coap.Request;
import coap.Response;

public class TransferLayer extends UpperLayer {
	
	private static int DEFAULT_BLOCK_SIZE = 512;

	public static void decodeBlock(Option blockOpt) {
		int value = blockOpt.getIntValue();
		
		int szx = value      & 0x7;
		int m   = value >> 3 & 0x1;
		int num = value >> 4      ;
		
		int size = 1 << (szx + 4);
		System.out.printf("NUM: %d, SZX: %d (%d bytes), M: %d", num, szx, size, m);
	}
	
	public static Option encodeBlock(int num, int szx, int m) {
		int value = 0;
		
		value |= (szx & 0x7)     ;
		value |= (m   & 0x1) << 3;
		value |= num         << 4;
		
		return new Option(value, OptionNumberRegistry.BLOCK);
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
			
			// TODO set options (Content-Type, Max-Age etc)
			
			// calculate 'more' bit 
			int m = blockSize < payloadLeft ? 1 : 0;
			
			// limit block size to size of payload left
			if (m == 0) {
				blockSize = payloadLeft;
			}
			
			// copy payload block
			
			byte[] blockPayload = new byte[blockSize];
			System.arraycopy(msg.getPayload(), payloadOffset, 
				blockPayload, 0, blockSize);
			
			block.setPayload(blockPayload);
			
			// calculate block option
			
			int value = 0;
			
			value |= (szx & 0x7)     ;
			value |= (m   & 0x1) << 3;
			value |= num         << 4;

			Option blockOpt = null;
			if (msg.isRequest()) {
				blockOpt = new Option(value, OptionNumberRegistry.BLOCK1);
			} else {
				blockOpt = new Option(value, OptionNumberRegistry.BLOCK2);
			}
			block.setOption(blockOpt);
			
			return block;
			
		} else {
			return null;
		}
	}
	
	@Override
	protected void doSendMessage(Message msg) throws IOException {
		
		if (msg.payloadSize() > DEFAULT_BLOCK_SIZE) {
			
			// split message up using block1 for requests and block2 for responses
			
			Message block = getBlock(msg, 0, 5);
			partialOut.put(block, msg);
			
			// send only first block and wait for reply
			sendMessageOverLowerLayer(block);

		} else {
			
			// send complete message
			sendMessageOverLowerLayer(msg);
		}
	}
	
	@Override
	protected void doReceiveMessage(Message msg) {
		
		// check Block option
		Option blockOpt = msg.getFirstOption(OptionNumberRegistry.BLOCK);
		
		Option block1 = msg.getFirstOption(OptionNumberRegistry.BLOCK1);
		Option block2 = msg.getFirstOption(OptionNumberRegistry.BLOCK2);
		
		if (block2 != null && msg.isRequest()) {
			// send blockwise response
		}
		
		if 	(block2 != null && msg.isResponse()) {
			// handle incoming payload using block2
		}
		
		if 	(block1 != null && msg.isRequest()) {
			// handle incoming payload using block1
		}
		
		if (block1 != null && msg.isResponse()) {
			// handle blockwise acknowledgement
		}
		
		if (blockOpt != null) {

			int value = blockOpt.getIntValue();
			
			int szx = value      & 0x7;
			int m   = value >> 3 & 0x1;
			int num = value >> 4      ;
			
			int size = 1 << (szx + 4);
			
			// retrieve token option
			Option tokenOpt = msg.getFirstOption(OptionNumberRegistry.TOKEN);
			if (tokenOpt != null) {
				
				int token = tokenOpt.getIntValue();
				
				if (m != 0) {
					// transfer not yet complete

					// check if message is a response to a GET request
					if (msg instanceof Response) {
						if ( true || ((Response)msg).getRequest() instanceof GETRequest) {
							
							// request the next block
							
							Request request = new GETRequest();
							request.setOption(tokenOpt);
							request.setOption(encodeBlock(num+1, szx, 0));
							
							try {
								sendMessageOverLowerLayer(request);
							} catch (IOException e) {
								System.out.printf("[%s] Failed to request next block for T%d: %s\n",
									getClass().getName(), token, e.getMessage());
							}
						}
					}
				
				}

				if (num == 0 && m != 0) {
					// first block: add to set of incomplete transfers
					incomplete.put(token, msg);
					
					System.out.printf("[%s] Blockwise transfer of T%d initiated\n", 
						getClass().getName(), token);
				} else {
					Message first = incomplete.get(token);

					System.out.printf("[%s] Receiving block #%d of T%d\n", 
						getClass().getName(), token);
					
					// append payload of this block to the message 
					// that initiated the transfer
					first.appendPayload(msg.getPayload());
					
					if (m == 0) {
						// complete the transfer
						
						System.out.printf("[%s] Blockwise transfer of T%d completed\n", 
							getClass().getName(), token);

						incomplete.remove(token);
						first.setComplete(true);
					}
				}
				
				if (m == 0) {
					
				}
				
				
			} else {
				System.out.printf("[%s] ERROR: Token missing for blockwise receiving of %s\n",
					getClass().getName(), msg.key());
			}
			
		}

		deliverMessage(msg);
	}
	
	private boolean isPayloadMsg(Message msg) {
		return 
			msg instanceof POSTRequest ||
			msg instanceof PUTRequest ||
			(msg instanceof Response && ((Response)msg).getRequest() instanceof GETRequest);
	}

	private Map<Integer, Message> incomplete
		= new HashMap<Integer, Message>();
	
	private Map<Message, Message> partialOut
	= new HashMap<Message, Message>();

}
