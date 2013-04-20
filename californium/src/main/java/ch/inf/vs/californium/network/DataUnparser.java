package ch.inf.vs.californium.network;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.Message;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

public class DataUnparser {

	// TODO: Move these constants to a more appropriate location
	// CoAP-specific constants:
	
	/** number of bits used for the encoding of the CoAP version field */
	public static final int VERSION_BITS     = 2;
	
	/** number of bits used for the encoding of the message type field */
	public static final int TYPE_BITS        = 2;
	
	/** number of bits used for the encoding of the token length field */
	public static final int TOKEN_LENGTH_BITS = 4;

	/** number of bits used for the encoding of the request method/response code field */
	public static final int CODE_BITS = 8;

	/** number of bits used for the encoding of the message ID */
	public static final int MESSAGE_ID_BITS = 16;

	/** number of bits used for the encoding of the option delta field */
	public static final int OPTION_DELTA_BITS = 4;
	
	/** number of bits used for the encoding of the option delta field */
	public static final int OPTION_LENGTH_BITS = 4;
	
	/** One byte which indicates indicates the end of options and the start of the payload. */
	public static final byte PAYLOAD_MARKER = (byte) 0xFF;
	
	public static final int EMPTY_CODE = 0;
	public static final int REQUEST_CODE_LOWER_BOUND = 1;
	public static final int REQUEST_CODE_UPPER_BOUNT = 31;
	public static final int RESPONSE_CODE_LOWER_BOUND = 64;
	public static final int RESPONSE_CODE_UPPER_BOUND = 191;
	
	private RawData raw;
	private DatagramReader reader;
	
	private int version;
	private int type;
	private int tokenlength;
	private int code;
	private int mid;
	
	public DataUnparser(RawData raw) {
		this.raw = raw;
		this.reader = new DatagramReader(raw.getBytes());
		this.version = reader.read(VERSION_BITS);
		this.type = reader.read(TYPE_BITS);
		this.tokenlength = reader.read(TOKEN_LENGTH_BITS);
		this.code = reader.read(CODE_BITS);
		this.mid = reader.read(MESSAGE_ID_BITS);
	}
	
	public boolean isRequest() {
		return code >= REQUEST_CODE_LOWER_BOUND &&
				code <= REQUEST_CODE_UPPER_BOUNT;
	}
	
	public boolean isResponse() {
		return code >= RESPONSE_CODE_LOWER_BOUND &&
				code <= RESPONSE_CODE_UPPER_BOUND;
	}
	
	public Request unparseRequest() {
		assert(isRequest());
		Request request = new Request(Code.valueOf(code));
		request.setType(Type.valueOf(type));
		request.setMid(mid);		
		
		if (tokenlength>0) {
			request.setToken(reader.readBytes(tokenlength));
		} else {
			request.setToken(new byte[0]);
		}
		
		throw new RuntimeException("Not implemented yet");
	}
	
	public Response unparseResponse() {
		assert(isResponse());
		return null;
	}
	
	public Message unparseEmptyMessage() {
		assert(!isRequest() && !isResponse());
		
		return null;
	}
}
