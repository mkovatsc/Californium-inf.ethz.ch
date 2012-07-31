/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.test;

import org.junit.Test;
import static org.junit.Assert.*;

import ch.ethz.inf.vs.californium.dtls.CCMBlockCipher;
import ch.ethz.inf.vs.californium.dtls.HandshakeException;
import ch.ethz.inf.vs.californium.util.ByteArrayUtils;

public class CCMBlockCipherTest {

	@Test
	public void testPacketVector1() throws HandshakeException {
		/*
		 * See http://tools.ietf.org/html/rfc3610#section-8: Packet Vector #1
		 */
		byte[] aesKey = new byte[] { (byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7, (byte) 0xC8, (byte) 0xC9, (byte) 0xCA, (byte) 0xCB, (byte) 0xCC, (byte) 0xCD, (byte) 0xCE, (byte) 0xCF };
		byte[] nonce = new byte[] { 0x00, 0x00, 0x00, 0x03, 0x02, 0x01, 0x00, (byte) 0xA0, (byte) 0xA1, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5 };
		byte[] a = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
		byte[] m = new byte[] { 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E };
		byte[] expectedC = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x58, (byte) 0x8C, (byte) 0x97, (byte) 0x9A, 0x61, (byte) 0xC6, 0x63, (byte) 0xD2, (byte) 0xF0, 0x66, (byte) 0xD0, (byte) 0xC2, (byte) 0xC0, (byte) 0xF9,
				(byte) 0x89, (byte) 0x80, 0x6D, 0x5F, 0x6B, 0x61, (byte) 0xDA, (byte) 0xC3, (byte) 0x84, 0x17, (byte) 0xE8, (byte) 0xD1, 0x2C, (byte) 0xFD, (byte) 0xF9, 0x26, (byte) 0xE0 };

		byte[] encrypted = CCMBlockCipher.encrypt(aesKey, nonce, a, m, 8);
		byte[] c = ByteArrayUtils.concatenate(a, encrypted);

		assertArrayEquals(expectedC, c);

		byte[] decrypted = CCMBlockCipher.decrypt(aesKey, nonce, a, encrypted, 8);

		assertArrayEquals(m, decrypted);
	}

	@Test
	public void testPacketVector4() throws HandshakeException {
		/*
		 * See http://tools.ietf.org/html/rfc3610#section-8: Packet Vector #4
		 */
		byte[] aesKey = new byte[] { (byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7, (byte) 0xC8, (byte) 0xC9, (byte) 0xCA, (byte) 0xCB, (byte) 0xCC, (byte) 0xCD, (byte) 0xCE, (byte) 0xCF };
		byte[] nonce = new byte[] { 0x00, 0x00, 0x00, 0x06, 0x05, 0x04, 0x03, (byte) 0xA0, (byte) 0xA1, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5 };
		byte[] a = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B };
		byte[] m = new byte[] { 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E };
		byte[] expectedC = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, (byte) 0xA2, (byte) 0x8C, 0x68, 0x65, (byte) 0x93, (byte) 0x9A, (byte) 0x9A, 0x79, (byte) 0xFA, (byte) 0xAA, 0x5C, 0x4C, 0x2A,
				(byte) 0x9D, 0x4A, (byte) 0x91, (byte) 0xCD, (byte) 0xAC, (byte) 0x8C, (byte) 0x96, (byte) 0xC8, 0x61, (byte) 0xB9, (byte) 0xC9, (byte) 0xE6, 0x1E, (byte) 0xF1 };

		byte[] encrypted = CCMBlockCipher.encrypt(aesKey, nonce, a, m, 8);
		byte[] c = ByteArrayUtils.concatenate(a, encrypted);

		assertArrayEquals(expectedC, c);

		byte[] decrypted = CCMBlockCipher.decrypt(aesKey, nonce, a, encrypted, 8);

		assertArrayEquals(m, decrypted);
	}

	@Test
	public void testPacketVector14() throws HandshakeException {
		/*
		 * See http://tools.ietf.org/html/rfc3610#section-8: Packet Vector #14
		 */
		byte[] aesKey = ByteArrayUtils.hexStreamToByteArray("D7828D13B2B0BDC325A76236DF93CC6B");
		byte[] nonce = ByteArrayUtils.hexStreamToByteArray("0033568EF7B2633C9696766CFA");
		byte[] a = ByteArrayUtils.hexStreamToByteArray("63018F76DC8A1BCB");
		byte[] m = ByteArrayUtils.hexStreamToByteArray("9020EA6F91BDD85AFA0039BA4BAFF9BFB79C7028949CD0EC");
		byte[] expectedC = ByteArrayUtils.hexStreamToByteArray("63018F76DC8A1BCB4CCB1E7CA981BEFAA0726C55D378061298C85C92814ABC33C52EE81D7D77C08A");

		byte[] encrypted = CCMBlockCipher.encrypt(aesKey, nonce, a, m, 8);
		byte[] c = ByteArrayUtils.concatenate(a, encrypted);

		assertArrayEquals(expectedC, c);

		byte[] decrypted = CCMBlockCipher.decrypt(aesKey, nonce, a, encrypted, 8);

		assertArrayEquals(m, decrypted);
	}

	@Test
	public void testPacketVector15() throws HandshakeException {
		/*
		 * See http://tools.ietf.org/html/rfc3610#section-8: Packet Vector #15
		 */
		byte[] aesKey = new byte[] { (byte) 0xD7, (byte) 0x82, (byte) 0x8D, (byte) 0x13, (byte) 0xB2, (byte) 0xB0, (byte) 0xBD, (byte) 0xC3, (byte) 0x25, (byte) 0xA7, (byte) 0x62, (byte) 0x36, (byte) 0xDF, (byte) 0x93, (byte) 0xCC, (byte) 0x6B };
		byte[] nonce = new byte[] { (byte) 0x00, (byte) 0x10, (byte) 0x3F, (byte) 0xE4, (byte) 0x13, (byte) 0x36, (byte) 0x71, (byte) 0x3C, (byte) 0x96, (byte) 0x96, (byte) 0x76, (byte) 0x6C, (byte) 0xFA };
		byte[] a = new byte[] { (byte) 0xAA, (byte) 0x6C, (byte) 0xFA, (byte) 0x36, (byte) 0xCA, (byte) 0xE8, (byte) 0x6B, (byte) 0x40 };
		byte[] m = new byte[] { (byte) 0xB9, (byte) 0x16, (byte) 0xE0, (byte) 0xEA, (byte) 0xCC, (byte) 0x1C, (byte) 0x00, (byte) 0xD7, (byte) 0xDC, (byte) 0xEC, (byte) 0x68, (byte) 0xEC, (byte) 0x0B, (byte) 0x3B, (byte) 0xBB, (byte) 0x1A,
				(byte) 0x02, (byte) 0xDE, (byte) 0x8A, (byte) 0x2D, (byte) 0x1A, (byte) 0xA3, (byte) 0x46, (byte) 0x13, (byte) 0x2E };
		byte[] expectedC = new byte[] { (byte) 0xAA, (byte) 0x6C, (byte) 0xFA, (byte) 0x36, (byte) 0xCA, (byte) 0xE8, (byte) 0x6B, (byte) 0x40, (byte) 0xB1, (byte) 0xD2, (byte) 0x3A, (byte) 0x22, (byte) 0x20, (byte) 0xDD, (byte) 0xC0, (byte) 0xAC,
				(byte) 0x90, (byte) 0x0D, (byte) 0x9A, (byte) 0xA0, (byte) 0x3C, (byte) 0x61, (byte) 0xFC, (byte) 0xF4, (byte) 0xA5, (byte) 0x59, (byte) 0xA4, (byte) 0x41, (byte) 0x77, (byte) 0x67, (byte) 0x08, (byte) 0x97, (byte) 0x08, (byte) 0xA7,
				(byte) 0x76, (byte) 0x79, (byte) 0x6E, (byte) 0xDB, (byte) 0x72, (byte) 0x35, (byte) 0x06 };

		byte[] encrypted = CCMBlockCipher.encrypt(aesKey, nonce, a, m, 8);
		byte[] c = ByteArrayUtils.concatenate(a, encrypted);

		assertArrayEquals(expectedC, c);

		byte[] decrypted = CCMBlockCipher.decrypt(aesKey, nonce, a, encrypted, 8);

		assertArrayEquals(m, decrypted);
	}

	@Test
	public void testPacketVector24() throws HandshakeException {
		/*
		 * See http://tools.ietf.org/html/rfc3610#section-8: Packet Vector #24
		 */
		byte[] aesKey = new byte[] { (byte) 0xD7, (byte) 0x82, (byte) 0x8D, (byte) 0x13, (byte) 0xB2, (byte) 0xB0, (byte) 0xBD, (byte) 0xC3, (byte) 0x25, (byte) 0xA7, (byte) 0x62, (byte) 0x36, (byte) 0xDF, (byte) 0x93, (byte) 0xCC, (byte) 0x6B };
		byte[] nonce = new byte[] { (byte) 0x00, (byte) 0x8D, (byte) 0x49, (byte) 0x3B, (byte) 0x30, (byte) 0xAE, (byte) 0x8B, (byte) 0x3C, (byte) 0x96, (byte) 0x96, (byte) 0x76, (byte) 0x6C, (byte) 0xFA };
		byte[] a = new byte[] { (byte) 0x6E, (byte) 0x37, (byte) 0xA6, (byte) 0xEF, (byte) 0x54, (byte) 0x6D, (byte) 0x95, (byte) 0x5D, (byte) 0x34, (byte) 0xAB, (byte) 0x60, (byte) 0x59 };
		byte[] m = new byte[] { (byte) 0xAB, (byte) 0xF2, (byte) 0x1C, (byte) 0x0B, (byte) 0x02, (byte) 0xFE, (byte) 0xB8, (byte) 0x8F, (byte) 0x85, (byte) 0x6D, (byte) 0xF4, (byte) 0xA3, (byte) 0x73, (byte) 0x81, (byte) 0xBC, (byte) 0xE3,
				(byte) 0xCC, (byte) 0x12, (byte) 0x85, (byte) 0x17, (byte) 0xD4 };
		byte[] expectedC = new byte[] { (byte) 0x6E, (byte) 0x37, (byte) 0xA6, (byte) 0xEF, (byte) 0x54, (byte) 0x6D, (byte) 0x95, (byte) 0x5D, (byte) 0x34, (byte) 0xAB, (byte) 0x60, (byte) 0x59, (byte) 0xF3, (byte) 0x29, (byte) 0x05, (byte) 0xB8,
				(byte) 0x8A, (byte) 0x64, (byte) 0x1B, (byte) 0x04, (byte) 0xB9, (byte) 0xC9, (byte) 0xFF, (byte) 0xB5, (byte) 0x8C, (byte) 0xC3, (byte) 0x90, (byte) 0x90, (byte) 0x0F, (byte) 0x3D, (byte) 0xA1, (byte) 0x2A, (byte) 0xB1, (byte) 0x6D,
				(byte) 0xCE, (byte) 0x9E, (byte) 0x82, (byte) 0xEF, (byte) 0xA1, (byte) 0x6D, (byte) 0xA6, (byte) 0x20, (byte) 0x59 };

		byte[] encrypted = CCMBlockCipher.encrypt(aesKey, nonce, a, m, 10);
		byte[] c = ByteArrayUtils.concatenate(a, encrypted);

		assertArrayEquals(expectedC, c);

		byte[] decrypted = CCMBlockCipher.decrypt(aesKey, nonce, a, encrypted, 10);

		assertArrayEquals(m, decrypted);

	}

	@Test
	public void testExampleVector1() throws HandshakeException {
		/*
		 * Found here:
		 * http://csrc.nist.gov/publications/nistpubs/800-38C/SP800-38
		 * C_updated-July20_2007.pdf: C.1 Example 1
		 */

		byte[] aesKey = ByteArrayUtils.hexStreamToByteArray("404142434445464748494a4b4c4d4e4f");
		byte[] nonce = ByteArrayUtils.hexStreamToByteArray("10111213141516");
		byte[] a = ByteArrayUtils.hexStreamToByteArray("0001020304050607");
		byte[] m = ByteArrayUtils.hexStreamToByteArray("20212223");
		byte[] expectedC = ByteArrayUtils.hexStreamToByteArray("7162015b4dac255d");

		byte[] encrypted = CCMBlockCipher.encrypt(aesKey, nonce, a, m, 4);
		assertArrayEquals(expectedC, encrypted);

		byte[] decrypted = CCMBlockCipher.decrypt(aesKey, nonce, a, encrypted, 4);
		assertArrayEquals(m, decrypted);
	}

	@Test
	public void testExampleVector2() throws HandshakeException {
		/*
		 * Found here:
		 * http://csrc.nist.gov/publications/nistpubs/800-38C/SP800-38
		 * C_updated-July20_2007.pdf: C.2 Example 2
		 */

		byte[] aesKey = ByteArrayUtils.hexStreamToByteArray("404142434445464748494a4b4c4d4e4f");
		byte[] nonce = ByteArrayUtils.hexStreamToByteArray("1011121314151617");
		byte[] a = ByteArrayUtils.hexStreamToByteArray("000102030405060708090a0b0c0d0e0f");
		byte[] m = ByteArrayUtils.hexStreamToByteArray("202122232425262728292a2b2c2d2e2f");
		byte[] expectedC = ByteArrayUtils.hexStreamToByteArray("d2a1f0e051ea5f62081a7792073d593d1fc64fbfaccd");

		byte[] encrypted = CCMBlockCipher.encrypt(aesKey, nonce, a, m, 6);
		assertArrayEquals(expectedC, encrypted);

		byte[] decrypted = CCMBlockCipher.decrypt(aesKey, nonce, a, encrypted, 6);
		assertArrayEquals(m, decrypted);
	}

	@Test
	public void testExampleVector3() throws HandshakeException {
		/*
		 * Found here:
		 * http://csrc.nist.gov/publications/nistpubs/800-38C/SP800-38
		 * C_updated-July20_2007.pdf: C.3 Example 3
		 */

		byte[] aesKey = ByteArrayUtils.hexStreamToByteArray("404142434445464748494a4b4c4d4e4f");
		byte[] nonce = ByteArrayUtils.hexStreamToByteArray("101112131415161718191a1b");
		byte[] a = ByteArrayUtils.hexStreamToByteArray("000102030405060708090a0b0c0d0e0f10111213");
		byte[] m = ByteArrayUtils.hexStreamToByteArray("202122232425262728292a2b2c2d2e2f3031323334353637");
		byte[] expectedC = ByteArrayUtils.hexStreamToByteArray("e3b201a9f5b71a7a9b1ceaeccd97e70b6176aad9a4428aa5484392fbc1b09951");

		byte[] encrypted = CCMBlockCipher.encrypt(aesKey, nonce, a, m, 8);
		assertArrayEquals(expectedC, encrypted);

		byte[] decrypted = CCMBlockCipher.decrypt(aesKey, nonce, a, encrypted, 8);
		assertArrayEquals(m, decrypted);
	}

	@Test
	public void testExampleVector4() throws HandshakeException {
		/*
		 * Found here:
		 * http://csrc.nist.gov/publications/nistpubs/800-38C/SP800-38
		 * C_updated-July20_2007.pdf: C.4 Example 4
		 */

		byte[] aesKey = ByteArrayUtils.hexStreamToByteArray("404142434445464748494a4b4c4d4e4f");
		byte[] nonce = ByteArrayUtils.hexStreamToByteArray("101112131415161718191a1b1c");
		byte[] aPart = ByteArrayUtils
				.hexStreamToByteArray("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff");
		byte[] a = new byte[] {};
		// create a with length = 524288
		for (int i = 0; i < 256; i++) {
			a = ByteArrayUtils.concatenate(a, aPart);
		}
		byte[] m = ByteArrayUtils.hexStreamToByteArray("202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f");
		byte[] expectedC = ByteArrayUtils.hexStreamToByteArray("69915dad1e84c6376a68c2967e4dab615ae0fd1faec44cc484828529463ccf72b4ac6bec93e8598e7f0dadbcea5b");

		byte[] encrypted = CCMBlockCipher.encrypt(aesKey, nonce, a, m, 14);
		assertArrayEquals(expectedC, encrypted);

		byte[] decrypted = CCMBlockCipher.decrypt(aesKey, nonce, a, encrypted, 14);
		assertArrayEquals(m, decrypted);
	}
}
