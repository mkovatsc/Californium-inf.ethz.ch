package ch.ethz.inf.vs.californium.dtls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import ch.ethz.inf.vs.californium.util.ByteArrayUtils;

public final class CCMBlockCipher {

	/**
	 * CCM is only defined for use with 128-bit block ciphers, such as AES.
	 */
	private static final int BLOCK_SIZE = 16;

	private static final String BLOCK_CIPHER = "AES";

	/**
	 * See <a href="http://tools.ietf.org/html/rfc3610#section-2.5">RFC 3610</a>
	 * for details.
	 * 
	 * @param key
	 *            the encryption key K.
	 * @param nonce
	 *            the nonce N.
	 * @param a
	 *            the additional authenticated data a.
	 * @param c
	 *            the encrypted and authenticated message c.
	 * @param authenticationBytes
	 *            Number of octets in authentication field.
	 * @return the byte[]
	 */
	public static byte[] decrypt(byte[] key, byte[] nonce, byte[] a, byte[] c, int authenticationBytes) {
		try {
			long lengthM = c.length - authenticationBytes;

			Cipher cipher = Cipher.getInstance(BLOCK_CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, BLOCK_CIPHER));

			/*
			 * Decryption starts by recomputing the key stream to recover the
			 * message m and the MAC value T.
			 */
			List<byte[]> S_i = generateKeyStreamBlocks(lengthM, nonce, cipher);
			byte[] encryptedM = new byte[(int) lengthM];
			System.arraycopy(c, 0, encryptedM, 0, (int) lengthM);

			byte[] concatedS_i = new byte[0];
			int numRounds = (int) (Math.ceil(lengthM / (double) BLOCK_SIZE) + 1);
			for (int i = 1; i < numRounds; i++) {
				concatedS_i = ByteArrayUtils.concatenate(concatedS_i, S_i.get(i));
			}
			byte[] m = ByteArrayUtils.xorArrays(encryptedM, concatedS_i);

			byte[] encryptedT = new byte[authenticationBytes];
			System.arraycopy(c, (int) lengthM, encryptedT, 0, authenticationBytes);
			byte[] T = ByteArrayUtils.xorArrays(encryptedT, ByteArrayUtils.truncate(S_i.get(0), authenticationBytes));

			/*
			 * The message and additional authentication data is then used to
			 * recompute the CBC-MAC value and check T.
			 */
			byte[] mac = computeCbcMac(nonce, m, a, cipher, authenticationBytes);

			/*
			 * If the T value is not correct, the receiver MUST NOT reveal any
			 * information except for the fact that T is incorrect. The receiver
			 * MUST NOT reveal the decrypted message, the value T, or any other
			 * information.
			 */
			if (Arrays.equals(T, mac)) {
				return m;
			} else {
				return null;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * See <a href="http://tools.ietf.org/html/rfc3610#section-2.2">RFC 3610</a>
	 * for details.
	 * 
	 * @param key
	 *            the encryption key K.
	 * @param nonce
	 *            the nonce N.
	 * @param a
	 *            the additional authenticated data a.
	 * @param m
	 *            the message to authenticate and encrypt.
	 * @param authenticationBytes
	 *            Number of octets in authentication field.
	 * @return the encrypted and authenticated message.
	 */
	public static byte[] encrypt(byte[] key, byte[] nonce, byte[] a, byte[] m, int authenticationBytes) {
		try {
			long lengthM = m.length;
			Cipher cipher = Cipher.getInstance(BLOCK_CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, BLOCK_CIPHER));

			// Authentication: http://tools.ietf.org/html/rfc3610#section-2.2

			byte[] T = computeCbcMac(nonce, m, a, cipher, authenticationBytes);

			// Encryption http://tools.ietf.org/html/rfc3610#section-2.3

			List<byte[]> S_i = generateKeyStreamBlocks(lengthM, nonce, cipher);

			/*
			 * The message is encrypted by XORing the octets of message m with
			 * the first l(m) octets of the concatenation of S_1, S_2, S_3, ...
			 * . Note that S_0 is not used to encrypt the message.
			 */
			byte[] concatedS_i = new byte[0];
			int numRounds = (int) (Math.ceil(lengthM / (double) BLOCK_SIZE) + 1);
			for (int i = 1; i < numRounds; i++) {
				concatedS_i = ByteArrayUtils.concatenate(concatedS_i, S_i.get(i));
			}
			byte[] encryptedMessage = ByteArrayUtils.xorArrays(m, concatedS_i);

			// U := T XOR first-M-bytes( S_0 )
			byte[] U = ByteArrayUtils.xorArrays(T, ByteArrayUtils.truncate(S_i.get(0), authenticationBytes));

			/*
			 * The final result c consists of the encrypted message followed by
			 * the encrypted authentication value U.
			 */
			byte[] c = ByteArrayUtils.concatenate(encryptedMessage, U);

			return c;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Computes CBC-MAC. See <a
	 * href="http://tools.ietf.org/html/rfc3610#section-2.2">RFC 3610 -
	 * Authentication</a> for details.
	 * 
	 * @param nonce
	 *            the nonce.
	 * @param m
	 *            the message to authenticate and encrypt.
	 * @param a
	 *            the additional authenticated data.
	 * @param cipher
	 *            the cipher.
	 * @param authenticationBytes
	 *            Number of octets in authentication field.
	 * @return the CBC-MAC
	 * @throws Exception
	 *             if cipher can not be realized.
	 */
	private static byte[] computeCbcMac(byte[] nonce, byte[] m, byte[] a, Cipher cipher, int authenticationBytes) throws Exception {
		long lengthM = m.length;
		int lengthA = a.length;
		int L = 15 - nonce.length;

		// build first block B_0
		// Octet Number Contents
		// ------------ ---------
		// 0 Flags
		// 1 ... 15-L Nonce N
		// 16-L ... 15 l(m)
		byte[] b0 = new byte[BLOCK_SIZE];

		int adata = 0;
		// The Adata bit is set to zero if l(a)=0, and set to one if l(a)>0
		if (lengthA > 0) {
			adata = 1;
		}
		// M' field is set to (M-2)/2
		int mPrime = (authenticationBytes - 2) / 2;
		// L' = L-1 (the zero value is reserved)
		int lPrime = L - 1;

		// Bit Number Contents
		// ---------- ----------------------
		// 7 Reserved (always zero)
		// 6 Adata
		// 5 ... 3 M'
		// 2 ... 0 L'

		// Flags = 64*Adata + 8*M' + L'
		b0[0] = (byte) (64 * adata + 8 * mPrime + lPrime);

		// 1 ... 15-L Nonce N
		System.arraycopy(nonce, 0, b0, 1, nonce.length);

		b0[14] = (byte) (lengthM >> 8);
		b0[15] = (byte) (lengthM);

		List<byte[]> blocks = new ArrayList<byte[]>();

		// If l(a)>0 (as indicated by the Adata field), then one or more blocks
		// of authentication data are added.
		if (lengthA > 0) {
			// First two octets Followed by Comment
			// ----------------- ----------------
			// -------------------------------
			// 0x0000 Nothing Reserved
			// 0x0001 ... 0xFEFF Nothing For 0 < l(a) < (2^16 - 2^8)
			// 0xFF00 ... 0xFFFD Nothing Reserved
			// 0xFFFE 4 octets of l(a) For (2^16 - 2^8) <= l(a) < 2^32
			// 0xFFFF 8 octets of l(a) For 2^32 <= l(a) < 2^64

			// 2^16 - 2^8
			int first = 65280;
			// 2^32
			long second = 4294967296L;

			byte[] aEncoded;
			if (lengthA > 0 && lengthA < first) {
				/*
				 * The blocks encoding a are formed by concatenating this string
				 * that encodes l(a) with a itself, and splitting the result
				 * into 16-octet blocks, and then padding the last block with
				 * zeroes if necessary.
				 */

				long length = 2 + lengthA;
				aEncoded = new byte[(int) length];

				aEncoded[0] = (byte) (lengthA >> 8);
				aEncoded[1] = (byte) (lengthA);

				System.arraycopy(a, 0, aEncoded, 2, a.length);

				blocks.addAll(ByteArrayUtils.splitAndPadd(aEncoded, BLOCK_SIZE));
			} else if (lengthA >= first && lengthA < second) {
				// TODO
			} else {
				// TODO
			}
		}
		/*
		 * After the (optional) additional authentication blocks have been
		 * added, we add the message blocks. The message blocks are formed by
		 * splitting the message m into 16-octet blocks, and then padding the
		 * last block with zeroes if necessary. If the message m consists of the
		 * empty string, then no blocks are added in this step.
		 */
		blocks.addAll(ByteArrayUtils.splitAndPadd(m, BLOCK_SIZE));

		byte[] X_i;
		// X_1 := E( K, B_0 )
		X_i = ByteArrayUtils.truncate(cipher.doFinal(b0), BLOCK_SIZE);

		// X_i+1 := E( K, X_i XOR B_i ) for i=1, ..., n
		for (byte[] block : blocks) {
			byte[] xor = ByteArrayUtils.xorArrays(block, X_i);
			X_i = ByteArrayUtils.truncate(cipher.doFinal(xor), BLOCK_SIZE);
		}

		// T := first-M-bytes( X_n+1 )
		byte[] T = ByteArrayUtils.truncate(X_i, authenticationBytes);
		return T;
	}

	/**
	 * See <a href="http://tools.ietf.org/html/rfc3610#section-2.3">RFC 3610 -
	 * Key Stream Blocks</a> for details.
	 * 
	 * @param lengthM
	 *            the length of the message.
	 * @param nonce
	 *            the nonce.
	 * @param cipher
	 *            the cipher.
	 * @return the key stream blocks.
	 * @throws Exception
	 *             if the cipher can not be realized.
	 */
	private static List<byte[]> generateKeyStreamBlocks(long lengthM, byte[] nonce, Cipher cipher) throws Exception {
		int L = 15 - nonce.length;

		List<byte[]> S_i = new ArrayList<byte[]>();
		// S_i := E( K, A_i ) for i=0, 1, 2, ...
		int numRounds = (int) (Math.ceil(lengthM / (double) BLOCK_SIZE) + 1);
		for (int i = 0; i < numRounds; i++) {
			byte[] S = new byte[BLOCK_SIZE];

			// Octet Number Contents
			// ------------ ---------
			// 0 Flags
			// 1 ... 15-L Nonce N
			// 16-L ... 15 Counter i

			int flag = L - 1;
			S[0] = (byte) flag;
			System.arraycopy(nonce, 0, S, 1, nonce.length);

			for (int j = L; j > 0; j--) {
				S[BLOCK_SIZE - j] = (byte) (i >> (j - 1) * 8);
			}
			S_i.add(ByteArrayUtils.truncate(cipher.doFinal(S), BLOCK_SIZE));
		}

		return S_i;
	}
}
