package ch.ethz.inf.vs.californium.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ByteArrayUtils {

	/**
	 * Adds a padding to the given array, such that a new array with the given
	 * length is generated.
	 * 
	 * @param array
	 *            the array to be padded.
	 * @param value
	 *            the padding value.
	 * @param newLength
	 *            the new length of the padded array.
	 * @return the array padded with the given value.
	 */
	public static byte[] padArray(byte[] array, byte value, int newLength) {
		int length = array.length;
		int paddingLength = newLength - length;

		if (paddingLength < 1) {
			return array;
		} else {
			byte[] padding = new byte[paddingLength];
			Arrays.fill(padding, value);

			return concatenate(array, padding);
		}

	}

	/**
	 * Truncates the given array to the request length.
	 * 
	 * @param array
	 *            the array to be truncated.
	 * @param newLength
	 *            the new length.
	 * @return the truncated array.
	 */
	public static byte[] truncate(byte[] array, int newLength) {
		if (array.length < newLength) {
			return array;
		} else {
			byte[] truncated = new byte[newLength];
			System.arraycopy(array, 0, truncated, 0, newLength);

			return truncated;
		}
	}

	/**
	 * Concatenates two byte arrays.
	 * 
	 * @param a
	 *            the first array.
	 * @param b
	 *            the second array.
	 * @return the concatenated array.
	 */
	public static byte[] concatenate(byte[] a, byte[] b) {
		int lengthA = a.length;
		int lengthB = b.length;

		byte[] concat = new byte[lengthA + lengthB];

		System.arraycopy(a, 0, concat, 0, lengthA);
		System.arraycopy(b, 0, concat, lengthA, lengthB);

		return concat;
	}

	/**
	 * Computes array-wise XOR.
	 * 
	 * @param a
	 *            the first array.
	 * @param b
	 *            the second array.
	 * @return the XOR-ed array.
	 */
	public static byte[] xorArrays(byte[] a, byte[] b) {
		byte[] xor = new byte[a.length];

		for (int i = 0; i < a.length; i++) {
			xor[i] = (byte) (a[i] ^ b[i]);
		}

		return xor;
	}

	/**
	 * Splits the given array into blocks of given size and adds padding to the
	 * last one, if necessary.
	 * 
	 * @param byteArray
	 *            the array.
	 * @param blocksize
	 *            the block size.
	 * @return a list of blocks of given size.
	 */
	public static List<byte[]> splitAndPad(byte[] byteArray, int blocksize) {
		List<byte[]> blocks = new ArrayList<byte[]>();
		int numBlocks = (int) Math.ceil(byteArray.length / (double) blocksize);

		for (int i = 0; i < numBlocks; i++) {

			byte[] block = new byte[blocksize];
			Arrays.fill(block, (byte) 0x00);
			if (i + 1 == numBlocks) {
				// the last block
				int remainingBytes = byteArray.length - (i * blocksize);
				System.arraycopy(byteArray, i * blocksize, block, 0, remainingBytes);
			} else {
				System.arraycopy(byteArray, i * blocksize, block, 0, blocksize);
			}
			blocks.add(block);
		}

		return blocks;
	}
}
