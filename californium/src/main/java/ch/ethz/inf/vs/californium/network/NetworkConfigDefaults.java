package ch.ethz.inf.vs.californium.network;

import ch.ethz.inf.vs.californium.network.connector.UDPConnector;

public class NetworkConfigDefaults {

	public static final String ACK_TIMEOUT = "ACK_TIMEOUT";
	public static final String ACK_RANDOM_FACTOR = "ACK_RANDOM_FACTOR";
	public static final String ACK_TIMEOUT_SCALE = "ACK_TIMEOUT_SCALE";
	public static final String NSTART = "NSTART";
	public static final String DEFAULT_LEISURE = "DEFAULT_LEISURE";
	public static final String PROBING_RATE = "PROBING_RATE";
	public static final String MAX_MESSAGE_SIZE = "MAX_MESSAGE_SIZE";
	public static final String DEFAULT_BLOCK_SIZE = "DEFAULT_BLOCK_SIZE";
	public static final String NOTIFICATION_MAX_AGE = "NOTIFICATION_MAX_AGE";
	public static final String ENABLE_DOUBLICATION = "ENABLE_DOUBLICATION";
	public static final String MARK_AND_SWEEP_INTERVAL = "MARK_AND_SWEEP_INTERVAL";
	public static final String EXCHANGE_LIFECYCLE = "EXCHANGE_LIFECYCLE";
	public static final String MAX_RETRANSMIT = "MAX_RETRANSMIT";
	public static final String DEFAULT_ENDPOINT_THREAD_COUNT = "DEFAULT_ENDPOINT_THREAD_COUNT";
	
	public static final String UDP_CONNECTOR_RECEIVE_BUFFER = "UDP_CONNECTOR_RECEIVE_BUFFER";
	public static final String UDP_CONNECTOR_SEND_BUFFER = "UDP_CONNECTOR_SEND_BUFFER";
	public static final String UDP_CONNECTOR_RECEIVER_THREAD_COUNT = "UDP_CONNECTOR_RECEIVER_THREAD_COUNT";
	public static final String UDP_CONNECTOR_SENDER_THREAD_COUNT = "UDP_CONNECTOR_SENDER_THREAD_COUNT";
	public static final String UDP_CONNECTOR_DATAGRAM_SIZE = "UDP_CONNECTOR_DATAGRAM_SIZE";
	public static final String UDP_CONNECTOR_OUT_CAPACITY = "UDP_CONNECTOR_OUT_CAPACITY";
	
	public static final String HTTP_PORT = "HTTP_PORT";
	public static final String HTTP_SERVER_SOCKET_TIMEOUT = "HTTP_SERVER_SOCKET_TIMEOUT";
	public static final String HTTP_SERVER_SOCKET_BUFFER_SIZE = "HTTP_SERVER_SOCKET_BUFFER_SIZE";
	public static final String CACHE_RESPONSE_MAX_AGE = "CACHE_RESPONSE_MAX_AGE";
	public static final String CACHE_SIZE = "CACHE_SIZE";

	public static void setDefaults(NetworkConfig config) {
		config.setInt(ACK_TIMEOUT, 2000);
		config.setFloat(ACK_RANDOM_FACTOR, 1.5f);
		config.setInt(ACK_TIMEOUT_SCALE, 2);
		config.setInt(NSTART, 1);
		config.setInt(DEFAULT_LEISURE, 5000);
		config.setFloat(PROBING_RATE, 1f);
		config.setInt(MAX_RETRANSMIT, 4);
		config.setLong(EXCHANGE_LIFECYCLE, 247 * 1000); // ms
		
		config.setInt(MAX_MESSAGE_SIZE, 1024);
		config.setInt(DEFAULT_BLOCK_SIZE, 512);
		
		config.setLong(NOTIFICATION_MAX_AGE, 128 * 1000); // ms
		config.setBoolean(ENABLE_DOUBLICATION, true);
		config.setLong(MARK_AND_SWEEP_INTERVAL, 10 * 1000);
		config.setInt(DEFAULT_ENDPOINT_THREAD_COUNT, 1);
		
		config.setInt(UDP_CONNECTOR_RECEIVE_BUFFER, UDPConnector.UNDEFINED);
		config.setInt(UDP_CONNECTOR_SEND_BUFFER, UDPConnector.UNDEFINED);
		config.setInt(UDP_CONNECTOR_RECEIVER_THREAD_COUNT, 1);
		config.setInt(UDP_CONNECTOR_SENDER_THREAD_COUNT, 1);
		config.setInt(UDP_CONNECTOR_DATAGRAM_SIZE, 2000);
		config.setInt(UDP_CONNECTOR_OUT_CAPACITY, Integer.MAX_VALUE); // unbounded
		
		config.setInt(HTTP_PORT, 8080);
		config.setInt(HTTP_SERVER_SOCKET_TIMEOUT, 100000);
		config.setInt(HTTP_SERVER_SOCKET_BUFFER_SIZE, 8192);
		config.setInt(CACHE_RESPONSE_MAX_AGE, 86400);
		config.setInt(CACHE_SIZE, 32);
	}
	
	// prevent instantiation
	private NetworkConfigDefaults() { }
}
