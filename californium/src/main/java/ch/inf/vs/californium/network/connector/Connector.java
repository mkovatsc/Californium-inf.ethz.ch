package ch.inf.vs.californium.network.connector;

import java.io.IOException;

import ch.inf.vs.californium.network.RawData;
import ch.inf.vs.californium.network.RawDataChannel;

public interface Connector {

	public void start() throws IOException;

	public void stop();

	public void destroy();

	public void send(RawData msg);
	
	public void setRawDataReceiver(RawDataChannel receiver);
}
