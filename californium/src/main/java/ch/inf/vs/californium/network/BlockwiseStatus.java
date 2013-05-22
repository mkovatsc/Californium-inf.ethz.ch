
package ch.inf.vs.californium.network;

import java.util.ArrayList;

/**
 * This class represents the status of a blockwise transfer of a request or a
 * response.
 * 
 * This class is package private. Instances of this class are stored inside an
 * exchange and only accessed/modified by the class BlockwiseLayer.
 */
class BlockwiseStatus {

	protected int currentNum;
	protected int currentSzx;
	protected boolean complete;

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
	protected ArrayList<byte[]> blocks = new ArrayList<>();

	public BlockwiseStatus() { }

	public BlockwiseStatus(int num, int szx) {
		this.currentNum = num;
		this.currentSzx = szx;
	}
}
