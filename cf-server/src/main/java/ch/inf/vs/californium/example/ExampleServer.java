package ch.inf.vs.californium.example;

import ch.inf.vs.californium.Server;

public class ExampleServer {

	public static void main(String[] args) {
		Server server = new Server();
		server.add(new HelloWorldResource("hello"));
		server.add(new StorageResource("storage"));
		server.add(new ImageResource("image"));
		server.add(new MirrorResource("mirror"));
		server.start();

	}

}
