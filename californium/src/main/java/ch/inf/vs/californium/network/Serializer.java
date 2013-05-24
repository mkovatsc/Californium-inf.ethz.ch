package ch.inf.vs.californium.network;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.serializer.DataSerializer;

public class Serializer {

	public RawData serialize(Request request) {
		byte[] bytes = request.getBytes();
		if (bytes == null)
			bytes = new DataSerializer().serializeRequest(request);
		request.setBytes(bytes);
		return new RawData(bytes, request.getDestination(), request.getDestinationPort());
	}

	public RawData serialize(Response response) {
		byte[] bytes = response.getBytes();
		if (bytes == null)
			bytes = new DataSerializer().serializeResponse(response);
		response.setBytes(bytes);
		return new RawData(bytes, response.getDestination(), response.getDestinationPort());
	}
	
	public RawData serialize(EmptyMessage message) {
		byte[] bytes = message.getBytes();
		if (bytes == null)
			bytes = new DataSerializer().serializeEmptyMessage(message);
		message.setBytes(bytes);
		return new RawData(bytes, message.getDestination(), message.getDestinationPort());
	}
}
