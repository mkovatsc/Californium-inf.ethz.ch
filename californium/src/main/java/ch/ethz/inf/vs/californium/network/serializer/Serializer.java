package ch.ethz.inf.vs.californium.network.serializer;

import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.elements.RawData;

/**
 * The serializer serializes requests, responses and empty messages to bytes,
 * i.e. {@link RawData}.
 */
public class Serializer {

	/**
	 * Serializes the specified request. Message identifier, message code,
	 * token, options and payload are converted into a byte array and wrapped in
	 * a {@link RawData} object. The request's destination address and port are
	 * stored as address and port in the RawData object.
	 * 
	 * @param request
	 *            the request
	 * @return the request as raw data
	 */
	public RawData serialize(Request request) {
		byte[] bytes = request.getBytes();
		if (bytes == null)
			bytes = new DataSerializer().serializeRequest(request);
		request.setBytes(bytes);
		return new RawData(bytes, request.getDestination(), request.getDestinationPort());
	}

	/**
	 * Serializes the specified response. Message identifier, message code,
	 * token, options and payload are converted into a byte array and wrapped in
	 * a {@link RawData} object. The response's destination address and port are
	 * stored as address and port in the RawData object.
	 *
	 * @param response the response
	 * @return the response as raw data
	 */
	public RawData serialize(Response response) {
		byte[] bytes = response.getBytes();
		if (bytes == null)
			bytes = new DataSerializer().serializeResponse(response);
		response.setBytes(bytes);
		return new RawData(bytes, response.getDestination(), response.getDestinationPort());
	}
	
	/**
	 * Serializes the specified empty message. Message identifier and code are
	 * converted into a byte array and wrapped in a {@link RawData} object. The
	 * message's destination address and port are stored as address and port in
	 * the RawData object.
	 * 
	 * @param message
	 *            the message
	 * @return the empty message as raw data
	 */
	public RawData serialize(EmptyMessage message) {
		byte[] bytes = message.getBytes();
		if (bytes == null)
			bytes = new DataSerializer().serializeEmptyMessage(message);
		message.setBytes(bytes);
		return new RawData(bytes, message.getDestination(), message.getDestinationPort());
	}
}
