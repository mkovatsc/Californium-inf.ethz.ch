package ch.ethz.inf.vs.californium.producer;

import java.net.URI;
import java.util.Iterator;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.network.serialization.Serializer;
import ch.ethz.inf.vs.elements.RawData;

/**
 * This producer is as economic with memory as possible. It only uses a single
 * {@link RawData} instance. To produce a new request, it only changes the MID
 * (bytes 2 and 3). This producer must only be used in strict single-threaded
 * environment (because there is actually only one single request that is reused
 * infinitely often).
 */
public class VeryEcoMessageProducer implements Iterator<byte[]> {

	private byte[] prototype;

	public VeryEcoMessageProducer(URI uri) {
		setURI(uri);
	}

	public VeryEcoMessageProducer() { }
	
	public void setURI(URI uri) {
		Serializer serializer = new Serializer();
		Request request = new Request(Code.GET);
		request.setType(Type.CON);
		request.setToken(new byte[0]);
		request.setMID(0);
		request.setURI(uri);
		prototype = serializer.serialize(request).getBytes();
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
