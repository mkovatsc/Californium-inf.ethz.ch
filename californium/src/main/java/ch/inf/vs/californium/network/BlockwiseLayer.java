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
	
	// TODO: Remember Block options from incoming messages
	
	private StackConfiguration config;
	
	public BlockwiseLayer(StackConfiguration config) {
		this.config = config;
	}
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		
		if (request.getPayloadSize() > config.getMaxMessageSize()) {
			LOGGER.info("Request payload is "+request.getPayloadSize()+" long. Send in blocks");
			RequestBlockAssembler assembler = new RequestBlockAssembler(request);
			exchange.setRequestAssembler(assembler);
			
			int szx = computeSZX(config.getDefaultBlockSize());
			Request block = assembler.getBlock(szx, 0);
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
			int szx;
			if (exchange.getRequest().getOptions().hasBlock2()) {
				// We take the szx the client asks for
				szx = exchange.getRequest().getOptions().getBlock2().getSzx();
			} else {
				// If the client has no preference, we take the server's default value
				szx = computeSZX(config.getDefaultBlockSize());
			}
			ResponseBlockAssembler assembler = new ResponseBlockAssembler(response, szx);
			exchange.setResponseAssembler(assembler);
			
			Response block = assembler.getBlock(0);
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
			Response second = assembler.getBlock(1);
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
//		exchange.setCurrentRequest(request); // done in retransmission layer

		if (request.getOptions().hasBlock1()) {
			BlockOption block1 = request.getOptions().getBlock1();
			LOGGER.info("Request contains block1 option "+block1);
			RequestBlockAssembler assembler = exchange.getRequestAssembler();
			if (assembler == null) {
				LOGGER.info("There is no assembler yet. Create and set new assembler");
				assembler = new RequestBlockAssembler();
				exchange.setRequestAssembler(assembler);
			}
			
			assembler.insert(request);
			if ( ! block1.isM()) {
				LOGGER.info("There are no more blocks to be expected. Deliver request");
				// this was the last block.
				exchange.setBlock1ToAck(block1);
				
				// The assembled request contains the options of the last block
				Request assembled = assembler.getAssembledRequest();
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
			RequestBlockAssembler assembler = exchange.getRequestAssembler();
			// block1.szx might be different to assembler.currentSzx
			
			if (!assembler.isComplete()) {
				// This should be the case => send next block
				int nextNum = assembler.getCurrentNum() + assembler.getCurrentSize() / block1.getSize();
				LOGGER.info("Send next block num = "+nextNum);
				Request nextBlock = assembler.getBlock(block1.getSzx(), nextNum);
				exchange.setCurrentRequest(nextBlock);
				super.sendRequest(exchange, nextBlock);
				ignore(response); // do not deliver
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
			ResponseBlockAssembler assembler;
			synchronized (exchange) {
				assembler = exchange.getResponseAssembler();
				if (assembler == null) {
					LOGGER.info("There is no response assembler. Create and set one");
					assembler = new ResponseBlockAssembler();
					exchange.setResponseAssembler(assembler);
				}
			}
			
			assembler.insert(response);
			
			if (response.getType() == Type.CON) {
				EmptyMessage ack = EmptyMessage.newACK(response);
				sendEmptyMessage(exchange, ack);
			}
			
			if ( ! block2.isM()) { // this was the last block.
				LOGGER.info("There are no more blocks to be expected. Deliver response");
				
				Response assembled = assembler.getAssembledResponse();
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

		ResponseBlockAssembler assembler = exchange.getResponseAssembler();
		if (message.getType() == Type.ACK && assembler != null) {
			LOGGER.info("Blockwise received ack");
			
			if (! assembler.isComplete()) {
				// send next block
				int nextNum = assembler.getCurrentNum() + 1;
				LOGGER.info("Send next block num = "+nextNum);
				Response nextBlock = assembler.getBlock(nextNum);
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
	
	protected class BlockwiseTransferForward {
		
		public void forwardSendRequest(Exchange exchange, Request request) {
			BlockwiseLayer.super.sendRequest(exchange, request);
		}
		
	}
	
	protected static class BlockwiseTransfer {
		
		private BlockwiseTransferForward forward;
		
		public void sendRequest(Exchange exchange, Request request) {
			forward.forwardSendRequest(exchange, request);
		}
		
	}
	
}
