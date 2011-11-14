package coap;

public class BlockOption extends Option {

	private static int encode(int num, int szx, boolean m) {
		int value = 0;
		
		value |= (szx & 0x7)     ;
		value |= (m ? 1 : 0) << 3;
		value |= num         << 4;
		
		return value;
	}	

	public BlockOption(int nr) {
		super(0, nr);
	}
	
	public BlockOption(int nr, int num, int szx, boolean m) {
		super(encode(num, szx, m), nr);
	}

	public void setValue(int num, int szx, boolean m) {
		setIntValue(encode(num, szx, m));
	}
	
	public int getNUM() {
		return getIntValue() >> 4;
	}
	public void setNUM(int num) {
		setValue(num, getSZX(), getM());
	}

	public int getSZX() {
		return getIntValue() & 0x7;
	}
	public void setSZX(int szx) {
		setValue(getNUM(), szx, getM());
	}
	public int getSize() {
		return decodeSZX(getIntValue() & 0x7);
	}
	public void setSize(int size) {
		setValue(getNUM(), encodeSZX(size), getM());
	}
	
	public boolean getM() {
		return (getIntValue() >> 3 & 0x1) != 0;
	}
	public void setM(boolean m) {
		setValue(getNUM(), getSZX(), m);
	}

	/*
	 * Decodes a 3-bit SZX value into a block size as specified by
	 * draft-ietf-core-block-03, section-2.1:
	 * 
	 * 0 --> 2^4 = 16 bytes
	 * ... 
	 * 6 --> 2^10 = 1024 bytes
	 * 
	 */
	public static int decodeSZX(int szx) {
		return 1 << (szx + 4);
	}

	/*
	 * Encodes a block size into a 3-bit SZX value as specified by
	 * draft-ietf-core-block-03, section-2.1:
	 * 
	 * 16 bytes = 2^4 --> 0
	 * ... 
	 * 1024 bytes = 2^10 -> 6
	 * 
	 */
	public static int encodeSZX(int blockSize) {
		return (int)(Math.log(blockSize)/Math.log(2)) - 4;
	}
	
	public static boolean validSZX(int szx) {
		return (szx >= 0 && szx <= 7);
	}
	
	@Override
	public String getDisplayValue() {
		return String.format("NUM: %d, SZX: %d (%d bytes), M: %b", 
			getNUM(), getSZX(), getSize(), getM());		
	}
	
}
