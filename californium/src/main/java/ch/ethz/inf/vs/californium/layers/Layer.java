/**
 * 
 */
package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;

/**
 * @author Francesco Corazza
 * 
 */
public interface Layer<T extends Message> extends MessageReceiver<T> {
    void registerReceiver(MessageReceiver<T> receiver);
    
    void sendMessage(T msg) throws IOException;
    
    void unregisterReceiver(MessageReceiver<T> receiver);
}
