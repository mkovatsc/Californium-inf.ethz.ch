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
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.coap;

import java.util.HashSet;
import java.util.Set;

import ch.ethz.inf.vs.californium.util.Log;


/*
 * This class describes the functionality of a Token Manager.
 * 
 * Its purpose is to manage tokens used for keeping state of
 * transactions and block-wise transfers. Communication layers use
 * a TokenManager to acquire token where needed and release
 * them after completion of the task.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class TokenManager {

	// Static Attributes ///////////////////////////////////////////////////////
	
	// the empty token, used as default value
	public static final Option emptyToken
		= new Option(new byte[0], OptionNumberRegistry.TOKEN);
	
	// Constructors ////////////////////////////////////////////////////////////
	
	/*
	 * Default constructor for a new TokenManager
	 */
	public TokenManager() {
		// TODO randomize initial token?
		this.nextValue = 0;
	}
	
	// Methods /////////////////////////////////////////////////////////////////
	
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
	public Option acquireToken(boolean preferEmptyToken) {
		
		Option token = null;
		if (preferEmptyToken && !isAcquired(emptyToken)) {
			token = emptyToken;
		} else {
			token = new Option(nextValue++, OptionNumberRegistry.TOKEN);
		}
		
		if (!acquiredTokens.add(token)) {
			Log.warning(this, "Token already acquired: %s\n", token.getDisplayValue());
		}
		
		return token;
	}
	
	public Option acquireToken() {
		return acquireToken(false);
	}
	
	/*
	 * Releases an acquired token and makes it available for reuse.
	 * 
	 * @param token The token to release
	 */
	public void releaseToken(Option token) {
		if (!acquiredTokens.remove(token)) {
			Log.warning(this, "Token to release is not acquired: %s\n", token.getDisplayValue());
		}
	}
	
	/*
	 * Checks if a token is acquired by this manager.
	 * 
	 * @param token The token to check
	 * @return True iff the token is currently in use
	 */
	public boolean isAcquired(Option token) {
		return acquiredTokens.contains(token);
	}
	
	
	// Attributes //////////////////////////////////////////////////////////////
	
	private Set<Option> acquiredTokens
		= new HashSet<Option>();

	private int nextValue;
}
