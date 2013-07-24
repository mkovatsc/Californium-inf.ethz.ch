package example;

import ch.inf.vs.californium.Server;

public class ExampleServer {

	public static void main(String[] args) {
		Server server = new Server(7777);
		server.add(new HelloWorldResource("hello"));
		server.add(new StorageResource("storage"));
		server.add(new ImageResource("image"));
		server.add(new GreetingResource("greeting"));
		server.start();

	}

}
