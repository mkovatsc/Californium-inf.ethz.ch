package coap;

/*
 * This class describes the CoAP Media Type Registry as defined in 
 * draft-ietf-core-coap-07, section 11.3
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class MediaTypeRegistry {

	// Constants ///////////////////////////////////////////////////////////////
	public static final int TEXT_PLAIN = 0;
	public static final int TEXT_XML = 1;
	public static final int TEXT_CSV = 2;
	public static final int TEXT_HTML = 3;
	public static final int IMAGE_GIF = 21; // 03
	public static final int IMAGE_JPEG = 22; // 03
	public static final int IMAGE_PNG = 23; // 03
	public static final int IMAGE_TIFF = 24; // 03
	public static final int AUDIO_RAW = 25; // 03
	public static final int VIDEO_RAW = 26; // 03
	public static final int APPLICATION_LINK_FORMAT = 40;
	public static final int APPLICATION_XML = 41;
	public static final int APPLICATION_OCTET_STREAM = 42;
	public static final int APPLICATION_RDF_XML = 43;
	public static final int APPLICATION_SOAP_XML = 44;
	public static final int APPLICATION_ATOM_XML = 45;
	public static final int APPLICATION_XMPP_XML = 46;
	public static final int APPLICATION_EXI = 47;
	public static final int APPLICATION_FASTINFOSET = 48; // 04
	public static final int APPLICATION_SOAP_FASTINFOSET = 49; // 04
	public static final int APPLICATION_JSON = 50; // 04
	public static final int APPLICATION_X_OBIX_BINARY = 51; // 04

	// implementation specific
	public static final int UNDEFINED         = -1;
	
	// Static Functions ////////////////////////////////////////////////////////

	public static String toString(int mediaType) {
		switch (mediaType) {
		case TEXT_PLAIN:
			//return "text/plain; charset=utf-8";
			return "text/plain";
		case TEXT_XML:
			return "text/xml";
		case TEXT_CSV:
			return "text/cvs";
		case TEXT_HTML:
			return "text/html";
			
		case IMAGE_GIF:
			return "image/gif";
		case IMAGE_JPEG:
			return "image/jpeg";
		case IMAGE_PNG:
			return "image/png";
		case IMAGE_TIFF:
			return "image/tiff";
			
		case APPLICATION_LINK_FORMAT:
			return "application/link-format";
		case APPLICATION_XML:
			return "application/xml";
		case APPLICATION_OCTET_STREAM:
			return "application/octet-stream";
		case APPLICATION_RDF_XML:
			return "application/rdf+xml";
		case APPLICATION_SOAP_XML:
			return "application/soap+xml";
		case APPLICATION_ATOM_XML:
			return "application/atom+xml";
		case APPLICATION_XMPP_XML:
			return "application/xmpp+xml";
		case APPLICATION_EXI:
			return "application/exi";
		case APPLICATION_FASTINFOSET:
			return "application/fastinfoset";
		case APPLICATION_SOAP_FASTINFOSET:
			return "application/soap+fastinfoset";
		case APPLICATION_JSON:
			return "application/json";
		case APPLICATION_X_OBIX_BINARY:
			return "application/x-obix-binary";
		default:
			return "Unknown media type: " + mediaType;
		}
	}
}
