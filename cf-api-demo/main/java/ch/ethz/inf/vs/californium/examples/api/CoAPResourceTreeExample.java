package ch.ethz.inf.vs.californium.examples.api;

import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class CoAPResourceTreeExample {

	public static void main(String[] args) {
		
		Server server = new Server();
		
		server.add(
			new ResourceBase("A").add(
				new ResourceBase("A1").add(
					new ResourceBase("A1_a"),
					new ResourceBase("A1_b"),
					new ResourceBase("A1_c"),
					new ResourceBase("A1_d")
				),
				new ResourceBase("A2").add(
					new ResourceBase("A2_a"),
					new ResourceBase("A2_b"),
					new ResourceBase("A2_c"),
					new ResourceBase("A2_d")
				)
			),
			new ResourceBase("B").add(
				new ResourceBase("B1").add(
					new ResourceBase("B1_a"),
					new ResourceBase("B1_b")
				)
			),
			new ResourceBase("C"),
			new ResourceBase("D")
		);
		
//		server
//			.add(new ResourceBase("A")
//				.add(new ResourceBase("A1")
//					.add(new ResourceBase("A1_a"))
//					.add(new ResourceBase("A1_b"))
//					.add(new ResourceBase("A1_c"))
//					.add(new ResourceBase("A1_d"))
//				)
//				.add(new ResourceBase("A2")
//					.add(new ResourceBase("A2_a"))
//					.add(new ResourceBase("A2_a"))
//					.add(new ResourceBase("A2_a"))
//					.add(new ResourceBase("A2_a"))
//				)
//			)
//			.add(new ResourceBase("B")
//				.add(new ResourceBase("B1")
//					.add(new ResourceBase("B1_a"))
//					.add(new ResourceBase("B1_b"))
//				)
//			)
//			.add(new ResourceBase("C"))
//			.add(new ResourceBase("D"));
		
		server.start();
		
	}
	
}
