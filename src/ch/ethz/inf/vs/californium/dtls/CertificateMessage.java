package ch.ethz.inf.vs.californium.dtls;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * The server MUST send a Certificate message whenever the agreed-upon key
 * exchange method uses certificates for authentication. This message will
 * always immediately follow the {@link ServerHello} message.
 * 
 * @author Stefan Jucker
 * 
 */
public class CertificateMessage extends HandshakeMessage {

	// Logging ///////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(CertificateMessage.class.getName());

	// DTLS-specific constants ///////////////////////////////////////////

	private static final int CERTIFICATE_LENGTH_BITS = 24;

	private static final int CERTIFICATE_LIST_LENGTH = 24;

	// Members ///////////////////////////////////////////////////////////

	/**
	 * This is a sequence (chain) of certificates. The sender's certificate MUST
	 * come first in the list.
	 */
	private X509Certificate[] certificateChain;
	
	/** The encoded chain of certificates */
	private List<byte[]> encodedChain;
	
	/** The total length of the {@link CertificateMessage}.  */
	private int messageLength;

	public CertificateMessage(X509Certificate[] certificates) {
		this.certificateChain = certificates;
	}

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		// the size of the certificate chain
		writer.write(getMessageLength() - 3, CERTIFICATE_LIST_LENGTH);
		for (byte[] encoded : encodedChain) {
			// the size of the current certificate
			writer.write(encoded.length, CERTIFICATE_LENGTH_BITS);
			// the encoded current certificate
			writer.writeBytes(encoded);
		}

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		int certificateChainLength = reader.read(CERTIFICATE_LENGTH_BITS);

		List<Certificate> certs = new ArrayList<Certificate>();

		CertificateFactory certificateFactory = null;
		while (certificateChainLength > 0) {
			int certificateLength = reader.read(CERTIFICATE_LENGTH_BITS);
			byte[] certificate = reader.readBytes(certificateLength);

			// the size of the length and the actual length of the encoded
			// certificate
			certificateChainLength -= 3 + certificateLength;

			try {
				if (certificateFactory == null) {
					certificateFactory = CertificateFactory.getInstance("X.509");
				}
				Certificate cert = certificateFactory.generateCertificate(new ByteArrayInputStream(certificate));
				certs.add(cert);
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return new CertificateMessage(certs.toArray(new X509Certificate[certs.size()]));
	}

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.CERTIFICATE;
	}

	@Override
	public int getMessageLength() {
		// the certificate chain length uses 3 bytes
		// each certificate's length in the chain also uses 3 bytes
		if (encodedChain == null) {
			messageLength = 3;
			encodedChain = new ArrayList<byte[]>(certificateChain.length);
			for (X509Certificate cert : certificateChain) {
				try {
					byte[] encoded = cert.getEncoded();
					encodedChain.add(encoded);

					// the length of the encoded certificate plus 3 bytes for
					// the length
					messageLength += encoded.length + 3;
				} catch (CertificateEncodingException e) {
					encodedChain = null;
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return messageLength;
	}

	public X509Certificate[] getCertificateChain() {
		return certificateChain;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\t\tCertificates Length: " + (getMessageLength() - 3) + "\n");
		int index = 0;
		for (X509Certificate cert : certificateChain) {
			sb.append("\t\t\tCertificate Length: " + encodedChain.get(index).length + "\n");
			sb.append("\t\t\tCertificate: " + cert.toString() + "\n");
			
			index++;
		}

		return sb.toString();
	}

}
