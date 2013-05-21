package ch.inf.vs.californium.network;

import java.util.logging.Logger;

import ch.inf.vs.californium.coap.BlockOption;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

public class BlockwiseLayer extends AbstractLayer {

	private final static Logger LOGGER = Logger.getLogger(BlockwiseLayer.class.getName());
	
	private StackConfiguration config;
	
	public BlockwiseLayer(StackConfiguration config) {
		this.config = config;
	}
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		
		if (request.getPayloadSize() > config.getMaxMessageSize()) {
			LOGGER.info("Request payload is "+request.getPayloadSize()+" long. Send in blocks");
			BlockwiseStatus status = new BlockwiseStatus();
			exchange.setRequestBlockStatus(status);
			
			int szx = computeSZX(config.getDefaultBlockSize());
			Request block = getRequestBlock(request, status, szx, 0);
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
				status.currentSzx = exchange.getRequest().getOptions().getBlock2().getSzx();
			} else {
				// If the client has no preference, we take the server's default value
				status.currentSzx = computeSZX(config.getDefaultBlockSize());
			}
			
			Response block = getResponsesBlock(response, status, 0);
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
			Response second = getResponsesBlock(response, status, 1);
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
		// TODO: Stop wrong ACKs
		super.sendEmptyMessage(exchange, message);
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
			
			status.requests.add(request);
			if ( ! block1.isM()) {
				LOGGER.info("There are no more blocks to be expected. Deliver request");
				// this was the last block.
				exchange.setBlock1ToAck(block1);
				
				// The assembled request contains the options of the last block
				Request assembled = getAssembledRequest(status);
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
		
		if (response.getOptions().hasBlock1()) { // TODO: and we also expect a block
			BlockOption block1 = response.getOptions().getBlock1();
			LOGGER.info("Response has block 1 option "+block1);
			
			BlockwiseStatus status = exchange.getRequestBlockStatus();
			if (! status.complete) {
				// This should be the case => send next block
				int nextNum = status.currentNum + status.currentSize / block1.getSize();
				LOGGER.info("Send next block num = "+nextNum);
				Request nextBlock = getRequestBlock(exchange.getRequest(), status, block1.getSzx(), nextNum);
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
			// of the response concurrently.
			BlockwiseStatus status;
			synchronized (exchange) {
				status = exchange.getResponseBlockStatus();
				if (status == null) {
					LOGGER.info("There is no response assembler status. Create and set one");
					status = new BlockwiseStatus();
					exchange.setResponseBlockStatus(status);
				}
			}
			
			status.responses.add(response);
			
			if (response.getType() == Type.CON) {
				EmptyMessage ack = EmptyMessage.newACK(response);
				sendEmptyMessage(exchange, ack);
			}
			
			if ( ! block2.isM()) { // this was the last block.
				LOGGER.info("There are no more blocks to be expected. Deliver response");
				
				Response assembled = getAssembledResponse(status);
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
			
			if (! status.complete) {
				// send next block
				int nextNum = status.currentNum + 1;
				LOGGER.info("Send next block num = "+nextNum);
				Response nextBlock = getResponsesBlock(exchange.getResponse(), status, nextNum);
				sendResponse(exchange, nextBlock);
			} else { // we are done
				LOGGER.info("The last block is acknowledged");
			}
			
		} else {
			super.receiveEmptyMessage(exchange, message);
		}
		
	}
	
	/*
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
	
	private Request getRequestBlock(Request request, BlockwiseStatus status, int szx, int num) {
		status.currentSzx = szx;
		status.currentNum = num;
		Request block = new Request(request.getCode());
		block.setOptions(request.getOptions());
		block.setDestination(request.getDestination());
		block.setDestinationPort(request.getDestinationPort());
		block.setToken(request.getToken());
		block.setType(Type.CON);
		
		status.currentSize = 1 << (4 + szx);
		int from = num * status.currentSize;
		int to = Math.min((num + 1) * status.currentSize, request.getPayloadSize());
		int length = to - from;
		byte[] blockPayload = new byte[length];
		System.arraycopy(request.getPayload(), from, blockPayload, 0, length);
		block.setPayload(blockPayload);
		
		boolean m = (to < request.getPayloadSize());
		block.getOptions().setBlock1(szx, m, num);
		
		status.complete = !m;
		return block;
	}
	
	private Request getAssembledRequest(BlockwiseStatus status) {
		Request last = status.requests.get(status.requests.size() - 1);
		Request request = new Request(last.getCode());
		request.setMid(last.getMid());
		request.setSource(last.getSource());
		request.setSourcePort(last.getSourcePort());
		request.setToken(last.getToken());
		request.setType(last.getType());
		request.setOptions(last.getOptions());
		
		int length = 0;
		for (Request block:status.requests)
			length += block.getPayloadSize();
		
		byte[] payload = new byte[length];
		int offset = 0;
		for (Request block:status.requests) {
			int blocklength = block.getPayloadSize();
			System.arraycopy(block.getPayload(), 0, payload, offset, blocklength);
			offset += blocklength;
		}
		request.setPayload(payload);
		return request;
	}
	
	private Response getResponsesBlock(Response response, BlockwiseStatus status, int num) {
		int szx = status.currentSzx;
		status.currentNum = num;
		Response block = new Response(response.getCode());
		block.setDestination(response.getDestination());
		block.setDestinationPort(response.getDestinationPort());
		block.setToken(response.getToken());
		block.setType(Type.CON);
		
		status.currentSize = 1 << (4 + szx);
		int from = num * status.currentSize;
		int to = Math.min((num + 1) * status.currentSize, response.getPayloadSize());
		int length = to - from;
		byte[] blockPayload = new byte[length];
		System.arraycopy(response.getPayload(), from, blockPayload, 0, length);
		block.setPayload(blockPayload);
		
		boolean m = (to < response.getPayloadSize());
		block.getOptions().setBlock2(szx, m, num);
		
		status.complete = !m;
		return block;
	}
	
	private Response getAssembledResponse(BlockwiseStatus status) {
		Response last = status.responses.get(status.responses.size() - 1);
		Response response = new Response(last.getCode());
		response.setMid(last.getMid());
		response.setSource(last.getSource());
		response.setSourcePort(last.getSourcePort());
		response.setToken(last.getToken());
		response.setType(last.getType());
		response.setOptions(last.getOptions());
		
		int length = 0;
		for (Response block:status.responses)
			length += block.getPayloadSize();
		
		byte[] payload = new byte[length];
		int offset = 0;
		for (Response block:status.responses) {
			int blocklength = block.getPayloadSize();
			System.arraycopy(block.getPayload(), 0, payload, offset, blocklength);
			offset += blocklength;
		}
		response.setPayload(payload);
		return response;
	}
	
}
