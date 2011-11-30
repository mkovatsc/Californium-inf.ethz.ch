package ch.ethz.inf.vs.californium.util;

import java.io.ByteArrayOutputStream;

/*
 * This class describes the functionality to write raw
 * network-ordered datagrams on bit-level.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class DatagramWriter {

	// Constructors ////////////////////////////////////////////////////////////

	/*
	 * Initializes a new BitWriter object
	 */
	public DatagramWriter() {

		// initialize underlying byte stream
		byteStream = new ByteArrayOutputStream();

		// initialize bit buffer
		currentByte = 0;
		currentBitIndex = Byte.SIZE - 1;
	}

	// Methods /////////////////////////////////////////////////////////////////

	/*
	 * Writes a sequence of bits to the stream
	 * 
	 * @param data An integer containing the bits to write
	 * 
	 * @param numBits The number of bits to write
	 */
	public void write(int data, int numBits) {

		if (numBits < 32 && data >= (1 << numBits)) {
			System.out.printf(
					"[%s] Warning: Truncating value %d to %d-bit integer\n",
					getClass().getName(), data, numBits);
		}

		for (int i = numBits - 1; i >= 0; i--) {

			// test bit
			boolean bit = (data >> i & 1) != 0;
			if (bit) {
				// set bit in current byte
				currentByte |= (1 << currentBitIndex);
			}

			// decrease current bit index
			--currentBitIndex;

			// check if current byte can be written
			if (currentBitIndex < 0) {
				writeCurrentByte();
			}
		}
	}

	/*
	 * Writes a sequence of bytes to the stream
	 * 
	 * @param bytes The sequence of bytes to write
	 */
	public void writeBytes(byte[] bytes) {

		// check if anything to do at all
		if (bytes == null)
			return;

		// are there bits left to write in buffer?
		if (currentBitIndex < Byte.SIZE - 1) {

			for (int i = 0; i < bytes.length; i++) {
				write(bytes[i], Byte.SIZE);
			}

		} else {

			// if bit buffer is empty, call can be delegated
			// to byte stream to increase
			byteStream.write(bytes, 0, bytes.length);
		}
	}

	// Functions ///////////////////////////////////////////////////////////////

	/*
	 * Returns a byte array containing the sequence of bits written
	 * 
	 * @Return The byte array containing the written bits
	 */
	public byte[] toByteArray() {

		// write any bits left in the buffer to the stream
		writeCurrentByte();

		// retrieve the byte array from the stream
		byte[] byteArray = byteStream.toByteArray();

		// reset stream for the sake of consistency
		byteStream.reset();

		// return the byte array
		return byteArray;
	}

	// Utilities ///////////////////////////////////////////////////////////////

	/*
	 * Writes pending bits to the stream
	 */
	private void writeCurrentByte() {

		if (currentBitIndex < Byte.SIZE - 1) {

			byteStream.write(currentByte);

			currentByte = 0;
			currentBitIndex = Byte.SIZE - 1;
		}
	}

	// Attributes //////////////////////////////////////////////////////////////

	private ByteArrayOutputStream byteStream;

	private byte currentByte;
	private int currentBitIndex;

}
