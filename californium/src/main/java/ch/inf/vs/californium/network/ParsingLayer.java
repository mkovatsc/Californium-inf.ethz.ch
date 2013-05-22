package ch.inf.vs.californium.network;

import java.util.logging.Logger;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.parser.DataParser;
import ch.inf.vs.californium.network.parser.DataSerializer;

public class ParsingLayer extends AbstractLayer implements RawDataChannel {
	
	private final static Logger LOGGER = Logger.getLogger(ParsingLayer.class.getName());
	
//	private DataParser parser; // TODO: ThreadLocal
	private DataParser unparser;
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		assert(exchange != null && request != null);
		byte[] bytes = new DataSerializer().serializeRequest(request);
		request.setBytes(bytes);
//		LOGGER.info("Parsed "+request+" to "+Arrays.toString(bytes));
		super.sendRequest(exchange, request);
	}

	@Override
	public void sendResponse(Exchange exchange, Response response) {
		byte[] bytes = new DataSerializer().serializeResponse(response);
		response.setBytes(bytes);
//		LOGGER.info("Parsed "+response+" to "+Arrays.toString(bytes));
		super.sendResponse(exchange, response);
	}

	@Override
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		byte[] bytes = new DataSerializer().serializeEmptyMessage(message);
		message.setBytes(bytes);
//		LOGGER.info("Parsed "+message+" to "+Arrays.toString(bytes));
		super.sendEmptyMessage(exchange, message);
	}

	@Override
	public void receiveData(RawData raw) {
		unparser = new DataParser(raw.getBytes()); // TODO: ThreadLocal<T>
		if (unparser.isRequest()) {
			Request request = unparser.parseRequest();
			request.setSource(raw.getAddress());
			request.setSourcePort(raw.getPort());
			receiveRequest(null, request);
			
		} else if (unparser.isResponse()) {
			Response response = unparser.parseResponse();
			response.setSource(raw.getAddress());
			response.setSourcePort(raw.getPort());
			receiveResponse(null, response);
			
		} else {
			EmptyMessage message = unparser.parseEmptyMessage();
			message.setSource(raw.getAddress());
			message.setSourcePort(raw.getPort());
			receiveEmptyMessage(null, message);
		}
	}
	
	@Override
	public void receiveRequest(Exchange exchange, Request request) {
		LOGGER.info("<== receive request "+request);
		super.receiveRequest(exchange, request);
	}

	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		LOGGER.info("<== receive response "+response);
		super.receiveResponse(exchange, response);
	}

	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		LOGGER.info("<== receive empty message "+message);
		super.receiveEmptyMessage(exchange, message);
	}

	@Override
	public void sendData(RawData raw) {
		/* 
		 * This method is not required because the bytes to send will traverse to the
		 * bottom of the stack inside the exchange object. 
		 */
	}
}
