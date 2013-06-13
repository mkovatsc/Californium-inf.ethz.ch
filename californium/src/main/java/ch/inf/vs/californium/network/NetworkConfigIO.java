package ch.inf.vs.californium.network;

import static ch.inf.vs.californium.network.NetworkConfigIO.Prop.ACK_RANDOM_FACTOR;
import static ch.inf.vs.californium.network.NetworkConfigIO.Prop.ACK_TIMEOUT;
import static ch.inf.vs.californium.network.NetworkConfigIO.Prop.DEFAULT_BLOCK_SIZE;
import static ch.inf.vs.californium.network.NetworkConfigIO.Prop.DEFAULT_LEISURE;
import static ch.inf.vs.californium.network.NetworkConfigIO.Prop.MARK_AND_SWEEP_INTERVAL;
import static ch.inf.vs.californium.network.NetworkConfigIO.Prop.MAX_MESSAGE_SIZE;
import static ch.inf.vs.californium.network.NetworkConfigIO.Prop.MAX_RETRANSMIT;
import static ch.inf.vs.californium.network.NetworkConfigIO.Prop.NOTIFICATION_MAX_AGE;
import static ch.inf.vs.californium.network.NetworkConfigIO.Prop.NSTART;
import static ch.inf.vs.californium.network.NetworkConfigIO.Prop.PROBING_RATE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import ch.inf.vs.californium.resources.CalifonriumLogger;

public class NetworkConfigIO {

	private final static Logger LOGGER = CalifonriumLogger.getLogger(NetworkConfigIO.class);
	
	public enum Prop {
		ACK_TIMEOUT,
		ACK_RANDOM_FACTOR,
		ACK_TIMEOUT_SCALE,
		MAX_RETRANSMIT,
		NSTART,
		DEFAULT_LEISURE,
		PROBING_RATE,
		MAX_MESSAGE_SIZE,
		DEFAULT_BLOCK_SIZE,
		NOTIFICATION_MAX_AGE,
		MARK_AND_SWEEP_INTERVAL
	}
	
	private Properties properties;
	
	public NetworkConfigIO() {
		this.properties = new Properties();
	}
	
	public void load(File configFile, NetworkConfig config) throws IOException {
		properties.load(new FileInputStream(configFile));
		for (Entry<?, ?> entry:properties.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			Prop prop = null;
			try {
				prop = Prop.valueOf(key);
			} catch (IllegalArgumentException e) {
				// key is not a known property. suppress e
				LOGGER.info("Load unknown property "+key+" with value "+value);
				config.setArbitrary(key, value);
			}
			if (prop != null) {
				switch (prop) {
				case ACK_TIMEOUT:             config.setAckTimeout(Integer.parseInt(value)); break;
				case ACK_RANDOM_FACTOR:       config.setAckRandomFactor(Float.parseFloat(value)); break;
				case MAX_RETRANSMIT:          config.setMaxRetransmit(Integer.parseInt(value)); break;
				case NSTART:                  config.setNStart(Integer.parseInt(value)); break;
				case DEFAULT_LEISURE:         config.setDefaultLeisure(Integer.parseInt(value)); break;
				case PROBING_RATE:            config.setProbingRate(Float.parseFloat(value)); break;
				case MAX_MESSAGE_SIZE:        config.setMaxMessageSize(Integer.parseInt(value)); break;
				case DEFAULT_BLOCK_SIZE:      config.setDefaultBlockSize(Integer.parseInt(value)); break;
				case NOTIFICATION_MAX_AGE:    config.setNotificationMaxAge(Long.parseLong(value)); break;
				case MARK_AND_SWEEP_INTERVAL: config.setMarkAndSweepInterval(Long.parseLong(value)); break;
				default: LOGGER.warning("NetworkConfigIO.load() does not know how to process property "+prop);
				}
			}
		}
	}
	
	public void store(File configFile, NetworkConfig config) throws IOException {
		Map<String, String> arbitrary = config.getArbitrary();
		properties.putAll(arbitrary);
		
		setProperty(ACK_TIMEOUT,             config.getAckTimeout());
		setProperty(ACK_RANDOM_FACTOR,       config.getAckRandomFactor());
		setProperty(MAX_RETRANSMIT,          config.getMaxRetransmit());
		setProperty(NSTART,                  config.getNStart());
		setProperty(DEFAULT_LEISURE,         config.getDefaultLeisure());
		setProperty(PROBING_RATE,            config.getProbingRate());
		setProperty(MAX_MESSAGE_SIZE,        config.getMaxMessageSize());
		setProperty(DEFAULT_BLOCK_SIZE,      config.getDefaultBlockSize());
		setProperty(NOTIFICATION_MAX_AGE,    config.getNotificationMaxAge());
		setProperty(MARK_AND_SWEEP_INTERVAL, config.getMarkAndSweepInterval());
		
		int expected = arbitrary.size() + Prop.values().length;
		int actual = properties.size();
		if (expected != actual) {
			LOGGER.warning("NetworkConfigIO.store() expected to store "+expected+" properties but endet up with only "+actual);
		}
		
		properties.store(new FileWriter(configFile), "Properties of Californium");
	}
	
	private void setProperty(Prop prop, Object value) {
		properties.setProperty(String.valueOf(prop), value.toString());
	}
	
}
