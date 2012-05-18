package ch.ethz.inf.vs.californium.dtls;

import java.util.Arrays;


/**
 * Application data messages are carried by the record layer and are fragmented,
 * compressed, and encrypted based on the current connection state. The messages
 * are treated as transparent data to the record layer.
 * 
 * @author Stefan Jucker
 * 
 */
public class ApplicationMessage implements DTLSMessage {

	private byte[] data;

	public ApplicationMessage(byte[] data) {
		this.data = data;
	}

	@Override
	public int getLength() {
		return data.length;
	}

	@Override
	public byte[] toByteArray() {
		return data;
	}

	public static DTLSMessage fromByteArray(byte[] byteArray) {
		return new ApplicationMessage(byteArray);
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("\tEncrypted Application Data: " + Arrays.toString(data) + "\n");
		
		return sb.toString();
	}

}
