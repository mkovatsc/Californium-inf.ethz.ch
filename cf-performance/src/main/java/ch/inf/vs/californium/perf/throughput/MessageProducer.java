package ch.inf.vs.californium.perf.throughput;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.network.RawData;
import ch.inf.vs.californium.network.serializer.Serializer;

public class MessageProducer implements Iterator<RawData> {

	ArrayList<RawData> array;

	private int amount;
	private int pointer;
	
	public MessageProducer(String targetURI, int amount) throws Exception {
		this.amount = amount;

		Serializer serializer = new Serializer();
		InetAddress source = InetAddress.getByName("192.168.1.33");

		int count = 0;
		this.array = new ArrayList<>(amount);
		for (int port = 1; port < (1 << 16) && count < amount; port++) {
			for (int mid = 0; mid < (1 << 16) && count < amount; mid++) {
				Request request = new Request(Code.GET);
				request.setToken(new byte[0]);
				request.setMID(mid);
				request.setURI(targetURI);

				RawData raw = serializer.serialize(request);
				raw.setAddress(source);
				raw.setPort(port);
				array.add(raw);
				count++;
			}
		}

		Collections.shuffle(array);
	}

	public static void printUsesMemory() {
		System.out.println("used memory: "
				+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
						.freeMemory()) / 1024 + "	KB");
	}

	@Override
	public boolean hasNext() {
		return pointer < amount;
	}

	@Override
	public RawData next() {
		return array.get(pointer++);
	}

	@Override
	public void remove() {}
}
