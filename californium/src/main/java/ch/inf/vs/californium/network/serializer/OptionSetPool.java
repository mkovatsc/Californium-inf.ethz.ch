package ch.inf.vs.californium.network.serializer;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.inf.vs.californium.coap.OptionSet;

public class OptionSetPool {

	public static /*final*/ int CAPACITY = 100000; // weak bound
	
	// TODO: Idea: ring buffer?

	private static Queue<OptionSet> options = new LinkedBlockingQueue<>();
	
	public static OptionSet getOptionSet() {
//		OptionSet set = options.poll();
//		if (set != null)
//			return set;
//		else 
			return new OptionSet();
	}
	
	public static void restore(OptionSet set) {
//		if (options.size() < CAPACITY) {
//			set.clear();
//			options.add(set);
//		}
	}
	
}
