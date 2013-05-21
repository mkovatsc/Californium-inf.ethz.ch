package ch.inf.vs.californium.coap;


public class BlockOption {

	private int szx;
	private boolean m;
	private int num;
	
	public BlockOption() { }
	
	public BlockOption(int szx, boolean m, int num) {
		this.setSzx(szx);
		this.setM(m);
		this.setNum(num);
	}
	
	// Copy constructor
	public BlockOption(BlockOption origin) {
		if (origin == null) throw new NullPointerException();
		this.setSzx(origin.getSzx());
		this.setM(origin.isM());
		this.setNum(origin.getNum());
	}
	
	public BlockOption(byte[] value) {
		if (value == null)
			throw new NullPointerException();
		if (value.length == 0 || value.length > 3)
			throw new IllegalArgumentException("Block option's length must be between 1 and 3 bytes inclusive");
		
		this.szx = value[0] & 0x7;
		this.m = (value[0] >> 3 & 0x1) == 1;
		this.num = (value[0] & 0xFF) >> 4 ;
		for (int i=1;i<value.length;i++)
			num += (value[i] & 0xff) << (i*8 - 4);
	}
	
	public int getSzx() {
		return szx;
	}

	public void setSzx(int szx) {
		if (szx < 0 || 7 < szx)
			throw new IllegalArgumentException("Block option's szx must be between 0 and 7 inclusive");
		this.szx = szx;
	}
	
	public int getSize() {
		return 1 << (4 + szx);
	}

	public boolean isM() {
		return m;
	}

	public void setM(boolean m) {
		this.m = m;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		if (num < 0 || (1<<20)-1 < num)
			throw new IllegalArgumentException("Block option's num must be between 0 and "+(1<<20-1)+" inclusive");
		this.num = num;
	}
	
	public byte[] getValue() {
		int last = szx | (m ? 1<<3 : 0);
		if (num < 1 << 4) {
			return new byte[] {(byte) (last | (num << 4))};
		} else if (num < 1 << 12) {
			return new byte[] {
					(byte) (last | (num << 4)),
					(byte) (num >> 4)
			};
		} else {
			return new byte[] {
					(byte) (last | (num << 4)),
					(byte) (num >> 4),
					(byte) (num >> 12)
			};
		}
	}
	
	@Override
	public String toString() {
		return "(szx="+szx+", m="+m+", num="+num+")";
	}
}
