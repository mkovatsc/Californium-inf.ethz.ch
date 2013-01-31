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

package ch.ethz.inf.vs.californium.layers;

import java.net.SocketException;

/**
 * The Class CoapStack encapsulate the layers needed to communicate to CoAP
 * nodes. It is used as a black box from the outside.
 * 
 * @author Francesco Corazza
 */
public class CoapStack extends UpperLayer {

	/**
	 * Instantiates a new coap stack.
	 * 
	 * @param udpPort
	 *            the udp port
	 * @param runAsDaemon
	 *            the run as daemon
	 * @param transferBlockSize
	 *            the transfer block size
	 * @param requestPerSecond
	 *            the request per second
	 * @throws SocketException
	 *             the socket exception
	 */
	public CoapStack(int udpPort, boolean runAsDaemon, int transferBlockSize, int requestPerSecond, boolean isSecured) throws SocketException {

		// initialize layers
		TokenLayer tokenLayer = new TokenLayer();
		TransferLayer transferLayer = new TransferLayer(transferBlockSize);
		MatchingLayer matchingLayer = new MatchingLayer();
		TransactionLayer transactionLayer = new TransactionLayer();
		// AdverseLayer adverseLayer = new AdverseLayer();
		// RateControlLayer rateControlLayer = new
		// RateControlLayer(requestPerSecond);
		UDPLayer udpLayer = null;
		DTLSLayer dtlsLayer = null;
		if (isSecured) {
			dtlsLayer = new DTLSLayer(udpPort, runAsDaemon);
		} else {
			udpLayer = new UDPLayer(udpPort, runAsDaemon);
		}

		// connect layers
		setLowerLayer(tokenLayer);
		tokenLayer.setLowerLayer(transferLayer);
		transferLayer.setLowerLayer(matchingLayer);
		matchingLayer.setLowerLayer(transactionLayer);
		if (isSecured) {
			transactionLayer.setLowerLayer(dtlsLayer);
		} else {
			transactionLayer.setLowerLayer(udpLayer);
		}

		// transactionLayer.setLowerLayer(rateControlLayer);
		// rateControlLayer.setLowerLayer(udpLayer);

		// transactionLayer.setLowerLayer(adverseLayer);
		// adverseLayer.setLowerLayer(udpLayer);
		
		LOG.info((isSecured) ? "CoapStack (secured) started" : "CoapStack started");
	}
}
