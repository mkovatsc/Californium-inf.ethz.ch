package ch.inf.vs.californium.perf.throughput;

import java.util.Iterator;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.network.serializer.Serializer;

public class VeryEcoMessageProducer implements Iterator<byte[]> {

	private byte[] prototype;
	
	public VeryEcoMessageProducer(String targetURI) {
		try {
			Serializer serializer = new Serializer();
			Request request = new Request(Code.GET);
			request.setType(Type.NON);
			request.setToken(new byte[0]);
			request.setMID(0);
			request.setURI(targetURI);
			prototype = serializer.serialize(request).getBytes();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public byte[] next() {
		// increase MID at location [2,3]
		if (++prototype[3] == 0)
			++prototype[2];
		return prototype;
	}
	
	@Override
	public void remove() { }
}
