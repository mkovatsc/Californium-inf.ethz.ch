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

import org.junit.Test;

import ch.ethz.inf.vs.californium.dtls.RawPublicKey;
import ch.ethz.inf.vs.californium.util.ByteArrayUtils;

public class RawPublicKeyTest {

	@Test
	public void testOIDEncoding1() {
		// Found here:
		// http://msdn.microsoft.com/en-us/library/windows/desktop/bb540809(v=vs.85).aspx
		int[] oid = new int[] { 1, 3, 6, 1, 4, 1, 311, 21, 20 };
		byte[] expected = new byte[] { (byte) 0x2b, (byte) 0x06, (byte) 0x01, (byte) 0x04, (byte) 0x01, (byte) 0x82, (byte) 0x37, (byte) 0x15, (byte) 0x14 };
		byte[] result = RawPublicKey.encodeOID(oid);

		assertArrayEquals(expected, result);

		int[] decoded = RawPublicKey.decodeOID(result);
		assertArrayEquals(decoded, oid);
	}

	@Test
	public void testOIDEncoding2() {
		// created using wireshark
		int[] oid = new int[] { 1, 2, 840, 113549, 1, 1, 1 };
		byte[] expected = new byte[] { (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x01, (byte) 0x01 };
		byte[] result = RawPublicKey.encodeOID(oid);

		assertArrayEquals(expected, result);

		int[] decoded = RawPublicKey.decodeOID(result);
		assertArrayEquals(decoded, oid);

	}

	@Test
	public void testRawPublicKeyEncoding1() {
		// created using wireshark
		int[] algorithmOID = new int[] { 1, 2, 840, 10045, 2, 1 };
		int[] parametersAlgorithm = new int[] { 1, 3, 132, 0, 35 };
		byte[] publicKey = ByteArrayUtils
				.hexStreamToByteArray("04012035623b5b6edb42b925346c648114a84c430bbd87cf033d96f64e22989d50c13bb64ba1d807594b4fad50e14c0ae4ccab7c6f728f239e32ab7309af9d0337411f01a81dec6435704ddd236e51c02a9ba89e9da173e0ac840092d40649134ded6b172cb64f249310f7f9d5a8c91c6227089d645fcf2fdebf7dc0ed911637febc5472a6");
		byte[] expected = ByteArrayUtils
				.hexStreamToByteArray("30819b301006072a8648ce3d020106052b810400230381860004012035623b5b6edb42b925346c648114a84c430bbd87cf033d96f64e22989d50c13bb64ba1d807594b4fad50e14c0ae4ccab7c6f728f239e32ab7309af9d0337411f01a81dec6435704ddd236e51c02a9ba89e9da173e0ac840092d40649134ded6b172cb64f249310f7f9d5a8c91c6227089d645fcf2fdebf7dc0ed911637febc5472a6");

		RawPublicKey rawPublicKey = new RawPublicKey(publicKey, algorithmOID, parametersAlgorithm);
		byte[] result = rawPublicKey.toByteArray();

		assertArrayEquals(expected, result);

		RawPublicKey newRawPublicKey = RawPublicKey.fromByteArray(result);
		assertArrayEquals(rawPublicKey.getParametersOID(), newRawPublicKey.getParametersOID());
		assertArrayEquals(rawPublicKey.getAlgorithmOID(), newRawPublicKey.getAlgorithmOID());
		assertArrayEquals(rawPublicKey.getSubjectPublicKey(), newRawPublicKey.getSubjectPublicKey());
	}

	@Test
	public void testRawPublicKeyEncoding2() {
		// created using wireshark
		int[] algorithmOID = new int[] { 1, 2, 840, 113549, 1, 1, 1 };
		int[] parametersAlgorithm = null;
		byte[] publicKey = ByteArrayUtils
				.hexStreamToByteArray("30818902818100d51457a096409f8408c6668deeece303b26685ac5dbb1cef1593fd1f20a71049245b39d260c89adcc0ce4034e65995b65250fe08254557735f3aed5fdfb65c9d8a9c62fd0f61bedcf6871d809d7f7c171377643c47f387241f6061e0811146e4dc505c3953e6683d863c5587c8befc8713d95aaa5dcc3f07c174cdc25e2716110203010001");
		byte[] expected = ByteArrayUtils
				.hexStreamToByteArray("30819f300d06092a864886f70d010101050003818d0030818902818100d51457a096409f8408c6668deeece303b26685ac5dbb1cef1593fd1f20a71049245b39d260c89adcc0ce4034e65995b65250fe08254557735f3aed5fdfb65c9d8a9c62fd0f61bedcf6871d809d7f7c171377643c47f387241f6061e0811146e4dc505c3953e6683d863c5587c8befc8713d95aaa5dcc3f07c174cdc25e2716110203010001");

		RawPublicKey rawPublicKey = new RawPublicKey(publicKey, algorithmOID, parametersAlgorithm);
		byte[] result = rawPublicKey.toByteArray();

		assertArrayEquals(expected, result);

		RawPublicKey newRawPublicKey = RawPublicKey.fromByteArray(result);
		assertArrayEquals(rawPublicKey.getParametersOID(), newRawPublicKey.getParametersOID());
		assertArrayEquals(rawPublicKey.getAlgorithmOID(), newRawPublicKey.getAlgorithmOID());
		assertArrayEquals(rawPublicKey.getSubjectPublicKey(), newRawPublicKey.getSubjectPublicKey());
	}

}
