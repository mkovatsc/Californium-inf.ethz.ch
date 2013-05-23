
package ch.inf.vs.californium.network.layer;

import java.util.ArrayList;

/**
 * This class represents the status of a blockwise transfer of a request or a
 * response.
 * 
 * This class is package private. Instances of this class are stored inside an
 * exchange and only accessed/modified by the class BlockwiseLayer.
 */
public class BlockwiseStatus {

	private int currentNum;
	private int currentSzx;
	private boolean complete;

	/*
	 * Unfortunately, we cannot use a ByteBuffer and just insert one payload
	 * after another. If a blockwise request is answered with a blockwise
	 * response, the first and second payload blocks are sent concurrently
	 * (blockwise-11). They might arrive out of order. If the first block goes
	 * lost, the client resends the last request block. Until the first response
	 * block arrives we might already have collected several response blocks.
	 * This is also the reason, why synchronization is required. (=>TODO)
	 */
	// Container for the payload of all blocks
	protected ArrayList<byte[]> blocks = new ArrayList<>(); // TODO: make private

	public BlockwiseStatus() { }

	public BlockwiseStatus(int num, int szx) {
		this.currentNum = num;
		this.currentSzx = szx;
	}

	public int getCurrentNum() {
		return currentNum;
	}

	public void setCurrentNum(int currentNum) {
		this.currentNum = currentNum;
	}

	public int getCurrentSzx() {
		return currentSzx;
	}

	public void setCurrentSzx(int currentSzx) {
		this.currentSzx = currentSzx;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}
}
