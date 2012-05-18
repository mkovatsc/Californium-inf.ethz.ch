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
		// TODO Auto-generated method stub
		return 0;
	}
	
	
}