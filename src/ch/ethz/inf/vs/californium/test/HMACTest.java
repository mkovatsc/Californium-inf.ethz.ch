package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertArrayEquals;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import ch.ethz.inf.vs.californium.dtls.Handshaker;

public class HMACTest {

	@Test
	public void testVector1() {
		/*
		 * See http://tools.ietf.org/html/rfc2104, test HMAC implementation
		 */
		try {
			byte[] key = new byte[] { 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b, 0x0b };
			byte[] data = "Hi There".getBytes();

			MessageDigest md;
			md = MessageDigest.getInstance("MD5");

			byte[] result = Handshaker.doHMAC(md, key, data);
			byte[] expected = new byte[] { (byte) 0x92, (byte) 0x94, (byte) 0x72, (byte) 0x7a, (byte) 0x36, (byte) 0x38, (byte) 0xbb, (byte) 0x1c, (byte) 0x13, (byte) 0xf4, (byte) 0x8e, (byte) 0xf8, (byte) 0x15, (byte) 0x8b, (byte) 0xfc, (byte) 0x9d };

			assertArrayEquals(expected, result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testVector2() {
		/*
		 * See http://tools.ietf.org/html/rfc2104, test HMAC implementation
		 */
		try {
			byte[] key = "Jefe".getBytes();
			byte[] data = "what do ya want for nothing?".getBytes();

			MessageDigest md;
			md = MessageDigest.getInstance("MD5");

			byte[] result = Handshaker.doHMAC(md, key, data);
			byte[] expected = new byte[] { 0x75, 0x0c, 0x78, 0x3e, 0x6a, (byte) 0xb0, (byte) 0xb5, 0x03, (byte) 0xea, (byte) 0xa8, 0x6e, 0x31, 0x0a, 0x5d, (byte) 0xb7, 0x38 };

			assertArrayEquals(expected, result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testVector3() {
		/*
		 * See http://tools.ietf.org/html/rfc2104, test HMAC implementation
		 */
		try {
			byte[] key = new byte[] { (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA };
			byte[] data = new byte[] { (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD,
					(byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD,
					(byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD, (byte) 0xDD };

			MessageDigest md;
			md = MessageDigest.getInstance("MD5");

			byte[] result = Handshaker.doHMAC(md, key, data);
			byte[] expected = new byte[] { (byte) 0x56, (byte) 0xbe, (byte) 0x34, (byte) 0x52, (byte) 0x1d, (byte) 0x14, (byte) 0x4c, (byte) 0x88, (byte) 0xdb, (byte) 0xb8, (byte) 0xc7, (byte) 0x33, (byte) 0xf0, (byte) 0xe8, (byte) 0xb3, (byte) 0xf6 };

			assertArrayEquals(expected, result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
