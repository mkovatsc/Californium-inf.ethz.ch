package ch.ethz.inf.vs.californium.benchmark;

import java.util.List;
import java.util.regex.Pattern;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource recursively computes the Fibonacci numbers and therefore needs
 * a lot of computing power to respond to a request. Use the query ?n=20 to
 * compute the 20. Fibonacci number, e.g.: coap://localhost:5683/fibonacci?n=20.
 */
public class FibonacciResource extends ResourceBase {

	private Pattern pattern;
	
	public FibonacciResource(String name) {
		super(name);
		this.pattern = Pattern.compile("n=\\d*");
	}

	@Override
	public void handleGET(Exchange exchange) {
		Request request = exchange.getRequest();
		int n = 20;
		if (request.getOptions().getURIQueryCount() > 0) {
			try {
				List<String> queries = request.getOptions().getURIQueries();
				for (String query:queries) {
					if (pattern.matcher(query).matches()) {
						n = Integer.parseInt(query.split("=")[1]);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				exchange.respond(ResponseCode.BAD_REQUEST, e.getMessage());
				return;
			}
		}
		
		int fib = fibonacci(n);
		exchange.respond("fibonacci("+n+") = "+fib);
	}
	
	/**
	 * Recursive Fibonacci algorithm
	 */
	private int fibonacci(int n) {
		if (n <= 1) return n;
		else return fibonacci(n-1) + fibonacci(n-2);
	}
	
}
