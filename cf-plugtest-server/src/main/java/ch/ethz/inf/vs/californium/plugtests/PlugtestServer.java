/*******************************************************************************
 * Copyright (c) 2014, Institute for Pervasive Computing, ETH Zurich.
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
package ch.ethz.inf.vs.californium.plugtests;

import java.net.SocketException;
import java.util.logging.Level;

import ch.ethz.inf.vs.californium.CaliforniumLogger;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.network.interceptors.MessageTracer;
import ch.ethz.inf.vs.californium.plugtests.resources.Create;
import ch.ethz.inf.vs.californium.plugtests.resources.DefaultTest;
import ch.ethz.inf.vs.californium.plugtests.resources.Large;
import ch.ethz.inf.vs.californium.plugtests.resources.LargeCreate;
import ch.ethz.inf.vs.californium.plugtests.resources.LargePost;
import ch.ethz.inf.vs.californium.plugtests.resources.LargeUpdate;
import ch.ethz.inf.vs.californium.plugtests.resources.Link1;
import ch.ethz.inf.vs.californium.plugtests.resources.Link2;
import ch.ethz.inf.vs.californium.plugtests.resources.Link3;
import ch.ethz.inf.vs.californium.plugtests.resources.LocationQuery;
import ch.ethz.inf.vs.californium.plugtests.resources.LongPath;
import ch.ethz.inf.vs.californium.plugtests.resources.MultiFormat;
import ch.ethz.inf.vs.californium.plugtests.resources.Observe;
import ch.ethz.inf.vs.californium.plugtests.resources.ObserveLarge;
import ch.ethz.inf.vs.californium.plugtests.resources.ObserveNon;
import ch.ethz.inf.vs.californium.plugtests.resources.ObservePumping;
import ch.ethz.inf.vs.californium.plugtests.resources.ObserveReset;
import ch.ethz.inf.vs.californium.plugtests.resources.Path;
import ch.ethz.inf.vs.californium.plugtests.resources.Query;
import ch.ethz.inf.vs.californium.plugtests.resources.Separate;
import ch.ethz.inf.vs.californium.plugtests.resources.Shutdown;
import ch.ethz.inf.vs.californium.plugtests.resources.Validate;
import ch.ethz.inf.vs.californium.server.Server;

// ETSI Plugtest environment
//import java.net.InetSocketAddress;
//import ch.ethz.inf.vs.californium.network.CoAPEndpoint;


/**
 * The class PlugtestServer implements the test specification for the
 * ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class PlugtestServer extends Server {

	static {
		CaliforniumLogger.initialize();
		CaliforniumLogger.setLevel(Level.FINER);
	}
	
    // exit codes for runtime errors
    public static final int ERR_INIT_FAILED = 1;
    
    // allows port configuration in Californium.properties
    private static final int port = NetworkConfig.getStandard().getInt(NetworkConfigDefaults.DEFAULT_COAP_PORT);
    
    public static void main(String[] args) {
    	
        // create server
        try {
            Server server = new PlugtestServer();
            // ETSI Plugtest environment
//            server.addEndpoint(new CoAPEndpoint(new InetSocketAddress("::1", port)));
//            server.addEndpoint(new CoAPEndpoint(new InetSocketAddress("127.0.0.1", port)));
//            server.addEndpoint(new CoAPEndpoint(new InetSocketAddress("2a01:c911:0:2010::10", port)));
//            server.addEndpoint(new CoAPEndpoint(new InetSocketAddress("10.200.1.2", port)));
            
            server.start();
            
            // add special interceptor for message traces
            for (Endpoint ep:server.getEndpoints()) {
            	ep.addInterceptor(new MessageTracer());
            }
            
            System.out.println(PlugtestServer.class.getSimpleName()+" listening on port " + port);
            
        } catch (Exception e) {
            
            System.err.printf("Failed to create "+PlugtestServer.class.getSimpleName()+": %s\n", e.getMessage());
            System.err.println("Exiting");
            System.exit(ERR_INIT_FAILED);
        }
        
    }
    
    /**
     * Constructor for a new PlugtestServer. Call {@code super(...)} to configure
     * the port, etc. according to the {@link LocalEndpoint} constructors.
     * <p>
     * Add all initial {@link LocalResource}s here.
     */
    public PlugtestServer() throws SocketException {
    	
    	NetworkConfig.getStandard() // used for plugtest
    			.setInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE, 64)
    			.setInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE, 64)
    			.setInt(NetworkConfigDefaults.NOTIFICATION_CHECK_INTERVAL_COUNT, 4)
    			.setInt(NetworkConfigDefaults.NOTIFICATION_CHECK_INTERVAL_TIME, 30000);
        
        // add resources to the server
        add(new DefaultTest());
        add(new LongPath());
        add(new Query());
        add(new Separate());
        add(new Large());
        add(new LargeUpdate());
        add(new LargeCreate());
        add(new LargePost());
        add(new Observe());
        add(new ObserveNon());
        add(new ObserveReset());
        add(new ObserveLarge());
        add(new ObservePumping());
        add(new LocationQuery());
        add(new MultiFormat());
        add(new Link1());
        add(new Link2());
        add(new Link3());
        add(new Path());
        add(new Validate());
        add(new Create());
        add(new Shutdown());
    }
    
    
    // Application entry point /////////////////////////////////////////////////
    
}
