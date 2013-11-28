
package ch.ethz.inf.vs.californium.network.dedupl;

import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.network.Matcher;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;

/**
 * The deduplication factory creates the deduplicator for a {@link Matcher}. If
 * a server wants to use another deduplicator than the three standard
 * deduplicators, it can create its own factory and install it with
 * {@link #setDeduplicatorFactory(DeduplicatorFactory)}.
 */
public class DeduplicatorFactory {

	/** The logger. */
	private static final Logger LOGGER = Logger.getLogger(DeduplicatorFactory.class.getCanonicalName());

	/** The factory. */
	private static DeduplicatorFactory factory;

	/**
	 * Returns the installed deduplicator factory.
	 * @return the deduplicator factory
	 */
	public static DeduplicatorFactory getDeduplicatorFactory() {
		if (factory == null) factory = new DeduplicatorFactory();
		return factory;
	}

	/**
	 * Installs the specified deduplicator factory.
	 * @param factory the factory
	 */
	public static void setDeduplicatorFactory(DeduplicatorFactory factory) {
		DeduplicatorFactory.factory = factory;
	}

	/**
	 * Creates a new deduplicator according to the specified configuration.
	 * @param config the configuration
	 * @return the deduplicator
	 */
	public Deduplicator createDeduplicator(NetworkConfig config) {
		String type = config.getString(NetworkConfigDefaults.DEDUPLICATOR);
		if (NetworkConfigDefaults.DEDUPLICATOR_MARK_AND_SWEEP.equals(type)) return new SweepDeduplicator(config);
		else if (NetworkConfigDefaults.DEDUPLICATOR_CROP_ROTATION.equals(type)) return new CropRotation(config);
		else if (NetworkConfigDefaults.NO_DEDUPLICATOR.equals(type)) return new NoDeduplicator();
		else {
			LOGGER.warning("Unknown deduplicator type: " + type);
			return new NoDeduplicator();
		}
	}

}
