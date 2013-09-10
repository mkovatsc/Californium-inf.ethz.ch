package ch.ethz.inf.vs.californium.network.dedupl;

import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;

public class DeduplicatorFactory {
	
	private static final Logger LOGGER = CalifonriumLogger.getLogger(DeduplicatorFactory.class);

	private static DeduplicatorFactory factory;
	
	public static DeduplicatorFactory getDeduplicatorFactory() {
		if (factory == null)
			factory = new DeduplicatorFactory();
		return factory;
	}
	
	public static void setDeduplicatorFactory(DeduplicatorFactory factory) {
		DeduplicatorFactory.factory = factory;
	}
	
	public Deduplicator createDeduplicator(NetworkConfig config) {
		String type = config.getString(NetworkConfigDefaults.DEDUPLICATOR);
		if (NetworkConfigDefaults.DEDUPLICATOR_MARK_AND_SWEEP.equals(type))
			return new MarkAndSweep(config);
		else if (NetworkConfigDefaults.DEDUPLICATOR_CROP_ROTATION.equals(type))
			return new CropRotation(config);
		else if (NetworkConfigDefaults.NO_DEDUPLICATOR.equals(type))
			return new NoDeduplicator();
		else {
			LOGGER.warning("Unknown deduplicator type: "+type);
			return new NoDeduplicator();
		}
	}
	
}
