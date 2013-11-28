package ch.ethz.inf.vs.californium.network.layer;

import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.MessageObserverAdapter;
import ch.ethz.inf.vs.californium.coap.OptionSet;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;


/**
 * Implementation of CoAP's blockwise protocol
 * (http://tools.ietf.org/html/draft-ietf-core-block-12).
 * <p>
 * The following example shows a GET request that is split into three blocks.
 * The server proposes a block size of 128, and the client agrees. The first two
 * ACKs contain 128 bytes of payload each, and third ACK contains between 1 and
 * 128 bytes.
 * 
 * <pre>
 *    CLIENT                                                     SERVER
 *      |                                                            |
 *      | CON [MID=1234], GET, /status                       ------> |
 *      |                                                            |
 *      | <------   ACK [MID=1234], 2.05 Content, 2:0/1/128          |
 *      |                                                            |
 *      | CON [MID=1235], GET, /status, 2:1/0/128            ------> |
 *      |                                                            |
 *      | <------   ACK [MID=1235], 2.05 Content, 2:1/1/128          |
 *      |                                                            |
 *      | CON [MID=1236], GET, /status, 2:2/0/128            ------> |
 *      |                                                            |
 *      | <------   ACK [MID=1236], 2.05 Content, 2:2/0/128          |
 * </pre>
 * 
 * The following example shows an atomic blockwise POST with separate blockwise
 * response.
 * 
 * <pre>
 *    CLIENT                                                     SERVER
 *      |                                                             |
 *      | CON [MID=1234], POST, /soap, 1:0/1/128 ------>              |
 *      |                                                             |
 *      | <------   ACK [MID=1234], 2.01 Created, 1:0/1/128           |
 *      |                                                             |
 *      | CON [MID=1235], POST, /soap, 1:1/1/128 ------>              |
 *      |                                                             |
 *      | <------   ACK [MID=1235], 2.01 Created, 1:1/1/128           |
 *      |                                                             |
 *      | CON [MID=1236], POST, /soap, 1:2/0/128, 2:0/0/64 ------>    |
 *      |                                                             |
 *      | <------   ACK [MID=1236], 2.01 Created, 1:2/0/128, 2:0/1/64 |
 *      |                                                             |
 *      | (initiative changes to server)                              |
 *      |                                                             |
 *      | <------   CON [MID=4713], 2.01 Created, 2:1/1/64            |
 *      |                                                             |
 *      | ACK [MID=4713], 0                           ------>         |
 *      |                                                             |
 *      | <------   CON [MID=4714], 2.01 Created, 2:2/1/64            |
 *      |                                                             |
 *      | ACK [MID=4714], 0                           ------>         |
 *      |                                                             |
 *      | <------   CON [MID=4715], 2.01 Created, 2:3/0/64            |
 *      |                                                             |
 *      | ACK [MID=4715], 0                           ------>         |
 * 
 * </pre>
 * <p>
 * Block size negotiation for GET requests is not implemented yet.
 */
public class BlockwiseLayer extends AbstractLayer {
	
	private final static Logger LOGGER = Logger.getLogger(BlockwiseLayer.class.getName());
	
	private NetworkConfig config;
	
	public BlockwiseLayer(NetworkConfig config) {
		this.config = config;
	}
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		if (request.getCode() == Code.GET)
			sendRequestGET(exchange, request);
		else sendRequestPOSTPUT(exchange, request);
	}

	@Override
	public void sendResponse(Exchange exchange, Response response) {
		if (exchange.getRequest().getCode() == Code.GET)
			sendResponseGET(exchange, response);
		else sendResponsePOSTPUT(exchange, response);
	}

	@Override
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		if (exchange == null) 
			super.sendEmptyMessage(exchange, message);
		else if (exchange.getRequest() == null) 
			super.sendEmptyMessage(exchange, message);
		else if (exchange.getRequest().getCode() == Code.GET)
			sendEmptyMessageGET(exchange, message);
		else sendEmptyMessagePOSTPUT(exchange, message);
	}

	@Override
	public void receiveRequest(Exchange exchange, Request request) {
		if (request.getCode() == Code.GET)
			receiveRequestGET(exchange, request);
		else receiveRequestPOSTPUT(exchange, request);
	}

	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		if (exchange.getRequest().getCode() == Code.GET)
			receiveResponseGET(exchange, response);
		else receiveResponsePOSTPUT(exchange, response);
	}

	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		if (exchange.getRequest().getCode() == Code.GET)
			receiveEmptyMessageGET(exchange, message);
		else receiveEmptyMessagePOSTPUT(exchange, message);
	}

	//////////////////////////////////////////////////
	////////// Blockwise GET implementation //////////
	//////////////////////////////////////////////////
	
	public void sendRequestGET(Exchange exchange, Request request) {
		exchange.setCurrentRequest(request);
		super.sendRequest(exchange, request);
	}

	public void sendResponseGET(Exchange exchange, Response response) {
		boolean blockwise = false;
		BlockwiseStatus status = null;
		
		int maxMsgSize = config.getInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE);
		if (exchange.getRequest().getOptions().hasBlock2()) {
			blockwise = true;
			BlockOption block2 = exchange.getRequest().getOptions().getBlock2();
			LOGGER.fine("Request had block2 option and is sent blockwise. Response: "+response);
			status = new BlockwiseStatus(block2.getNum(), block2.getSzx());
		
		} else if (response.getPayloadSize() > maxMsgSize) {
			blockwise = true;
			LOGGER.fine("Response payload is "+response.getPayloadSize()+" long. Send in blocks. Response: "+response);
			status = new BlockwiseStatus();
		}
		
		if (blockwise) {
			// status must not be null
			
			exchange.setResponseBlockStatus(status);
			if (exchange.getRequest().getOptions().hasBlock2()) {
				// We take the szx the client asks for
				// TODO: also set starting block in case of random access GET
				status.setCurrentSzx(exchange.getRequest().getOptions().getBlock2().getSzx());
			} else {
				// If the client has no preference, we take the server's default value
				int blocksize = config.getInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE);
				status.setCurrentSzx( computeSZX(blocksize) );
			}
			
			// send piggy-backet block
			// TODO: Client might want to change szx
			Response block = extractResponsesBlock(response, status);
			block.setMID(exchange.getCurrentRequest().getMID());
			block.setType(Type.ACK); 
			exchange.setCurrentResponse(block);
			super.sendResponse(exchange, block);

		} else {
			exchange.setCurrentResponse(response);
			super.sendResponse(exchange, response);
		}
	}

	public void sendEmptyMessageGET(Exchange exchange, EmptyMessage message) {
		super.sendEmptyMessage(exchange, message);
	}

	public void receiveRequestGET(Exchange exchange, Request request) {
		if (request.getOptions().hasBlock2()) {
			BlockOption block2 = request.getOptions().getBlock2();
			
			Response response = exchange.getResponse();
			// Check, whether we have already generated a response
			if (response == null) {
				// We first need to generate the response
				if (block2.getNum() != 0) {
					LOGGER.warning("Random access in blockwise layer is not implemented yet");
					exchange.reject();
					return;
				} else {
					// deliver request and cut it later when in blockwise layer again
					exchange.setRequest(request);
					super.receiveRequest(exchange, request);
				}
			} else {
			
				// The response is already generated and the next block must be sent now.
				BlockwiseStatus status = exchange.getResponseBlockStatus();
				LOGGER.fine("Send next block, current status: "+status);
				status.setCurrentNum(block2.getNum());
				Response block = extractResponsesBlock(response, status);
				block.setMID(exchange.getCurrentRequest().getMID());
				block.setType(Type.ACK); 
				exchange.setCurrentResponse(block);
				super.sendResponse(exchange, block);
			}
			
		} else {
			// No blockwise transfer is involved
			exchange.setRequest(request);
			super.receiveRequest(exchange, request);
		}
	}

	public void receiveResponseGET(Exchange exchange, Response response) {
		if (response.getOptions().hasBlock2()) {
			BlockOption block2 = response.getOptions().getBlock2();
			LOGGER.fine("Response has block 2 option " + block2);
			BlockwiseStatus status = exchange.getResponseBlockStatus();
			if (status == null) {
				exchange.getRequest().setAcknowledged(true);
				LOGGER.fine("There is no response assembler status. Create and set one");
				status = new BlockwiseStatus();
				exchange.setResponseBlockStatus(status);
			}
			
			status.addBlock(response.getPayload());
			
			if ( ! block2.isM()) { // this was the last block.
				LOGGER.fine("There are no more blocks to be expected. Deliver response");
				Response assembled = new Response(response.getCode()); // getAssembledResponse(status, response);
				assembleMessage(status, assembled, response);
				assembled.setType(Type.ACK);
				assembled.setAcknowledged(true);
				LOGGER.fine("Assembled response from "+status.getBlockCount()+" blocks: "+assembled);
				super.receiveResponse(exchange, assembled);
			} else {
				LOGGER.fine("We wait for more blocks to come and do not deliver response yet");

				Request request = exchange.getRequest();
				int num = block2.getNum() + 1;
				int szx = block2.getSzx();
				boolean m = false;
				Request block = new Request(Code.GET);
				block.setOptions(new OptionSet(request.getOptions()));
				block.setDestination(request.getDestination());
				block.setDestinationPort(request.getDestinationPort());
				block.setToken(request.getToken());
				block.setType(Type.CON);
				block.getOptions().setBlock2(szx, m, num);
				
				exchange.setCurrentRequest(block);
				super.sendRequest(exchange, block);
				// ignore response
			}
		
		} else {
			// no block2 option => normal response
			exchange.setResponse(response);
			super.receiveResponse(exchange, response);
		}
	}

	public void receiveEmptyMessageGET(Exchange exchange, EmptyMessage message) {
		super.receiveEmptyMessage(exchange, message);
	}
	
	///////////////////////////////////////////////////////////
	////////// Blockwise POST and PUT implementation //////////
	///////////////////////////////////////////////////////////
	
	public void sendRequestPOSTPUT(final Exchange exchange, Request request) {
		int maxMsgSize = config.getInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE);
		if (request.getPayloadSize() > maxMsgSize) {
			LOGGER.info("Request payload is "+request.getPayloadSize()+" long. Since it is larger than "+maxMsgSize+" we send it blockwise");
			BlockwiseStatus status = new BlockwiseStatus();
			exchange.setRequestBlockStatus(status);
			
			int blocksize = config.getInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE);
			LOGGER.info("Default blocksize: "+blocksize);
			status.setCurrentSzx( computeSZX(blocksize) );
			Request block = getRequestBlock(request, status);
			exchange.setCurrentRequest(block);
			
			request.addMessageObserver(new MessageObserverAdapter() {
				@Override public void canceled() {
					exchange.getCurrentRequest().cancel();
				}
			});
			if (request.isCanceled())
				block.cancel();
			
			super.sendRequest(exchange, block);
		
		} else {
			exchange.setCurrentRequest(request);
			super.sendRequest(exchange, request);
		}
	}
	
	public void sendResponsePOSTPUT(Exchange exchange, Response response) {
		// If the request was sent with a block1 option the response has to send its
		// first block piggy-backed with the Block1 option of the last request block
		BlockOption block1 = exchange.getBlock1ToAck();
		exchange.setBlock1ToAck(null);
		
		int maxMsgSize = config.getInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE);
		if (response.getPayloadSize() > maxMsgSize ) {
			LOGGER.info("Response payload is "+response.getPayloadSize()+" long. Send in blocks");
			BlockwiseStatus status = new BlockwiseStatus();
			exchange.setResponseBlockStatus(status);
			if (exchange.getRequest().getOptions().hasBlock2()) {
				// We take the szx the client asks for
				status.setCurrentSzx(exchange.getRequest().getOptions().getBlock2().getSzx());
			} else {
				// If the client has no preference, we take the server's default value
				int blocksize = config.getInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE);
				status.setCurrentSzx( computeSZX(blocksize) );
			}
			
			Response block = extractResponsesBlock(response, status);
			block.setMID(exchange.getCurrentRequest().getMID());
			block.setType(Type.ACK); // First response block to blockwise request must be piggy-backed ack
			/* if the first blocks goes lost and we receive a duplicate of the
			 * request, we resend this block. The rest of the response blocks are
			 * reliably retransmitted due to being CONs. (draft blockwise-11)*/
			exchange.setCurrentResponse(block);
			if (block1 != null) {
				block.getOptions().setBlock1(block1);
			}
			super.sendResponse(exchange, block);
			
			// Initiative changes to server!!!
			LOGGER.fine("Initiative changes to server");
			status.setCurrentNum(1);
			Response second = extractResponsesBlock(response, status);
			/* We don't want the second block to be the currentResponse because
			 * if we receive the last request block (duplicate) we have to resend
			 * the first block from above. (draft blockwise-11)*/
			second.setType(Type.CON);
			super.sendResponse(exchange, second);

		} else {
			if (block1 != null) {
				// We need to force this to be a piggy-backed ack, even if the
				// resource wants to send is as con and has sent an empty ack
				// that we must have stopped (draft blockwise-11).
				response.getOptions().setBlock1(block1);
				LOGGER.fine("Current request is "+exchange.getCurrentRequest());
				response.setMID(exchange.getCurrentRequest().getMID());
				response.setType(Type.ACK); // Response to blockwise request must be piggy-backed ack
			}
			exchange.setCurrentResponse(response); // not really necessary
			super.sendResponse(exchange, response);
		}
	}

	public void sendEmptyMessagePOSTPUT(Exchange exchange, EmptyMessage message) {
		
		/*
		 * After receiving a blockwise request, the client expects a
		 * piggy-backed response and includes the Block1 option of the last
		 * request block. We must not respond with an empty ACK as the client
		 * might get confused. (blockwise-11)
		 */
		// Stop wrong ACKs that try to acknowledge the response
		if (message.getType() == Type.ACK && exchange.getBlock1ToAck() != null) {
			// ignore message
		} else {
			super.sendEmptyMessage(exchange, message);
		}
	}

	public void receiveRequestPOSTPUT(Exchange exchange, Request request) {
		if (request.getOptions().hasBlock1()) {
			BlockOption block1 = request.getOptions().getBlock1();
			LOGGER.fine("Request contains block1 option "+block1);
			BlockwiseStatus status = exchange.getRequestBlockStatus();
			if (status == null) {
				LOGGER.fine("There is no assembler status yet. Create and set new status");
				status = new BlockwiseStatus();
				exchange.setRequestBlockStatus(status);
			}
			
			status.addBlock(request.getPayload());
			if ( ! block1.isM()) {
				LOGGER.fine("There are no more blocks to be expected. Deliver request");
				// this was the last block.
				exchange.setBlock1ToAck(block1);
				
				// The assembled request contains the options of the last block
				Request assembled = new Request(request.getCode()); // getAssembledRequest(status, request);
				assembleMessage(status, assembled, request);
				assembled.setAcknowledged(true);
				exchange.setRequest(assembled);
				super.receiveRequest(exchange, assembled);
			} else {
				LOGGER.fine("We wait for more blocks to come and do not deliver request yet");
				
				// Request code must be POST or PUT because others have no payload
				// (Ask the draft why we send back "changed")
				Response piggybacked = Response.createPiggybackedResponse(request, ResponseCode.CHANGED);
				piggybacked.getOptions().setBlock1(block1.getSzx(), true, block1.getNum());
				piggybacked.setLast(false);
				super.sendResponse(exchange, piggybacked);
				// do not deliver request
			}
			
		} else {
			exchange.setRequest(request); // request is not only current but really the one request
			super.receiveRequest(exchange, request);
		}
	}
	
	public void receiveResponsePOSTPUT(Exchange exchange, Response response) {
		if (!response.getOptions().hasBlock1() && !response.getOptions().hasBlock2()) {
			// no Block 1 or 2 option
			super.receiveResponse(exchange, response);
			return;
		}
		
		if (response.getOptions().hasBlock1()) {
			BlockOption block1 = response.getOptions().getBlock1();
			LOGGER.fine("Response has block 1 option "+block1);
			
			BlockwiseStatus status = exchange.getRequestBlockStatus();
			if (! status.isComplete()) {
				// This should be the case => send next block
				int currentSize = 1 << (4 + status.getCurrentSzx());
				int nextNum = status.getCurrentNum() + currentSize / block1.getSize();
				LOGGER.fine("Send next block num = "+nextNum);
				status.setCurrentNum(nextNum);
				status.setCurrentSzx(block1.getSzx());
				Request nextBlock = getRequestBlock(exchange.getRequest(), status);
				exchange.setCurrentRequest(nextBlock);
				super.sendRequest(exchange, nextBlock);
				// do not deliver
				
			} else if (!response.getOptions().hasBlock2()) {
				// All request block have been acknowledged and we receive a piggy-backed
				// response that needs no blockwise transfer. Thus, deliver it.
				super.receiveResponse(exchange, response);
			}
		}
		
		if (response.getOptions().hasBlock2()) {
			BlockOption block2 = response.getOptions().getBlock2();
			LOGGER.fine("Response has block 2 option "+block2);
			// Synchronize because we might receive the first and second block
			// of the response concurrently (blockwise-11).
			BlockwiseStatus status;
			synchronized (exchange) {
				status = exchange.getResponseBlockStatus();
				if (status == null) {
					exchange.getRequest().setAcknowledged(true);
					LOGGER.fine("There is no response assembler status. Create and set one");
					status = new BlockwiseStatus();
					exchange.setResponseBlockStatus(status);
				}
			}
			
			status.addBlock(response.getPayload());
			
			if ( ! block2.isM()) { // this was the last block.
				// FIXME: If the first block (piggy-backed) has not arrived yet,
				// we need to wait for it. This might change in a future draft.
				LOGGER.fine("There are no more blocks to be expected. Deliver response");
				Response assembled = new Response(response.getCode()); // getAssembledResponse(status, response);
				assembleMessage(status, assembled, response);
				assembled.setAcknowledged(true);
				LOGGER.fine("Assembled response from "+status.getBlockCount()+" blocks: "+assembled);
				super.receiveResponse(exchange, assembled);
			} else {
				LOGGER.fine("We wait for more blocks to come and do not deliver response yet");
				// do not deliver
			}
		}
	}

	public void receiveEmptyMessagePOSTPUT(Exchange exchange, EmptyMessage message) {
		BlockwiseStatus status = exchange.getResponseBlockStatus();
		if (message.getType() == Type.ACK && status != null) {
			LOGGER.fine("Blockwise received ack");
			
			if (! status.isComplete()) {
				// send next block
				int nextNum = status.getCurrentNum() + 1;
				LOGGER.fine("Send next block num = "+nextNum);
				status.setCurrentNum(nextNum);
				Response nextBlock = extractResponsesBlock(exchange.getResponse(), status);
				sendResponse(exchange, nextBlock);
			} else { // we are done
				LOGGER.fine("The last block is acknowledged");
				super.receiveEmptyMessage(exchange, message);
			}
			
		} else {
			super.receiveEmptyMessage(exchange, message);
		}
	}
	
	//////////////////////////////////////
	////////// Helper functions //////////
	//////////////////////////////////////
	
	/**
	 * Encodes a block size into a 3-bit SZX value as specified by
	 * draft-ietf-core-block-03, section-2.1:
	 * 
	 * 16 bytes = 2^4 --> 0
	 * ... 
	 * 1024 bytes = 2^10 -> 6
	 * 
	 */
	private int computeSZX(int blockSize) {
		return (int)(Math.log(blockSize)/Math.log(2)) - 4;
	}
	
	private Request getRequestBlock(Request request, BlockwiseStatus status) {
		int num = status.getCurrentNum();
		int szx = status.getCurrentSzx();
		Request block = new Request(request.getCode());
		block.setOptions(new OptionSet(request.getOptions()));
		block.setDestination(request.getDestination());
		block.setDestinationPort(request.getDestinationPort());
		block.setToken(request.getToken());
		block.setType(Type.CON);
		
		int currentSize = 1 << (4 + szx);
		int from = num * currentSize;
		int to = Math.min((num + 1) * currentSize, request.getPayloadSize());
		int length = to - from;
		byte[] blockPayload = new byte[length];
		System.arraycopy(request.getPayload(), from, blockPayload, 0, length);
		block.setPayload(blockPayload);
		
		boolean m = (to < request.getPayloadSize());
		block.getOptions().setBlock1(szx, m, num);
		
		status.setComplete(!m);
		return block;
	}
	
	private Response extractResponsesBlock(Response response, BlockwiseStatus status) {
		int szx = status.getCurrentSzx();
		int num = status.getCurrentNum();
		Response block = new Response(response.getCode());
		block.setDestination(response.getDestination());
		block.setDestinationPort(response.getDestinationPort());
		block.setToken(response.getToken());
		block.setOptions(new OptionSet(response.getOptions()));
		block.setType(Type.CON);
		
		if (response.getPayloadSize() > 0) {
			int currentSize = 1 << (4 + szx);
			int from = num * currentSize;
			int to = Math.min((num + 1) * currentSize, response.getPayloadSize());
			int length = to - from;
			byte[] blockPayload = new byte[length];
			System.arraycopy(response.getPayload(), from, blockPayload, 0, length);
			block.setPayload(blockPayload);
			
			boolean m = (to < response.getPayloadSize());
			block.getOptions().setBlock2(szx, m, num);
			block.setLast(!m);
			
			status.setComplete(!m);
		} else {
			block.getOptions().setBlock2(szx, false, 0);
			block.setLast(true);
			status.setComplete(true);
		}
		return block;
	}
	
	private void assembleMessage(BlockwiseStatus status, Message message, Message last) {
		message.setMID(last.getMID());
		message.setSource(last.getSource());
		message.setSourcePort(last.getSourcePort());
		message.setToken(last.getToken());
		message.setType(last.getType());
		message.setOptions(new OptionSet(last.getOptions()));
		
		int length = 0;
		for (byte[] block:status.getBlocks())
			length += block.length;
		
		byte[] payload = new byte[length];
		int offset = 0;
		for (byte[] block:status.getBlocks()) {
			System.arraycopy(block, 0, payload, offset, block.length);
			offset += block.length;
		}
		
		message.setPayload(payload);
	}
}
