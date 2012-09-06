/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
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

import ch.ethz.inf.vs.californium.endpoint.ProxyEndpoint;
import ch.ethz.inf.vs.californium.util.Log;

/**
 * The Class ProxyExample.
 * 
 * @author Francesco Corazza
 */
public class ExampleProxy extends ProxyEndpoint {
    
    public static void main(String[] args) {
        Log.setLevel(Level.ALL);
        Log.init();
        
        // create the proxy
        try {
            ProxyEndpoint proxy = new ExampleProxy();
            proxy.start();
            
            System.out.println("Proxy started");
            
            System.out.println("Proxy listening on CoAP port (server): " + proxy.getPort(false));
            System.out.println("Proxy listening on HTTP port (server): " + proxy.getPort(true));
        } catch (SocketException e) {
            LOG.severe("Failed to create Proxy: \n" + e.getMessage());
            System.exit(-1);
        }
    }
    
    /**
     * Instantiates a new proxy example.
     * 
     * @throws SocketException
     *             the socket exception
     */
    public ExampleProxy() throws SocketException {
        
    }
}
