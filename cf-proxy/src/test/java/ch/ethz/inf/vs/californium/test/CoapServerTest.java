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

package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.resources.proxy.OptionNumberRegistry;

/**
 * The Class CoapProxyTest.
 * Before to start this test, both server and proxy should be running.
 * 
 * @author Francesco Corazza
 */
public class CoapServerTest {
    
    // private static final String SERVER_LOCATION =
    // "coap://[2001:620:8:35c1:ca2a:14ff:fe12:8af9]:5684";
    
    private static final int PORT = 5684;
    private static final String SERVER_LOCATION = "coap://localhost:" + PORT;
    private static Process serverProcess;
    private static final boolean isStandAlone = true;
    private static final String jarName = "cf-test-server-0.8.4-SNAPSHOT.jar";
    
    /**
     * Sets the up before class.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        if (isStandAlone) {
            
            try {
                // get the current path
                String path = new java.io.File("").getAbsolutePath();
                path = path.substring(0, path.lastIndexOf(File.separator));
                
                // create the command line
                String absoluteFile = path + File.separator + "run" + File.separator + jarName;
                // String command =
                // "java -jar " + absoluteFile + " " + PORT;
                // System.out.println(command);
                
                // Run the server in a separate system process
                ProcessBuilder processBuilder =
                                new ProcessBuilder("java", "-jar", absoluteFile,
                                                Integer.toString(PORT));
                processBuilder.redirectErrorStream(true);
                serverProcess = processBuilder.start();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            
            // // Then retrieve the process output
            // InputStream in = serverProcess.getInputStream();
            // InputStream err = serverProcess.getErrorStream();
            
        }
    }
    
    /**
     * Tear down after class.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        if (isStandAlone && serverProcess != null) {
            serverProcess.destroy();
        }
    }
    
    /**
     * Storage post delete test.
     */
    //@Test
    public final void deleteTest() {
        String postResource = "storage";
        String requestPayload = "subResource2";
        
        Request postRequest = new Request(Code.POST);
        postRequest.setPayload(requestPayload);
        postRequest.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
        
        Response postResponse = executeRequest(postRequest, postResource);
        assertNotNull(postResponse);
        assertEquals(postResponse.getCode(), ResponseCode.CREATED);
        
        Request deleteRequest = new Request(Code.DELETE);
        Response deleteResponse =
                        executeRequest(deleteRequest, postResource + "/" + requestPayload);
        
        assertNotNull(deleteResponse);
        assertEquals(deleteResponse.getCode(), ResponseCode.DELETED);
    }
    
    //@Test
    public final void getImageTest() {
        if (!isStandAlone) {
            String resource = "image";
            
            Request getRequest = new Request(Code.GET);
            int acceptType = MediaTypeRegistry.IMAGE_PNG;
            getRequest.getOptions().setAccept(acceptType);
            Response response = executeRequest(getRequest, resource);
            
            assertNotNull(response);
            assertEquals(ResponseCode.CONTENT, response.getCode());
            assertEquals((int) response.getOptions().getContentFormat(), acceptType);
        }
    }
    
    //@Test
    public final void getLargeTest() {
        String resource = "large";
        
        Request getRequest = new Request(Code.GET);
        Response response = executeRequest(getRequest, resource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.CONTENT);
    }
    
    //@Test
    public final void getQueryTest() {
        String resource = "query?";
        String parameter0 = "a=1";
        String parameter1 = "b=2";
        
        Request getRequest = new Request(Code.GET);;
        Response response = executeRequest(getRequest, resource + parameter0 + "&" + parameter1);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.CONTENT);
        String[] parameters = response.getPayloadString().split("\n");
        assertTrue(parameters[0].equalsIgnoreCase(parameter0)
                        && parameters[1].equalsIgnoreCase(parameter1));
    }
    
    /**
     * Hello world get test.
     */
    //@Test
    public final void getTest() {
        String resource = "helloWorld";
        
        Request getRequest = new Request(Code.GET);;
        Response response = executeRequest(getRequest, resource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.CONTENT);
    }
    
    /**
     * Long path get test.
     */
    //@Test
    public final void longPathGetTest() {
        String resource = "seg1/seg2/seg3";
        
        Request getRequest = new Request(Code.GET);;
        Response response = executeRequest(getRequest, resource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.CONTENT);
    }
    
    /**
     * To upper post test.
     */
    //@Test
    public final void postTest() {
        String postResource = "toUpper";
        String requestPayload = "aaa";
        
        Request postRequest = new Request(Code.POST);
        postRequest.setPayload(requestPayload);
        postRequest.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
        
        Response response = executeRequest(postRequest, postResource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.CONTENT);
        
        String responsePayload = response.getPayloadString();
        assertEquals(responsePayload, requestPayload.toUpperCase());
    }
    
    /**
     * Storage post get test.
     */
    //@Test
    public final void postTest2() {
        String postResource = "storage";
        String requestPayload = Long.toString(Calendar.getInstance().getTimeInMillis());
        
        Request postRequest = new Request(Code.POST);
        postRequest.setPayload(requestPayload);
        postRequest.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
        
        Response postResponse = executeRequest(postRequest, postResource);
        assertNotNull(postResponse);
        assertEquals(postResponse.getCode(), ResponseCode.CREATED);
        
        Request getRequest = new Request(Code.GET);;
        Response getResponse = executeRequest(getRequest, postResource + "/" + requestPayload);
        
        assertNotNull(getResponse);
        assertEquals(getResponse.getCode(), ResponseCode.CONTENT);
        
        String responsePayload = getResponse.getPayloadString();
        assertTrue(responsePayload.equals(requestPayload));
    }
    
    //@Test
    public final void proxyNotSupportedTest() {
        String proxyUri = "coap://localhost/helloWorld";
        
        Request getRequest = new Request(Code.GET);;
        getRequest.getOptions().setProxyURI(proxyUri);
        Response response = executeRequest(getRequest, "");
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.PROXY_NOT_SUPPORTED);
    }
    
    /**
     * Storage put get test.
     */
    //@Test
    public final void putTest() {
        String putResource = "storage";
        String requestPayload = "aaa";
        
        Request putRequest = new Request(Code.PUT);
        putRequest.setPayload(requestPayload);
        putRequest.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
        
        Response putResponse = executeRequest(putRequest, putResource);
        assertNotNull(putResponse);
        assertEquals(putResponse.getCode(), ResponseCode.CHANGED);
        
        Request getRequest = new Request(Code.GET);;
        Response getResponse = executeRequest(getRequest, putResource);
        
        assertNotNull(getResponse);
        assertEquals(getResponse.getCode(), ResponseCode.CONTENT);
        
        String responsePayload = getResponse.getPayloadString();
        assertTrue(responsePayload.equals(requestPayload));
    }
    
    //@Test
    public final void separateTest() {
        String resource = "separate";
        
        Request getRequest = new Request(Code.GET);;
        Response response = executeRequest(getRequest, resource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.CONTENT);
    }
    
    //@Test
    public final void wrongAcceptContentGetTest() {
        String resource = "image";
        
        Request getRequest = new Request(Code.GET);;
        int acceptType = MediaTypeRegistry.VIDEO_RAW;
        getRequest.getOptions().setAccept(acceptType);
        Response response = executeRequest(getRequest, resource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.NOT_ACCEPTABLE);
    }
    
    /**
     * To upper post wrong content type test.
     */
    //@Test
    public final void wrongContentTypePostTest() {
        String postResource = "toUpper";
        String requestPayload = "aaa";
        
        Request postRequest = new Request(Code.POST);
        postRequest.setPayload(requestPayload.getBytes());
        postRequest.getOptions().setContentFormat(MediaTypeRegistry.IMAGE_JPEG);
        
        Response response = executeRequest(postRequest, postResource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
    }
    
    /**
     * Time resource delete fail test.
     */
    //@Test
    public final void wrongDeleteTest() {
        String resource = "image";
        
        Request getRequest = new Request(Code.DELETE);
        Response response = executeRequest(getRequest, resource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.METHOD_NOT_ALLOWED);
    }
    
    /**
     * Time resource post fail test.
     */
    //@Test
    public final void wrongPostTest() {
        String resource = "helloWorld";
        
        Request getRequest = new Request(Code.POST);
        Response response = executeRequest(getRequest, resource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.METHOD_NOT_ALLOWED);
    }
    
    /**
     * Time resource put fail test.
     */
    //@Test
    public final void wrongPutTest() {
        String resource = "helloWorld";
        
        Request getRequest = new Request(Code.PUT);
        Response response = executeRequest(getRequest, resource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.METHOD_NOT_ALLOWED);
    }
    
    //@Test
    public final void wrongResourceTest() {
        String resource = "inexistent";
        
        Request getRequest = new Request(Code.GET);;
        Response response = executeRequest(getRequest, resource);
        
        assertNotNull(response);
        assertEquals(response.getCode(), ResponseCode.NOT_FOUND);
    }
    
    /**
     * Execute request.
     * 
     * @param request the request
     * @param resource the resource
     * @return the response
     */
    private Response executeRequest(Request request, String resource) {
        
        request.setURI(SERVER_LOCATION + "/" + resource);
        
//        request.setToken(TokenManager.getInstance().acquireToken());
        
        // enable response queue for synchronous I/O
//        request.enableResponseQueue(true);
        
        // execute the request
//        try {
            request.send();
//        } catch (IOException e) {
//            System.err.println("Failed to execute request: " + e.getMessage());
//        }
        
        // receive response
        Response response = null;
        try {
            response = request.waitForResponse(1000);
        } catch (InterruptedException e) {
            System.err.println("Receiving of response interrupted: " + e.getMessage());
        }
        
        return response;
    }
    
}
