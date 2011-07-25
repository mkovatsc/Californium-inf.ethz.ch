package util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/*
 * This class implements the functionality of a Properties registry.
 * 
 * It is used to manage CoAP- and Californium-specific constants in a central
 * place. The properties are initialized in the init() section and can be overriden
 * by a user-defined .properties file. If the file does not exist upon the initialization,
 * it will be created so that a valid configuration always exists.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 *  
 */
public class Properties extends java.util.Properties {

	/**
	 * auto-generated to eliminate warning
	 */
	private static final long serialVersionUID = -8883688751651970877L;

	// header for the properties file
	private static final String HEADER 
		= "Californium CoAP Properties file";
	
	// name of the default properties file
	private static final String DEFAULT_FILENAME 
		= "Californium.properties";
	
	// static initialization of the properties
	private void init() {
		
		/*** CoAP Protocol constants ***/
		
		// default CoAP port as defined in draft-ietf-core-coap-05, section 7.1:
		// MUST be supported by a server for resource discovery and
		// SHOULD be supported for providing access to other resources.
		set("DEFAULT_PORT", 5683);
		
		// CoAP URI scheme name as defined in draft-ietf-core-coap-05, section 11.4:
		set("URI_SCHEME_NAME", "coap");
		
		// constants to calculate initial timeout for confirmable messages,
		// used by the exponential backoff mechanism
		set("RESPONSE_TIMEOUT", 2000); // [milliseconds]
		set("RESPONSE_RANDOM_FACTOR", 1.5);

		// maximal number of retransmissions before the attempt
		// to transmit a message is canceled		
		set("MAX_RETRANSMIT", 4);
		
		/*** Implementation-specific ***/
		
		// buffer size for incoming datagrams, in bytes
		// TODO find best value
		set("RX_BUFFER_SIZE", 4 * 1024); // [bytes]
		
		// capacity for caches used for duplicate detection and retransmissions
		set("MESSAGE_CACHE_SIZE", 32); // [messages]
		
		// time limit for transactions to complete,
		// used to avoid infinite waits for replies to non-confirmables
		// and separate responses
		set("DEFAULT_TRANSACTION_TIMEOUT", 10000); // [milliseconds]
		
		// the default block size for block-wise transfers
		// must be power of two between 16 and 1024
		set("DEFAULT_BLOCK_SIZE", 512); // [bytes]
		
	}

	// default properties used by the library
	public static Properties std = new Properties(DEFAULT_FILENAME);
	
	// Constructors ////////////////////////////////////////////////////////////
	
	public Properties(String fileName) {
		init();
		initUserDefined(fileName);
	}
	
	public void set(String key, String value) {
		setProperty(key, value);
	}
	
	public void set(String key, int value) {
		setProperty(key, String.valueOf(value));
	}
	
	public void set(String key, double value) {
		setProperty(key, String.valueOf(value));
	}
	
	public String getStr(String key) {
		String value = getProperty(key);
		if (value == null) {
			Log.error(this, "Undefined string property: %s", key);
		}
		return value;
	}
	
	public int getInt(String key) {
		String value = getProperty(key);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				Log.error(this, "Invalid integer property: %s=%s", key, value);
			}
		} else {
			Log.error(this, "Undefined integer property: %s", key);
		}
		return 0;
	}
	
	public double getDbl(String key) {
		String value = getProperty(key);
		if (value != null) {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				Log.error(this, "Invalid double property: %s=%s", key, value);
			}
		} else {
			Log.error(this, "Undefined double property: %s", key);
		}
		return 0.0;		
	}
	
	public void load(String fileName) throws IOException {
		InputStream in = new FileInputStream(fileName);
		load(in);
	}
	
	public void store(String fileName) throws IOException {
		OutputStream out = new FileOutputStream(fileName);
		store(out, HEADER);
	}

	private void initUserDefined(String fileName) {
		try {
			load(fileName);
		} catch (IOException e) {
			// file does not exist:
			// write default properties
			try {
				store(fileName);
			} catch (IOException e1) {
				Log.warning(this, "Failed to create configuration file: %s",
					e1.getMessage());
			}
		}
	}


}
