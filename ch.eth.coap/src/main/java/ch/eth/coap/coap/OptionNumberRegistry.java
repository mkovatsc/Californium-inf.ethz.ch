package ch.eth.coap.coap;

/*
 * This class describes the CoAP Option Number Registry 
 * as defined in draft-ietf-core-coap-07, 
 * sections 11.2 and 5.4.5
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class OptionNumberRegistry {

	// Constants ///////////////////////////////////////////////////////////////

	public static final int RESERVED_0     = 0;

	public static final int CONTENT_TYPE   = 1;
	public static final int MAX_AGE        = 2;
	public static final int PROXY_URI      = 3;
	public static final int ETAG           = 4;
	public static final int URI_HOST       = 5;
	public static final int LOCATION_PATH  = 6;
	public static final int URI_PORT       = 7;
	public static final int LOCATION_QUERY = 8;
	public static final int URI_PATH       = 9;
	public static final int OBSERVE        = 10; // draft-ietf-core-observe-02
	public static final int TOKEN          = 11;
	public static final int ACCEPT         = 12;
	//public static final int BLOCK          = 13; // deprecated, draft-ietf-core-block-02
	public static final int IF_MATCH       = 13;
	public static final int URI_QUERY      = 15;
	public static final int BLOCK2         = 17; // draft-ietf-core-block-03
	public static final int BLOCK1         = 19; // draft-ietf-core-block-03
	public static final int IF_NONE_MATCH  = 21;

	public static final int FENCEPOST_DIVISOR = 14;

	// Formats
	// ///////////////////////////////////////////////////////////////////

	public static enum optionFormats {
		integer, string, opaque, unknown, error
	}

	// Static Functions ////////////////////////////////////////////////////////

	/*
	 * Checks whether an option is elective
	 * 
	 * @param optionNumber The option number to check
	 * 
	 * @return True iff the option is elective
	 */
	public static boolean isElective(int optionNumber) {
		return (optionNumber & 1) == 0;
	}

	/*
	 * Checks whether an option is critical
	 * 
	 * @param optionNumber The option number to check
	 * 
	 * @return True iff the option is critical
	 */
	public static boolean isCritical(int optionNumber) {
		return (optionNumber & 1) == 1;
	}

	/*
	 * Checks whether an option is a fencepost option
	 * 
	 * @param optionNumber The option number to check
	 * 
	 * @return True iff the option is a fencepost option
	 */
	public static boolean isFencepost(int optionNumber) {
		return optionNumber % FENCEPOST_DIVISOR == 0;
	}

	/*
	 * Returns the next fencepost option number following a given option number
	 * 
	 * @param optionNumber The option number
	 * 
	 * @return The smallest fencepost option number larger than the given option
	 * number
	 */
	public static int nextFencepost(int optionNumber) {
		return (optionNumber / FENCEPOST_DIVISOR + 1) * FENCEPOST_DIVISOR;
	}

	/*
	 * Returns a string representation of the option number
	 * 
	 * @param code The option number to describe
	 * 
	 * @return A string describing the option number
	 */
	public static String toString(int optionNumber) {
		switch (optionNumber) {
		case RESERVED_0:
			return "Reserved (0)";
		case CONTENT_TYPE:
			return "Content-Type";
		case MAX_AGE:
			return "Max-Age";
		case PROXY_URI:
			return "Proxy-Uri";
		case ETAG:
			return "ETag";
		case URI_HOST:
			return "Uri-Host";
		case LOCATION_PATH:
			return "Location-Path";
		case URI_PORT:
			return "Uri-Port";
		case LOCATION_QUERY:
			return "Location-Query";
		case URI_PATH:
			return "Uri-Path";
		case OBSERVE:
			return "Observe";
		case TOKEN:
			return "Token";
		//case BLOCK:
		//	return "Block";
		case ACCEPT:
			return"Accept";
		case IF_MATCH:
			return "If-Match";
		case URI_QUERY:
			return "Uri-Query";
		case BLOCK2:
			return "Block2";
		case BLOCK1:
			return "Block1";
		case IF_NONE_MATCH:
			return "If-None-Match";
		}
		return String.format("Unknown option [number %d]", optionNumber);
	}

	/*
	 * Returns the option format based on the option number
	 * 
	 * @param optionNumber The option number
	 * 
	 * @return The option format corresponding to the option number
	 */
	public static optionFormats getFormatByNr(int optionNumber) {
		switch (optionNumber) {
		case RESERVED_0:
			return optionFormats.unknown;
		case CONTENT_TYPE:
			return optionFormats.integer;
		case PROXY_URI:
			return optionFormats.string;
		case ETAG:
			return optionFormats.opaque;
		case URI_HOST:
			return optionFormats.string;
		case LOCATION_PATH:
			return optionFormats.string;
		case URI_PORT:
			return optionFormats.integer;
		case LOCATION_QUERY:
			return optionFormats.string;
		case URI_PATH:
			return optionFormats.string;
		case TOKEN:
			return optionFormats.opaque;
		case URI_QUERY:
			return optionFormats.string;
		default:
			return optionFormats.error;
		}
	}

	/*
	 * This method returns an Option with it's default value corresponding to a
	 * given option number
	 * 
	 * @param optionNumber The given option number
	 * 
	 * @return An option corresponding to the given option number containing the
	 * default value as specified in draft-ietf-core-coap-05
	 */
	public static Option getDefaultOption(int optionNumber) {
		switch (optionNumber) {
		case CONTENT_TYPE:
			return new Option(0, CONTENT_TYPE);
		case MAX_AGE:
			return new Option(60, MAX_AGE);
		case PROXY_URI:
			return new Option("", PROXY_URI);
		case ETAG:
			return new Option(new byte[0], ETAG);
		case URI_HOST:
			// Use function which takes the IP as an argument
			return null;
		case LOCATION_PATH:
			return new Option("", LOCATION_PATH);
		case URI_PORT:
			// Use function which takes the UDP port as an argument
			return null;
		case LOCATION_QUERY:
			return new Option("", LOCATION_QUERY);
		case URI_PATH:
			return new Option("", URI_PATH);
		case TOKEN:
			return new Option(new byte[0], TOKEN);
		case URI_QUERY:
			return new Option("", URI_QUERY);
		default:
			return null;
		}
	}

	/*
	 * This method returns an Uri-Host Option with a given IP address
	 * corresponding to the given option number
	 * 
	 * @param optionNumber The given option number
	 * 
	 * @param ipAddress The given IP address as string
	 * 
	 * @return An option corresponding to the given option number containing the
	 * given IP as specified in draft-ietf-core-coap-05
	 */
	public static Option getDefaultOption(int optionNumber, String ipAddress) {
		return new Option(ipAddress, URI_HOST);
	}

	/*
	 * This method returns an Uri-Port Option with a given UDP port
	 * corresponding to the given option number
	 * 
	 * @param optionNumber The given option number
	 * 
	 * @param ipAddress The given UDP port as integer
	 * 
	 * @return An option corresponding to the given option number containing the
	 * given UDP port as specified in draft-ietf-core-coap-05
	 */
	public static Option getDefaultOption(int optionNumber, int udpPort) {
		return new Option(udpPort, URI_PORT);
	}
}
