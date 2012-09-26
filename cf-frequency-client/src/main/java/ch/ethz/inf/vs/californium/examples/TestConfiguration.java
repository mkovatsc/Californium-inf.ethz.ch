/**
 * 
 */

package ch.ethz.inf.vs.californium.examples;

import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

/**
 * @author Francesco Corazza
 * 
 */
public class TestConfiguration {
    /** PARAMS */
    /** The seconds of test. */
    private final int secondsOfTest = 20;
    /** The requests per second. */
    private final int requestsPerSecond = 100;
    
    /** The send burst. */
    private final boolean burst = false;
    
    /** The test proxy. */
    private final boolean enableProxying = false;
    
    /** RESOURCES */
    /** The uri of the proxy. */
    private final String proxyAddress = "coap://localhost/proxy";
    
    /** The proxy uri option. */
    private final Option proxyUriOption = new Option("coap://localhost:5684/timeResource",
                    OptionNumberRegistry.PROXY_URI);
    
    public TestConfiguration(String[] params) {
        
    }
    
    /**
     * @return the proxyAddress
     */
    public String getProxyAddress() {
        return proxyAddress;
    }
    
    /**
     * @return the proxyUriOption
     */
    public Option getProxyUriOption() {
        return proxyUriOption;
    }
    
    /**
     * @return the requestsPerSecond
     */
    public int getRequestsPerSecond() {
        return requestsPerSecond;
    }
    
    /**
     * @return the secondsOfTest
     */
    public int getSecondsOfTest() {
        return secondsOfTest;
    }
    
    /**
     * @return the burst
     */
    public boolean isBurst() {
        return burst;
    }
    
    /**
     * @return the enableProxying
     */
    public boolean isEnableProxying() {
        return enableProxying;
    }
    
}
