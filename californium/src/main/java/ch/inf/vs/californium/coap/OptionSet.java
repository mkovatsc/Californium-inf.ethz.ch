package ch.inf.vs.californium.coap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OptionSet {

	public static final Long DEFAULT_MAX_AGE = 60L;
	
	private List<byte[]> if_match_list;
	private String       uri_host;
	private List<byte[]> etag_list;
	private boolean      if_none_match; // true if option is set
	private Integer      uri_port; // null if no port is explicitly defined
	private List<String> location_path_list;
	private List<String> uri_path_list;
	private Integer      content_format;
	private Long         max_age; // (0-4 bytes)
	private List<String> uri_query_list;
	private Integer      accept;
	private List<String> location_query_list;
	private String       proxy_uri;
	private String       proxy_scheme;
	// TODO: Blockoption & Opbserver option
	
	// TODO: Arbitrary options
	private List<Option> others = new LinkedList<>();
	
	// TODO: When receiving, uri_host/port should be those from the sender 
	public OptionSet() {
		if_match_list       = new LinkedList<byte[]>();
		uri_host            = null; // from sender
		etag_list           = new LinkedList<byte[]>();
		if_none_match       = false;
		uri_port            = null; // from sender
		location_path_list  = new LinkedList<String>();
		uri_path_list       = new LinkedList<String>();
		content_format      = null;
		max_age             = DEFAULT_MAX_AGE;
		uri_query_list      = new LinkedList<String>();
		accept              = null;
		location_query_list = new LinkedList<String>();
		proxy_uri           = null;
		proxy_scheme        = null;
	}
	
	public List<byte[]> getIfMatchs() {
		return if_match_list;
	}
	
	public int getIfMatchCount() {
		return if_match_list.size();
	}
	
	public void addIfMatch(byte[] opaque) {
		if (opaque==null)
			throw new IllegalArgumentException("If-Match option must not be null");
		if (opaque.length > 8)
			throw new IllegalArgumentException("Content of If-Match option is too large");
		if_match_list.add(opaque);
	}
	
	public void removeIfMatch(byte[] opaque) {
		if_match_list.remove(opaque);
	}
	
	public void clearIfMatchs() {
		if_match_list.clear();
	}
	
	public String getURIHost() {
		return uri_host;
	}
	
	public void setURIHOST(String host) {
		if (host==null)
			throw new NullPointerException("URI-Host must not be null");
		if (host.length() < 1 || 255 < host.length())
			throw new IllegalArgumentException("URI-Host option's length must be between 1 and 255 inclusive");
		this.uri_host = host;
	}
	
	public List<byte[]> getETag() {
		return etag_list;
	}
	
	public int getETagCount() {
		return etag_list.size();
	}
	
	public void addETag(byte[] opaque) {
		if (opaque==null)
			throw new IllegalArgumentException("ETag option must not be null");
		if (opaque.length < 1 || 8 < opaque.length)
			throw new IllegalArgumentException("ETag option's length must be between 1 and 8 inclusive");
		etag_list.add(opaque);
	}
	
	public void removeETag(byte[] opaque) {
		etag_list.remove(opaque);
	}
	
	public void cleatETags() {
		etag_list.clear();
	}
	
	public boolean hasIfNoneMatch() {
		return if_none_match;
	}
	
	public void seIfNoneMatch(boolean b) {
		if_none_match = b;
	}
	
	public Integer getURIPort() {
		return uri_port;
	}
	
	public boolean hasURIPort() {
		return uri_port != null;
	}
	
	public void setURIPort(int port) {
		if (port < 0 || 1<<16-1 < port)
			throw new IllegalArgumentException("URI port option must be between 0 and "+(1<<16-1)+" (2 bytes) inclusive");
		uri_port = port;
	}
	
	public void removeURIPort() {
		uri_port = null;
	}
	
	public List<String> getLocationPaths() {
		return location_path_list;
	}
	
	public int getLocationPathCount() {
		return location_path_list.size();
	}
	
	public void addLocationPath(String path) {
		if (path == null)
			throw new IllegalArgumentException("Location path option must not be null");
		if (path.length() > 255)
			throw new IllegalArgumentException("Location path option's length must be between 0 and 255 inclusive");
		location_path_list.add(path);
	}
	
	public void removeLocationPath(String path) {
		location_path_list.remove(path);
	}
	
	public void clearLocationPaths() {
		location_path_list.clear();
	}
	
	public List<String> getURIPaths() {
		return uri_path_list;
	}
	
	public int getURIPathCount() {
		return uri_path_list.size();
	}
	
	public void addURIPath(String path) {
		if (path == null)
			throw new IllegalArgumentException("URI path option must not be null");
		if (path.length() > 255)
			throw new IllegalArgumentException("URI path option's length must be between 0 and 255 inclusive");
		uri_path_list.add(path);
	}
	
	public void removeURIPath(String path) {
		uri_path_list.remove(path);
	}
	
	public void clearURIPaths() {
		uri_path_list.clear();
	}
	
	public Integer getContentFormat() {
		return content_format;
	}
	
	public boolean hasContentFormat() {
		return content_format != null;
	}
	
	public void setContentFormat(int format) {
		content_format = format;
	}
	
	public void removeContentFormat() {
		content_format = null;
	}
	
	public Long getMaxAge() {
		return max_age;
	}
	
	public boolean hasMaxAge() {
		return max_age != null;
	}
	
	public void setMaxAge(long age) {
		if (age < 0 || (1L<<32-1) < age)
			throw new IllegalArgumentException("Max-Age option must be between 0 and "+(1L<<32-1)+" (4 bytes) inclusive");
		max_age = age;
	}
	
	public void removeMaxAge() {
		max_age = null;
	}
	
	public List<String> getURIQueries() {
		return uri_query_list;
	}
	
	public int getURIQueryCount() {
		return uri_query_list.size();
	}
	
	public void addURIQuery(String query) {
		if (query == null)
			throw new NullPointerException("URI-Query option must not be null");
		if (query.length() > 255)
			throw new IllegalArgumentException("URI-Qurty option's length must be between 0 and 255 inclusive");
		uri_query_list.add(query);
	}
	
	public void removeURIQuery(String query) {
		uri_query_list.remove(query);
	}
	
	public void clearURIQuery() {
		uri_query_list.clear();
	}
	
	public Integer getAccept() {
		return accept;
	}
	
	public boolean hasAccept() {
		return accept != null;
	}
	
	public void setAccept(int acc) {
		if (acc < 0 || acc > (1<<16-1))
			throw new IllegalArgumentException("Accept option must be between 0 and "+(1<<16-1)+" (2 bytes) inclusive");
	}
	
	public void removeAccept() {
		accept = null;
	}
	
	public List<String> getLocationQueries() {
		return location_query_list;
	}
	
	public int getLocationQueryCount() {
		return location_query_list.size();
	}
	
	public void addLocationQuery(String query) {
		if (query == null)
			throw new NullPointerException("Location Query option must not be null");
		if (query.length() > 255)
			throw new IllegalArgumentException("Location Query option's length must be between 0 and 255 inclusive");
		location_query_list.add(query);
	}
	
	public void removeLocationQuery(String query) {
		location_query_list.remove(query);
	}
	
	public void clearLocationQuery() {
		location_query_list.clear();
	}
	
	public String getProxyURI() {
		return proxy_uri;
	}
	
	public boolean hasProxyURI() {
		return proxy_uri != null;
	}
	
	public void setProxyURI(String uri) {
		if (uri == null)
			throw new NullPointerException("Proxy URI option must not be null");
		if (uri.length() < 1 || 1034 < uri.length())
			throw new IllegalArgumentException();
		proxy_uri = uri;
	}
	
	public void removeProxyURI() {
		proxy_uri = null;
	}
	
	public String getProxyScheme() {
		return proxy_scheme;
	}
	
	public boolean hasProxyScheme() {
		return proxy_scheme != null;
	}
	
	public void setProxyScheme(String scheme) {
		if (scheme == null)
			throw new NullPointerException("Proxy Scheme option must not be null");
		if (scheme.length() < 1 || 255 < scheme.length())
			throw new IllegalArgumentException("Proxy Scheme option's length must be between 1 and 255 inclusive");
		proxy_scheme = scheme;
	}
	
	public void clearProxyScheme() {
		proxy_scheme = null;
	}
	
	public boolean hasOption(int number) {
		// TODO: arbitrary or CoAP defined option
		throw new RuntimeException("Not implemented yet");
	}
	
	public List<Option> getOptions(int number) {
		// TODO: arbitrary or CoAP defined option
		throw new RuntimeException("Not implemented yet");
	}
	
	/**
	 * Returns all options in a sorted list
	 * @return
	 */
	public List<Option> getOptions() {
		List<Option> options = new ArrayList<>(others);
		if (getIfMatchCount()>0)
			for (byte[] value:if_match_list)
				options.add(Option.newIfMatch(value));
		// TODO
		
		return options;
	}
	
	public void addOption(Option o) {
		// TODO: arbitrary or CoAP defined option
		throw new RuntimeException("Not implemented yet");
	}
	
}
