package ch.inf.vs.californium.coap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	private BlockOption  block1;
	private BlockOption  block2;
	
	// TODO: Opbserver option
	
	// Arbitrary options
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
		max_age             = null;
		uri_query_list      = new LinkedList<String>();
		accept              = null;
		location_query_list = new LinkedList<String>();
		proxy_uri           = null;
		proxy_scheme        = null;
		block1              = null;
		block2              = null;
	}
	
	public List<byte[]> getIfMatchs() {
		return if_match_list;
	}
	
	public int getIfMatchCount() {
		return if_match_list.size();
	}
	
	public OptionSet addIfMatch(byte[] opaque) {
		if (opaque==null)
			throw new IllegalArgumentException("If-Match option must not be null");
		if (opaque.length > 8)
			throw new IllegalArgumentException("Content of If-Match option is too large");
		if_match_list.add(opaque);
		return this;
	}
	
	public OptionSet removeIfMatch(byte[] opaque) {
		if_match_list.remove(opaque);
		return this;
	}
	
	public OptionSet clearIfMatchs() {
		if_match_list.clear();
		return this;
	}
	
	public String getURIHost() {
		return uri_host;
	}
	
	public boolean hasURIHost() {
		return uri_host != null;
	}
	
	public OptionSet setURIHost(String host) {
		if (host==null)
			throw new NullPointerException("URI-Host must not be null");
		if (host.length() < 1 || 255 < host.length())
			throw new IllegalArgumentException("URI-Host option's length must be between 1 and 255 inclusive");
		this.uri_host = host;
		return this;
	}
	
	public List<byte[]> getETags() {
		return etag_list;
	}
	
	public int getETagCount() {
		return etag_list.size();
	}
	
	public OptionSet addETag(byte[] opaque) {
		if (opaque==null)
			throw new IllegalArgumentException("ETag option must not be null");
		if (opaque.length < 1 || 8 < opaque.length)
			throw new IllegalArgumentException("ETag option's length must be between 1 and 8 inclusive");
		etag_list.add(opaque);
		return this;
	}
	
	public OptionSet removeETag(byte[] opaque) {
		etag_list.remove(opaque);
		return this;
	}
	
	public OptionSet cleatETags() {
		etag_list.clear();
		return this;
	}
	
	public boolean hasIfNoneMatch() {
		return if_none_match;
	}
	
	public OptionSet setIfNoneMatch(boolean b) {
		if_none_match = b;
		return this;
	}
	
	public Integer getURIPort() {
		return uri_port;
	}
	
	public boolean hasURIPort() {
		return uri_port != null;
	}
	
	public OptionSet setURIPort(int port) {
		if (port < 0 || (1<<16)-1 < port)
			throw new IllegalArgumentException("URI port option must be between 0 and "+((1<<16)-1)+" (2 bytes) inclusive");
		uri_port = port;
		return this;
	}
	
	public OptionSet removeURIPort() {
		uri_port = null;
		return this;
	}
	
	public List<String> getLocationPaths() {
		return location_path_list;
	}
	
	public int getLocationPathCount() {
		return location_path_list.size();
	}
	
	public OptionSet addLocationPath(String path) {
		if (path == null)
			throw new IllegalArgumentException("Location path option must not be null");
		if (path.length() > 255)
			throw new IllegalArgumentException("Location path option's length must be between 0 and 255 inclusive");
		location_path_list.add(path);
		return this;
	}
	
	public OptionSet removeLocationPath(String path) {
		location_path_list.remove(path);
		return this;
	}
	
	public OptionSet clearLocationPaths() {
		location_path_list.clear();
		return this;
	}
	
	public List<String> getURIPaths() {
		return uri_path_list;
	}
	
	public int getURIPathCount() {
		return uri_path_list.size();
	}
	
	public OptionSet addURIPath(String path) {
		if (path == null)
			throw new IllegalArgumentException("URI path option must not be null");
		if (path.length() > 255)
			throw new IllegalArgumentException("URI path option's length must be between 0 and 255 inclusive");
		uri_path_list.add(path);
		return this;
	}
	
	public OptionSet removeURIPath(String path) {
		uri_path_list.remove(path);
		return this;
	}
	
	public OptionSet clearURIPaths() {
		uri_path_list.clear();
		return this;
	}
	
	public Integer getContentFormat() {
		return content_format;
	}
	
	public boolean hasContentFormat() {
		return content_format != null;
	}
	
	public OptionSet setContentFormat(int format) {
		content_format = format;
		return this;
	}
	
	public OptionSet removeContentFormat() {
		content_format = null;
		return this;
	}
	
	public Long getMaxAge() {
		return max_age;
	}
	
	// Remember that the absence of a Max-Age option means its
	// default value DEFAULT_MAX_AGE (60L).
	public boolean hasMaxAge() {
		return max_age != null;
	}
	
	public OptionSet setMaxAge(long age) {
		if (age < 0 || ((1L<<32)-1) < age)
			throw new IllegalArgumentException("Max-Age option must be between 0 and "+((1L<<32)-1)+" (4 bytes) inclusive");
		max_age = age;
		return this;
	}
	
	public OptionSet removeMaxAge() {
		max_age = null;
		return this;
	}
	
	public List<String> getURIQueries() {
		return uri_query_list;
	}
	
	public int getURIQueryCount() {
		return uri_query_list.size();
	}
	
	public OptionSet addURIQuery(String query) {
		if (query == null)
			throw new NullPointerException("URI-Query option must not be null");
		if (query.length() > 255)
			throw new IllegalArgumentException("URI-Qurty option's length must be between 0 and 255 inclusive");
		uri_query_list.add(query);
		return this;
	}
	
	public OptionSet removeURIQuery(String query) {
		uri_query_list.remove(query);
		return this;
	}
	
	public OptionSet clearURIQuery() {
		uri_query_list.clear();
		return this;
	}
	
	public Integer getAccept() {
		return accept;
	}
	
	public boolean hasAccept() {
		return accept != null;
	}
	
	public OptionSet setAccept(int acc) {
		if (acc < 0 || acc > ((1<<16)-1))
			throw new IllegalArgumentException("Accept option must be between 0 and "+((1<<16)-1)+" (2 bytes) inclusive");
		accept = acc;
		return this;
	}
	
	public OptionSet removeAccept() {
		accept = null;
		return this;
	}
	
	public List<String> getLocationQueries() {
		return location_query_list;
	}
	
	public int getLocationQueryCount() {
		return location_query_list.size();
	}
	
	public OptionSet addLocationQuery(String query) {
		if (query == null)
			throw new NullPointerException("Location Query option must not be null");
		if (query.length() > 255)
			throw new IllegalArgumentException("Location Query option's length must be between 0 and 255 inclusive");
		location_query_list.add(query);
		return this;
	}
	
	public OptionSet removeLocationQuery(String query) {
		location_query_list.remove(query);
		return this;
	}
	
	public OptionSet clearLocationQuery() {
		location_query_list.clear();
		return this;
	}
	
	public String getProxyURI() {
		return proxy_uri;
	}
	
	public boolean hasProxyURI() {
		return proxy_uri != null;
	}
	
	public OptionSet setProxyURI(String uri) {
		if (uri == null)
			throw new NullPointerException("Proxy URI option must not be null");
		if (uri.length() < 1 || 1034 < uri.length())
			throw new IllegalArgumentException();
		proxy_uri = uri;
		return this;
	}
	
	public OptionSet removeProxyURI() {
		proxy_uri = null;
		return this;
	}
	
	public String getProxyScheme() {
		return proxy_scheme;
	}
	
	public boolean hasProxyScheme() {
		return proxy_scheme != null;
	}
	
	public OptionSet setProxyScheme(String scheme) {
		if (scheme == null)
			throw new NullPointerException("Proxy Scheme option must not be null");
		if (scheme.length() < 1 || 255 < scheme.length())
			throw new IllegalArgumentException("Proxy Scheme option's length must be between 1 and 255 inclusive");
		proxy_scheme = scheme;
		return this;
	}
	
	public OptionSet clearProxyScheme() {
		proxy_scheme = null;
		return this;
	}
	
	public BlockOption getBlock1() {
		return block1;
	}
	
	public boolean hasBlock1() {
		return block1 != null;
	}

	public void setBlock1(int szx, boolean m, int num) {
		this.block1 = new BlockOption(szx, m, num);
	}
	
	public void setBlock1(byte[] value) {
		this.block1 = new BlockOption(value);
	}
	
	public void setBlock1(BlockOption block1) {
		this.block1 = block1;
	}
	
	public void removeBlock1() {
		this.block1 = null;
	}

	public BlockOption getBlock2() {
		return block2;
	}
	
	public boolean hasBlock2() {
		return block2 != null;
	}

	public void setBlock2(int szx, boolean m, int num) {
		this.block2 = new BlockOption(szx, m, num);
	}
	
	public void setBlock2(byte[] value) {
		this.block2 = new BlockOption(value);
	}
	
	public void setBlock2(BlockOption block2) {
		this.block2 = block2;
	}
	
	public void removeBlock2() {
		this.block2 = null;
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
	public List<Option> asSortedList() {
		ArrayList<Option> options = new ArrayList<>();
		for (byte[] value:if_match_list)
			options.add(new Option(CoAP.OptionRegistry.IF_MATCH, value));
		if (hasURIHost())
			options.add(new Option(CoAP.OptionRegistry.URI_HOST, getURIHost()));
		for (byte[] value:etag_list)
			options.add(new Option(CoAP.OptionRegistry.ETAG, value));
		if (hasIfNoneMatch())
			options.add(new Option(CoAP.OptionRegistry.IF_NONE_MATCH));
		if (hasURIPort())
			options.add(new Option(CoAP.OptionRegistry.URI_PORT, getURIPort()));
		for (String str:location_path_list)
			options.add(new Option(CoAP.OptionRegistry.LOCATION_PATH, str));
		for (String str:uri_path_list)
			options.add(new Option(CoAP.OptionRegistry.URI_PATH, str));
		if (hasContentFormat())
			options.add(new Option(CoAP.OptionRegistry.CONTENT_TYPE, getContentFormat()));
		if (hasMaxAge())
			options.add(new Option(CoAP.OptionRegistry.MAX_AGE, getMaxAge()));
		for (String str:uri_query_list)
			options.add(new Option(CoAP.OptionRegistry.URI_QUERY, str));
		if (hasAccept())
			options.add(new Option(CoAP.OptionRegistry.ACCEPT, getAccept()));
		for (String str:getLocationQueries())
			options.add(new Option(CoAP.OptionRegistry.LOCATION_QUERY, str));
		if (hasProxyURI())
			options.add(new Option(CoAP.OptionRegistry.PROXY_URI, getProxyURI()));
		if (hasProxyScheme())
			options.add(new Option(CoAP.OptionRegistry.PROXY_SCHEME, getProxyScheme()));
		
		if (hasBlock1())
			options.add(new Option(CoAP.OptionRegistry.BLOCK1, getBlock1().getValue()));
		if (hasBlock2())
			options.add(new Option(CoAP.OptionRegistry.BLOCK2, getBlock2().getValue()));
		
		// TODO: observer

		options.addAll(others);

		Collections.sort(options);
		return options;
	}

	// Arbitrary or CoAP defined option
	public OptionSet addOption(Option o) {
		others.add(o);
		return this;
	}
	
	@Override
	public String toString() {
		List<String> os = new ArrayList<>();
		if (getIfMatchCount() > 0)
			os.add("If-Match="+toHexString(if_match_list));
		if (hasURIHost())
			os.add("URI-Host="+uri_host);
		if (getETagCount() > 0)
			os.add("ETag="+toHexString(etag_list));
		if (hasIfNoneMatch())
			os.add("If-None-Match="+toHexString(if_match_list));
		if (hasURIPort())
			os.add("URI-Port="+uri_port);
		if (getLocationPathCount() > 0)
			os.add("Location-Path="+Arrays.toString(location_path_list.toArray()));
		if (getURIPathCount() > 0)
			os.add("URI-Path="+Arrays.toString(uri_path_list.toArray()));
		if (hasContentFormat())
			os.add("Content-Format="+content_format);
		if (hasMaxAge() /*&& max_age != CoAP.OptionRegistry.Default.MAX_AGE*/)
			os.add("Max-Age="+max_age);
		if (getURIQueryCount() > 0)
			os.add("URI-Query="+Arrays.toString(uri_query_list.toArray()));
		if (hasAccept())
			os.add("Accept="+accept);
		if (getLocationQueryCount() > 0)
			os.add("Location-Query="+Arrays.toString(location_query_list.toArray()));
		if (hasProxyURI())
			os.add("Proxy-URI="+proxy_uri);
		if (hasProxyScheme())
			os.add("Proxy-Scheme="+proxy_scheme);

		if (hasBlock1())
			os.add("Block1="+block1);
		if (hasBlock2())
			os.add("Block2="+block2);
		
		// TODO: observer
		
		for (Option o:others)
			os.add(o.toString());
		
		return "OptionSet="+Arrays.toString(os.toArray());
	}
		
	private String toHexString(List<byte[]> list) {
		List<String> hexs = new ArrayList<>(list.size());
		for (byte[] bytes:list)
			hexs.add(toHexString(bytes));
		return Arrays.toString(hexs.toArray());
	}
	
	private String toHexString(byte[] bytes) {
		   StringBuilder sb = new StringBuilder();
		   for(byte b:bytes)
		      sb.append(String.format("%02x", b & 0xFF));
		   return sb.toString();
	}
}
