package ch.ethz.inf.vs.californium.server;

import java.util.logging.Handler;
import java.util.logging.Logger;

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
		   StringBuilder sb = new StringBuilder();
		   for(byte b:bytes)
		      sb.append(String.format("%02x", b & 0xFF));
		   return sb.toString();
	}
}