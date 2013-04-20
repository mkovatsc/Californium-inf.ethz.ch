package ch.inf.vs.californium.network;

import java.io.IOException;

public interface Connector {

	public void start() throws IOException;

	public void stop();

	public void destroy();

	public void send(RawData msg);
	
	public void setRawDataReceiver(RawDataReceiver receiver);
}
