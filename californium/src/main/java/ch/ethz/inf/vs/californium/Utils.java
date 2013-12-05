package ch.ethz.inf.vs.californium;

import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

/**
 * The class Utils contains auxiliary methods for Californium.
 *
 */
public class Utils {

	/*
	 * Prevent initialization
	 */
	private Utils() { }
	
	/**
	 * Converts the specified byte array to a hexadecimal string.
	 *
	 * @param bytes the byte array
	 * @return the hexadecimal code string
	 */
	public static String toHexString(byte[] bytes) {
		if (bytes == null) return "null";
		   StringBuilder sb = new StringBuilder();
		   for(byte b:bytes)
		      sb.append(String.format("%02x", b & 0xFF));
		   return sb.toString();
	}

	/**
	 * Formats a {@link Request} into a readable String representation. 
	 * 
	 * @param msg
	 * @return
	 */
	public static String prettyPrint(Request r) {
	
	        StringBuilder sb = new StringBuilder();
	        
	        sb.append("==[ CoAP Request ]============================================\n");
	
	
	        //sb.append(String.format("Address: %s:%d\n", r.getSource().toString(), r.getSourcePort()));
	        sb.append(String.format("MID    : %d\n", r.getMID()));
	        sb.append(String.format("Token  : %s\n", r.getTokenString()));
	        sb.append(String.format("Type   : %s\n", r.getType().toString()));
	        sb.append(String.format("Method : %s\n", r.getCode().toString()));
	        sb.append(String.format("Options: %s\n", r.getOptions().toString()));
	        sb.append(String.format("Payload: %d Bytes\n", r.getPayloadSize()));
	        if (r.getPayloadSize() > 0 && MediaTypeRegistry.isPrintable(r.getOptions().getContentFormat())) {
	        	sb.append("---------------------------------------------------------------");
	        	sb.append(r.getPayloadString());
	        }
	        sb.append("===============================================================");
	
	        return sb.toString();
	}

	/**
	 * Formats a {@link Request} into a readable String representation. 
	 * 
	 * @param msg
	 * @return
	 */
	public static String prettyPrint(Response r) {
	
	        StringBuilder sb = new StringBuilder();
	        
	        sb.append("==[ CoAP Response ]===========================================\n");
	
	        //sb.append(String.format("Address: %s:%d\n", r.getDestination().toString(), r.getDestinationPort()));
	        sb.append(String.format("MID    : %d\n", r.getMID()));
	        sb.append(String.format("Token  : %s\n", r.getTokenString()));
	        sb.append(String.format("Type   : %s\n", r.getType().toString()));
	        sb.append(String.format("Status : %s\n", r.getCode().toString()));
	        sb.append(String.format("Options: %s\n", r.getOptions().toString()));
	        sb.append(String.format("Payload: %d Bytes\n", r.getPayloadSize()));
	        if (r.getPayloadSize() > 0 && MediaTypeRegistry.isPrintable(r.getOptions().getContentFormat())) {
	        	sb.append("---------------------------------------------------------------\n");
	        	sb.append(r.getPayloadString());
	        	sb.append("\n");
	        }
	        sb.append("===============================================================");
	
	        return sb.toString();
	}
}