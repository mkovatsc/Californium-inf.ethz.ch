/*******************************************************************************
 * Copyright (c) 2013, Institute for Pervasive Computing, ETH Zurich.
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
package ch.ethz.inf.vs.californium.examples;

import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.examples.plugtest.Create;
import ch.ethz.inf.vs.californium.examples.plugtest.DefaultTest;
import ch.ethz.inf.vs.californium.examples.plugtest.Large;
import ch.ethz.inf.vs.californium.examples.plugtest.LargeCreate;
import ch.ethz.inf.vs.californium.examples.plugtest.LargePost;
import ch.ethz.inf.vs.californium.examples.plugtest.LargeUpdate;
import ch.ethz.inf.vs.californium.examples.plugtest.Link1;
import ch.ethz.inf.vs.californium.examples.plugtest.Link2;
import ch.ethz.inf.vs.californium.examples.plugtest.Link3;
import ch.ethz.inf.vs.californium.examples.plugtest.LocationQuery;
import ch.ethz.inf.vs.californium.examples.plugtest.LongPath;
import ch.ethz.inf.vs.californium.examples.plugtest.MultiFormat;
import ch.ethz.inf.vs.californium.examples.plugtest.Observe;
import ch.ethz.inf.vs.californium.examples.plugtest.Path;
import ch.ethz.inf.vs.californium.examples.plugtest.Query;
import ch.ethz.inf.vs.californium.examples.plugtest.Separate;
import ch.ethz.inf.vs.californium.examples.plugtest.Shutdown;
import ch.ethz.inf.vs.californium.examples.plugtest.Validate;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.Server;

/**
 * The class PlugtestServer implements the test specification for the
 * ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class PlugtestServer extends Server {
    
	private static final Logger Log = CalifonriumLogger.getLogger(PlugtestServer.class);
	
    // exit codes for runtime errors
    public static final int ERR_INIT_FAILED = 1;
    
    public static void main(String[] args) {
        
        Log.setLevel(Level.INFO);
    	
        // create server
        try {
            Server server = new PlugtestServer();
            server.start();
            
            System.out.printf(PlugtestServer.class.getSimpleName()+" listening\n");
            
        } catch (Exception e) {
            
            System.err.printf("Failed to create "+PlugtestServer.class.getSimpleName()+": %s\n", e.getMessage());
            System.err.println("Exiting");
            System.exit(ERR_INIT_FAILED);
        }
        
    }
    
    // Logging /////////////////////////////////////////////////////////////////
    
    /**
     * Constructor for a new PlugtestServer. Call {@code super(...)} to configure
     * the port, etc. according to the {@link LocalEndpoint} constructors.
     * <p>
     * Add all initial {@link LocalResource}s here.
     */
    public PlugtestServer() throws SocketException {
    	
    	NetworkConfig.getStandard()
    			.setInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE, 64) // used for plugtest
    			.setInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE, 64); // used for plugtest
        
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
