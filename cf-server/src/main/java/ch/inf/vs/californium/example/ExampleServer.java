package ch.inf.vs.californium.example;

import ch.inf.vs.californium.Server;

/**
 * This is an example server that contains a few resources for demonstration.
 * 
 * @author Martin Lanter
 */
public class ExampleServer {

	public static void main(String[] args) {
		Server server = new Server();
		server.add(new HelloWorldResource("hello"));
		server.add(new StorageResource("storage"));
		server.add(new ImageResource("image"));
		server.add(new MirrorResource("mirror"));
		server.add(new LargeResource("large"));
		server.add(new RunningResource("running", server));
		server.start();

	}

}
