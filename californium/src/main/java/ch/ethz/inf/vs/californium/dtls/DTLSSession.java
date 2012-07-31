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

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import ch.ethz.inf.vs.californium.dtls.CipherSuite.KeyExchangeAlgorithm;

/**
 * Represents a DTLS session between two peers. Keeps track of the current and
 * pending read/write states, the current epoch and sequence number, etc.
 * 
 * @author Stefan Jucker
 * 
 */
public class DTLSSession {
	/**
	 * An arbitrary byte sequence chosen by the server to identify an active or
	 * resumable session state.
	 */
	private SessionId sessionIdentifier = null;

	/** X509v3 certificate of the peer. This element of the state may be null. */
	private X509Certificate peerCertificate = null;

	/** The algorithm used to compress data prior to encryption. */
	private CompressionMethod compressionMethod;

	/**
	 * Specifies the pseudorandom function (PRF) used to generate keying
	 * material, the bulk data encryption algorithm (such as null, AES, etc.)
	 * and the MAC algorithm (such as HMAC-SHA1). It also defines cryptographic
	 * attributes such as the mac_length. (See Appendix A.6 for formal
	 * definition.)
	 */
	private CipherSuite cipherSuite;

	/** 48-byte secret shared between the client and server. */
	private byte[] masterSecret = null;

	/**
	 * A flag indicating whether the session can be used to initiate new
	 * connections.
	 */
	private boolean isResumable = false;

	/**
	 * Whether the session is active and application data can be sent to the
	 * peer.
	 */
	private boolean isActive = false;

	/**
	 * Whether this entity is considered the "client" or the "server" in this
	 * connection.
	 */
	private boolean isClient;

	private DTLSConnectionState readState = new DTLSConnectionState();
	private DTLSConnectionState writeState = new DTLSConnectionState();

	/** The current epoch, incremented with every Change Cipher Spec. */
	private int readEpoch = 0;
	private int writeEpoch = 0;

	/** The next sequence number the record must have for each epoch separately. */
	private Map<Integer, Integer> sequenceNumbers = new HashMap<Integer, Integer>();
	
	/** The key exchange algorithm used in this session. */
	private KeyExchangeAlgorithm keyExchange;
	
	/**
	 * Indicates whether only the RawPublicKey is sent or a full X.509
	 * certificates.
	 */
	private boolean sendRawPublicKey = false;
	
	/**
	 * Indicates whether the peer sends a RawPublicKey.
	 */
	private boolean receiveRawPublicKey = false;

	// Constructor ////////////////////////////////////////////////////

	/**
	 * Called when initializing a fresh session.
	 * 
	 * @param isClient
	 *            whether the entity represents a client or a server.
	 */
	public DTLSSession(boolean isClient) {
		this.isClient = isClient;
		this.cipherSuite = CipherSuite.SSL_NULL_WITH_NULL_NULL;
		this.compressionMethod = CompressionMethod.NULL;
		this.sequenceNumbers.put(0, 0);
	}

	// Getters and Setters ////////////////////////////////////////////

	public SessionId getSessionIdentifier() {
		return sessionIdentifier;
	}

	public void setSessionIdentifier(SessionId sessionIdentifier) {
		this.sessionIdentifier = sessionIdentifier;
	}

	public X509Certificate getPeerCertificate() {
		return peerCertificate;
	}

	public void setPeerCertificate(X509Certificate peerCertificate) {
		this.peerCertificate = peerCertificate;
	}

	public CompressionMethod getCompressionMethod() {
		return compressionMethod;
	}

	public void setCompressionMethod(CompressionMethod compressionMethod) {
		this.compressionMethod = compressionMethod;
	}

	public CipherSuite getCipherSuite() {
		return cipherSuite;
	}

	public void setCipherSuite(CipherSuite cipherSuite) {
		this.cipherSuite = cipherSuite;
	}

	public boolean isResumable() {
		return isResumable;
	}

	public void setResumable(boolean isResumable) {
		this.isResumable = isResumable;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean isClient() {
		return this.isClient;
	}

	public void setClient(boolean isClient) {
		this.isClient = isClient;
	}

	public int getWriteEpoch() {
		return writeEpoch;
	}
	
	public void setWriteEpoch(int epoch) {
		this.writeEpoch = epoch;
	}

	public int getReadEpoch() {
		return readEpoch;
	}
	
	public void setReadEpoch(int epoch) {
		this.readEpoch = epoch;
	}

	public void incrementReadEpoch() {
		this.readEpoch++;
	}

	/**
	 * Increments the epoch and sets the sequence number of the new epoch to 0.
	 */
	public void incrementWriteEpoch() {
		this.writeEpoch++;
		// Sequence numbers are maintained separately for each epoch, with each
		// sequence_number initially being 0 for each epoch.
		this.sequenceNumbers.put(writeEpoch, 0);
	}

	public int getSequenceNumber() {
		return getSequenceNumber(writeEpoch);
	}

	/**
	 * Gets the smallest unused sequence number from this epoch.
	 * 
	 * @param epoch
	 *            the epoch from which to get the sequence number.
	 * @return the next sequence number.
	 */
	public int getSequenceNumber(int epoch) {
		int sequenceNumber = this.sequenceNumbers.get(epoch);
		this.sequenceNumbers.put(epoch, sequenceNumber + 1);
		return sequenceNumber;
	}

	public DTLSConnectionState getReadState() {
		return readState;
	}

	public void setReadState(DTLSConnectionState readState) {
		this.readState = readState;
	}

	public DTLSConnectionState getWriteState() {
		return writeState;
	}

	public void setWriteState(DTLSConnectionState writeState) {
		this.writeState = writeState;
	}

	public KeyExchangeAlgorithm getKeyExchange() {
		return keyExchange;
	}

	public void setKeyExchange(KeyExchangeAlgorithm keyExchange) {
		this.keyExchange = keyExchange;
	}

	public byte[] getMasterSecret() {
		return masterSecret;
	}

	public void setMasterSecret(byte[] masterSecret) {
		// don't overwrite the master secret, once it has been set in this session
		if (this.masterSecret != null) {
			this.masterSecret = masterSecret;
		}
	}

	public boolean sendRawPublicKey() {
		return sendRawPublicKey;
	}

	public void setSendRawPublicKey(boolean sendRawPublicKey) {
		this.sendRawPublicKey = sendRawPublicKey;
	}

	public boolean receiveRawPublicKey() {
		return receiveRawPublicKey;
	}

	public void setReceiveRawPublicKey(boolean receiveRawPublicKey) {
		this.receiveRawPublicKey = receiveRawPublicKey;
	}

}
