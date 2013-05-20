package ch.inf.vs.californium.network;

import java.util.ArrayList;

import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.Response;

/**
 * We could call this a manager.
 */
public class ResponseBlockAssembler {

	private Response response;
	
	private int currentNum;
	private int currentSzx;
	private int currentSize;
	private boolean complete;
	
	public ResponseBlockAssembler(Response response, int currentSzx) {
		this.response = response;
		this.blocks = null;
		this.currentNum = 0;
		this.currentSzx = currentSzx;
	}
	
	public Response getBlock(int num) {
		int szx = currentSzx;
		this.currentNum = num;
		Response block = new Response(response.getCode());
		block.setDestination(response.getDestination());
		block.setDestinationPort(response.getDestinationPort());
		block.setToken(response.getToken());
		block.setType(Type.CON);
		
		this.currentSize = 1 << (4 + szx);
		int from = num * currentSize;
		int to = Math.min((num + 1) * currentSize, response.getPayloadSize());
		int length = to - from;
		byte[] blockPayload = new byte[length];
		System.arraycopy(response.getPayload(), from, blockPayload, 0, length);
		block.setPayload(blockPayload);
		
		boolean m = (to < response.getPayloadSize());
		block.getOptions().setBlock2(szx, m, num);
		
		this.complete = !m;
		return block;
	}
	
	private ArrayList<Response> blocks;
	
	public ResponseBlockAssembler() {
		this.response = null;
		this.blocks = new ArrayList<>();
		this.currentNum = 0;
		this.currentSzx = 0;
	}
	
	public void insert(Response block) {
		blocks.add(block);
	}
	
	public Response getAssembledResponse() {
		Response last = blocks.get(blocks.size() - 1);
		Response response = new Response(last.getCode());
		response.setMid(last.getMid());
		response.setSource(last.getSource());
		response.setSourcePort(last.getSourcePort());
		response.setToken(last.getToken());
		response.setType(last.getType());
		
		int length = 0;
		for (Response block:blocks)
			length += block.getPayloadSize();
		
		byte[] payload = new byte[length];
		int offset = 0;
		for (Response block:blocks) {
			int blocklength = block.getPayloadSize();
			System.arraycopy(block.getPayload(), 0, payload, offset, blocklength);
			offset += blocklength;
		}
		response.setPayload(payload);
		return response;
	}

	public int getCurrentNum() {
		return currentNum;
	}

	public int getCurrentSzx() {
		return currentSzx;
	}
	
	public int getCurrentSize() {
		return currentSize;
	}
	
	public boolean isComplete() {
		return complete;
	}
}
