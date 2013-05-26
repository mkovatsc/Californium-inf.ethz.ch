
package ch.inf.vs.californium.network;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.connector.UDPConnector;
import ch.inf.vs.californium.network.layer.CoapStack;

/**
 * This is the interface needed between a CoAP stack and a connector. The
 * connector forwards raw data to the method receiveData() and the CoAP stack
 * forwards messages to the corresponding method sendX(). {@link Endpoint} uses
 * this interface to connect {@link CoapStack} to {@link UDPConnector}.
 */
public interface StackBottom {

	public void sendRequest(Exchange exchange, Request request);

	public void sendResponse(Exchange exchange, Response response);

	public void sendEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);

	public void receiveData(RawData raw);

}
