/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.persistingservice;

import java.net.SocketException;
import java.util.logging.Level;

import ch.ethz.inf.vs.californium.coap.CommunicatorFactory;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator;
import ch.ethz.inf.vs.californium.endpoint.LocalEndpoint;
import ch.ethz.inf.vs.californium.endpoint.ServerEndpoint;
import ch.ethz.inf.vs.californium.util.Log;
import ch.ethz.inf.vs.californium.util.Properties;
import ch.ethz.inf.vs.persistingservice.database.DatabaseConnection;
import ch.ethz.inf.vs.persistingservice.resources.PersistingServiceResource;

/**
 * The Class PersistingServer starts the persisting server and creates and starts a couchdb connection.
 * <p>
 * The persisting server port can be defined in the Califorinium.properties file.
 * The couchdb connection uses couchdb's standard port of 5984.
 */
public class PersistingServer extends ServerEndpoint {
	
	public static final int ERR_INIT_FAILED = 1;
	private int udpPort = 0;
	private boolean runAsDaemon = false;
	private int transferBlockSize = 0;
	private int requestPerSecond = 0;
	
	/**
	 * Instantiates a new persisting server and creates the topmost resource: persistingservice
	 *
	 * @throws SocketException the socket exception
	 */
	public PersistingServer() throws SocketException {
		this(Properties.std.getInt("DEFAULT_PORT"));
	}

	/**
	 * Instantiates a new persisting server and creates the topmost resource: persistingservice
	 *
	 * @param udpPort
	 *            the udp port
	 * @throws SocketException
	 *             the socket exception
	 */
	public PersistingServer(int udpPort) throws SocketException {
		this(udpPort, 0, false, 0);
	}

	/**
	 * Instantiates a new persisting server and creates the topmost resource: persistingservice
	 *
	 * @param udpPort
	 *            the udp port
	 * @param defaultBlockSze
	 *            the default block sze
	 * @param daemon
	 *            the daemon
	 * @param requestPerSecond
	 *            the request per second
	 * @throws SocketException
	 *             the socket exception
	 */
	public PersistingServer(int udpPort, int transferBlockSize, boolean runAsDaemon, int requestPerSecond) throws SocketException {
		super();

		this.udpPort = udpPort;
		this.transferBlockSize = transferBlockSize;
		this.runAsDaemon = runAsDaemon;
		this.requestPerSecond = requestPerSecond;

		// add Persisting Service resource
		addResource(new PersistingServiceResource("persistingservice"));
		
	}

	/**
	 * Gets the port.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return CommunicatorFactory.getInstance().getCommunicator().getPort();
	}


	@Override
	protected void createCommunicator() {
		// get the communicator factory
		CommunicatorFactory factory = CommunicatorFactory.getInstance();

		// set the parameters of the communicator
		factory.setUdpPort(udpPort);
		factory.setTransferBlockSize(transferBlockSize);
		factory.setRunAsDaemon(runAsDaemon);
		factory.setRequestPerSecond(requestPerSecond);

		// initialize communicator
		Communicator communicator = factory.getCommunicator();

		// register the endpoint as a receiver of the communicator
		communicator.registerReceiver(this);
	}

    @Override
    public void handleRequest(Request request) {
        
        // dispatch to requested resource
        super.handleRequest(request);
    }

	@Override
	protected void responseProduced(Response response) {
		// Do Nothing
		
	}    
	
	
	
	
	/**
	 * The main method creates a new persitingservice object and starts the persisting service server.
	 * It also establishes a connection to the couchdb database and starts the http client to access the database.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		
		Log.setLevel(Level.WARNING);
		Log.init();
		
		try {
			PersistingServer DBServer = new PersistingServer();
			DBServer.start();
			System.out.println("Persisting Service listening on port: " + DBServer.getPort()); // DBServer.getPort());
			
			DatabaseConnection dbConnection = new DatabaseConnection();
			dbConnection.startDB();
			System.out.println("Database Connection was established on port: " + dbConnection.PORT);
		} catch (SocketException e) {
			System.err.println("Failed to initialize Database Server");
			System.err.println("CLASS: DatabaseServer");
			System.exit(ERR_INIT_FAILED);
		}
	}
	
}
