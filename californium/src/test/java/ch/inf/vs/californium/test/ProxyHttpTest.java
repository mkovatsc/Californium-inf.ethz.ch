package ch.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.EndpointManager;
import ch.inf.vs.californium.resources.proxy.ProxyHttpClientResource;

public class ProxyHttpTest {

	private static final int PROXY_PORT = 7778;
	private static final String PROXY = "proxy";
	
	private static final String TARGET = "http://lantersoft.ch/robots.txt";

	private Server server_proxy;
	
	@Before
	public void setupServers() {
		try {
			System.out.println("\nStart "+getClass().getSimpleName());
			EndpointManager.clear();
			
			server_proxy = new Server(PROXY_PORT);
			server_proxy.add(new ProxyHttpClientResource(PROXY));
			server_proxy.start();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@After
	public void shutdownServer() {
		try {
			server_proxy.destroy();
			System.out.println("End "+getClass().getSimpleName());
			Thread.sleep(100);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Test
	public void test() throws Exception {
		Request request = new Request(Code.GET);
		request.setURI("coap://localhost:"+PROXY_PORT + "/" + PROXY);
		request.getOptions().setProxyURI(TARGET);
		request.send();
		
		Response response = request.waitForResponse(1000);
		assertNotNull(response);
		
		String payload = response.getPayloadString().trim();
		String expected = getExpectedResponse().trim();
		assertEquals(payload, expected);
	}
	
	private String getExpectedResponse() throws Exception {
		URL url;
		HttpURLConnection connection = null;
		try {
			// Create connection
			url = new URL(TARGET);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
//			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//
//			connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
//			connection.setRequestProperty("Content-Language", "en-US");

			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes("");
			wr.flush();
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r').append('\n');
			}
			rd.close();
			return response.toString();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;

		} finally {
			if (connection != null) {
				try {
					connection.disconnect();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
