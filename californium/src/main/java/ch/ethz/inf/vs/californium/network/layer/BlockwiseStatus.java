
package ch.ethz.inf.vs.californium.network.layer;

import java.util.ArrayList;

/**
 * This class represents the status of a blockwise transfer of a request or a
 * response.
 * 
 * This class is package private. Instances of this class are stored inside an
 * exchange and only accessed/modified by the class BlockwiseLayer.
 */
public class BlockwiseStatus {

	/** The current num. */
	private int currentNum;
	
	/** The current szx. */
	private int currentSzx;
	
	/** Indicates whether the blockwise transfer has completed. */
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
	/** The blocks. */
	protected ArrayList<byte[]> blocks = new ArrayList<>(); // TODO: make private

	/**
	 * Instantiates a new blockwise status.
	 */
	public BlockwiseStatus() { }

	/**
	 * Instantiates a new blockwise status.
	 *
	 * @param num the num
	 * @param szx the szx
	 */
	public BlockwiseStatus(int num, int szx) {
		this.currentNum = num;
		this.currentSzx = szx;
	}

	/**
	 * Gets the current num.
	 *
	 * @return the current num
	 */
	public int getCurrentNum() {
		return currentNum;
	}

	/**
	 * Sets the current num.
	 *
	 * @param currentNum the new current num
	 */
	public void setCurrentNum(int currentNum) {
		this.currentNum = currentNum;
	}

	/**
	 * Gets the current szx.
	 *
	 * @return the current szx
	 */
	public int getCurrentSzx() {
		return currentSzx;
	}

	/**
	 * Sets the current szx.
	 *
	 * @param currentSzx the new current szx
	 */
	public void setCurrentSzx(int currentSzx) {
		this.currentSzx = currentSzx;
	}

	/**
	 * Checks if is complete.
	 *
	 * @return true, if is complete
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * Sets the complete.
	 *
	 * @param complete the new complete
	 */
	public void setComplete(boolean complete) {
		this.complete = complete;
	}
	
	@Override
	public String toString() {
		return String.format("[currentNum=%d, currentSzx=%d, complete=%b]",
				currentNum, currentSzx, complete);
	}
}
