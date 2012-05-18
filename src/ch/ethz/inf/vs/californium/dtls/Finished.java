package ch.ethz.inf.vs.californium.dtls;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import sun.security.internal.spec.TlsPrfParameterSpec;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * A Finished message is always sent immediately after a
 * {@link ChangeCipherSpecMessage} to verify that the key exchange and
 * authentication processes were successful. It is essential that a
 * {@link ChangeCipherSpecMessage} be received between the other handshake
 * messages and the Finished message. The Finished message is the first one
 * protected with the just negotiated algorithms, keys, and secrets. The value
 * handshake_messages includes all handshake messages starting at {@link ClientHello} up
 * to, but not including, this {@link Finished} message.
 * 
 * @author Stefan Jucker
 * 
 */
@SuppressWarnings("deprecation")
public class Finished extends HandshakeMessage {

	// Logging ///////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(Finished.class.getName());

	// DTLS-specific constants ///////////////////////////////////////////

	private final static String FINISH_LABEL_CLIENT = "client finished";

	private final static String FINISH_LABEL_SERVER = "server finished";

	private final static int VERIFY_DATA_LENGTH = 12; // in bytes

	// Members ///////////////////////////////////////////////////////////
	
	private byte[] verifyData;

	// Constructors /////////////////////////////////////////////////////

	public Finished(SecretKey masterSecret, boolean isClient, byte[] handshakeHash) {
		verifyData = getVerifyData(masterSecret, isClient, handshakeHash);
	}

	public Finished(byte[] verifyData) {
		this.verifyData = verifyData;
	}

	public boolean verifyData(SecretKey masterSecret, boolean isClient, byte[] handshakeHash) {
		byte[] myVerifyData = getVerifyData(masterSecret, isClient, handshakeHash);

		return Arrays.equals(myVerifyData, verifyData);
	}

	private byte[] getVerifyData(SecretKey masterSecret, boolean isClient, byte[] handshakeHash) {
		byte[] data = null;

		String label = (isClient) ? FINISH_LABEL_CLIENT : FINISH_LABEL_SERVER;

		try {
			TlsPrfParameterSpec spec = new TlsPrfParameterSpec(masterSecret, label, handshakeHash, VERIFY_DATA_LENGTH, "SHA-256", 32, 64);
			KeyGenerator prf = KeyGenerator.getInstance("SunTlsPrf");

			prf.init(spec);

			SecretKey prfKey = prf.generateKey();
			data = prfKey.getEncoded();

		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return data;
	}

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		writer.writeBytes(verifyData);

		return writer.toByteArray();
	}
	
	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		
		byte[] verifyData = reader.readBytesLeft();
		
		return new Finished(verifyData);
	}

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.FINISHED;
	}

	@Override
	public int getMessageLength() {
		return verifyData.length;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\t\t" + Arrays.toString(verifyData) + "\n");
		
		return sb.toString();
	}

}
