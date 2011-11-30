package ch.ethz.inf.vs.californium.coap;

import java.util.HashMap;

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
	
	// initializer
	private static final HashMap<Integer, String[]> registry = new HashMap<Integer, String[]>();
	static
	{
		add(TEXT_PLAIN,						"text/plain",					"txt");
		//add(TEXT_XML,						"text/xml",						"xml"); // obsolete, use application/xml
		add(TEXT_CSV,						"text/cvs",						"cvs");
		add(TEXT_HTML,						"text/html",					"html");
		
		add(IMAGE_GIF,						"image/gif",					"gif");
		add(IMAGE_JPEG,						"image/jpeg",					"jpg");
		add(IMAGE_PNG,						"image/png",					"png");
		add(IMAGE_TIFF,						"image/tiff",					"tif");
		
		add(APPLICATION_LINK_FORMAT,		"application/link-format",		"wlnk");
		add(APPLICATION_XML,				"application/xml",				"xml");
		add(APPLICATION_OCTET_STREAM,		"application/octet-stream",		"bin");
		add(APPLICATION_RDF_XML,			"application/rdf+xml",			"rdf");
		add(APPLICATION_SOAP_XML,			"application/soap+xml",			"soap");
		add(APPLICATION_ATOM_XML,			"application/atom+xml",			"atom");
		add(APPLICATION_XMPP_XML,			"application/xmpp+xml",			"xmpp");
		add(APPLICATION_EXI,				"application/exi",				"exi");
		add(APPLICATION_FASTINFOSET,		"application/fastinfoset",		"finf");
		add(APPLICATION_SOAP_FASTINFOSET,	"application/soap+fastinfoset",	"soap.finf");
		add(APPLICATION_JSON,				"application/json",				"json");
		add(APPLICATION_X_OBIX_BINARY,		"application/x-obix-binary",	"obix");
	}
	
	// Static Functions ////////////////////////////////////////////////////////

	private static void add(int mediaType, String string, String extension) {
		registry.put(mediaType, new String[]{string, extension});
	}

	public static String toString(int mediaType) {
		String texts[] = registry.get(mediaType);
		
		if (texts!=null) {
			return texts[0];
		} else {
			return "Unknown media type: " + mediaType;
		}
	}
	
	public static String toFileExtension(int mediaType) {
		String texts[] = registry.get(mediaType);
		
		if (texts!=null) {
			return texts[1];
		} else {
			return "unknown";
		}
	}
}
