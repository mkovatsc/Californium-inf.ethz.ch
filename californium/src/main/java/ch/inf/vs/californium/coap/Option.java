package ch.inf.vs.californium.coap;

import java.util.Arrays;

public class Option implements Comparable<Option> {

	private int number;
	private byte[] value;
	
	public Option() {
		this.value = new byte[0];
	}
	
	// Constructors
	
	public Option(int number) {
		this.number = number;
		this.value = new byte[0];
	}
	
	public Option(int number, String str) {
		this.number = number;
		setStringValue(str);
	}
	
	public Option(int number, int val) {
		this.number = number;
		setIntegerValue(val);
	}
	
	public Option(int number, long val) {
		this.number = number;
		setLongValue(val);
	}
	
	public Option(int number, byte[] opaque) {
		this.number = number;
		setValue(opaque);
	}
	
	// Getter and Setter
	
	public int getLength() {
		return value.length;
	}
	
	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}
	
	public byte[] getValue() {
		return value;
	}
	
	public String getStringValue() {
		return new String(value);
	}
	
	public int getIntegerValue() {
		int ret = 0;
		for (int i=0;i<value.length;i++) {
			ret += value[i] << (i*8);
		}
		return ret;
	}
	
	public long getLongValue() {
		long ret = 0;
		for (int i=0;i<value.length;i++) {
			ret += value[i] << (i*8);
		}
		return ret;
	}

	public void setValue(byte[] value) {
		if (value == null)
			throw new NullPointerException();
		// TODO: Should we remove leading zeros?
		this.value = value;
	}
	
	public void setStringValue(String str) {
		if (str == null)
			throw new NullPointerException();
		value = str.getBytes();
	}
	
	public void setIntegerValue(int val) {
		int length = 0;
		for (int i=0;i<4;i++)
			if (val >= 1<<(i*8) || val < 0) length++;
			else break;
		value = new byte[length];
		for (int i=0;i<length;i++)
			value[i] = (byte) (val >> i*8);
	}
	
	public void setLongValue(long val) {
		int length = 0;
		for (int i=0;i<8;i++)
			if (val >= 1L<<(i*8) || val < 0) length++;
			else break;
		value = new byte[length];
		for (int i=0;i<length;i++)
			value[i] = (byte) (val >> i*8);
	}
	
	public boolean isCritical() {
		// Critical = (onum & 1);
		return (number & 1) != 0;
	}
	
	public boolean isUnSafe() {
		// UnSafe = (onum & 2);
		return (number & 2) != 0;
	}
	
	public boolean isNoCacheKey() {
		// NoCacheKey = ((onum & 0x1e) == 0x1c);
		return (number & 0x1E) == 0x1C;
	}
	
	@Override
	public int compareTo(Option o) {
		return number - o.number;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Option))
			return false;
		
		Option op = (Option) o;
		return number == op.number && Arrays.equals(value, op.value);
	}
	
	@Override
	public int hashCode() {
		return number*31 + value.hashCode();
	}
	
	@Override
	public String toString() {
		return "("+number+":"+Arrays.toString(value)+")";
	}
}
