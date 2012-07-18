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
package ch.ethz.inf.vs.californium.coap;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The TokenManager stores all tokens currently used in transfers. New transfers
 * can acquire unique tokens from the manager.
 * 
 * @author Matthias Kovatsch
 */
public class TokenManager {

// Logging /////////////////////////////////////////////////////////////////////
	
	private static final Logger LOG = Logger.getLogger(TokenManager.class.getName());
	
// Static Attributes ///////////////////////////////////////////////////////////
	
	// the empty token, used as default value
	public static final byte[] emptyToken = new byte[0];
	
	private static TokenManager singleton = new TokenManager();

// Members /////////////////////////////////////////////////////////////////////
	
	private Set<byte[]> acquiredTokens = new HashSet<byte[]>();

	private long currentToken;
	
// Constructors ////////////////////////////////////////////////////////////////
	
	/**
	 * Default singleton constructor.
	 */
	private TokenManager() {
		this.currentToken = (long) (Math.random() * 0x100l);
	}
	
	public static TokenManager getInstance() {
		if (singleton==null) {
			synchronized (Communicator.class) {
				if (singleton==null) {
					singleton = new TokenManager();
				}
			}
		}
		return singleton;
	}
	
// Methods /////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns the next message ID to use out of the consecutive 16-bit range.
	 * 
	 * @return the current message ID
	 */
	private byte[] nextToken() {

		++this.currentToken;
		
		LOG.fine("Token value: "+currentToken);
		
		long temp = this.currentToken;
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(OptionNumberRegistry.TOKEN_LEN);  
		
		while (temp>0 && byteStream.size()<OptionNumberRegistry.TOKEN_LEN) {
			byteStream.write((int)(temp & 0xff));
			temp >>>= 8;
		}
		
		return byteStream.toByteArray();
	}
	
	/*
	 * Returns an unique token.
	 * 
	 * @param preferEmptyToken If set to true, the caller will receive
	 * the empty token if it is available. This is useful for reducing
	 * datagram sizes in transactions that are expected to complete
	 * in short time. On the other hand, empty tokens are not preferred
	 * in block-wise transfers, as the empty token is then not available
	 * for concurrent transactions.
	 * 
	 */
	public synchronized byte[] acquireToken(boolean preferEmptyToken) {
		
		byte[] token = null;
		if (preferEmptyToken && acquiredTokens.add(emptyToken)) {
			token = emptyToken;
		} else {
			do {
				token = nextToken();
			} while (!acquiredTokens.add(token));
		}
		
		return token;
	}
	
	public byte[] acquireToken() {
		return acquireToken(false);
	}
	
	/*
	 * Releases an acquired token and makes it available for reuse.
	 * 
	 * @param token The token to release
	 */
	public synchronized void releaseToken(byte[] token) {
		
		if (!acquiredTokens.remove(token)) {
			LOG.warning(String.format("Token to release is not acquired: %s\n", Option.hex(token)));
		}
	}
	
	/*
	 * Checks if a token is acquired by this manager.
	 * 
	 * @param token The token to check
	 * @return True iff the token is currently in use
	 */
	public synchronized boolean isAcquired(byte[] token) {
		return acquiredTokens.contains(token);
	}
	
	
}
