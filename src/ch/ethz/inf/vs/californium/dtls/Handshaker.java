package ch.ethz.inf.vs.californium.dtls;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import sun.security.internal.spec.TlsKeyMaterialParameterSpec;
import sun.security.internal.spec.TlsKeyMaterialSpec;
import sun.security.internal.spec.TlsMasterSecretParameterSpec;
import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.dtls.CipherSuite.KeyExchangeAlgorithm;

@SuppressWarnings("deprecation")
public abstract class Handshaker {

	protected static final Logger LOG = Logger.getLogger(Handshaker.class.getName());

	protected boolean isClient;

	protected int state = -1;

	protected DatagramSocket socket;
	protected EndpointAddress endpointAddress;

	protected ProtocolVersion usedProtocol;
	protected SessionId sessionId;
	protected Random clientRandom;
	protected Random serverRandom;
	protected CipherSuite cipherSuite;
	protected CompressionMethod compressionMethod;

	protected KeyExchangeAlgorithm keyExchange;

	private SecretKey masterSecret;

	private SecretKey clientWriteMACKey;
	private SecretKey serverWriteMACKey;

	private IvParameterSpec clientWriteIV;
	private IvParameterSpec serverWriteIV;

	private SecretKey clientWriteKey;
	private SecretKey serverWriteKey;

	protected DTLSSession session = null;

	private int sequenceNumber = 0;

	public BlockingQueue<DTLSSession> queue = new ArrayBlockingQueue<DTLSSession>(1);

	public Handshaker() {

	}

	public Handshaker(DatagramSocket socket, EndpointAddress endpointAddress, boolean isClient) {
		this.socket = socket;
		this.endpointAddress = endpointAddress;
		this.isClient = isClient;
	}

	/**
	 * Processes the handshake message according to its {@link HandshakeType}
	 * and reacts according to the protocol specification.
	 * 
	 * @param message the received {@link HandshakeMessage}.
	 * @throws IOException
	 */
	public abstract void processMessage(HandshakeMessage message) throws IOException;

	public abstract void startHandshake() throws IOException;

	public abstract HandshakeMessage getStartHandshakeMessage();

	public abstract void sendHandshakeMessage(HandshakeMessage message) throws IOException;

	public CipherSuite getCipherSuite() {
		return cipherSuite;
	}

	public void setCipherSuite(CipherSuite cipherSuite) {
		this.cipherSuite = cipherSuite;
		this.keyExchange = cipherSuite.getKeyExchange();
	}

	protected void generateKeys(SecretKey premasterSecret) {
		SecretKey masterSecret = generateMasterSecretKey(premasterSecret);

		try {
			int majorVersion = 3;
			int minorVersion = 2;
			// TODO get this from cipher suite
			TlsKeyMaterialParameterSpec keyMaterialParameterSpec = new TlsKeyMaterialParameterSpec(masterSecret, majorVersion, minorVersion, clientRandom.getRandomBytes(), serverRandom.getRandomBytes(), cipherSuite.getBulkCipher().toString(), 16, 8,
					4, 8, "SHA-256", 32, 64);
			KeyGenerator kg = KeyGenerator.getInstance("SunTlsKeyMaterial");
			kg.init(keyMaterialParameterSpec);
			TlsKeyMaterialSpec keySpec = (TlsKeyMaterialSpec) kg.generateKey();

			clientWriteKey = keySpec.getClientCipherKey();
			serverWriteKey = keySpec.getServerCipherKey();

			clientWriteIV = keySpec.getClientIv();
			serverWriteIV = keySpec.getServerIv();

			clientWriteMACKey = keySpec.getClientMacKey();
			serverWriteMACKey = keySpec.getServerMacKey();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private SecretKey generateMasterSecretKey(SecretKey premasterSecret) {
		try {
			KeyGenerator generator = KeyGenerator.getInstance("SunTlsMasterSecret");

			int majorVersion = 3;
			int minorVersion = 2;
			// TODO get this from cipher suite
			TlsMasterSecretParameterSpec spec = new TlsMasterSecretParameterSpec(premasterSecret, majorVersion, minorVersion, clientRandom.getRandomBytes(), serverRandom.getRandomBytes(), "SHA-256", 32, 64);
			generator.init(spec);
			masterSecret = generator.generateKey();

			return masterSecret;
		} catch (Exception e) {
			return null;
		}
	}

	public SecretKey getMasterSecret() {
		return masterSecret;
	}

	public SecretKey getClientWriteMACKey() {
		return clientWriteMACKey;
	}

	public SecretKey getServerWriteMACKey() {
		return serverWriteMACKey;
	}

	public IvParameterSpec getClientWriteIV() {
		return clientWriteIV;
	}

	public IvParameterSpec getServerWriteIV() {
		return serverWriteIV;
	}

	public SecretKey getClientWriteKey() {
		return clientWriteKey;
	}

	public SecretKey getServerWriteKey() {
		return serverWriteKey;
	}

	public DTLSSession getSession() {
		return session;
	}

	public void setSession(DTLSSession session) {
		this.session = session;
	}

	public void setSequenceNumber(HandshakeMessage message) {
		message.setMessageSeq(sequenceNumber);
		sequenceNumber++;
	}
}
