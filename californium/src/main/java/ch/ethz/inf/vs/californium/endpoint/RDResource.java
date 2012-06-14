/**
 * 
 */
package ch.ethz.inf.vs.californium.endpoint;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;

/**
 * The resource is the context of the Resource Directory
 * 
 * @author Francesco Corazza
 * 
 */
public class RDResource extends LocalResource {
    public RDResource() {
        super("rd");
        setTitle("GET/POST available resources");
        setResourceType("ResourceDirectory");
        isObservable(false);
    }
    
    @Override
    public void performGET(GETRequest request) {
        request.respond(CodeRegistry.RESP_NOT_IMPLEMENTED);
    }
    
    @Override
    public void performPOST(POSTRequest request) {
        request.respond(CodeRegistry.RESP_NOT_IMPLEMENTED);
    }
    
    private void lookup(String resourceKey) {
        // TODO Auto-generated method stub
        
    }
    
    private String registerResource(Resource resource) {
        // TODO Auto-generated method stub
        
        return "";
    }
    
    private void removeResource(String resourceKey) {
        // TODO Auto-generated method stub
        
    }
    
    private void updateResource(String resourceKey) {
        // TODO Auto-generated method stub
        
    }
    
    private void validateResource(String resourceKey) {
        // TODO Auto-generated method stub
        
    }
    
}
