/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class TransferLayer provides support for
 * <a href="http://tools.ietf.org/html/draft-ietf-core-block">blockwise transfers</a>.
 * 
 * @author Matthias Kovatsch
 */
public class TransferLayer extends UpperLayer {
	
	private class TransferContext {
		public Message cache;
		public String uriPath;
		public BlockOption current;
		
		// TODO: timer
		
		TransferContext(Message msg) {
			
			if (msg instanceof Request) {
				this.cache = msg;
				this.uriPath = msg.getUriPath();
				this.current = (BlockOption) msg.getFirstOption(OptionNumberRegistry.BLOCK1);
			} else if (msg instanceof Response) {
				
				msg.requiresToken(false); // FIXME check if still required after new TokenLayer
				
				this.cache = msg;
				this.uriPath = ((Response)msg).getRequest().getUriPath();
				this.current = (BlockOption) msg.getFirstOption(OptionNumberRegistry.BLOCK2);
			}
			
			LOG.finest(String.format("Created new transfer context for %s: %s", this.uriPath, msg.sequenceKey()));
		}
	}
	

// Members /////////////////////////////////////////////////////////////////////
	
	private Map<String, TransferContext> incoming = new HashMap<String, TransferContext>();
	private Map<String, TransferContext> outgoing = new HashMap<String, TransferContext>();
	
	// default block size used for the transfer
	private int defaultSZX;
	
	// Constructors ////////////////////////////////////////////////////////////
	
	/**
	 * Constructor for a new TransferLayer
	 * 
	 * @param defaultBlockSize the block size to use if not indicated by block option
	 */
	public TransferLayer(int defaultBlockSize) {
		
		if (defaultBlockSize==0) {
			defaultBlockSize = Properties.std.getInt("DEFAULT_BLOCK_SIZE");
		}
		
		if (defaultBlockSize > 0) {
		
			defaultSZX = BlockOption.encodeSZX(defaultBlockSize);
			if (!BlockOption.validSZX(defaultSZX)) {
				
				defaultSZX = defaultBlockSize > 1024 ? 6 : BlockOption.encodeSZX(defaultBlockSize & 0x07f0);
				LOG.warning(String.format("Unsupported block size %d, using %d instead", defaultBlockSize, BlockOption.decodeSZX(defaultSZX)));
			}
			
		} else {
			// disable outgoing blockwise transfers
			defaultSZX = -1;
		}
	}
	
	public TransferLayer() {
		this(0);
	}

	// I/O implementation //////////////////////////////////////////////////////
	

	//TODO ETag matching
	
	@Override
	protected void doSendMessage(Message msg) throws IOException {
		
		int sendSZX = defaultSZX;
		int sendNUM = 0;
		
		// block negotiation
		if (msg instanceof Response && ((Response)msg).getRequest()!=null) {
			BlockOption buddyBlock = (BlockOption) ((Response)msg).getRequest().getFirstOption(OptionNumberRegistry.BLOCK2);
			if (buddyBlock!=null) {
				if (buddyBlock.getSZX()<defaultSZX) {
					sendSZX = buddyBlock.getSZX();
				}
				sendNUM = buddyBlock.getNUM();
			}
		}
		
		// check if transfer needs to be split up
		if (msg.payloadSize() > BlockOption.decodeSZX(sendSZX)) {
			// split message up using block1 for requests and block2 for responses
			
			Message msgBlock = getBlock(msg, sendNUM, sendSZX);
			
			if (msgBlock!=null) {
				
				// send block and wait for reply
				sendMessageOverLowerLayer(msgBlock);
				
				BlockOption block1 = (BlockOption) msgBlock.getFirstOption(OptionNumberRegistry.BLOCK1);
				BlockOption block2 = (BlockOption) msgBlock.getFirstOption(OptionNumberRegistry.BLOCK2);

				// only cache if blocks remaining for request
				if (block1!=null && block1.getM() || block2!=null && block2.getM()) {

					msg.setOption(block1);
					msg.setOption(block2);
					
					TransferContext transfer = new TransferContext(msg);
					outgoing.put(msg.sequenceKey(), transfer);
					
					LOG.fine(String.format("Cached blockwise transfer for NUM %d: %s", sendNUM, msg.sequenceKey()));
				} else {
					// must be block2 by client
					LOG.finer(String.format("Answered block request without caching: %s | %s", msg.sequenceKey(), block2));
				}
				
			} else {
				// must be block2 by client
				LOG.info(String.format("Rejecting initial out-of-scope request: %s | NUM: %d, SZX: %d (%d bytes), M: n/a, %d bytes available", msg.sequenceKey(), sendNUM, sendSZX, BlockOption.decodeSZX(sendSZX), msg.payloadSize()));
				handleOutOfScopeError(msg.newReply(true));
			}
			
		} else {
			// send complete message
			sendMessageOverLowerLayer(msg);
		}
	}
	
	@Override
	protected void doReceiveMessage(Message msg) {

		BlockOption blockIn = null;
		BlockOption blockOut = null;
		
		if (msg instanceof Request) {
			blockIn = (BlockOption) msg.getFirstOption(OptionNumberRegistry.BLOCK1);
			blockOut = (BlockOption) msg.getFirstOption(OptionNumberRegistry.BLOCK2);
		} else if (msg instanceof Response) {
			blockIn = (BlockOption) msg.getFirstOption(OptionNumberRegistry.BLOCK2);
			blockOut = (BlockOption) msg.getFirstOption(OptionNumberRegistry.BLOCK1);
		} else {
			LOG.warning(String.format("Unknown message type received: %s", msg.key()));
			return;
		}
			
		if (blockIn!=null || msg.requiresBlockwise()) {
				
			handleIncomingPayload(msg, blockIn);
			return;
				
		} else if (blockOut!=null) {
			
			LOG.finer(String.format("Received demand for next block: %s | %s", msg.sequenceKey(), blockOut));
				
			TransferContext transfer = outgoing.get(msg.sequenceKey());
				
			if (transfer!=null) {
				
				if (msg instanceof Request && !msg.getUriPath().equals(transfer.uriPath)) {
				
					outgoing.remove(msg.sequenceKey());
					LOG.fine(String.format("Freed blockwise transfer by client token reuse: %s", msg.sequenceKey()));
					
				} else {
			
					// use cached representation
					Message resp = getBlock(transfer.cache, blockOut.getNUM(), blockOut.getSZX());
						
					if (resp!=null) {
		
						// update message ID
						resp.setMID(msg.getMID());
							
						try {
							LOG.finer(String.format("Sending next block: %s | %s", resp.sequenceKey(), blockOut));
							sendMessageOverLowerLayer(resp);
						} catch (IOException e) {
							LOG.severe(String.format("Failed to send block response: %s", e.getMessage()));
						}
							
						BlockOption respBlock = (BlockOption) resp.getFirstOption(blockOut.getOptionNumber());
							
						// remove transfer context if completed
						if (!respBlock.getM()) {
							outgoing.remove(msg.sequenceKey());
							LOG.fine(String.format("Freed blockwise transfer by completion: %s", resp.sequenceKey()));
						}
						return;
							
					} else {
						LOG.warning(String.format("Rejecting out-of-scope request with cached response (freed): %s | %s, %d bytes available", msg.sequenceKey(), blockOut, transfer.cache.payloadSize()));
						outgoing.remove(msg.sequenceKey());
						handleOutOfScopeError(msg.newReply(true));
						return;
					}
				}
			}
		}

		// get current representation
		deliverMessage(msg);
	}
	
	private void handleIncomingPayload(Message msg, BlockOption blockOpt) {
		
		TransferContext transfer = incoming.get(msg.sequenceKey());
		
		if (blockOpt.getNUM()>0 && transfer != null) {
			
			// compare block offsets
			if (blockOpt.getNUM()*blockOpt.getSize()==(transfer.current.getNUM()+1)*transfer.current.getSize() ) {
								
				// append received payload to first response and update message ID
				transfer.cache.appendPayload(msg.getPayload());
				
				// update info
				transfer.cache.setMID(msg.getMID());
				
				LOG.fine(String.format("Received next block:  %s | %s", msg.sequenceKey(), blockOpt)); // extra space to match "Demanding next block" indent
				
			} else {
				LOG.info(String.format("Dropping wrong block: %s | %s", msg.sequenceKey(), blockOpt));
			}
		
			
		} else if (blockOpt.getNUM()==0 && msg.payloadSize()>0) {
			
			// TODO peek if method, content-type, etc. allowed
			
			// create new transfer context
			transfer = new TransferContext(msg);
			incoming.put(msg.sequenceKey(), transfer);
			
			LOG.fine(String.format("Incoming blockwise transfer: %s | %s", msg.sequenceKey(), blockOpt));
			
		} else {
			
			LOG.info(String.format("Rejecting out-of-order block: %s | %s", msg.sequenceKey(), blockOpt));
			handleIncompleteError(msg.newReply(true));
			return;
		}
		
		if (blockOpt.getM()) {
			Message reply = null;
			
			int demandSZX = blockOpt.getSZX();
			int demandNUM = blockOpt.getNUM();

			// block size negotiation
			if (demandSZX>defaultSZX) {
				demandNUM = demandSZX/defaultSZX * demandNUM;
				demandSZX = defaultSZX; 
			}
			
			if (msg instanceof Response) {

				reply = new Request(CodeRegistry.METHOD_GET, !msg.isNonConfirmable()); // msg could be ACK or CON
				reply.setURI("coap://" + msg.getPeerAddress().toString() + transfer.uriPath);
				
				// get next block
				++demandNUM;

			} else if (msg instanceof Request) {

				// picked arbitrary code, cannot decide if created or changed without putting resource logic here
				reply = new Response(CodeRegistry.RESP_VALID, msg.isConfirmable());
				reply.setPeerAddress(msg.getPeerAddress());
				
				if (msg.isConfirmable()) reply.setMID(msg.getMID());
				
				// increase NUM for next block after ACK
				
			} else {
				LOG.severe(String.format("Unsupported message type: %s", msg.key()));
				return;
			}
			
			// MORE=1 for Block1, as Cf handles transfers atomically
			BlockOption next = new BlockOption(blockOpt.getOptionNumber(), demandNUM, demandSZX, blockOpt.getOptionNumber()==OptionNumberRegistry.BLOCK1);
			
			// echo options
			reply.setOption(msg.getFirstOption(OptionNumberRegistry.TOKEN));
			reply.setOption(next);

			try {
				
				LOG.fine(String.format("Demanding next block: %s | %s", reply.sequenceKey(), next));
				
				sendMessageOverLowerLayer(reply);
				
			} catch (IOException e) {
				LOG.severe(String.format("Failed to request block: %s", e.getMessage()));
			}

			// update incoming transfer
			transfer.current = blockOpt;
			
		} else {
			
			// set final block option
			transfer.cache.setOption(blockOpt);
			
			LOG.fine(String.format("Finished blockwise transfer: %s", msg.sequenceKey()));
			incoming.remove(msg.sequenceKey());
			
			deliverMessage(transfer.cache);
		}
	}
	
	private void handleOutOfScopeError(Message resp) {
		
		resp.setCode(CodeRegistry.RESP_BAD_REQUEST);
		resp.setPayload("BlockOutOfScope");
		
		try {
			sendMessageOverLowerLayer(resp);
			
		} catch (IOException e) {
			LOG.severe(String.format("Failed to send error message: %s", e.getMessage()));
		}
	}
	
	private void handleIncompleteError(Message resp) {
		
		resp.setCode(CodeRegistry.RESP_REQUEST_ENTITY_INCOMPLETE);
		resp.setPayload("Start with block num 0");
		
		try {
			sendMessageOverLowerLayer(resp);
		} catch (IOException e) {
			LOG.severe(String.format("Failed to send error message: %s", e.getMessage()));
		}
	}
	
	
	// Static Methods //////////////////////////////////////////////////////////

	private static Message getBlock(Message msg, int num, int szx) {
		
		int blockSize = 1 << (szx + 4);
		int payloadOffset = num * blockSize;
		int payloadLeft = msg.payloadSize() - payloadOffset;
		
		if (payloadLeft > 0) {
			Message block = new Message(msg.getType(), msg.getCode());
			
			block.setMID(msg.getMID());
			block.setPeerAddress(msg.getPeerAddress());
			
			// use same options
			for (Option opt : msg.getOptions()) {
				block.addOption(opt);
			}
			
			// calculate 'more' bit 
			boolean m = blockSize < payloadLeft;
			
			// limit block size to size of payload left
			if (!m) {
				blockSize = payloadLeft;
			}
			
			// copy payload block
			
			byte[] blockPayload = new byte[blockSize];
			System.arraycopy(msg.getPayload(), payloadOffset, blockPayload, 0, blockSize);
			
			block.setPayload(blockPayload);
			
			Option blockOpt = null;
			if (msg instanceof Request) {
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
