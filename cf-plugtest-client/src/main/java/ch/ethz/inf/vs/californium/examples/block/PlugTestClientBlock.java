package ch.ethz.inf.vs.californium.examples.block;


public class PlugTestClientBlock {

	public static String serverURI = "localhost:5683";
	
	public static void main(String[] args) throws Exception {
		
		Plugtest test = new TD_COAP_BLOCK_01();
		test.execute();
		
	}

}
