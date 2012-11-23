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
package ch.ethz.inf.vs.californium.examples;

import java.net.SocketException;
import java.util.logging.Level;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.endpoint.LocalEndpoint;
import ch.ethz.inf.vs.californium.endpoint.ServerEndpoint;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.examples.plugtest.DefaultTest;
import ch.ethz.inf.vs.californium.examples.plugtest.Large;
import ch.ethz.inf.vs.californium.examples.plugtest.LargeCreate;
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
import ch.ethz.inf.vs.californium.util.Log;

/**
 * The class PlugtestServer implements the test specification for the
 * ETSI IoT CoAP Plugtests, Paris, France, 24 - 25 March 2012.
 * 
 * @author Matthias Kovatsch
 */
public class PlugtestServer extends ServerEndpoint {
    
    // exit codes for runtime errors
    public static final int ERR_INIT_FAILED = 1;
    
    public static void main(String[] args) {
        
        Log.setLevel(Level.INFO);
        Log.init();
        
        // create server
        try {
            
            LocalEndpoint server = new PlugtestServer();
            server.start();
            
            System.out.printf(PlugtestServer.class.getSimpleName()+" listening on port %d.\n", server.getPort());
            
        } catch (SocketException e) {
            
            System.err.printf("Failed to create "+PlugtestServer.class.getSimpleName()+": %s\n", e.getMessage());
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
        
        // add resources to the server
        addResource(new DefaultTest());
        addResource(new LongPath());
        addResource(new Query());
        addResource(new Separate());
        addResource(new Large());
        addResource(new LargeUpdate());
        addResource(new LargeCreate());
        addResource(new Observe());
        addResource(new LocationQuery());
        addResource(new MultiFormat());
        addResource(new Link1());
        addResource(new Link2());
        addResource(new Link3());
        addResource(new Path());
    }
    
    
    // Application entry point /////////////////////////////////////////////////
    
    @Override
    public void handleRequest(Request request) {
        
        // Add additional handling like special logging here.
        request.prettyPrint();
        
        // dispatch to requested resource
        super.handleRequest(request);
    }
    
}
