package coap;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import util.Properties;

import layers.UDPLayer;

/*
 * This class describes the functionality of the CoAP messages
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class Message {
	
	// CoAP specific definitions ///////////////////////////////////////////////
	
	// number of bits used for the encoding of the CoAP version field
	public static final int VERSION_BITS     = 2;
	
	// number of bits used for the encoding of the message type field
	public static final int TYPE_BITS        = 2;
	
	// number of bits used for the encoding of the option count field
	public static final int OPTIONCOUNT_BITS = 4;
	
	// number of bits used for the encoding of the request method/
	// response code field
	public static final int CODE_BITS        = 8;
	
	// number of bits used for the encoding of the transaction ID
	public static final int ID_BITS         = 16;
	
	// number of bits used for the encoding of the option delta
	public static final int OPTIONDELTA_BITS = 4;
	
	// number of bits used for the encoding of the base option length field
	// if all bits in this field are set to one, the extended option length
	// field is additionally used to encode the option length
	public static final int OPTIONLENGTH_BASE_BITS     = 4;
	
	// number of bits used for the encoding of the extended option length field
	// this field is used when all bits in the base option length field 
	// are set to one
	public static final int OPTIONLENGTH_EXTENDED_BITS = 8;
	
	// Derived constants ///////////////////////////////////////////////////////
	
	public static final int MAX_ID 
		= (1 << ID_BITS)- 1;
	
	// maximum option delta that can be encoded without using fencepost options
	public static final int MAX_OPTIONDELTA
		= (1 << OPTIONDELTA_BITS) - 1;
	
	// maximum option length that can be encoded using 
	// the base option length field only
	public static final int MAX_OPTIONLENGTH_BASE      
		= (1 << OPTIONLENGTH_BASE_BITS) - 2;
	
	// Static Functions ////////////////////////////////////////////////////////
	
	public Message newReply(boolean ack) {

		// TODO use this for Request.respond() or vice versa
		
		Message reply = new Message();
		
		// set message type
		if (type == messageType.Confirmable) {
			reply.type = ack ? 
				messageType.Acknowledgement : messageType.Reset;
		} else {
			reply.type = messageType.Non_Confirmable;
		}
		
		// echo the message ID
		reply.messageID = this.messageID;
		
		// echo token
		reply.setOption(getFirstOption(OptionNumberRegistry.TOKEN));
		reply.needsToken = needsToken;
		
		// set the receiver URI of the reply to the sender of this message
		reply.uri = this.uri;
		
		// create an empty reply by default
		reply.code = CodeRegistry.EMPTY_MESSAGE;
		
		return reply;
		
	}
	
	public static Message newAcknowledgement(Message msg) {
		
		Message ack = new Message();
		
		// set message type to Acknowledgement
		ack.setType(messageType.Acknowledgement);
		
		// echo the Message ID
		ack.setID(msg.getID());
		
		// set receiver URI to sender URI of the message
		// to acknowledge
		ack.setURI(msg.getURI());
		
		ack.needsToken = msg.needsToken;
		
		// create an empty Acknowledgement by default,
		// can be piggy-backed with a response by the user
		ack.setCode(CodeRegistry.EMPTY_MESSAGE);
		
		return ack;
	}
	
	public static Message newReset(Message msg) {
		
		Message rst = new Message();
		
		// set message type to Reset
		rst.setType(messageType.Reset);
		
		// echo the Message ID
		rst.setID(msg.getID());
		
		// set receiver URI to sender URI of the message
		// to reset
		rst.setURI(msg.getURI());
		
		rst.needsToken = msg.needsToken;
		
		// Reset must be empty
		rst.setCode(CodeRegistry.EMPTY_MESSAGE);
		
		return rst;
	}
	
	/*
	 * Matches two messages to buddies if they have the same message ID
	 * 
	 * @param msg1 The first message
	 * @param msg2 the second message
	 * @return True iif the messages were matched to buddies
	 */
	public static boolean matchBuddies(Message msg1, Message msg2) {
		
		if (
			msg1 != null && msg2 != null &&  // both messages must exist
			msg1 != msg2 &&                  // no message can be its own buddy 
			msg1.getID() == msg2.getID()     // buddy condition: same IDs
		) {
			
			assert msg1.buddy == null;
			assert msg2.buddy == null;
			
			msg1.buddy = msg2;
			msg2.buddy = msg1;
			
			return true;
			
		} else {
			return false;
		}
	}
	
	
	// Constructors ////////////////////////////////////////////////////////////
	/*
	 * Default constructor for a new CoAP message
	 */
	public Message () {
	}

	/*
	 * Constructor for a new CoAP message
	 * 
	 * @param type The type of the CoAP message
	 * @param code The code of the CoAP message (See class CodeRegistry)
	 */
	public Message(messageType type, int code) {
		this.type = type;
		this.code = code;
	}	
	
	/*
	 * Constructor for a new CoAP message
	 * 
	 * @param uri The URI of the CoAP message
	 * @param payload The payload of the CoAP message
	 */
	public Message(URI uri, messageType type, int code, int id, byte[] payload) {
		this.uri = uri;
		this.type = type;
		this.code = code;
		this.messageID = id;
		this.payload = payload;
	}
	
	// Serialization ///////////////////////////////////////////////////////////

	/*
	 * Encodes the message into its raw binary representation
	 * as specified in draft-ietf-core-coap-05, section 3.1
	 * 
	 * @return A byte array containing the CoAP encoding of the message
	 * 
	 */
	public byte[] toByteArray() {
		
		// create datagram writer to encode options
		DatagramWriter optWriter = new DatagramWriter(); 
		
		int optionCount = 0;
		int lastOptionNumber = 0;
		for (Option opt : getOptionList()) {
			
			// do not encode options with default values
			if (opt.isDefaultValue()) continue;
			
			// calculate option delta
			int optionDelta = opt.getOptionNumber() - lastOptionNumber;
			
			// ensure that option delta value can be encoded correctly
			while (optionDelta > MAX_OPTIONDELTA) {
				
				// option delta is too large to be encoded:
				// add fencepost options in order to reduce the option delta
				
				// get fencepost option that is next to the last option
				int fencepostNumber = 
					OptionNumberRegistry.nextFencepost(lastOptionNumber);
				
				// calculate fencepost delta
				int fencepostDelta = fencepostNumber - lastOptionNumber;
				
				// correctness assertions
				//assert fencepostDelta > 0: "Fencepost liveness";
				//assert fencepostDelta <= MAX_OPTIONDELTA: "Fencepost safety";
				if (fencepostDelta <= 0) {
					System.out.printf("Fencepost liveness violated: delta = %d\n", fencepostDelta);
				}
				
				if (fencepostDelta > MAX_OPTIONDELTA) {
					System.out.printf("Fencepost safety violated: delta = %d\n", fencepostDelta);
				}

				
				// write fencepost option delta
				optWriter.write(fencepostDelta, OPTIONDELTA_BITS);
				
				// fencepost have an empty value
				optWriter.write(0, OPTIONLENGTH_BASE_BITS);
				//System.out.printf("DEBUG: %d\n", fencepostDelta);
				
				// increment option count
				++optionCount;
				
				// update last option number
				lastOptionNumber = fencepostNumber;
				
				// update option delta
				optionDelta -= fencepostDelta;
			}
			
			// write option delta
			optWriter.write(optionDelta, OPTIONDELTA_BITS);
			
			// write option length
			int length = opt.getLength();
			if (length <= MAX_OPTIONLENGTH_BASE) {
				
				// use option length base field only to encode
				// option lengths less or equal than MAX_OPTIONLENGTH_BASE
				
				optWriter.write(length, OPTIONLENGTH_BASE_BITS);
				
			} else {
				
				// use both option length base and extended field
				// to encode option lengths greater than MAX_OPTIONLENGTH_BASE
				
				int baseLength = MAX_OPTIONLENGTH_BASE + 1;
				optWriter.write(baseLength, OPTIONLENGTH_BASE_BITS);
				
				int extLength = length - baseLength;
				optWriter.write(extLength, OPTIONLENGTH_EXTENDED_BITS);
				
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
		writer.write(optionCount, OPTIONCOUNT_BITS);
		writer.write(code, CODE_BITS);
		writer.write(messageID, ID_BITS);
		
	
		// write options
		writer.writeBytes(optWriter.toByteArray());
		
		//write payload
		writer.writeBytes(payload);

		// return encoded message
		return writer.toByteArray();
	}

	/*
	 * Decodes the message from the its binary representation
	 * as specified in draft-ietf-core-coap-05, section 3.1
	 * 
	 * @param byteArray A byte array containing the CoAP encoding of the message
	 * 
	 */
	public static Message fromByteArray(byte[] byteArray) {

		//Initialize DatagramReader
		DatagramReader datagram = new DatagramReader(byteArray);
		
		//Read current version
		int version = datagram.read(VERSION_BITS);
		
		//Read current type
		messageType type = getTypeByID(datagram.read(TYPE_BITS));
		
		//Read number of options
		int optionCount = datagram.read(OPTIONCOUNT_BITS);
		
		//Read code
		int code = datagram.read(CODE_BITS);
		if (!CodeRegistry.isValid(code)) {
			System.out.printf("ERROR: Invalid message code: %d\n", code);
			return null;
		}

		// create new message with subtype according to code number
		Message msg;
		try {
			msg = CodeRegistry.getMessageClass(code).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
		msg.version = version;
		msg.type = type;
		msg.code = code;
		
		//Read message ID
		msg.messageID = datagram.read(ID_BITS);
		
		//Current option nr initialization
		int currentOption = 0;

		//Loop over all options
		//System.out.println("DEBUG OPTION CNT: " + optionCount);
		for (int i=0; i < optionCount; i++) {
			
			//Read option delta bits
			int optionDelta = datagram.read(OPTIONDELTA_BITS);
			
			currentOption += optionDelta;
			//System.out.printf("DEBUG MSG: %d\n", optionDelta);
			if (OptionNumberRegistry.isFencepost(currentOption))
			{
				//Read number of options
				datagram.read(OPTIONLENGTH_BASE_BITS);
				
				//Fencepost: Reset Option to 0
				//TODO: FIX
				//currentOption = 0;
			} else {
				
				//Read option length
				int length = datagram.read(OPTIONLENGTH_BASE_BITS);
				
				if (length > MAX_OPTIONLENGTH_BASE)
				{
					//Read extended option length
					//length = datagram.read(OPTIONLENGTH_EXTENDED_BITS)
					//		 - (MAX_OPTIONLENGTH_BASE + 1);
					
					length += datagram.read(OPTIONLENGTH_EXTENDED_BITS);
				}
				//Read option
				//Option opt = new Option (datagram.readBytes(length), currentOption);
				Option opt = Option.fromNumber(currentOption);
				opt.setValue(datagram.readBytes(length));
				
				//Add option to message
				msg.addOption(opt);
			}
			
		}

		//Get payload
		msg.payload = datagram.readBytesLeft();
		
		// incoming message already have a token, 
		// including implicit empty token
		msg.needsToken = false;
		
		return msg;
	}
	
	
	// Procedures //////////////////////////////////////////////////////////////
	
	/*
	 * This procedure sets the URI of this CoAP message
	 * 
	 * @param uri The URI to which the current message URI should be set to
	 */
	public void setURI(URI uri) {
		
		// include Uri Options as specified in 
		// draft-ietf-core-coap-05, section 6.3
		
		// TODO unclear when/how to include Uri-Host and Uri-Port options
		
		if (uri != null) {
			
			// set Uri-Path options
			String path = uri.getPath();
			if (path != null && path.length() > 1) {
				
				// NOTE: Use this code for compatibility with draft 3
				// which doesn't allow several Uri-Path options, as in draft 5.
				//setOption(new Option(path.substring(1), OptionNumberRegistry.URI_PATH));
				
				List<Option> uriPaths = Option.split(OptionNumberRegistry.URI_PATH, path, "/");
				setOptions(OptionNumberRegistry.URI_PATH, uriPaths);

			}
			
			// set Uri-Query options
			String query = uri.getQuery();
			if (query != null) {

				// split the query into arguments
				List<Option> uriQuery = Option.split(OptionNumberRegistry.URI_QUERY, query, "&");
				setOptions(OptionNumberRegistry.URI_QUERY, uriQuery);
			}
			
		}
		
		// finally, set new Uri
		this.uri = uri;
	}

	// Option get/set methods //////////////////////////////////////////////////
	
	public Option getToken() {
		Option opt = getFirstOption(OptionNumberRegistry.TOKEN);
		return opt != null ? opt : TokenManager.emptyToken;
	}
	
	public void setToken(Option token) {
		setOption(token);
	}
	
	public int getContentType() {
		Option opt = getFirstOption(OptionNumberRegistry.CONTENT_TYPE);
		return opt != null ? opt.getIntValue() : MediaTypeRegistry.UNDEFINED;
	}
	
	public void setContentType(int ct) {
		if (ct != MediaTypeRegistry.UNDEFINED) {
			setOption(new Option(ct, OptionNumberRegistry.CONTENT_TYPE));
		} else {
			setOptions(OptionNumberRegistry.CONTENT_TYPE, null);
		}
	}
	
	public String getLocationPath() {
		return Option.join(getOptions(OptionNumberRegistry.LOCATION_PATH), "/");
	}
	
	public void setLocationPath(String locationPath) {
		setOptions(OptionNumberRegistry.LOCATION_PATH, 
			Option.split(OptionNumberRegistry.LOCATION_PATH, locationPath, "/"));
	}
	
	public boolean setURI(String uri) {
		try {
			setURI(new URI(uri));
			return true;
		} catch (URISyntaxException e) {
			System.out.printf("[%s] Failed to set URI: %s\n",
				getClass().getName(), e.getMessage());
			return false;
		}
	}
	
	/*
	 * This procedure sets the payload of this CoAP message
	 * 
	 * @param payload The payload to which the current message payload should
	 *                be set to
	 */
	public void setPayload(byte[] payload) {
		this.payload = payload;
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
			setOption(new Option(mediaType, OptionNumberRegistry.CONTENT_TYPE));
		}
	}
	
	public void setPayload(String payload) {
		setPayload(payload, MediaTypeRegistry.PLAIN);
	}
	
	/*
	 * This procedure sets the type of this CoAP message
	 * 
	 * @param msgType The message type to which the current message type should
	 *                be set to
	 */
	public void setType(messageType msgType) {
		this.type = msgType;
	}
	
	/*
	 * This procedure sets the code of this CoAP message
	 * 
	 * @param code The message code to which the current message code should
	 *             be set to
	 */
	public void setCode(int code) {
		this.code = code;
	}
	
	/*
	 * This procedure sets the ID of this CoAP message
	 * 
	 * @param id The message ID to which the current message ID should
	 *           be set to
	 */
	public void setID(int id) {
		this.messageID = id;
	}
	
	// Functions ///////////////////////////////////////////////////////////////
		
	/*
	 * This function returns the URI of this CoAP message
	 * 
	 * @return The current URI
	 */
	public URI getURI() {
		return this.uri;
	}
	
	/*
	 * This function returns the payload of this CoAP message
	 * 
	 * @return The current payload.
	 */
	public byte[] getPayload() {
		return this.payload;
	}
	
	public String getPayloadString() {
		try {
			return payload != null ? new String(payload, "UTF-8") : null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * This function returns the version of this CoAP message
	 * 
	 * @return The current version.
	 */
	public int getVersion() {
		return this.version;
	}
	
	/*
	 * This function returns the type of this CoAP message
	 * 
	 * @return The current type.
	 */
	public messageType getType() {
		return this.type;
	}
	
	/*
	 * This function returns the code of this CoAP message
	 * 
	 * @return The current code.
	 */
	public int getCode() {
		return this.code;
	}
	
	/*
	 * This function returns the ID of this CoAP message
	 * 
	 * @return The current ID.
	 */
	public int getID() {
		return this.messageID;
	}

	
	/*
	 * This procedure adds an option to the list of options of this CoAP message
	 * 
	 * @param opt The option which should be added to the list of options of the
	 *            current CoAP message
	 */
	public void addOption(Option opt) {
		
		List<Option> list = optionMap.get(opt.getOptionNumber());
		if (list == null) {
			list = new ArrayList<Option>();
			optionMap.put(opt.getOptionNumber(), list);
		}
		list.add(opt);
	}

	/*
	 * This procedure removes all options of the given number from this CoAP message
	 * 
	 * @param optionNumber The number of the options to remove
	 *            
	 */	
	public void removeOption(int optionNumber) {
		optionMap.remove(optionNumber);
	}
	
	/*
	 * This function returns all options with the given option number
	 * 
	 * @param optionNumber The option number
	 * @return A list containing the options with the given number
	 */
	public List<Option> getOptions(int optionNumber) {
		return optionMap.get(optionNumber);
	}

	/*
	 * Sets all options with the specified option number
	 * 
	 * @param optionNumber The option number
	 * @param opt The list of the options
	 */
	public void setOptions(int optionNumber, List<Option> opt) {
		// TODO Check if all options are consistent with optionNumber
		optionMap.put(optionNumber, opt);
		
		if (optionNumber == OptionNumberRegistry.TOKEN) {
			needsToken = false;
		}

	}
	
	//TODO: Comment
	public void setOptionMap(Map<Integer, List<Option>> optMap) {
		optionMap = optMap;
	}
	
	//TODO: Comment
	public Map<Integer, List<Option>> getOptionMap() {
		return optionMap;
	}
	
	/*
	 * Returns the first option with the specified option number
	 * 
	 * @param optionNumber The option number
	 * @return The first option with the specified number, or null
	 */
	public Option getFirstOption(int optionNumber) {
		
		List<Option> list = getOptions(optionNumber);
		return list != null && !list.isEmpty() ? list.get(0) : null;
	}
	
	/*
	 * Sets the option with the specified option number
	 * 
	 * @param opt The option to set
	 */
	public void setOption(Option opt) {

		if (opt != null) {
			List<Option> options = new ArrayList<Option>();
			options.add(opt);
			setOptions(opt.getOptionNumber(), options);
			
			if (opt.getOptionNumber() == OptionNumberRegistry.TOKEN) {
				needsToken = false;
			}
		}
	}

	/*
	 * Returns a sorted list of all included options
	 * 
	 * @return A sorted list of all options (copy)
	 */
	public List<Option> getOptionList() {

		List<Option> list = new ArrayList<Option>();
		
		for (List<Option> option : optionMap.values()) {
			if (option != null) {
				list.addAll(option);
			}
		}
		
		return list;
	}	
	
	/*
	 * This function returns the number of options of this CoAP message
	 * 
	 * @return The current number of options.
	 */
	public int getOptionCount() {
		return getOptionList().size();
	}
	
	/*
	 * Appends data to this message's payload.
	 * 
	 * @param block The byte array containing the data to append
	 */
	public synchronized void appendPayload(byte[] block) {
	
		if (block != null) {
			if (payload != null) {
		
				byte[] oldPayload = payload;
				payload = new byte[oldPayload.length + block.length];
				System.arraycopy(oldPayload, 0,	payload, 0, 
					oldPayload.length);
				System.arraycopy(block, 0, payload, oldPayload.length, 
					block.length);
				
			} else {
				
				payload = block.clone();
			}
			
			// wake up threads waiting in readPayload()
			notifyAll();
			
			// call notification method
			payloadAppended(block);
		}		
	}
	
	/*
	 * Reads the byte at the given position from the payload and blocks
	 * if the data is not yet available.
	 * 
	 * @pos The position of the byte to read
	 * @return The byte at the given position, or -1 if it does not exist
	 */
	public synchronized int readPayload(int pos) {
		
		// check if there is data to read
		while (pos >= payload.length) {
			
			// all payload was read
			if (complete) {
				return -1;
			} else try {
				// wait until more data is appended
				wait();
			} catch (InterruptedException e) {
				// TODO Think more about this
				return -1;
			}
		}
		return payload[pos];		
	}
	
	public int payloadSize() {
		return payload != null ? payload.length : 0;
	}
	
	/*
	 * Checks whether the message is complete, i.e. its payload
	 * was completely received.
	 * 
	 * @return True iff the message is complete
	 */
	public boolean isComplete() {
		return complete;
	}
	
	/*
	 * Sets the complete flag of this message.
	 * 
	 * @param complete The value of the complete flag
	 */
	public void setComplete(boolean complete) {
		this.complete = complete;
		if (complete) {
			completed();
		}
	}
	
	/*
	 * Sets the timestamp associated with this message.
	 * 
	 * @param timestamp The new timestamp, in milliseconds
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	/*
	 * Returns the timestamp associated with this message.
	 * 
	 * @return The timestamp of the message, in milliseconds
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
	/*
	 * Notification method that is called when the message's complete flag
	 * changed to true.
	 * 
	 * Subclasses may override this method to add custom handling code.
	 */
	protected void completed() {
		// do nothing
	}
	
	/*
	 * Notification method that is called when the transmission of this
	 * message was cancelled due to timeout.
	 * 
	 *  Subclasses may override this method to add custom handling code.
	 */
	public void handleTimeout() {
		// do nothing
	}
	
	/*
	 * Notification method that is called whenever payload was appended
	 * using the appendPayload() method.
	 * 
	 * Subclasses may override this method to add custom handling code.
	 * 
	 * @param block A byte array containing the data that was appended
	 */
	protected void payloadAppended(byte[] block) {
		// do nothing
	}
	
	/*
	 * This function returns the buddy of this CoAP message
	 * Two messages are buddies iif they have the same message ID
	 * 
	 * @return The buddy of the message, if any
	 */
	public Message getBuddy() {
		return this.buddy;
	}
	
	/*
	 * TODO: description
	 */
	public static messageType getTypeByID(int id) {
		switch (id) {
			case 0:
				return messageType.Confirmable;
			case 1:
				return messageType.Non_Confirmable;
			case 2:
				return messageType.Acknowledgement;
			case 3:
				return messageType.Reset;
			default:
				return messageType.Confirmable;
		}
	}
	
	/*
	 * This function checks if the message is a request message
	 * 
	 * @return True if the message is a request
	 */
	public boolean isRequest() {
		return CodeRegistry.isRequest(code);
	}
	
	/*
	 * This function checks if the message is a response message
	 * 
	 * @return True if the message is a response
	 */
	public boolean isResponse() {
		return CodeRegistry.isResponse(code);
	}

	public boolean isConfirmable() {
		return type == messageType.Confirmable;
	}
	
	public boolean isNonConfirmable() {
		return type == messageType.Non_Confirmable;
	}
	
	public boolean isAcknowledgement() {
		return type == messageType.Acknowledgement;
	}
	
	public boolean isReset() {
		return type == messageType.Reset;
	}
	
	public boolean isReply() {
		return isAcknowledgement() || isReset();
	}
	
	public boolean hasFormat(int mediaType) {
		/*Option opt = getFirstOption(OptionNumberRegistry.CONTENT_TYPE);
		return opt != null ? opt.getIntValue() == mediaType : false;
		*/
		return (getContentType() == mediaType);
	}
	
	public boolean hasOption(int optionNumber) {
		return getFirstOption(optionNumber) != null;
	}
	
	@Override
	public String toString() {

		String typeStr = "???";
		if (type != null) switch (type) {
			case Confirmable     : typeStr = "CON"; break;
			case Non_Confirmable : typeStr = "NON"; break;
			case Acknowledgement : typeStr = "ACK"; break;
			case Reset           : typeStr = "RST"; break;
			default              : typeStr = "???"; break;
		}
		String payloadStr = payload != null ? new String(payload) : null;
		return String.format("%s: [%s] %s '%s'(%d)",
			key(), typeStr, CodeRegistry.toString(code), 
			payloadStr, payloadSize());
	}
	
	public String typeString() {
		if (type != null) switch (type) {
			case Confirmable     : return "CON";
			case Non_Confirmable : return "NON";
			case Acknowledgement : return "ACK";
			case Reset           : return "RST";
			default              : return "???";
		}
		return null;
	}
	
	public void log(PrintStream out) {
		
		
		String kind = "MESSAGE ";
		if (isRequest()) {
			kind = "REQUEST ";
		} else if (isResponse()) {
			kind = "RESPONSE";
		}
		out.printf("==[ COAP %s ]============================================\n", kind);
		
		List<Option> options = getOptionList();
		
		out.printf("URI    : %s\n", uri != null ? uri.toString() : "NULL");
		out.printf("ID     : %d\n", messageID);
		out.printf("Type   : %s\n", typeString());
		out.printf("Code   : %s\n", CodeRegistry.toString(code));
		out.printf("Options: %d\n", options.size());
		for (Option opt : options) {
			out.printf("  * %s: %s (%d Bytes)\n", 
				opt.getName(), opt.getDisplayValue(), opt.getLength()
			);
		}
		out.printf("Payload: %d Bytes\n", payloadSize());
		out.println("---------------------------------------------------------------");
		if (payloadSize() > 0) out.println(getPayloadString());
		out.println("===============================================================");
		
	}
	
	public void log() {
		log(System.out);
	}
	
	public String endpointID() {
		InetAddress address = null;
		try {
			address = getAddress();
		} catch (UnknownHostException e) {
		}
		int port = uri != null ? uri.getPort() : -1;
		if (port < 0) {
			port = Properties.std.getInt("DEFAULT_PORT");
		}
		return String.format("%s:%d", 
			address != null ? address.getHostAddress() : "NULL",
			port
		);
	}
	
	/*
	 * Returns a string that is assumed to uniquely identify a message
	 * 
	 * Note that for incoming messages, the message ID is not sufficient
	 * as different remote endpoints may use the same message ID.
	 * Therefore, the message key includes the identifier of the sender
	 * next to the message id. 
	 * 
	 * @return A string identifying the message
	 */
	public String key() {
		return String.format("%s|%s#%d", 
			endpointID(), typeString(),	messageID);
	}
	
	public InetAddress getAddress() throws UnknownHostException {
		return InetAddress.getByName(uri != null ? uri.getHost() : null);
	}
	
	public String transferID() {
		Option tokenOpt = getFirstOption(OptionNumberRegistry.TOKEN);
		String token = tokenOpt != null ? tokenOpt.getDisplayValue() : "";
		return String.format("%s[%s]",
			endpointID(), token);
	}
	
	/*
	 * This method is overridden by subclasses according to the Visitor Pattern
	 *
	 * @param handler A handler for this message
	 */
	public void handleBy(MessageHandler handler) {
		// do nothing
	}
	
	public boolean needsToken() {
		return needsToken;
	}
	
	public void setNeedsToken(boolean value) {
		needsToken = value;
	}
	
	// Attributes //////////////////////////////////////////////////////////////
	
	//The message's URI
	private URI uri;
	
	//The message's payload
	private byte[] payload;
	
	// indicates whether the message's payload is complete
	private boolean complete;
	
	/*
	 * The message's version. This must be set to 1. Other numbers are reserved
	 * for future versions
	 */
	private int version = 1;
	
	//The message's type.
	private messageType type;
	
	/*
	 * The message's code
	 * 
	 *      0: Empty
	 *   1-31: Request
	 * 64-191: Response
	 */
	private int code;
	
	//The message's ID
	private int messageID = -1;
	
	// The message's buddy. Two messages are buddies iff
	// they have the same message ID
	private Message buddy;
	
	//The message's options
	private Map<Integer, List<Option>> optionMap
		= new TreeMap<Integer, List<Option>>();
	
	//A time stamp associated with the message
	private long timestamp;
	
	// indicates if the message requires a token
	// this is required to handle implicit empty tokens (default value)
	protected boolean needsToken = true;
	
	// Declarations ////////////////////////////////////////////////////////////
	/*
	 * The message's type which can have the following values:
	 * 
	 * 0: Confirmable
	 * 1: Non-Confirmable
	 * 2: Acknowledgment
	 * 3: Reset
	 */
	public enum messageType {
		Confirmable,
		Non_Confirmable,
		Acknowledgement,
		Reset
	}
}
