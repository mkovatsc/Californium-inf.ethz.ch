package ch.inf.vs.californium.network;

import java.util.ArrayList;

import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

/**
 * We could call this a manager.
 */
public class BlockwiseStatus {

	protected int currentNum;
	protected int currentSzx;
	protected int currentSize;
	protected boolean complete;
	
	// TODO: There is redundancy here:
	protected ArrayList<Request> requests;
	protected ArrayList<Response> responses;
	
	public BlockwiseStatus() {
		this.requests = null;
		this.currentNum = 0;
		this.currentSzx = 0;
		this.requests = new ArrayList<>();
		this.responses = new ArrayList<>();
	}
	
}
