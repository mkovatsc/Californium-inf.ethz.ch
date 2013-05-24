package ch.inf.vs.californium.network.layer;

import java.util.logging.Logger;

import ch.inf.vs.californium.coap.BlockOption;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Message;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.NetworkConfig;

public class BlockwiseLayer extends AbstractLayer {

	private final static Logger LOGGER = Logger.getLogger(BlockwiseLayer.class.getName());
	
	// TODO: blockwise responses to GET requests work differently than POST and PUT
	
	private NetworkConfig config;
	
	public BlockwiseLayer(NetworkConfig config) {
		this.config = config;
	}
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		
		if (request.getPayloadSize() > config.getMaxMessageSize()) {
			LOGGER.info("Request payload is "+request.getPayloadSize()+" long. Send in blocks");
			BlockwiseStatus status = new BlockwiseStatus();
			exchange.setRequestBlockStatus(status);
			
			status.setCurrentSzx( computeSZX(config.getDefaultBlockSize()) );
			Request block = getRequestBlock(request, status);
			exchange.setCurrentRequest(block);
			
			super.sendRequest(exchange, block);
		
		} else {
			exchange.setCurrentRequest(request); // not really necessary
			super.sendRequest(exchange, request);
		}
	}
	
	@Override
	public void sendResponse(Exchange exchange, Response response) {
		
		// If the request was sent with a block1 option the response has to send its
		// first block piggy-backed with the Block1 option of the last request block
		BlockOption block1 = exchange.getBlock1ToAck();
		exchange.setBlock1ToAck(null);
		
		if (response.getPayloadSize() > config.getMaxMessageSize()) {
			LOGGER.info("Response payload is "+response.getPayloadSize()+" long. Send in blocks");
			BlockwiseStatus status = new BlockwiseStatus();
			exchange.setResponseBlockStatus(status);
			if (exchange.getRequest().getOptions().hasBlock2()) {
				// We take the szx the client asks for
				status.setCurrentSzx(exchange.getRequest().getOptions().getBlock2().getSzx());
			} else {
				// If the client has no preference, we take the server's default value
				status.setCurrentSzx( computeSZX(config.getDefaultBlockSize()) );
			}
			
			Response block = extractResponsesBlock(response, status);
			block.setMid(exchange.getCurrentRequest().getMid());
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
			LOGGER.info("Initiative changes to server");
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
				LOGGER.info("Current request is "+exchange.getCurrentRequest());
				response.setMid(exchange.getCurrentRequest().getMid());
				response.setType(Type.ACK); // Response to blockwise request must be piggy-backed ack
			}
			exchange.setCurrentResponse(response); // not really necessary
			super.sendResponse(exchange, response);
		}
	}

	@Override
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		/*
		 * After receiving a blockwise request, the client expects a
		 * piggy-backed response and includes the Block1 option of the last
		 * request block. We must not respond with an empty ACK as the client
		 * might get confused. (blockwise-11)
		 */
		// Stop wrong ACKs that try to acknowledge the response
		if (message.getType() == Type.ACK && exchange.getBlock1ToAck() != null) {
			ignore(message);
		} else {
			super.sendEmptyMessage(exchange, message);
		}
	}

	@Override
	public void receiveRequest(Exchange exchange, Request request) {

		if (request.getOptions().hasBlock1()) {
			BlockOption block1 = request.getOptions().getBlock1();
			LOGGER.info("Request contains block1 option "+block1);
			BlockwiseStatus status = exchange.getRequestBlockStatus();
			if (status == null) {
				LOGGER.info("There is no assembler status yet. Create and set new status");
				status = new BlockwiseStatus();
				exchange.setRequestBlockStatus(status);
			}
			
			status.blocks.add(request.getPayload());
			if ( ! block1.isM()) {
				LOGGER.info("There are no more blocks to be expected. Deliver request");
				// this was the last block.
				exchange.setBlock1ToAck(block1);
				
				// The assembled request contains the options of the last block
				Request assembled = new Request(request.getCode()); // getAssembledRequest(status, request);
				assembleMessage(status, assembled, request);
				assembled.setAcknowledged(true);
				exchange.setRequest(assembled);
				super.receiveRequest(exchange, assembled);
			} else {
				LOGGER.info("We wait for more blocks to come and do not deliver request yet");
				
				// Request code must be POST or PUT because others have no payload
				// (Ask the draft why we send back "changed")
				Response piggybacked = Response.createPiggybackedResponse(request, ResponseCode.CHANGED);
				piggybacked.getOptions().setBlock1(block1.getSzx(), true, block1.getNum());
				sendResponse(exchange, piggybacked);
				ignore(request); // do not deliver
			}
			
		} else {
			LOGGER.info("Request has no block 1 option");
			exchange.setRequest(request); // request is not only current but really the one request
			super.receiveRequest(exchange, request);
		}
	}
	
	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		if (!response.getOptions().hasBlock1() && !response.getOptions().hasBlock2()) {
			// no Block 1 or 2 option
			super.receiveResponse(exchange, response);
			return;
		}
		
		if (response.getOptions().hasBlock1()) {
			BlockOption block1 = response.getOptions().getBlock1();
			LOGGER.info("Response has block 1 option "+block1);
			
			BlockwiseStatus status = exchange.getRequestBlockStatus();
			if (! status.isComplete()) {
				// This should be the case => send next block
				int currentSize = 1 << (4 + status.getCurrentSzx());
				int nextNum = status.getCurrentNum() + currentSize / block1.getSize();
				LOGGER.info("Send next block num = "+nextNum);
				status.setCurrentNum(nextNum);
				status.setCurrentSzx(block1.getSzx());
				Request nextBlock = getRequestBlock(exchange.getRequest(), status);
				exchange.setCurrentRequest(nextBlock);
				super.sendRequest(exchange, nextBlock);
				ignore(response);
			} else if (!response.getOptions().hasBlock2()) {
				// All request block have been acknowledged and we receive a piggy-backed
				// response that needs no blockwise transfer. Thus, deliver it.
				super.receiveResponse(exchange, response);
			}
		}
		
		if (response.getOptions().hasBlock2()) {
			BlockOption block2 = response.getOptions().getBlock2();
			LOGGER.info("Response has block 2 option "+block2);
			// Synchronize because we might receive the first and second block
			// of the response concurrently (blockwise-11).
			BlockwiseStatus status;
			synchronized (exchange) {
				status = exchange.getResponseBlockStatus();
				if (status == null) {
					LOGGER.info("There is no response assembler status. Create and set one");
					status = new BlockwiseStatus();
					exchange.setResponseBlockStatus(status);
				}
			}
			
			status.blocks.add(response.getPayload());
			
			if (response.getType() == Type.CON) {
				EmptyMessage ack = EmptyMessage.newACK(response);
				sendEmptyMessage(exchange, ack);
			}
			
			if ( ! block2.isM()) { // this was the last block.
				// TODO: What if the first block (piggy-backed) has not arrived yet?
				LOGGER.info("There are no more blocks to be expected. Deliver response");
				Response assembled = new Response(response.getCode()); // getAssembledResponse(status, response);
				assembleMessage(status, assembled, response);
				assembled.setAcknowledged(true);
				LOGGER.info("Assembled response: "+assembled);
				super.receiveResponse(exchange, assembled);
			} else {
				LOGGER.info("We wait for more blocks to come and do not deliver request yet");
				ignore(response); // do not deliver
			}
		}
	}

	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {

		BlockwiseStatus status = exchange.getResponseBlockStatus();
		if (message.getType() == Type.ACK && status != null) {
			LOGGER.info("Blockwise received ack");
			
			if (! status.isComplete()) {
				// send next block
				int nextNum = status.getCurrentNum() + 1;
				LOGGER.info("Send next block num = "+nextNum);
				status.setCurrentNum(nextNum);
				Response nextBlock = extractResponsesBlock(exchange.getResponse(), status);
				sendResponse(exchange, nextBlock);
			} else { // we are done
				LOGGER.info("The last block is acknowledged");
			}
			
		} else {
			super.receiveEmptyMessage(exchange, message);
		}
		
	}
	
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
		block.setOptions(request.getOptions());
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
		block.setType(Type.CON);
		
		int currentSize = 1 << (4 + szx);
		int from = num * currentSize;
		int to = Math.min((num + 1) * currentSize, response.getPayloadSize());
		int length = to - from;
		byte[] blockPayload = new byte[length];
		System.arraycopy(response.getPayload(), from, blockPayload, 0, length);
		block.setPayload(blockPayload);
		
		boolean m = (to < response.getPayloadSize());
		block.getOptions().setBlock2(szx, m, num);
		
		status.setComplete(!m);
		return block;
	}
	
	private void assembleMessage(BlockwiseStatus status, Message message, Message last) {
		message.setMid(last.getMid());
		message.setSource(last.getSource());
		message.setSourcePort(last.getSourcePort());
		message.setToken(last.getToken());
		message.setType(last.getType());
		message.setOptions(last.getOptions());
		
		int length = 0;
		for (byte[] block:status.blocks)
			length += block.length;
		
		byte[] payload = new byte[length];
		int offset = 0;
		for (byte[] block:status.blocks) {
			System.arraycopy(block, 0, payload, offset, block.length);
			offset += block.length;
		}
		
		message.setPayload(payload);
	}
	
}
