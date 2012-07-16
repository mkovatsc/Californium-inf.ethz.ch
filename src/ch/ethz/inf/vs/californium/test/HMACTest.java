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

import static org.junit.Assert.assertArrayEquals;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import ch.ethz.inf.vs.californium.dtls.Handshaker;
import ch.ethz.inf.vs.californium.util.ByteArrayUtils;

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

	@Test
	public void testCase1() {
		/*
		 * See http://tools.ietf.org/html/rfc4231#section-4.2
		 */
		try {
			byte[] key = new byte[] { (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b,
					(byte) 0x0b, (byte) 0x0b, (byte) 0x0b, (byte) 0x0b };
			byte[] data = "Hi There".getBytes();

			MessageDigest md;
			md = MessageDigest.getInstance("SHA-256");

			byte[] result = Handshaker.doHMAC(md, key, data);
			byte[] expected = new byte[] { (byte) 0xb0, (byte) 0x34, (byte) 0x4c, (byte) 0x61, (byte) 0xd8, (byte) 0xdb, (byte) 0x38, (byte) 0x53, (byte) 0x5c, (byte) 0xa8, (byte) 0xaf, (byte) 0xce, (byte) 0xaf, (byte) 0x0b, (byte) 0xf1,
					(byte) 0x2b, (byte) 0x88, (byte) 0x1d, (byte) 0xc2, (byte) 0x00, (byte) 0xc9, (byte) 0x83, (byte) 0x3d, (byte) 0xa7, (byte) 0x26, (byte) 0xe9, (byte) 0x37, (byte) 0x6c, (byte) 0x2e, (byte) 0x32, (byte) 0xcf, (byte) 0xf7 };

			assertArrayEquals(expected, result);

			md = MessageDigest.getInstance("SHA-512");
			result = Handshaker.doHMAC(md, key, data);
			expected = new byte[] { (byte) 0x87, (byte) 0xaa, (byte) 0x7c, (byte) 0xde, (byte) 0xa5, (byte) 0xef, (byte) 0x61, (byte) 0x9d, (byte) 0x4f, (byte) 0xf0, (byte) 0xb4, (byte) 0x24, (byte) 0x1a, (byte) 0x1d, (byte) 0x6c, (byte) 0xb0,
					(byte) 0x23, (byte) 0x79, (byte) 0xf4, (byte) 0xe2, (byte) 0xce, (byte) 0x4e, (byte) 0xc2, (byte) 0x78, (byte) 0x7a, (byte) 0xd0, (byte) 0xb3, (byte) 0x05, (byte) 0x45, (byte) 0xe1, (byte) 0x7c, (byte) 0xde, (byte) 0xda,
					(byte) 0xa8, (byte) 0x33, (byte) 0xb7, (byte) 0xd6, (byte) 0xb8, (byte) 0xa7, (byte) 0x02, (byte) 0x03, (byte) 0x8b, (byte) 0x27, (byte) 0x4e, (byte) 0xae, (byte) 0xa3, (byte) 0xf4, (byte) 0xe4, (byte) 0xbe, (byte) 0x9d,
					(byte) 0x91, (byte) 0x4e, (byte) 0xeb, (byte) 0x61, (byte) 0xf1, (byte) 0x70, (byte) 0x2e, (byte) 0x69, (byte) 0x6c, (byte) 0x20, (byte) 0x3a, (byte) 0x12, (byte) 0x68, (byte) 0x54 };

			assertArrayEquals(expected, result);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCase2() {
		/*
		 * See http://tools.ietf.org/html/rfc4231#section-4.3
		 */
		try {
			byte[] key = "Jefe".getBytes();
			byte[] data = "what do ya want for nothing?".getBytes();

			MessageDigest md;
			md = MessageDigest.getInstance("SHA-256");

			byte[] result = Handshaker.doHMAC(md, key, data);
			byte[] expected = new byte[] { (byte) 0x5b, (byte) 0xdc, (byte) 0xc1, (byte) 0x46, (byte) 0xbf, (byte) 0x60, (byte) 0x75, (byte) 0x4e, (byte) 0x6a, (byte) 0x04, (byte) 0x24, (byte) 0x26, (byte) 0x08, (byte) 0x95, (byte) 0x75,
					(byte) 0xc7, (byte) 0x5a, (byte) 0x00, (byte) 0x3f, (byte) 0x08, (byte) 0x9d, (byte) 0x27, (byte) 0x39, (byte) 0x83, (byte) 0x9d, (byte) 0xec, (byte) 0x58, (byte) 0xb9, (byte) 0x64, (byte) 0xec, (byte) 0x38, (byte) 0x43 };

			assertArrayEquals(expected, result);

			md = MessageDigest.getInstance("SHA-512");
			result = Handshaker.doHMAC(md, key, data);
			expected = new byte[] { (byte) 0x16, (byte) 0x4b, (byte) 0x7a, (byte) 0x7b, (byte) 0xfc, (byte) 0xf8, (byte) 0x19, (byte) 0xe2, (byte) 0xe3, (byte) 0x95, (byte) 0xfb, (byte) 0xe7, (byte) 0x3b, (byte) 0x56, (byte) 0xe0, (byte) 0xa3,
					(byte) 0x87, (byte) 0xbd, (byte) 0x64, (byte) 0x22, (byte) 0x2e, (byte) 0x83, (byte) 0x1f, (byte) 0xd6, (byte) 0x10, (byte) 0x27, (byte) 0x0c, (byte) 0xd7, (byte) 0xea, (byte) 0x25, (byte) 0x05, (byte) 0x54, (byte) 0x97,
					(byte) 0x58, (byte) 0xbf, (byte) 0x75, (byte) 0xc0, (byte) 0x5a, (byte) 0x99, (byte) 0x4a, (byte) 0x6d, (byte) 0x03, (byte) 0x4f, (byte) 0x65, (byte) 0xf8, (byte) 0xf0, (byte) 0xe6, (byte) 0xfd, (byte) 0xca, (byte) 0xea,
					(byte) 0xb1, (byte) 0xa3, (byte) 0x4d, (byte) 0x4a, (byte) 0x6b, (byte) 0x4b, (byte) 0x63, (byte) 0x6e, (byte) 0x07, (byte) 0x0a, (byte) 0x38, (byte) 0xbc, (byte) 0xe7, (byte) 0x37 };

			assertArrayEquals(expected, result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCase3() {
		/*
		 * See http://tools.ietf.org/html/rfc4231#section-4.4
		 */
		try {
			byte[] key = ByteArrayUtils.hexStreamToByteArray("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
			byte[] data = ByteArrayUtils.hexStreamToByteArray("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");

			MessageDigest md;
			md = MessageDigest.getInstance("SHA-256");

			byte[] result = Handshaker.doHMAC(md, key, data);
			byte[] expected = ByteArrayUtils.hexStreamToByteArray("773ea91e36800e46854db8ebd09181a72959098b3ef8c122d9635514ced565fe");
			assertArrayEquals(expected, result);

			md = MessageDigest.getInstance("SHA-512");
			result = Handshaker.doHMAC(md, key, data);
			expected = ByteArrayUtils.hexStreamToByteArray("fa73b0089d56a284efb0f0756c890be9b1b5dbdd8ee81a3655f83e33b2279d39bf3e848279a722c806b485a47e67c807b946a337bee8942674278859e13292fb");

			assertArrayEquals(expected, result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCase4() {
		/*
		 * See http://tools.ietf.org/html/rfc4231#section-4.5
		 */
		try {
			byte[] key = ByteArrayUtils.hexStreamToByteArray("0102030405060708090a0b0c0d0e0f10111213141516171819");
			byte[] data = ByteArrayUtils.hexStreamToByteArray("cdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcd");

			MessageDigest md;
			md = MessageDigest.getInstance("SHA-256");

			byte[] result = Handshaker.doHMAC(md, key, data);
			byte[] expected = ByteArrayUtils.hexStreamToByteArray("82558a389a443c0ea4cc819899f2083a85f0faa3e578f8077a2e3ff46729665b");
			assertArrayEquals(expected, result);

			md = MessageDigest.getInstance("SHA-512");
			result = Handshaker.doHMAC(md, key, data);
			expected = ByteArrayUtils.hexStreamToByteArray("b0ba465637458c6990e5a8c5f61d4af7e576d97ff94b872de76f8050361ee3dba91ca5c11aa25eb4d679275cc5788063a5f19741120c4f2de2adebeb10a298dd");

			assertArrayEquals(expected, result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCase5() {
		/*
		 * See http://tools.ietf.org/html/rfc4231#section-4.6
		 */
		try {
			byte[] key = ByteArrayUtils.hexStreamToByteArray("0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c");
			byte[] data = ByteArrayUtils.hexStreamToByteArray("546573742057697468205472756e636174696f6e");

			MessageDigest md;
			md = MessageDigest.getInstance("SHA-256");

			byte[] result = ByteArrayUtils.truncate(Handshaker.doHMAC(md, key, data), 16);
			byte[] expected = ByteArrayUtils.hexStreamToByteArray("a3b6167473100ee06e0c796c2955552b");
			assertArrayEquals(expected, result);

			md = MessageDigest.getInstance("SHA-512");
			result = ByteArrayUtils.truncate(Handshaker.doHMAC(md, key, data), 16);
			expected = ByteArrayUtils.hexStreamToByteArray("415fad6271580a531d4179bc891d87a6");

			assertArrayEquals(expected, result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCase6() {
		/*
		 * See http://tools.ietf.org/html/rfc4231#section-4.7
		 */
		try {
			byte[] key = ByteArrayUtils
					.hexStreamToByteArray("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
			byte[] data = ByteArrayUtils.hexStreamToByteArray("54657374205573696e67204c6172676572205468616e20426c6f636b2d53697a65204b6579202d2048617368204b6579204669727374");

			MessageDigest md;
			md = MessageDigest.getInstance("SHA-256");

			byte[] result = Handshaker.doHMAC(md, key, data);
			byte[] expected = ByteArrayUtils.hexStreamToByteArray("60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54");
			assertArrayEquals(expected, result);
			
			md = MessageDigest.getInstance("SHA-384");
			result = Handshaker.doHMAC(md, key, data);
			expected = ByteArrayUtils.hexStreamToByteArray("4ece084485813e9088d2c63a041bc5b44f9ef1012a2b588f3cd11f05033ac4c60c2ef6ab4030fe8296248df163f44952");

			assertArrayEquals(expected, result);

			md = MessageDigest.getInstance("SHA-512");
			result = Handshaker.doHMAC(md, key, data);
			expected = ByteArrayUtils.hexStreamToByteArray("80b24263c7c1a3ebb71493c1dd7be8b49b46d1f41b4aeec1121b013783f8f3526b56d037e05f2598bd0fd2215d6a1e5295e64f73f63f0aec8b915a985d786598");

			assertArrayEquals(expected, result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCase7() {
		/*
		 * See http://tools.ietf.org/html/rfc4231#section-4.8
		 */
		try {
			byte[] key = new byte[] { (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
					(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
					(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
					(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
					(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
					(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
					(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
					(byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa };
			byte[] data = "This is a test using a larger than block-size key and a larger than block-size data. The key needs to be hashed before being used by the HMAC algorithm.".getBytes();

			MessageDigest md;
			md = MessageDigest.getInstance("SHA-256");

			byte[] result = Handshaker.doHMAC(md, key, data);
			byte[] expected = new byte[] { (byte) 0x9b, (byte) 0x09, (byte) 0xff, (byte) 0xa7, (byte) 0x1b, (byte) 0x94, (byte) 0x2f, (byte) 0xcb, (byte) 0x27, (byte) 0x63, (byte) 0x5f, (byte) 0xbc, (byte) 0xd5, (byte) 0xb0, (byte) 0xe9,
					(byte) 0x44, (byte) 0xbf, (byte) 0xdc, (byte) 0x63, (byte) 0x64, (byte) 0x4f, (byte) 0x07, (byte) 0x13, (byte) 0x93, (byte) 0x8a, (byte) 0x7f, (byte) 0x51, (byte) 0x53, (byte) 0x5c, (byte) 0x3a, (byte) 0x35, (byte) 0xe2 };

			assertArrayEquals(expected, result);

			md = MessageDigest.getInstance("SHA-512");
			result = Handshaker.doHMAC(md, key, data);
			expected = new byte[] { (byte) 0xe3, (byte) 0x7b, (byte) 0x6a, (byte) 0x77, (byte) 0x5d, (byte) 0xc8, (byte) 0x7d, (byte) 0xba, (byte) 0xa4, (byte) 0xdf, (byte) 0xa9, (byte) 0xf9, (byte) 0x6e, (byte) 0x5e, (byte) 0x3f, (byte) 0xfd,
					(byte) 0xde, (byte) 0xbd, (byte) 0x71, (byte) 0xf8, (byte) 0x86, (byte) 0x72, (byte) 0x89, (byte) 0x86, (byte) 0x5d, (byte) 0xf5, (byte) 0xa3, (byte) 0x2d, (byte) 0x20, (byte) 0xcd, (byte) 0xc9, (byte) 0x44, (byte) 0xb6,
					(byte) 0x02, (byte) 0x2c, (byte) 0xac, (byte) 0x3c, (byte) 0x49, (byte) 0x82, (byte) 0xb1, (byte) 0x0d, (byte) 0x5e, (byte) 0xeb, (byte) 0x55, (byte) 0xc3, (byte) 0xe4, (byte) 0xde, (byte) 0x15, (byte) 0x13, (byte) 0x46,
					(byte) 0x76, (byte) 0xfb, (byte) 0x6d, (byte) 0xe0, (byte) 0x44, (byte) 0x60, (byte) 0x65, (byte) 0xc9, (byte) 0x74, (byte) 0x40, (byte) 0xfa, (byte) 0x8c, (byte) 0x6a, (byte) 0x58 };

			assertArrayEquals(expected, result);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
