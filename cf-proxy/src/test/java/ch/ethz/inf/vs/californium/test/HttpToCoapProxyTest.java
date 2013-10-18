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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.HttpTranslator;
import ch.ethz.inf.vs.californium.util.TranslationException;

/**
 * The Class CoapProxyTest.
 * Before to start this test, both server and proxy should be running.
 * 
 * @author Francesco Corazza
 */
public class HttpToCoapProxyTest {
    
    private static final int HTTP_PORT = 8080;
    
    /** The Constant proxyLocation. */
    private static final String PROXY_LOCATION = "coap://localhost";
    
    /** The Constant serverLocation. */
    private static final String SERVER_LOCATION = "coap://localhost:5683";
    
    /**
     * Storage post delete test.
     */
    @Test
    public final void deleteTest() {
        String postResource = "storage";
        String requestPayload = "subResource2";
        
        Request postRequest = new POSTRequest();
        postRequest.setPayload(requestPayload);
        postRequest.setContentType(MediaTypeRegistry.TEXT_PLAIN);
        
        Response postResponse = executeRequest(postRequest, postResource, true);
        assertNotNull(postResponse);
        assertTrue(postResponse.getCode() == CodeRegistry.RESP_CREATED);
        
        Request deleteRequest = new DELETERequest();
        Response deleteResponse = executeRequest(deleteRequest, postResource + "/"
                        + requestPayload, true);
        
        assertNotNull(deleteResponse);
        assertTrue(deleteResponse.getCode() == CodeRegistry.RESP_DELETED);
    }
    
    @Test
    public final void getImageTest() {
        String resource = "image";
        
        Request getRequest = new GETRequest();
        int acceptType = MediaTypeRegistry.IMAGE_PNG;
        getRequest.setAccept(acceptType);
        Response response = executeRequest(getRequest, resource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
        assertTrue(response.getContentType() == acceptType);
    }
    
    @Test
    public final void getLargeTest() {
        String resource = "large";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
    }
    
    @Test
    public final void getQueryTest() {
        String resource = "query?";
        String parameter0 = "a=1";
        String parameter1 = "b=2";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource + parameter0 + "&" + parameter1,
                                           true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
        String[] parameters = response.getPayloadString().split("\n");
        assertTrue(parameters[0].equalsIgnoreCase(parameter0) && parameters[1]
                        .equalsIgnoreCase(parameter1));
    }
    
    /**
     * Hello world get test.
     */
    @Test
    public final void getTest() {
        String resource = "helloWorld";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
    }
    
    /**
     * Local proxy resource test.
     */
    @Test
    public final void localProxyResourceTest() {
        String resource = "proxy/stats";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, false);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
    }
    
    /**
     * Long path get test.
     */
    @Test
    public final void longPathGetTest() {
        String resource = "seg1/seg2/seg3";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
    }
    
    /**
     * To upper post test.
     */
    @Test
    public final void postTest() {
        String postResource = "toUpper";
        String requestPayload = "aaa";
        
        Request postRequest = new POSTRequest();
        postRequest.setPayload(requestPayload);
        postRequest.setContentType(MediaTypeRegistry.TEXT_PLAIN);
        
        Response response = executeRequest(postRequest, postResource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_CONTENT);
        
        String responsePayload = response.getPayloadString();
        assertTrue(responsePayload.equals(requestPayload.toUpperCase()));
    }
    
    /**
     * Storage post get test.
     */
    @Test
    public final void postTest2() {
        String postResource = "storage";
        String requestPayload = Long.toString(Calendar.getInstance().getTimeInMillis());
        
        Request postRequest = new POSTRequest();
        postRequest.setPayload(requestPayload);
        postRequest.setContentType(MediaTypeRegistry.TEXT_PLAIN);
        
        Response postResponse = executeRequest(postRequest, postResource, true);
        assertNotNull(postResponse);
        assertTrue(postResponse.getCode() == CodeRegistry.RESP_CREATED);
        
        Request getRequest = new GETRequest();
        Response getResponse = executeRequest(getRequest, postResource + "/"
                        + requestPayload, true);
        
        assertNotNull(getResponse);
        assertTrue(getResponse.getCode() == CodeRegistry.RESP_CONTENT);
        
        String responsePayload = getResponse.getPayloadString();
        assertTrue(responsePayload.equals(requestPayload));
    }
    
    /**
     * Storage put get test.
     */
    @Test
    public final void putTest() {
        String putResource = "storage";
        String requestPayload = "aaa";
        
        Request putRequest = new PUTRequest();
        putRequest.setPayload(requestPayload);
        putRequest.setContentType(MediaTypeRegistry.TEXT_PLAIN);
        
        Response putResponse = executeRequest(putRequest, putResource, true);
        assertNotNull(putResponse);
        assertTrue(putResponse.getCode() == CodeRegistry.RESP_CHANGED);
        
        Request getRequest = new GETRequest();
        Response getResponse = executeRequest(getRequest, putResource, true);
        
        assertNotNull(getResponse);
        assertTrue(getResponse.getCode() == CodeRegistry.RESP_CONTENT);
        
        String responsePayload = getResponse.getPayloadString();
        assertTrue(responsePayload.equals(requestPayload));
    }
    
    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        
    }
    
    /**
     * Tear down.
     */
    @After
    public void tearDown() {
        // TODO
    }
    
    @Test
    public final void wrongAcceptContentGetTest() {
        String resource = "image";
        
        Request getRequest = new GETRequest();
        int acceptType = MediaTypeRegistry.VIDEO_RAW;
        getRequest.setAccept(acceptType);
        Response response = executeRequest(getRequest, resource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_NOT_ACCEPTABLE);
    }
    
    /**
     * To upper post wrong content type test.
     */
    @Test
    public final void wrongContentTypePostTest() {
        String postResource = "toUpper";
        String requestPayload = "aaa";
        
        Request postRequest = new POSTRequest();
        postRequest.setPayload(requestPayload.getBytes());
        postRequest.setContentType(MediaTypeRegistry.IMAGE_JPEG);
        
        Response response = executeRequest(postRequest, postResource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_UNSUPPORTED_MEDIA_TYPE);
    }
    
    /**
     * Time resource delete fail test.
     */
    @Test
    public final void wrongDeleteTest() {
        String resource = "image";
        
        Request getRequest = new DELETERequest();
        Response response = executeRequest(getRequest, resource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_METHOD_NOT_ALLOWED);
    }
    
    @Test
    public final void wrongGetQueryTest() {
        String resource = "query?";
        String parameter0 = "a=1";
        String parameter1 = "%";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource + parameter0 + "&" + parameter1,
                                           true);
        
        assertNotNull(response);
        // TODO check
        assertTrue(response.getCode() == CodeRegistry.RESP_BAD_OPTION);
    }
    
    @Test
    public final void wrongLocalResourceTest() {
        String resource = "inexistent";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, false);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_NOT_FOUND);
    }
    
    @Test
    public final void wrongMalformedResourceTest() {
        String resource = " 53!\"£$%&/()=";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_BAD_OPTION);
    }
    
    @Test
    public final void wrongMalformedUriServerTest() {
        String resource = "";
        String coapServer = "coap://localhost:a";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, true, coapServer);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_BAD_OPTION);
    }
    
    @Test
    public final void wrongMalformedUriServerTest2() {
        String resource = "";
        String coapServer = "coap:///localhost";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, true, coapServer);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_BAD_OPTION);
    }
    
    @Test
    public final void wrongMalformedUriServerTest3() {
        String resource = "";
        String coapServer = "localhost??";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, true, coapServer);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_BAD_OPTION);
    }
    
    /**
     * Time resource post fail test.
     */
    @Test
    public final void wrongPostTest() {
        String resource = "helloWorld";
        
        Request getRequest = new POSTRequest();
        Response response = executeRequest(getRequest, resource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_METHOD_NOT_ALLOWED);
    }
    
    /**
     * Time resource put fail test.
     */
    @Test
    public final void wrongPutTest() {
        String resource = "helloWorld";
        
        Request getRequest = new PUTRequest();
        Response response = executeRequest(getRequest, resource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_METHOD_NOT_ALLOWED);
    }
    
    @Test
    public final void wrongResourceTest() {
        String resource = "inexistent";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, true);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_NOT_FOUND);
    }
    
    @Test
    public final void wrongServerTest() {
        String resource = "inexistent";
        String coapServer = "coap://localhost:5685";
        
        Request getRequest = new GETRequest();
        Response response = executeRequest(getRequest, resource, true, coapServer);
        
        assertNotNull(response);
        assertTrue(response.getCode() == CodeRegistry.RESP_NOT_FOUND);
    }
    
    private Response executeRequest(Request request, String resource, boolean enableProxying) {
        return executeRequest(request, resource, enableProxying, SERVER_LOCATION);
    }
    
    /**
     * Execute request.
     * 
     * @param request the request
     * @param resource the resource
     * @param enableProxying the enable proxying
     * @return the response
     */
    private Response executeRequest(Request request, String resource, boolean enableProxying, String serverLocation) {
        String proxyResource = "proxy";
        
        // set the resource desired in the proxy-uri option or in the uri-path
        // depending if the proxying is enabled or not
        if (enableProxying) {
            Option proxyUriOption = new Option(serverLocation + "/" + resource,
                                               OptionNumberRegistry.PROXY_URI);
            request.setOption(proxyUriOption);
        } else {
            proxyResource = resource;
        }
        
        Option uriPathOption = new Option(proxyResource,
                                          OptionNumberRegistry.URI_PATH);
        request.setOption(uriPathOption);
        request.setURI(PROXY_LOCATION);
        
        // receive response
        Response response = forward(request);
        
        return response;
    }
    
    private Response forward(Request coapRequest) {
        // init
        HttpParams httpParams = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
        // TODO check the user agent
        HttpProtocolParams.setUserAgent(httpParams, "Mozilla/5.0");
        HttpProtocolParams.setUseExpectContinue(httpParams, true);
        
        HttpProcessor httpProcessor = new ImmutableHttpProcessor(new HttpRequestInterceptor[]
            {
                // Required protocol interceptors
                new RequestContent(), new RequestTargetHost(),
                // Recommended protocol interceptors
                new RequestConnControl(), new RequestUserAgent(), new RequestExpectContinue() });
        
        HttpRequestExecutor httpExecutor = new HttpRequestExecutor();
        
        HttpContext httpContext = new BasicHttpContext(null);
        
        Response coapResponse = null;
        
        // get the requested host
        // if the port is not specified, it returns -1, but it is coherent
        // with the HttpHost object
        HttpHost httpHost = new HttpHost(coapRequest.getPeerAddress().getAddress().getHostAddress());
        
        DefaultHttpClientConnection connection = new DefaultHttpClientConnection();
        ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
        
        httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, connection);
        httpContext.setAttribute(ExecutionContext.HTTP_TARGET_HOST, httpHost);
        
        // create the connection if not already active
        if (!connection.isOpen()) {
            // TODO edit the port based on the scheme chosen
            
            /* connection */
            try {
                Socket socket = new Socket(httpHost.getHostName(),
                                           httpHost.getPort() == -1 ? HTTP_PORT : httpHost
                                                           .getPort());
                connection.bind(socket, httpParams);
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        /* request */
        HttpRequest httpRequest = null;
        try {
            httpRequest = HttpTranslator.getHttpRequest(coapRequest);
            
            // DEBUG
            System.out.println(">> Request: " + httpRequest.getRequestLine());
            
            // preprocess the request
            httpRequest.setParams(httpParams);
            httpExecutor.preProcess(httpRequest, httpProcessor, httpContext);

        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TranslationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        /* response */
        try {
            // send the request
            HttpResponse httpResponse = httpExecutor.execute(httpRequest, connection, httpContext);
            httpResponse.setParams(httpParams);
            httpExecutor.postProcess(httpResponse, httpProcessor, httpContext);
            
            // DEBUG
            System.out.println("<< Response: " + httpResponse.getStatusLine());
            // the entity of the response, if non repeatable, could be
            // consumed only one time, so do not debug it!
            // System.out.println(EntityUtils.toString(httpResponse.getEntity()));
            
            // translate the received http response in a coap response
            coapResponse = HttpTranslator.getCoapResponse(httpResponse, coapRequest);
            
            // close the connection if not keepalive
            if (!connStrategy.keepAlive(httpResponse, httpContext)) {
                connection.close();
            } else {
                System.out.println("Connection kept alive...");
            }
            
            // }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create a new response: " + e.getMessage());
            return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
        } catch (ParseException e) {
            System.out.println("Failed to create a new request: " + e.getMessage());
            return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            System.out.println("Failed to create a new request: " + e.getMessage());
            return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
        } catch (HttpException e) {
            System.out.println("Failed to create a new request: " + e.getMessage());
            return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
        } catch (TranslationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (IOException e) {
            }
        }
        
        return coapResponse;
    }
    
}
