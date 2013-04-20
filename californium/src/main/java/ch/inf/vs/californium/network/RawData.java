package ch.inf.vs.californium.network;


/*
 * TODO: Can we recycle the memory of these objects? Is it beneficial?
 */
class RawData {

	private byte[] bytes;
	
	public RawData(byte[] bytes) {
		this.bytes = bytes;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public int getSize() {
		return bytes.length;
	}
	
}
