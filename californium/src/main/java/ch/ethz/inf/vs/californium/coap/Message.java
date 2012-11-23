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

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.layers.UpperLayer;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The Class Message provides the object representation of a CoAP message.
 * Besides providing the corresponding setters and getters, the class is
 * responsible for parsing and serializing the objects from/to byte arrays.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, Francesco Corazza and Matthias
 *         Kovatsch
 */
public class Message {

	// Logging /////////////////////////////////////////////////////////////////////

	private static final int DEFAULT_PORT = Properties.std.getInt("DEFAULT_PORT");

	protected static final Logger LOG = Logger.getLogger(Message.class.getName());
	
	// CoAP-specific constants /////////////////////////////////////////////////////
	
	/** number of bits used for the encoding of the CoAP version field */
	public static final int VERSION_BITS     = 2;
	
	/** number of bits used for the encoding of the message type field */
	public static final int TYPE_BITS        = 2;
	
	/** number of bits used for the encoding of the option count field */
	public static final int OPTIONCOUNT_BITS = 4;

	/** number of bits used for the encoding of the request method/response code field */
	public static final int CODE_BITS = 8;

	/** number of bits used for the encoding of the message ID */
	public static final int ID_BITS = 16;

	/** number of bits used for the encoding of the option delta */
	public static final int OPTIONDELTA_BITS = 4;
	
	/**
	 * Number of bits used for the encoding of the base option length field.
	 * If all bits in this field are set to one, the extended option length field is additionally used to encode the option length.
	 */
	public static final int OPTIONLENGTH_BASE_BITS     = 4;
	
	/** 
	 * Number of bits used for the encoding of the extended option length field.
	 * This field is used when all bits in the base option length field are set to one.
	 */
	public static final int OPTIONLENGTH_EXTENDED_BITS = 8;

	/** CoAP version supported by this Californium version */
	public static final int SUPPORTED_VERSION = 1; // I-D
	
	/** Maximum option delta that can be encoded without using option jump format */
	public static final int MAX_OPTIONDELTA = 14; // coap-12
	
	/** Number of bits used to encode the one-byte option jump value */
	public static final int SINGLE_OPTIONJUMP_BITS = 8; // coap-12
	
	// Derived constants ///////////////////////////////////////////////////////////
	
	/** maximum option length that can be encoded using the base option length field only */
	public static final int MAX_OPTIONLENGTH_BASE = (1 << OPTIONLENGTH_BASE_BITS) - 2;


	// Members
	// /////////////////////////////////////////////////////////////////////

	private EndpointAddress peerAddress = null;

	// Members
	// /////////////////////////////////////////////////////////////////////

	private byte[] payload = null;

	/* The CoAP version used */
	private final int version = SUPPORTED_VERSION;

	/* The message type (CON, NON, ACK, or RST). */

	private messageType type = null;
	
	/* The message ID. Set according to request or handled by {@link ch.ethz.inf.vs.californium.layers.TransactionLayer} when -1. */
	private int messageID = -1;
	
	/* The list of header options set for the message. */
	private Map<Integer, List<Option>> optionMap = new TreeMap<Integer, List<Option>>();
	
	private long timestamp = -1;
	
	private int retransmissioned = 0;
	
	// indicates if the message requires a token
	// this is required to handle implicit empty tokens (default value)
	protected boolean requiresToken = true;
	protected boolean requiresBlockwise = false;

	/**
	 * The message code: 0: Empty 1-31: Request 64-191: Response
	 */
	private int code = 0;

	/**
	 * Extended constructor for a new CoAP message, e.g., empty ACK or RST
	 * 
	 * @param type the type of the CoAP message
	 * @param code the code of the CoAP message (See class CodeRegistry)
	 */
	public Message(messageType type, int code) {
		this.type = type;
		this.code = code;
	}	
	
	// Serialization ///////////////////////////////////////////////////////////////

	/**
	 * Decodes the message from the its binary representation as specified in
	 * draft-ietf-core-coap-12, section 3.1
	 * 
	 * @param byteArray
	 *            A byte array containing the CoAP encoding of the message
	 * 
	 * @return a parsed CoAP message as correspondingly extended Message object, e.g., GETRequest
	 */
	public static Message fromByteArray(byte[] byteArray) {

		// Initialize DatagramReader
		DatagramReader datagram = new DatagramReader(byteArray);

		// Read current version
		int version = datagram.read(VERSION_BITS);
		if (version != SUPPORTED_VERSION) {
			return null;
		}
		
		//Read current type
		messageType type = getTypeByValue(datagram.read(TYPE_BITS));
		
		//Read number of options
		int optionCount = datagram.read(OPTIONCOUNT_BITS);

		// create new message with subtype according to code number
		Message msg = CodeRegistry.getMessageSubClass(datagram.read(CODE_BITS));

		msg.type = type;

		// Read message ID

		msg.messageID = datagram.read(ID_BITS);

		// Current option nr initialization
		int currentOption = 0;
		boolean hasMoreOptions = optionCount == 15;
		for (int i = 0; (i < optionCount || hasMoreOptions) && datagram.bytesAvailable(); i++) {
			// first 4 option bits: either option jump or option delta
			int optionDelta = datagram.read(OPTIONDELTA_BITS);
			
			if (optionDelta == 15) {
				// option jump or end-of-options marker
				int bits = datagram.read(4);
				switch (bits) {
				case 0:
					// end-of-options marker read (0xF0), payload follows
					hasMoreOptions = false;
					continue;
				case 1:
					// 0xF1 (Delta = 15)
					optionDelta = datagram.read(OPTIONDELTA_BITS) + 15;
					break;
					
				case 2:
					// Delta = ((Option Jump Value) + 2) * 8
					optionDelta = (datagram.read(8) + 2) * 8 + datagram.read(OPTIONDELTA_BITS);
					break;
					
				case 3:
					// Delta = ((Option Jump Value) + 258) * 8
					optionDelta = (datagram.read(16) + 258) * 8 + datagram.read(OPTIONDELTA_BITS);
					break;

				default:
					break;
				}
				
			}
			currentOption += optionDelta;
			
			int length = datagram.read(OPTIONLENGTH_BASE_BITS);
			if (length == 15) {
				/*
				 * When the Length field is set to 15, another byte is added as
				 * an 8-bit unsigned integer whose value is added to the 15,
				 * allowing option value lengths of 15-270 bytes. For option
				 * lengths beyond 270 bytes, we reserve the value 255 of an
				 * extension byte to mean
				 * "add 255, read another extension byte".
				 */
				int additionalLength = 0;
				do {
					additionalLength = datagram.read(8);
					length += additionalLength;
				} while (additionalLength >= 255);
			}
			
			// Read option
			Option opt = Option.fromNumber(currentOption);
			opt.setValue(datagram.readBytes(length));

			// Add option to message
			msg.addOption(opt);
			
		}

		// Get payload
		msg.payload = datagram.readBytesLeft();

		// incoming message already have a token, including implicit empty token
		msg.requiresToken = false;

		return msg;
	}

	// I/O implementation //////////////////////////////////////////////////////////
	
	/**
	 * Converts a numeric type value into the messageType enum.
	 * 
	 * @param numeric
	 *            the type value
	 * @return
	 */
	public static messageType getTypeByValue(int numeric) {
		switch (numeric) {
		case 0:
			return messageType.CON;
		case 1:
			return messageType.NON;
		case 2:
			return messageType.ACK;
		case 3:
			return messageType.RST;
		default:
			return messageType.CON;
		}
	}

	/**
	 * Default constructor for a new CoAP message
	 */
	public Message() {
	}

	// Constructors ////////////////////////////////////////////////////////////////

	/*
	 * Constructor for a new CoAP message
	 * @param uri the URI of the CoAP message
	 * @param payload the payload of the CoAP message
	 */
	public Message(URI address, messageType type, int code, int mid, byte[] payload) {
		this.setURI(address);
		this.type = type;
		this.code = code;
		messageID = mid;
		this.payload = payload;
	}

	/**
	 * Accepts this message with an empty ACK. Use this method only at
	 * application level, as the ACK will be sent through the whole stack.
	 * <p>
	 * Within the stack use {@link #newAccept()} and send it through the
	 * corresponding {@link UpperLayer#sendMessageOverLowerLayer(Message)}.
	 */
	public void accept() {
		if (isConfirmable()) {
			Message ack = newAccept();

			ack.send();
		}
	}

	/**
	 * This method adds an option to the list of options of this CoAP message.
	 * 
	 * @param option
	 *            the option which should be added to the list of options of the
	 *            current CoAP message
	 */
	public void addOption(Option option) {

		if (option == null) {
			throw new NullPointerException();
		}

		int optionNumber = option.getOptionNumber();
		List<Option> list = optionMap.get(optionNumber);

		if (list == null) {
			list = new ArrayList<Option>();
			optionMap.put(optionNumber, list);
		}

		list.add(option);

		if (optionNumber == OptionNumberRegistry.TOKEN) {
			requiresToken = false;
		}
	}

	// Serialization
	// ///////////////////////////////////////////////////////////////

	/**
	 * Adds all given options
	 * 
	 * @param option
	 *            the list of the options
	 */
	public void addOptions(List<Option> options) {

		for (Option option : options) {
			addOption(option);
		}
	}

	/**
	 * Appends data to this message's payload.
	 * 
	 * @param block
	 *            the byte array containing the data to append
	 */
	public synchronized void appendPayload(byte[] block) {

		if (block != null) {
			if (payload != null) {

				byte[] oldPayload = payload;
				payload = new byte[oldPayload.length + block.length];
				System.arraycopy(oldPayload, 0, payload, 0, oldPayload.length);
				System.arraycopy(block, 0, payload, oldPayload.length, block.length);

			} else {

				payload = block.clone();
			}

			// wake up threads waiting in readPayload()
			notifyAll();

			// call notification method
			payloadAppended(block);
		}
	}

	// I/O implementation
	// //////////////////////////////////////////////////////////

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Message other = (Message) obj;
		if (code != other.code) {
			return false;
		}
		if (messageID != other.messageID) {
			return false;
		}
		if (optionMap == null) {
			if (other.optionMap != null) {
				return false;
			}
		} else if (!optionMap.equals(other.optionMap)) {
			return false;
		}
		if (!Arrays.equals(payload, other.payload)) {
			return false;
		}
		if (peerAddress == null) {
			if (other.peerAddress != null) {
				return false;
			}
		} else if (!peerAddress.equals(other.peerAddress)) {
			return false;
		}
		// if (retransmissioned != other.retransmissioned) return false;
		// if (timestamp != other.timestamp) return false;
		if (type != other.type) {
			return false;
		}
		if (version != other.version) {
			return false;
		}
		return true;
	}

	/**
	 * This function returns the code of this CoAP message (method or status
	 * code).
	 * 
	 * @return the current code
	 */
	public int getCode() {
		return code;
	}

	public URI getCompleteUri() throws URISyntaxException {
		StringBuilder builder = new StringBuilder();
		builder.append("coap://");
		builder.append(getFirstOption(OptionNumberRegistry.URI_HOST));
		builder.append(":" + getFirstOption(OptionNumberRegistry.URI_PORT));
		builder.append(getUriPath());
		String query = getQuery();
		if (query != null && !query.isEmpty()) {
			builder.append("?" + query);
		}

		return new URI(builder.toString());
	}

	public int getContentType() {
		Option opt = getFirstOption(OptionNumberRegistry.CONTENT_TYPE);
		return opt != null ? opt.getIntValue() : MediaTypeRegistry.UNDEFINED;
	}

	public int getFirstAccept() {
		Option opt = getFirstOption(OptionNumberRegistry.ACCEPT);
		return opt != null ? opt.getIntValue() : MediaTypeRegistry.UNDEFINED;
	}

	// Methods
	// /////////////////////////////////////////////////////////////////////

	/**
	 * A convenience method that returns the first option with the specified
	 * option number. Also used for options that MUST occur only once.
	 * 
	 * @param optionNumber
	 *            the option number
	 * @return The first option with the specified number, or null
	 */
	public Option getFirstOption(int optionNumber) {

		List<Option> list = getOptions(optionNumber);
		return list != null && !list.isEmpty() ? list.get(0) : null;
	}

	public String getLocationPath() {
		return Option.join(getOptions(OptionNumberRegistry.LOCATION_PATH), "/");
	}
	
	public String getLocationQuery() {
		return "?" + Option.join(getOptions(OptionNumberRegistry.LOCATION_QUERY), "&");
	}

	// Getters and Setters
	// /////////////////////////////////////////////////////////

	public int getMaxAge() {
		Option opt = getFirstOption(OptionNumberRegistry.MAX_AGE);
		return opt != null ? opt.getIntValue() : Option.DEFAULT_MAX_AGE;
	}
	
	public int getSize() {
		Option opt = getFirstOption(OptionNumberRegistry.SIZE);
		return opt != null ? opt.getIntValue() : 0;
	}

	/**
	 * This function returns the 16-bit message ID of this CoAP message.
	 * 
	 * @return the message ID
	 */
	public int getMID() {
		return messageID;
	}

	/**
	 * This function returns the number of options of this CoAP message.
	 * 
	 * @return The current number of options
	 */
	public int getOptionCount() {
		return getOptions().size();
	}

	/**
	 * Returns a sorted list of all included options.
	 * 
	 * @return A sorted list of all options (copy)
	 */
	public List<Option> getOptions() {

		List<Option> list = new ArrayList<Option>();

		for (List<Option> option : optionMap.values()) {
			list.addAll(option);
		}

		return list;
	}

	/**
	 * This function returns all options with the given option number.
	 * 
	 * @param optionNumber
	 *            the option number
	 * 
	 * @return A list containing the options with the given number
	 */
	public List<Option> getOptions(int optionNumber) {
		List<Option> ret = optionMap.get(optionNumber);
		if (ret != null) {
			return ret;
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * This function returns the payload of this CoAP message as byte array.
	 * 
	 * @return the payload
	 */
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * This function returns the payload of this CoAP message as String.
	 * 
	 * @return the payload
	 */
	public String getPayloadString() {
		try {
			return payload != null ? new String(payload, "UTF-8") : null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public EndpointAddress getPeerAddress() {
		return peerAddress;
	}

	public URI getProxyUri() throws URISyntaxException {
		URI proxyUri = null;

		String proxyUriString = Option.join(getOptions(OptionNumberRegistry.PROXY_URI), "/");
		// decode the uri to translate the application/x-www-form-urlencoded
		// format
		try {
			proxyUriString = URLDecoder.decode(proxyUriString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.warning("UTF-8 do not support this encoding: " + e.getMessage());
			throw new URISyntaxException("UTF-8 do not support this encoding", e.getMessage());
		} catch (Throwable e) {
			LOG.warning("Error deconding uri: " + e.getMessage());
			throw new URISyntaxException("Error deconding uri", e.getMessage());
		}

		// add the scheme
		// FIXME not clean
		if (!proxyUriString.matches("^coap://.*") && !proxyUriString.matches("^coaps://.*") && !proxyUriString.matches("^http://.*") && !proxyUriString.matches("^https://.*")) {
			proxyUriString = "coap://" + proxyUriString;
		}

		// create the URI
		if (proxyUriString != null && !proxyUriString.isEmpty()) {
			proxyUri = new URI(proxyUriString);
		}

		return proxyUri;
	}

	public String getQuery() {
		return Option.join(getOptions(OptionNumberRegistry.URI_QUERY), "&");
	}

	public int getRetransmissioned() {
		return retransmissioned;
	}

	/**
	 * Returns the timestamp associated with this message.
	 * 
	 * @return The timestamp of the message, in milliseconds
	 */
	public long getTimestamp() {
		return timestamp;
	}

	public byte[] getToken() {
		Option opt = getFirstOption(OptionNumberRegistry.TOKEN);
		return opt != null ? opt.getRawValue() : TokenManager.emptyToken;
	}

	public String getTokenString() {
		return Option.hex(getToken());
	}

	/**
	 * This function returns the type of this CoAP message (CON, NON, ACK, or
	 * RST).
	 * 
	 * @return the current type
	 */
	public messageType getType() {
		return type;
	}

	public String getUriPath() {
		return "/" + Option.join(getOptions(OptionNumberRegistry.URI_PATH), "/");
	}

	/**
	 * This function returns the version of this CoAP message.
	 * 
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * This method is overridden by subclasses according to the Visitor Pattern.
	 * 
	 * @param handler
	 *            the handler for this message
	 */
	public void handleBy(MessageHandler handler) {
		// do nothing
	}

	/**
	 * Notification method that is called when the transmission of this message
	 * was cancelled due to timeout.
	 * 
	 * Subclasses may override this method to add custom handling code.
	 */
	public void handleTimeout() {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + code;
//		result = prime * result + messageID;
//		result = prime * result + (optionMap == null ? 0 : optionMap.hashCode());
//		result = prime * result + Arrays.hashCode(payload);
//		result = prime * result + (peerAddress == null ? 0 : peerAddress.toString().hashCode());
//		// TODO check
//		// result = prime * result + retransmissioned;
//		result = prime * result + (int) (timestamp ^ timestamp >>> 32);
//		result = prime * result + (type == null ? 0 : type.hashCode());
//		result = prime * result + version;
//		return result;
//	}

	public boolean hasOption(int optionNumber) {
		return getFirstOption(optionNumber) != null;
	}

	// Other getters/setters ///////////////////////////////////////////////////

	public boolean isAcknowledgement() {
		return type == messageType.ACK;
	}

	// Other getters/setters ///////////////////////////////////////////////////

	public boolean isConfirmable() {
		return type == messageType.CON;
	}

	// Other getters/setters ///////////////////////////////////////////////////

	public boolean isEmptyACK() {
		return isAcknowledgement() && getCode() == CodeRegistry.EMPTY_MESSAGE;
	}

	// Other getters/setters ///////////////////////////////////////////////////

	public boolean isNonConfirmable() {
		return type == messageType.NON;
	}

	/**
	 * Returns true if the option proxy-uri is set.
	 */
	public boolean isProxyUriSet() {
		// check if the proxy-uri option is set or not
		int proxyUriOptNumber = OptionNumberRegistry.PROXY_URI;
		return !getOptions(proxyUriOptNumber).isEmpty();
	}

	// Other getters/setters ///////////////////////////////////////////////////

	public boolean isReply() {
		return isAcknowledgement() || isReset();
	}

	public boolean isReset() {
		return type == messageType.RST;
	}

	/**
	 * Returns a string that is assumed to uniquely identify a message.
	 * 
	 * @return A string identifying the message
	 */
	public String key() {
		return String.format("%s|%d|%s", peerAddress != null ? peerAddress.toString() : "local", messageID, typeString());
	}

	/**
	 * Creates a new ACK message with peer address and MID matching to this
	 * message.
	 * 
	 * @return A new ACK message
	 */
	public Message newAccept() {
		Message ack = new Message(messageType.ACK, CodeRegistry.EMPTY_MESSAGE);

		ack.setPeerAddress(getPeerAddress());
		ack.setMID(getMID());

		return ack;
	}

	/**
	 * Creates a new RST message with peer address and MID matching to this
	 * message.
	 * 
	 * @return A new RST message
	 */
	public Message newReject() {

		Message rst = new Message(messageType.RST, CodeRegistry.EMPTY_MESSAGE);

		rst.setPeerAddress(getPeerAddress());
		rst.setMID(getMID());

		return rst;
	}

	// Other getters/setters ///////////////////////////////////////////////////

	/**
	 * This method creates a matching reply for requests. It is addressed to the
	 * peer and has the same message ID and token.
	 * 
	 * @param ack
	 *            set true to send ACK else RST
	 * 
	 * @return A new {@link Message}
	 */
	// TODO does not fit into Message class
	public Message newReply(boolean ack) {

		// TODO use this for Request.respond() or vice versa

		Message reply = new Message();

		// set message type
		if (type == messageType.CON) {
			reply.type = ack ? messageType.ACK : messageType.RST;
		} else {
			reply.type = messageType.NON;
		}

		// echo the message ID
		reply.messageID = messageID;

		// set the receiver URI of the reply to the sender of this message
		reply.peerAddress = peerAddress;

		// echo token
		reply.setOption(getFirstOption(OptionNumberRegistry.TOKEN));
		reply.requiresToken = requiresToken;

		// create an empty reply by default
		reply.code = CodeRegistry.EMPTY_MESSAGE;

		return reply;
	}

	public int payloadSize() {
		return payload != null ? payload.length : 0;
	}

	public void prettyPrint() {
		prettyPrint(System.out);
	}

	public void prettyPrint(PrintStream out) {

		String kind = "MESSAGE ";
		if (this instanceof Request) {
			kind = "REQUEST ";
		} else if (this instanceof Response) {
			kind = "RESPONSE";
		}
		out.printf("==[ CoAP %s ]============================================\n", kind);

		List<Option> options = getOptions();

		out.printf("Address: %s\n", peerAddress.toString() == null ? "null" : peerAddress.toString());
		out.printf("MID    : %d\n", messageID);
		out.printf("Type   : %s\n", typeString());
		out.printf("Code   : %s\n", CodeRegistry.toString(code));
		out.printf("Options: %d\n", options.size());
		for (Option opt : options) {
			out.printf("  * %s: %s (%d Bytes)\n", opt.getName(), opt.toString(), opt.getLength());
		}
		out.printf("Payload: %d Bytes\n", payloadSize());
		if (payloadSize() > 0 && MediaTypeRegistry.isPrintable(getContentType())) {
			out.println("---------------------------------------------------------------");
			out.println(getPayloadString());
		}
		out.println("===============================================================");

	}

	/**
	 * Rejects this message with an empty RST. Use this method only at
	 * application level, as the RST will be sent through the whole stack.
	 * <p>
	 * Within the stack use {@link #newAccept()} and send it through the
	 * corresponding {@link UpperLayer#sendMessageOverLowerLayer(Message)}.
	 */
	public void reject() {

		Message rst = newReject();

		rst.send();
	}

	/**
	 * This method removes all options of the given number from this CoAP
	 * message.
	 * 
	 * @param optionNumber
	 *            the number of the options to remove
	 * 
	 */
	public void removeOptions(int optionNumber) {
		optionMap.remove(optionNumber);
	}

	public boolean requiresBlockwise() {
		return requiresBlockwise;
	}

	public void requiresBlockwise(boolean value) {
		requiresBlockwise = value;
	}

	public boolean requiresToken() {
		return requiresToken && getCode() != CodeRegistry.EMPTY_MESSAGE && !(this instanceof Response);
	}

	public void requiresToken(boolean value) {
		requiresToken = value;
	}

	public void send() {

		try {
			Communicator communicator = CommunicatorFactory.getInstance().getCommunicator();
			communicator.sendMessage(this);
		} catch (IOException e) {
			LOG.severe(String.format("Could not respond to message: %s\n%s", key(), e.getMessage()));
		}
	}

	/**
	 * Returns a string that is assumed to uniquely identify a transfer. A
	 * transfer exceeds matching message IDs, as multiple transactions are
	 * involved, e.g., for separate responses or blockwise transfers. The
	 * transfer matching is done using the token (including the empty default
	 * token.
	 * 
	 * @return A string identifying the transfer
	 */
	public String sequenceKey() {
		return String.format("%s#%s", peerAddress != null ? peerAddress.toString() : "local", getTokenString());
	}

	public void setAccept(int ct) {
		if (ct != MediaTypeRegistry.UNDEFINED) {
			addOption(new Option(ct, OptionNumberRegistry.ACCEPT));
		} else {
			removeOptions(OptionNumberRegistry.ACCEPT);
		}
	}

	/**
	 * This method sets the code of this CoAP message (method or status code).
	 * 
	 * @param code
	 *            the message code to set to
	 */
	public void setCode(int code) {
		this.code = code;
	}

	public void setContentType(int ct) {
		if (ct != MediaTypeRegistry.UNDEFINED) {
			setOption(new Option(ct, OptionNumberRegistry.CONTENT_TYPE));
		} else {
			removeOptions(OptionNumberRegistry.CONTENT_TYPE);
		}
	}

	public void setLocationPath(String locationPath) {
		setOptions(Option.split(OptionNumberRegistry.LOCATION_PATH, locationPath, "/"));
	}
	
	public void setLocationQuery(String locationQuery) {
		if (locationQuery.startsWith("?")) {
			locationQuery = locationQuery.substring(1);
		}
		setOptions(Option.split(OptionNumberRegistry.LOCATION_QUERY, locationQuery, "&"));
	}

	public void setMaxAge(int timeInSec) {
		setOption(new Option(timeInSec, OptionNumberRegistry.MAX_AGE));
	}
	
	public void setSize(int size) {
		setOption(new Option(size, OptionNumberRegistry.SIZE));
	}
	

	/**
	 * This method sets the 16-bit message ID of this CoAP message.
	 * 
	 * @param mid
	 *            the MID to set to
	 */
	public void setMID(int mid) {
		messageID = mid;
	}

	/**
	 * Sets this option and overwrites all options with the same number.
	 * 
	 * @param option
	 */
	public void setOption(Option option) {
		// check important to allow convenient setting of options that might be
		// null (e.g., Token)
		if (option != null) {
			removeOptions(option.getOptionNumber());
			addOption(option);
		}
	}

	/**
	 * Sets all given options and overwrites all options with the same numbers.
	 * 
	 * @param option
	 *            the list of the options
	 */
	public void setOptions(List<Option> options) {
		if (options != null) {
			for (Option option : options) {
				removeOptions(option.getOptionNumber());
			}

			addOptions(options);
		}
	}

	/**
	 * This method sets a payload of this CoAP message replacing any existing
	 * one.
	 * 
	 * @param payload
	 *            the payload to set to
	 */
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public void setPayload(String payload) {
		setPayload(payload, MediaTypeRegistry.UNDEFINED);
	}

	public void setPayload(String payload, int mediaType) {
		if (payload != null) {
			try {
				// set internal byte array
				setPayload(payload.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return;
			}

			// set content type option
			if (mediaType != MediaTypeRegistry.UNDEFINED) {
				setOption(new Option(mediaType, OptionNumberRegistry.CONTENT_TYPE));
			}
		}
	}

	public void setPeerAddress(EndpointAddress a) {
		peerAddress = a;
	}

	public void setRetransmissioned(int retransmissioned) {
		this.retransmissioned = retransmissioned;
	}

	/**
	 * Sets the timestamp associated with this message.
	 * 
	 * @param timestamp
	 *            the new timestamp, in milliseconds
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setToken(byte[] token) {
		if (token!=TokenManager.emptyToken) setOption(new Option(token, OptionNumberRegistry.TOKEN));
		requiresToken = false;
	}

	/**
	 * This method sets the type of this CoAP message (CON, NON, ACK, or RST).
	 * 
	 * @param msgType
	 *            the type for the message
	 */
	public void setType(messageType msgType) {
		type = msgType;
	}

	/**
	 * This is a convenience method to set peer address and Uri-* options via
	 * URI string.
	 * 
	 * @param uri
	 *            the URI string defining the target resource
	 */
	public boolean setURI(String uri) {
		try {
			setURI(new URI(uri));
			return true;
		} catch (URISyntaxException e) {
			LOG.warning(String.format("Failed to set URI: %s", e.getMessage()));
			return false;
		}
	}

	/**
	 * This is a convenience method to set peer address and Uri options via URI
	 * object.
	 * 
	 * @param uri
	 *            the URI defining the target resource
	 */
	public void setURI(URI uri) {

		if (this instanceof Request) {

			// set Uri-Host options
			String host = uri.getHost();
			if (host != null) {
				setOption(new Option(host, OptionNumberRegistry.URI_HOST));
			}

			// set uri-port option
			int port = uri.getPort();
			if (port <= 0) {
				// assume the default port
				port = DEFAULT_PORT;
			}
			setOption(new Option(port, OptionNumberRegistry.URI_PORT));

			// set Uri-Path options
			String path = uri.getPath();
			if (path != null && path.length() > 1) {
				List<Option> uriPath = Option.split(OptionNumberRegistry.URI_PATH, path, "/");
				setOptions(uriPath);
			}

			// set Uri-Query options
			String query = uri.getQuery();
			if (query != null) {
				List<Option> uriQuery = Option.split(OptionNumberRegistry.URI_QUERY, query, "&");
				setOptions(uriQuery);
			}
		}

		setPeerAddress(new EndpointAddress(uri));
	}

	/**
	 * Encodes the message into its raw binary representation as specified in
	 * draft-ietf-core-coap-12, section 3.1
	 * 
	 * @return A byte array containing the CoAP encoding of the message
	 * 
	 */
	public byte[] toByteArray() {

		// create datagram writer to encode options
		DatagramWriter optWriter = new DatagramWriter();

		int optionCount = 0;
		int lastOptionNumber = 0;
		for (Option opt : getOptions()) {

			// do not encode options with default values
			if (opt.isDefaultValue()) {
				continue;
			}

			// calculate option delta
			int optionDelta = opt.getOptionNumber() - lastOptionNumber;
			
			/*
			 * The Option Jump mechanism is used when the delta to the next
			 * option number is larger than 14.
			 */
			if (optionDelta > MAX_OPTIONDELTA) {
				/*
				 * For the formats that include an Option Jump Value, the actual
				 * addition to the current Option number is computed as follows:
				 * Delta = ((Option Jump Value) + N) * 8 where N is 2 for the
				 * one-byte version and N is 258 for the two-byte version.
				 */
				if (optionDelta < 30) {
					optWriter.write(0xF1, SINGLE_OPTIONJUMP_BITS);
					optionDelta -= 15;
					
				} else if (optionDelta < 2064) {
					int optionJumpValue = (optionDelta / 8) - 2;
					optionDelta -= (optionJumpValue + 2) * 8;
					optWriter.write(0xF2, SINGLE_OPTIONJUMP_BITS);
					optWriter.write(optionJumpValue, SINGLE_OPTIONJUMP_BITS);
					
				} else if (optionDelta < 526345) {
					int optionJumpValue = (optionDelta / 8) - 258;
					optionDelta -= (optionJumpValue + 258) * 8;
					optWriter.write(0xF3, SINGLE_OPTIONJUMP_BITS);
					optWriter.write(optionJumpValue, 2 * SINGLE_OPTIONJUMP_BITS);
					
				} else if (optionDelta < 526359) {
					// TODO maybe find other way instead of subtraction
					int optionJumpValue = ((optionDelta - 14) / 8) - 258;
					optionDelta -= (optionJumpValue + 258) * 8;
					optWriter.write(0xF3, SINGLE_OPTIONJUMP_BITS);
					optWriter.write(optionJumpValue, 2 * SINGLE_OPTIONJUMP_BITS);
					
				} else {
					LOG.severe("Option delta too large. Actual delta: " + optionDelta);
					return new byte[] {}; // TODO throw exception?
				}
			}
			
			// write option delta
			optWriter.write(optionDelta, OPTIONDELTA_BITS);

			// write option length
			int length = opt.getLength();
			if (length <= MAX_OPTIONLENGTH_BASE) {

				optWriter.write(length, OPTIONLENGTH_BASE_BITS);
			} else if (length <= 1034) {
				/*
				 * When the Length field is set to 15, another byte is added as
				 * an 8-bit unsigned integer whose value is added to the 15,
				 * allowing option value lengths of 15-270 bytes. For option
				 * lengths beyond 270 bytes, we reserve the value 255 of an
				 * extension byte to mean
				 * "add 255, read another extension byte". Options that are
				 * longer than 1034 bytes MUST NOT be sent
				 */
				optWriter.write(15, 4);
				int rounds = (length - 15) / 255;
				
				for (int i = 0; i < rounds; i++) {
					optWriter.write(255, 8);
				}
				int remainingLength = length - ((rounds * 255) + 15);
				optWriter.write(remainingLength, 8);
				
			} else {
				LOG.severe("Option length larger than allowed 1034. Actual length: " + length);
				return new byte[] {}; // TODO throw exception?
			}

			// write option value
			optWriter.writeBytes(opt.getRawValue());

			// increment option count
			++optionCount;

			// update last option number
			lastOptionNumber = opt.getOptionNumber();
		}

		// create datagram writer to encode message data
		DatagramWriter writer = new DatagramWriter();

		// write fixed-size CoAP header
		writer.write(version, VERSION_BITS);
		writer.write(type.ordinal(), TYPE_BITS);
		if (optionCount < 15) {
			writer.write(optionCount, OPTIONCOUNT_BITS);
		} else {
			writer.write(15, OPTIONCOUNT_BITS);
		}
		writer.write(code, CODE_BITS);
		writer.write(messageID, ID_BITS);

		// write options
		writer.writeBytes(optWriter.toByteArray());
		
		if (optionCount > 14) {
			// end-of-options marker when there are more than 14 options
			writer.write(0xf0, 8);
		}

		// write payload
		writer.writeBytes(payload);

		// return encoded message
		return writer.toByteArray();
	}

	@Override
	public String toString() {

		String typeStr = "???";
		if (type != null) {
			switch (type) {
			case CON:
				typeStr = "CON";
				break;
			case NON:
				typeStr = "NON";
				break;
			case ACK:
				typeStr = "ACK";
				break;
			case RST:
				typeStr = "RST";
				break;
			default:
				typeStr = "???";
				break;
			}
		}
		String payloadStr = payload != null ? new String(payload) : null;
		return String.format("%s: [%s] %s '%s'(%d)", key(), typeStr, CodeRegistry.toString(code), payloadStr, payloadSize());
	}

	/**
	 * Returns a string that is assumed to uniquely identify a transaction. A
	 * transaction matches two buddies that have the same message ID between one
	 * this and the peer endpoint.
	 * 
	 * @return A string identifying the transaction
	 */
	public String transactionKey() {
		return String.format("%s|%d", peerAddress != null ? peerAddress.toString() : "local", messageID);
	}

	public String typeString() {
		if (type != null) {
			switch (type) {
			case CON:
				return "CON";
			case NON:
				return "NON";
			case ACK:
				return "ACK";
			case RST:
				return "RST";
			default:
				return "???";
			}
		}
		return null;
	}

	/**
	 * Notification method that is called whenever payload was appended using
	 * the appendPayload() method.
	 * 
	 * Subclasses may override this method to add custom handling code.
	 * 
	 * @param block
	 *            A byte array containing the data that was appended
	 */
	protected void payloadAppended(byte[] block) {
		// do nothing
	}

	/**
	 * The message's type which can have the following values:
	 * 
	 * 0: Confirmable 1: Non-Confirmable 2: Acknowledgment 3: Reset
	 */
	public enum messageType {
		CON, NON, ACK, RST
	}
}
