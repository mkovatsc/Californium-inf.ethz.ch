/**
 * 
 */
package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.CoapMessage;

/**
 * @author Francesco Corazza
 * 
 */
public interface Layer extends MessageReceiver {
    void registerReceiver(MessageReceiver receiver);
    
    void sendMessage(CoapMessage msg) throws IOException;
    
    void unregisterReceiver(MessageReceiver receiver);
}
