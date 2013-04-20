package ch.inf.vs.californium.coap;

public class Option {

	/*
	 * The old version of Cf abreviates ints as much as possible. This is a bad
	 * idea because it assumes that leading zeros can be eliminated, though this
	 * might not be expected.
	 */
	
	private int number;
	
	private byte[] value;
	
	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public byte[] getValue() {
		return value;
	}

	public void setValue(byte[] value) {
		this.value = value;
	}
	
	// TODO Option implementation
	
	// TODO: Should we move these methods to OptionSet?
	public static Option newIfMatch(byte[] str) {
		throw new RuntimeException("Not yet implemented");
	}
	
}
