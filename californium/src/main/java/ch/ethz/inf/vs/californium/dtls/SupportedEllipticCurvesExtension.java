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
package ch.ethz.inf.vs.californium.dtls;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.dtls.HelloExtensions.ExtensionType;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * The supported elliptic curves extension. For details see <a
 * href="http://tools.ietf.org/html/rfc4492#section-5.1.1">RFC 4492</a>.
 * 
 * @author Stefan Jucker
 * 
 */
public class SupportedEllipticCurvesExtension extends HelloExtension {

	// DTLS-specific constants ////////////////////////////////////////

	private static final int LIST_LENGTH_BITS = 16;

	private static final int CURVE_BITS = 16;
	
	// Members ////////////////////////////////////////////////////////
	
	/** The list holding the supported named curves IDs */
	private List<Integer> ellipticCurveList;
	
	// Constructor ////////////////////////////////////////////////////

	/**
	 * 
	 * @param ellipticCurveList
	 *            the list of supported named curves.
	 */
	public SupportedEllipticCurvesExtension(List<Integer> ellipticCurveList) {
		super(ExtensionType.ELLIPTIC_CURVES);
		this.ellipticCurveList = ellipticCurveList;
	}
	
	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		int listLength = ellipticCurveList.size() * 2;
		writer.write(listLength + 2, LENGTH_BITS);
		writer.write(listLength, LIST_LENGTH_BITS);

		for (Integer curveId : ellipticCurveList) {
			writer.write(curveId, CURVE_BITS);
		}

		return writer.toByteArray();
	}

	public static HelloExtension fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		int listLength = reader.read(LIST_LENGTH_BITS);

		List<Integer> ellipticCurveList = new ArrayList<Integer>();
		while (listLength > 0) {
			int id = reader.read(CURVE_BITS);
			ellipticCurveList.add(id);

			listLength -= 2;
		}

		return new SupportedEllipticCurvesExtension(ellipticCurveList);
	}
	
	// Methods ////////////////////////////////////////////////////////
	
	@Override
	public int getLength() {
		// fixed: type (2 bytes), length (2 bytes), list length (2 bytes)
		// variable: number of named curves * 2 (2 bytes for each curve)
		return 6 + (ellipticCurveList.size() * 2);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\t\t\t\tLength: " + (getLength() - 4) + "\n");
		sb.append("\t\t\t\tElliptic Curves Length: " + (getLength() - 6) + "\n");
		sb.append("\t\t\t\tElliptic Curves (" + ellipticCurveList.size() + " curves):\n");

		for (Integer curveId : ellipticCurveList) {
			String curveName = ECDHServerKeyExchange.NAMED_CURVE_TABLE[curveId];
			sb.append("\t\t\t\t\tElliptic Curve: " + curveName + " (" + curveId + ")\n");
		}

		return sb.toString();
	}

	public List<Integer> getEllipticCurveList() {
		return ellipticCurveList;
	}

}