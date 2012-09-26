/**
 * 
 */

package ch.ethz.inf.vs.californium.examples;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyHttpClientResource;

/**
 * @author Francesco Corazza
 * 
 */
public class TwitterResource extends LocalResource {
    
    private ProxyHttpClientResource httpClientResource;
    
    public TwitterResource(String resourceIdentifier) {
        super(resourceIdentifier);
    }
    
    @Override
    public void performGET(GETRequest request) {
        // lazy init
        if (httpClientResource == null) {
            synchronized (this) {
                if (httpClientResource == null) {
                    httpClientResource = new ProxyHttpClientResource();
                }
            }
        }
        
        String twitterResource = "http://search.twitter.com/search.json?q=%23CoAP&src=hash";
        Request twitterRequest = new Request(CodeRegistry.METHOD_GET);
        twitterRequest.setOption(new Option(twitterResource, OptionNumberRegistry.PROXY_URI));
        Response twitterResponse = httpClientResource.forwardRequest(twitterRequest);
        
        request.respond(twitterResponse);
    }
}
