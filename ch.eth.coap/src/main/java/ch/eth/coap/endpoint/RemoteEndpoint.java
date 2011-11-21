package ch.eth.coap.endpoint;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import ch.eth.coap.coap.Request;
import ch.eth.coap.coap.Response;

public class RemoteEndpoint extends Endpoint {

	public static Endpoint fromURI(String uri) {
		try {
			return new RemoteEndpoint(new URI(uri));
		} catch (URISyntaxException e) {
			System.out.printf(
					"[%s] Failed to create RemoteEndpoint from URI: %s\n",
					"JCoAP", e.getMessage());
			return null;
		}
	}

	public RemoteEndpoint(URI uri) {

		this.communicator = Request.defaultCommunicator();
		this.communicator.registerReceiver(this);

		this.uri = uri;
	}

	@Override
	public void execute(Request request) throws IOException {

		if (request != null) {

			// set authority specific part of the request's URI

			String scheme = uri.getScheme();
			String authority = uri.getAuthority();
			String path = request.getURI() != null ? request.getURI().getPath()
					: uri.getPath();
			String query = request.getURI() != null ? request.getURI()
					.getQuery() : uri.getQuery();
			String fragment = request.getURI() != null ? request.getURI()
					.getFragment() : uri.getFragment();

			try {

				request.setURI(new URI(scheme, authority, path, query, fragment));

			} catch (URISyntaxException e) {

				System.out.printf("[%s] Failed to assign URI to request: %s\n",
						getClass().getName(), e.getMessage());
			}

			// execute the request
			request.execute();
		}

	}

	protected URI uri;

	@Override
	public void handleRequest(Request request) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleResponse(Response response) {
		// response.handle();
	}
}
