package ch.ethz.inf.vs.californium.dtls;

/**
 * The Class ProtocolVersion.
 */
public class ProtocolVersion implements Comparable<ProtocolVersion> {
	
	/** The minor. */
	private int minor;
	
	/** The major. */
	private int major;
	
	/**
	 * The latest version supported.
	 */
	public ProtocolVersion() {
		this.major = 254;
		this.minor = 253;
	}

	/**
	 * Instantiates a new protocol version.
	 *
	
	 * @param major the major
	 * @param minor the minor
	 */
	public ProtocolVersion(int major, int minor) {
		this.minor = minor;
		this.major = major;
	}
	
	/**
	 * Gets the minor.
	 *
	 * @return the minor
	 */
	public int getMinor() {
		return minor;
	}

	/**
	 * Gets the major.
	 *
	 * @return the major
	 */
	public int getMajor() {
		return major;
	}

	@Override
	public int compareTo(ProtocolVersion o) {
		/*
		 * Example, version 1.0 (254,255) is smaller than version 1.2 (254,253)
		 */
		
		if (major == o.getMajor()) {
			if (minor < o.getMajor()) {
				return 1;
			} else if (minor > o.getMajor()) {
				return -1;
			} else {
				return 0;
			}
		} else if (major < o.getMajor()) {
			return 1;
		} else {
			return -1;
		}
	}
	
	
}