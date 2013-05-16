package ch.inf.vs.californium.network;

interface RawDataChannel {

	public void receiveData(RawData msg);
	
	public void sendData(RawData msg);
	
}
