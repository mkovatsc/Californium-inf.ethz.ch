package ch.inf.vs.californium.network;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkConfig {
	
	private int ack_timeout = 2000;
	private float ack_random_factor = 1.5f;
	private int ack_timeout_scale = 2;
	private int max_retransmit = 4;
	private int nstart = 1;
	private int default_leisure = 5000;
	private float probing_rate = 1f;
	
	private int max_message_size = 1024; // if larger, use blockwise
	private int default_block_size = 512; // one of 2^{4,5,6,7,8,9,10}
	
	private long notification_max_age = 128 * 1000; // ms
	
	private long mark_and_sweep_interval = 6*1000; // ms
	
	private final Map<String, String> arbitrary = new ConcurrentHashMap<>();
	
	public NetworkConfig() { }
	
	public NetworkConfig(File configFile) throws IOException {
		load(configFile);
	}
	
	public void load(File configFile) throws IOException {
		new NetworkConfigIO().load(configFile, this);
	}
	
	public void store(File configFile) throws IOException {
		new NetworkConfigIO().store(configFile, this);
	}
	
	public String getArbitrary(String key) {
		if (key == null) throw new NullPointerException();
		return arbitrary.get(key);
	}

	public Map<String, String> getArbitrary() {
		return arbitrary;
	}
	
	public String setArbitrary(String key, String value) {
		if (key == null) throw new NullPointerException();
		if (value == null) throw new NullPointerException();
		return arbitrary.put(key, value);
	}
	
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
		return 10000; // ms // TODO: compute
	}

	public long getNotificationMaxAge() {
		return notification_max_age;
	}

	public void setNotificationMaxAge(long notification_max_age) {
		this.notification_max_age = notification_max_age;
	}
}
