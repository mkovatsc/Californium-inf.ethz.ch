/**
 * 
 */
package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.MessageReceiver;

/**
 * @author Francesco Corazza
 * 
 */
public interface Layer extends MessageReceiver {
    void registerReceiver(MessageReceiver receiver);
    
    void sendMessage(Message msg) throws IOException;
    
    void unregisterReceiver(MessageReceiver receiver);
}
