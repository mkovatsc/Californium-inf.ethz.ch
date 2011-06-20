package coap;

/*
 * This class describes the CoAP Media Type Registry as defined in 
 * draft-ietf-core-coap-06, section 11.3
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class MediaTypeRegistry {

	// Constants ///////////////////////////////////////////////////////////////

	public static final int PLAIN = 0;
	public static final int XML = 41;
	public static final int OCTET_STREAM = 42;
	public static final int EXI = 47;
	public static final int JSON = 50;

	// draft-ietf-core-link-format-04, section 7.4
	public static final int LINK_FORMAT = 40;

	// implementation specific
	public static final int UNDEFINED         = -1;
	
	// Static Functions ////////////////////////////////////////////////////////

	public static String toString(int mediaType) {
		switch (mediaType) {
		case PLAIN:
			//return "text/plain; charset=utf-8";
			return "text/plain";
		case LINK_FORMAT:
			return "application/link-format";
		case XML:
			return "application/xml";
		case OCTET_STREAM:
			return "application/octet-stream";
		case EXI:
			return "application/exi";
		case JSON:
			return "application/json";
		default:
			return "Unknown media type: " + mediaType;
		}
	}
}
