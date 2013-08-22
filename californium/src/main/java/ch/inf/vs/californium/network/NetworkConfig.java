package ch.inf.vs.californium.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.CalifonriumLogger;


public class NetworkConfig {

	private static final Logger LOGGER = CalifonriumLogger.getLogger(NetworkConfig.class);
	
	public static final String DEFAULT = "Californium.properties";
	public static final String DEFAULT_HEADER = "Californium CoAP Properties file";
	
	private static NetworkConfig standard;
	
	/**
	 * Gives access to the standard network configuration. When a new endpoint
	 * or server is created without a specific network configuration, it will
	 * use this standard configuration.
	 * 
	 * @return the standard configuration
	 */
	public static NetworkConfig getStandard() {
		if (standard == null) {
			synchronized (NetworkConfig.class) {
				if (standard == null)
					createStandardWithFile(new File(DEFAULT));
			}
		}
		return standard;
	}
	
	public static void setStandard(NetworkConfig standard) {
		NetworkConfig.standard = standard;
	}
	
	public static NetworkConfig createStandardWithoutFile() {
		LOGGER.info("Create standard properties without a file");
		return standard = new NetworkConfig();
	}
	
	public static NetworkConfig createStandardWithFile(File file) {
		LOGGER.info("Create standard properties with file "+file);
		standard = new NetworkConfig();
		if (file.exists()) {
			try {
				standard.load(file);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Error while loading properties from "+file.getAbsolutePath(), e);
			}
		} else {
			try {
				standard.store(file);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Error while storing properties to "+file.getAbsolutePath(), e);
			}
		}
		return standard;
	}
	
	private Properties properties;
	
	public NetworkConfig() {
		this.properties = new Properties();
		NetworkConfigDefaults.setDefaults(this);
	}
	
//	public NetworkConfig(NetworkConfig defaults) {
//		this(defaults.properties);
//	}
//	
//	public NetworkConfig(Properties defaults) {
//		this.properties = new Properties(defaults);
//	}
	
	public void load(File file) throws IOException {
		InputStream inStream = new FileInputStream(file);
		properties.load(inStream);
	}
	
	public void store(File file) throws IOException {
		store(file, DEFAULT_HEADER);
	}
	
	public void store(File file, String header) throws IOException {
		if (file == null)
			throw new NullPointerException();
		properties.store(new FileWriter(file), header);
	}
	
	public String getString(String key) {
		return properties.getProperty(key);
	}
	
	public int getInt(String key) {
		String value = properties.getProperty(key);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				LOGGER.log(Level.WARNING, "Could not convert property \"" + key + "\" with value \"" + value + "\" to integer", e);
			}
		} else {
			LOGGER.warning("Property \"" + key + "is undefined");
		}
		return 0;
	}
	
	public long getLong(String key) {
		String value = properties.getProperty(key);
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			LOGGER.log(Level.WARNING, "Could not convert property \"" + key + "\" with value \"" + value + "\" to long", e);
			return 0;
		}
	}
	
	public float getFloat(String key) {
		String value = properties.getProperty(key);
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			LOGGER.log(Level.WARNING, "Could not convert property \"" + key + "\" with value \"" + value + "\" to float", e);
			return 0;
		}
	}
	
	public double getDouble(String key) {
		String value = properties.getProperty(key);
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			LOGGER.log(Level.WARNING, "Could not convert property \"" + key + "\" with value \"" + value + "\" to double", e);
			return 0;
		}
	}
	
	public boolean getBoolean(String key) {
		String value = properties.getProperty(key);
		try {
			return Boolean.parseBoolean(value);
		} catch (NumberFormatException e) {
			LOGGER.log(Level.WARNING, "Could not convert property \"" + key + "\" with value \"" + value + "\" to boolean", e);
			return false;
		}
	}
	
	public NetworkConfig set(String key, Object value) {
		properties.put(key, String.valueOf(value));
		return this;
	}
	
	public NetworkConfig setString(String key, String value) {
		properties.put(key, String.valueOf(value));
		return this;
	}
	
	public NetworkConfig setInt(String key, int value) {
		properties.put(key, String.valueOf(value));
		return this;
	}
	
	public NetworkConfig setLong(String key, long value) {
		properties.put(key, String.valueOf(value));
		return this;
	}
	
	public NetworkConfig setFloat(String key, float value) {
		properties.put(key, String.valueOf(value));
		return this;
	}
	
	public NetworkConfig setDouble(String key, double value) {
		properties.put(key, String.valueOf(value));
		return this;
	}

	public NetworkConfig setBoolean(String key, boolean value) {
		properties.put(key, String.valueOf(value));
		return this;
	}
}
