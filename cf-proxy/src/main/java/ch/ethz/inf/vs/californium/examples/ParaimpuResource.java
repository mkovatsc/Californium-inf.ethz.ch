/**
 * 
 */

package ch.ethz.inf.vs.californium.examples;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyCoapClientResource;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyHttpClientResource;

/**
 * @author Francesco Corazza
 * 
 */
public class ParaimpuResource extends LocalResource {
    
    private ProxyCoapClientResource coapClientResource;
    private ProxyHttpClientResource httpClientResource;
    
    public ParaimpuResource(String resourceIdentifier) {
        super("paraimpu");
    }
    
    @Override
    public void performGET(GETRequest request) {
        request.respond(CodeRegistry.RESP_CONTENT,
                        "POST:\n resource address\n paraimpu address\n\nDefault:\ncoap://[2001:620:8:101f:250:c2ff:ff18:8d32]:5683/sensors/temp\nhttp://paraimpu.crs4.it/data/new");
    }
    
    @Override
    public void performPOST(POSTRequest request) {
        // lazy init
        if (coapClientResource == null) {
            synchronized (this) {
                if (coapClientResource == null) {
                    coapClientResource = new ProxyCoapClientResource();
                }
            }
        }
        if (httpClientResource == null) {
            synchronized (this) {
                if (httpClientResource == null) {
                    httpClientResource = new ProxyHttpClientResource();
                }
            }
        }
        
        // set the parameters
        String[] content = request.getPayloadString().split("\n");
        String resourceAddress = content[0];
        String paraimpuAddress = content[1];
        
        // send the request to the coap resource
        Request resourceRequest = new Request(CodeRegistry.METHOD_GET);
        resourceRequest.setOption(new Option(resourceAddress, OptionNumberRegistry.PROXY_URI));
        Response resourceResponse = coapClientResource.forwardRequest(resourceRequest);
        
        if (resourceResponse == null) {
            request.respond(CodeRegistry.RESP_BAD_GATEWAY, resourceAddress + " have not answered",
                            MediaTypeRegistry.TEXT_PLAIN);
            return;
        }
        
        String resourceData = resourceResponse.getPayloadString();
        String payload =
                        "{\"token\":\"952e1839-77b4-4392-946a-84de72e10f77\", \"content-type\":\"text/plain\", \"data\":\""
                                        + resourceData + "\"}";
        
        // send the retrieved data to the paraimpu server
        Request paraimpuRequest = new Request(CodeRegistry.METHOD_POST);
        paraimpuRequest.setOption(new Option(paraimpuAddress, OptionNumberRegistry.PROXY_URI));
        paraimpuRequest.setPayload(payload);
        Response paraimpuResponse = httpClientResource.forwardRequest(paraimpuRequest);
        
        request.respond(paraimpuResponse);
    }
}
