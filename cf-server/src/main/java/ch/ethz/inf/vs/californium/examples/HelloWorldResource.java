package ch.ethz.inf.vs.californium.examples;

import java.util.concurrent.atomic.AtomicInteger;

import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.resources.ResourceBase;

/**
 * This resource responds with a kind "hello world" to GET requests.
 * 
 * @author Martin Lanter
 */
public class HelloWorldResource extends ResourceBase {

	private AtomicInteger counter = new AtomicInteger();
	private volatile int last;
	private volatile long ts;
	
	public HelloWorldResource(String name) {
		super(name);
	}
	
	@Override
	public void processGET(Exchange exchange) {
		counter.incrementAndGet();
//		try {
//			Thread.sleep(10);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		exchange.respond("hello world");
	}

}
