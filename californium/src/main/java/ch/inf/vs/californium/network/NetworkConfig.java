package ch.inf.vs.californium.network;

public class NetworkConfig {

	private int ack_timeout = 2000;
	private float ack_random_factor = 1.5f;
	private int max_retransmit = 4;
	private int nstart = 1;
	private int default_leisure = 5000;
	private float probing_rate = 1f;
	
	private int max_message_size = 1024; // if larger, use blockwise
	private int default_block_size = 512; // one of 2^{4,5,6,7,8,9,10}
	
	private long mark_and_sweep_interval = 6*1000; // ms
	
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
	
	public int getMaxRetransmit() {
		return max_retransmit;
	}
	
	public void setMaxRetransmit(int max_retransmit) {
		this.max_retransmit = max_retransmit;
	}
	
	public int getNstart() {
		return nstart;
	}
	
	public void setNstart(int nstart) {
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
}
