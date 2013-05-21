package ch.inf.vs.californium.network;

import java.util.ArrayList;

import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.Request;

/**
 * We could call this a manager.
 */
public class RequestBlockAssembler {

	private Request request;
	
	private int currentNum;
	private int currentSzx;
	private int currentSize;
	private boolean complete;
	
	public RequestBlockAssembler(Request request) {
		this.request = request;
		this.blocks = null;
		this.currentNum = 0;
		this.currentSzx = 0;
	}
	
	public Request getBlock(int szx, int num) {
		this.currentSzx = szx;
		this.currentNum = num;
		Request block = new Request(request.getCode());
		block.setOptions(request.getOptions());
		block.setDestination(request.getDestination());
		block.setDestinationPort(request.getDestinationPort());
		block.setToken(request.getToken());
		block.setType(Type.CON);
		
		this.currentSize = 1 << (4 + szx);
		int from = num * currentSize;
		int to = Math.min((num + 1) * currentSize, request.getPayloadSize());
		int length = to - from;
		byte[] blockPayload = new byte[length];
		System.arraycopy(request.getPayload(), from, blockPayload, 0, length);
		block.setPayload(blockPayload);
		
		boolean m = (to < request.getPayloadSize());
		block.getOptions().setBlock1(szx, m, num);
		
		this.complete = !m;
		return block;
	}
	
	private ArrayList<Request> blocks;
	
	public RequestBlockAssembler() {
		this.request = null;
		this.blocks = new ArrayList<>();
		this.currentNum = 0;
		this.currentSzx = 0;
	}
	
	public void insert(Request block) {
		blocks.add(block);
	}
	
	public Request getAssembledRequest() {
		Request last = blocks.get(blocks.size() - 1);
		Request request = new Request(last.getCode());
		request.setMid(last.getMid());
		request.setSource(last.getSource());
		request.setSourcePort(last.getSourcePort());
		request.setToken(last.getToken());
		request.setType(last.getType());
		request.setOptions(last.getOptions());
		
		int length = 0;
		for (Request block:blocks)
			length += block.getPayloadSize();
		
		byte[] payload = new byte[length];
		int offset = 0;
		for (Request block:blocks) {
			int blocklength = block.getPayloadSize();
			System.arraycopy(block.getPayload(), 0, payload, offset, blocklength);
			offset += blocklength;
		}
		request.setPayload(payload);
		return request;
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
