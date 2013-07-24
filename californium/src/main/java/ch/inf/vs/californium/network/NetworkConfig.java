package ch.inf.vs.californium.network;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.inf.vs.californium.network.connector.Connector;
import ch.inf.vs.californium.network.layer.CoapStack;

/**
 * The NetworkConfig defines the configuration of a stack. Each {@link Endpoint}
 * uses such a configuration to define the behavior of its {@link CoapStack} and
 * {@link Connector}. Some variables are predefined. Arbitrary variables can be
 * added as String key-value pair with
 * 
 * <pre>
 * {@code
 *  coinfig.setArbitrary("ARBITRARY_KEY", "ARBITRARY_VALUE");
 * }
 * </pre>
 * <p>
 * NetworkConfig uses the class {@link NetworkConfigIO} to store and load its
 * values to a file.
 * <p>
 * Not all details are completely clear yet (TODO).
 */
public class NetworkConfig {
	
	// TODO: Need to be observable. For instance to change mark_and_sweep and
	// instantly reschedule timer.
	
	private int ack_timeout = 2000;
	private float ack_random_factor = 1.5f;
	private int ack_timeout_scale = 2;
	private int max_retransmit = 4;
	private int nstart = 1;
	private int default_leisure = 5000;
	private float probing_rate = 1f;
	
	private int max_message_size = 64; // if larger, use blockwise
	private int default_block_size = 64; // one of 2^{4,5,6,7,8,9,10}
	
	private long notification_max_age = 128 * 1000; // ms
	
	private long mark_and_sweep_interval = 6*1000; // ms
	
	private long exchange_lifecycle = 50*1000;

	private int receive_buffer = 0; // default, TODO: update if changed
	private int send_buffer = 0; // default, TODO: update if changed
	
	/** The map of arbitrary String key-value pairs */
	private final Map<String, String> arbitrary = new ConcurrentHashMap<>();
	
	/**
	 * Constructs a new NetworkConfiguration with the default values.
	 */
	public NetworkConfig() { }
	
	/**
	 * Constructs a new NetworkConfiguration and loads the values from the
	 * specified config file.
	 *
	 * @param configFile the config file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public NetworkConfig(File configFile) throws IOException {
		load(configFile);
	}
	

	/**
	 * Loads the values of this configuration form the specified file.
	 * 
	 * @param configFile
	 *            the config file
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void load(File configFile) throws IOException {
		new NetworkConfigIO().load(configFile, this);
	}
	
	/**
	 * Stores the configuration the the specified file.
	 * 
	 * @param configFile
	 *            the config file
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void store(File configFile) throws IOException {
		new NetworkConfigIO().store(configFile, this);
	}
	
	/**
	 * Gets the String value for the specified key or null if not found.
	 * 
	 * @param key the key
	 * @return the value or null if not found
	 * @throws NullPointerException if the key is null
	 */
	public String getArbitrary(String key) {
		if (key == null) throw new NullPointerException();
		return arbitrary.get(key);
	}

	/**
	 * Gets the map of arbitrary key-value pairs.
	 *
	 * @return the map of arbitrary key-value pairs
	 */
	public Map<String, String> getArbitrary() {
		return arbitrary;
	}
	
	/**
	 * Associates the specified value with the specified key in this
	 * configuration. If the configuration previously contained a mapping for
	 * the key, the old value is replaced by the specified value.
	 * 
	 * @param key
	 *            the key with which the specified value is be associated
	 * @param value
	 *            the value to be associated with the specified key
	 * @return the previous value associated with <tt>key</tt> or <tt>null</tt>
	 *         if there was no mapping for <tt>key</tt>.
	 * @throws NullPointerException
	 *             if either the key or the value is null
	 */
	public String setArbitrary(String key, String value) {
		if (key == null) throw new NullPointerException();
		if (value == null) throw new NullPointerException();
		return arbitrary.put(key, value);
	}
	
	//////////////////////// Getter and Setter ////////////////////////
	
	public int getAckTimeout() {
		return ack_timeout;
	}
	
	public void setAckTimeout(int ack_timeout) {
		this.ack_timeout = ack_timeout;
	}
	
	public float getAckRandomFactor() {
		return ack_random_factor;
	}
	
	public void setAckRandomFactor(float ack_random_factor) {
		this.ack_random_factor = ack_random_factor;
	}

	public int getAckTimeoutScale() {
		return ack_timeout_scale;
	}

	public void setAckTimeoutScale(int ack_timeout_scale) {
		this.ack_timeout_scale = ack_timeout_scale;
	}
	
	public int getMaxRetransmit() {
		return max_retransmit;
	}
	
	public void setMaxRetransmit(int max_retransmit) {
		this.max_retransmit = max_retransmit;
	}
	
	public int getNStart() {
		return nstart;
	}
	
	public void setNStart(int nstart) {
		this.nstart = nstart;
	}
	
	public int getDefaultLeisure() {
		return default_leisure;
	}
	
	public void setDefaultLeisure(int default_leisure) {
		this.default_leisure = default_leisure;
	}
	
	public float getProbingRate() {
		return probing_rate;
	}
	
	public void setProbingRate(float probing_rate) {
		this.probing_rate = probing_rate;
	}

	public int getMaxMessageSize() {
		return max_message_size;
	}

	public void setMaxMessageSize(int max_message_size) {
		this.max_message_size = max_message_size;
	}

	public int getDefaultBlockSize() {
		return default_block_size;
	}

	public void setDefaultBlockSize(int default_block_size) {
		this.default_block_size = default_block_size;
	}

	public long getMarkAndSweepInterval() {
		return mark_and_sweep_interval;
	}

	public void setMarkAndSweepInterval(long mark_and_sweep_interval) {
		this.mark_and_sweep_interval = mark_and_sweep_interval;
	}
	
	public long getExchangeLifecycle() {
		return exchange_lifecycle; // ms // TODO: compute
	}
	
	public void setExchangeLifecycle(long exchangeLifecycle) {
		this.exchange_lifecycle = exchangeLifecycle;
	}

	public long getNotificationMaxAge() {
		return notification_max_age;
	}

	public void setNotificationMaxAge(long notification_max_age) {
		this.notification_max_age = notification_max_age;
	}

	public int getReceiveBuffer() {
		return receive_buffer;
	}

	public void setReceiveBuffer(int receiveBuffer) {
		this.receive_buffer = receiveBuffer;
	}

	public int getSendBuffer() {
		return send_buffer;
	}

	public void setSendBuffer(int sendBuffer) {
		this.send_buffer = sendBuffer;
	}
}
