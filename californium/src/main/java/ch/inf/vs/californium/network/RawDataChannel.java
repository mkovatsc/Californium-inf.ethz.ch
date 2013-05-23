package ch.inf.vs.californium.network;

public interface RawDataChannel {

	public void receiveData(RawData msg);
	
	public void sendData(RawData msg);
	
}
