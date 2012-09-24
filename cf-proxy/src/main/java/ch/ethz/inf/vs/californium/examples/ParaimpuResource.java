/**
 * 
 */

package ch.ethz.inf.vs.californium.examples;

import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyCoapClientResource;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyHttpClientResource;

/**
 * @author Francesco Corazza
 * 
 */
public class ParaimpuResource extends LocalResource {
    
    private static final ProxyCoapClientResource coapClientResource = new ProxyCoapClientResource();
    private static final ProxyHttpClientResource httpClientResource = new ProxyHttpClientResource();
    
    public ParaimpuResource(String resourceIdentifier) {
        super("paraimpu");
    }
    
    @Override
    public void performPOST(POSTRequest request) {
        // // set the parameters
        // String[] content = request.getPayloadString().split("\n");
        // String resourceAddress = content[0];
        // String paraimpuAddress = content[1];
        //
        // // send the request to the coap resource
        // coapClientResource.performPOST(request)
        // Response resourceResponse = getResponse(resourceAddress);
        // if(resourceResponse == null) {
        // request.respond(CodeRegistry.RESP_BAD_GATEWAY, resourceAddress + " have not answered",
        // MediaTypeRegistry.TEXT_PLAIN);
        // }
        // String resourceData = resourceResponse.getPayloadString();
        //
        // // send the retrieved data to the paraimpuServer
        
    }
    
}
