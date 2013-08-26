package ch.ethz.inf.vs.californium.intrapc;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.OptionSet;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.Exchange.Origin;
import ch.ethz.inf.vs.californium.network.RawData;

/**
 * This test computes the number of bytes occupied by a specific instance of a class.
 */
/**
 * http://www.java-tutorial.ch/core-java-tutorial/java-size-of-objects
 * 
 * Integer: 16 bytes
 * String 4char: 52 bytes
 * String 16char: 76 bytes
 * String 40char: 127 bytes
 * InetAddress IPv4: 32 bytes
 * InetAddress IPv6: 88 bytes
 * RawData IPv4 0B: 72 bytes
 * RawData IPv6 0B: 128 bytes
 * RawData IPv4 50B: 128 bytes
 * RawData IPv6 50B: 184 bytes
 * OptionSet empty: 88 bytes
 * Request no option, no addr: 112 bytes
 * Request with option, no addr: 200 bytes
 * Response no option, no addr: 104 bytes
 * Response with option, no addr: 192 bytes
 * Exchange no req/resp: 176 bytes
 * LinkedList: 32 bytes
 * ArrayList: 40 bytes
 * ConcurrentLinkedQueue: 48 bytes
 * CopyOnWriteArrayList: 88 bytes
 * ConcurrentHashMap empty: 224 bytes
 * ConcurrentHashMap with 100 IPv6: 24502 bytes
 * 	==> Key with 127 B, Exch with 64 B, Overhead with 60 B
 *
 *
 * old values:
 * OptionSet empty: 304 bytes
 * Request no option, no addr: 96 bytes
 * Request with option, no addr: 400 bytes
 * Response no option, no addr: 88 bytes
 * Response with option, no addr: 392 bytes
 */
@SuppressWarnings({"rawtypes", "unused"})
public class BenchmarkObjectSize {

    public static void main(String[] args) {
	sizeOfInteger();
	sizeOfString();
	sizeOfInetAddress();
	sizeOfEmptyRawData();
	sizeOf50ByteRawData();
	sizeOfOptionSet();
	sizeOfRequest();
	sizeOfResponse();
	sizeOfExchange();
	sizeOfList();
//	sizeOfConcurrentHashMapWith100Elements();
    }
    
    private static void sizeOfInteger() {
	SizeOfUtil.execute(new Instantiator<Integer>(Integer.class) {
	    public Integer execute() {
		return new Integer(3);
	    }
	});
    }
    
    private static void sizeOfString() {
	final Random rand = new Random();
	SizeOfUtil.execute(new Instantiator<String>(String.class, " 4char") {
	    public String execute() {
		return ""+(rand.nextLong()%(10000));
	    }
	});
	SizeOfUtil.execute(new Instantiator<String>(String.class, " localhost") {
	    public String execute() {
		return ""+(rand.nextLong()%(1000000000L));
	    }
	});
	SizeOfUtil.execute(new Instantiator<String>(String.class, " 16char") {
	    public String execute() {
		return ""+(rand.nextLong()%(10000000000L * 1000000L));
	    }
	});
	SizeOfUtil.execute(new Instantiator<String>(String.class, " 40char") {
	    public String execute() {
		return ""+(rand.nextLong()%(10000000000L))+(rand.nextLong()%(10000000000L))+(rand.nextLong()%(10000000000L))+(rand.nextLong()%(10000000000L));
	    }
	});
    }
    
    private static void sizeOfInetAddress() {
	SizeOfUtil.execute(new Instantiator<InetAddress>(InetAddress.class, " IPv4") {
	    public InetAddress execute() {
		return nextIPv4();
	    }
	});
	SizeOfUtil.execute(new Instantiator<InetAddress>(InetAddress.class, " IPv6") {
	    public InetAddress execute() {
		return nextIPv6();
	    }
	});
    }
    
    private static void sizeOfEmptyRawData() {
	SizeOfUtil.execute(new Instantiator<RawData>(RawData.class, " IPv4 0B") {
	    public RawData execute() {
		return new RawData(new byte[0], nextIPv4(), 0);
	    }
	});
	SizeOfUtil.execute(new Instantiator<RawData>(RawData.class, " IPv6 0B") {
	    public RawData execute() {
		return new RawData(new byte[0], nextIPv6(), 0);
	    }
	});
    }
    
    private static void sizeOf50ByteRawData() {
	SizeOfUtil.execute(new Instantiator<RawData>(RawData.class, " IPv4 50B") {
	    public RawData execute() {
		return new RawData(new byte[50], nextIPv4(), 0);
	    }
	});
	SizeOfUtil.execute(new Instantiator<RawData>(RawData.class, " IPv6 50B") {
	    public RawData execute() {
		return new RawData(new byte[50], nextIPv6(), 0);
	    }
	});
    }
    
    private static void sizeOfOptionSet() {
	SizeOfUtil.execute(new Instantiator<OptionSet>(OptionSet.class, " empty") {
	    public OptionSet execute() {
		return new OptionSet();
	    }
	});
	SizeOfUtil.execute(new Instantiator<OptionSet>(OptionSet.class, " with URI") {
	    public OptionSet execute() {
	    	Request request = new Request(Code.GET);
	    	request.setURI("coap://localhost:5683/ress");
	    	return request.getOptions();
	    }
	});
    }
    
    private static void sizeOfRequest() {
	SizeOfUtil.execute(new Instantiator<Request>(Request.class, " no option, no addr") {
	    public Request execute() {
		Request request = new Request(Code.POST);
		request.setToken(new byte[0]);
		request.setMID(77);
		return request;
	    }
	});
	SizeOfUtil.execute(new Instantiator<Request>(Request.class, " with option, no addr") {
	    public Request execute() {
		Request request = new Request(Code.POST);
		request.setToken(new byte[0]);
		request.setMID(77);
//		request.getOptions(); // lazy initialization
		request.setURI("coap://localhost:5683/ress");
		return request;
	    }
	});
    }
    
    private static void sizeOfResponse() {
	SizeOfUtil.execute(new Instantiator<Response>(Response.class, " no option, no addr") {
	    public Response execute() {
		Response response = new Response(ResponseCode.CONTENT);
		response.setToken(new byte[0]);
		response.setMID(77);
		return response;
	    }
	});
	SizeOfUtil.execute(new Instantiator<Response>(Response.class, " with option, no addr") {
	    public Response execute() {
		Response response = new Response(ResponseCode.CONTENT);
		response.setToken(new byte[0]);
		response.setMID(77);
		response.getOptions(); // lazy initialization
		return response;
	    }
	});
    }
    
    private static void sizeOfExchange() {
	SizeOfUtil.execute(new Instantiator<Exchange>(Exchange.class, " no req/resp") {
	    public Exchange execute() {
		Exchange exchange = new Exchange(null, Origin.LOCAL);
		return exchange;
	    }
	});
    }
    
    private static void sizeOfList() {
	SizeOfUtil.execute(new Instantiator<LinkedList>(LinkedList.class) {
	    public LinkedList<?> execute() {
		return new LinkedList();
	    }
	});
	SizeOfUtil.execute(new Instantiator<ArrayList>(ArrayList.class) {
		public ArrayList<?> execute() {
		return new ArrayList(0);
	    }
	});
	SizeOfUtil.execute(new Instantiator<ConcurrentLinkedQueue>(ConcurrentLinkedQueue.class) {
	    public ConcurrentLinkedQueue<?> execute() {
		return new ConcurrentLinkedQueue();
	    }
	});
	SizeOfUtil.execute(new Instantiator<CopyOnWriteArrayList>(CopyOnWriteArrayList.class) {
	    public CopyOnWriteArrayList<?> execute() {
		return new CopyOnWriteArrayList();
	    }
	});
    }
    
	private static void sizeOfConcurrentHashMapWith100Elements() {
	SizeOfUtil.execute(new Instantiator<ConcurrentHashMap>(ConcurrentHashMap.class, " empty") {
	    public ConcurrentHashMap execute() {
		return new ConcurrentHashMap<String, Exchange>();
	    }
	});
//	SizeOfUtil.execute(new Instantiator<ConcurrentHashMap>(ConcurrentHashMap.class, " with 100 IPv6") {
//	    public ConcurrentHashMap execute() {
//		ConcurrentHashMap<String, Exchange> map = new ConcurrentHashMap<String, Exchange>();
//		Random rand = new Random();
//		for (int i=0;i<100;i++) {
//		    String key = nextIPv6().getHostAddress()+":"+rand.nextInt(1<<16)+rand.nextInt(1<<16);
//		    Exchange value = new Exchange(null, false);
//		    map.put(key, value);
//		}
//		return map;
//	    }
//	});
    }
    
    private static long c4 = 0;
    private static InetAddress nextIPv4() {
	try {
	    byte[] b = new byte[4];
	    b[3] = (byte) (c4 & 0xFF);
	    b[2] = (byte) ((c4 >> 8) & 0xFF);
	    b[1] = (byte) ((c4 >> 16) & 0xFF);
	    b[0] = (byte) ((c4 >> 24) & 0xFF);
	    c4++;
	    if (c4==0) System.err.println("WARNING: LAST IPv4 IS USED");
	    return InetAddress.getByAddress(b);
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    private static long c6 = 0;
    private static InetAddress nextIPv6() {
	try {
    	byte[] b = new byte[16];
    	b[7] = b[15] = (byte) (c6 & 0xFF);
    	b[6] = b[14] = (byte) ((c6 >> 8) & 0xFF);
    	b[5] = b[13] = (byte) ((c6 >> 16) & 0xFF);
    	b[4] = b[12] = (byte) ((c6 >> 24) & 0xFF);
    	b[3] = b[11] = (byte) (c6 & 0xFF);
    	b[2] = b[10] = (byte) ((c6 >> 8) & 0xFF);
    	b[1] = b[ 9] = (byte) ((c6 >> 16) & 0xFF);
    	b[0] = b[ 8] = (byte) ((c6 >> 24) & 0xFF);
    	c6++;
    	return InetAddress.getByAddress(b);
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }
}

class SizeOfUtil {
	private static final Runtime runtime = Runtime.getRuntime();
	private static final int OBJECT_COUNT = 100000;

	/**
	 * Return the size of an object instantiated using the instantiator
	 * 
	 * @param instantiator
	 * @return byte size of the instantiate object
	 */ 	 
	static public int execute(Instantiator<?> instantiator) {
		runGarbageCollection();
		usedMemory();
		Object[] objects = new Object[OBJECT_COUNT + 1];
		long heapSize = 0;
		for (int i = 0; i < OBJECT_COUNT + 1; ++i) {
			Object object = instantiator.execute();
			if (i > 0)
				objects[i] = object;
			else {
				object = null;
				runGarbageCollection();
				heapSize = usedMemory();
			}
		}
		runGarbageCollection();
		long heap2 = usedMemory(); // Take an after heap snapshot:
		final int size = Math.round(((float) (heap2 - heapSize)) / OBJECT_COUNT);
		
		for (int i = 1; i < OBJECT_COUNT + 1; ++i)
			objects[i] = null;
		objects = null;
		String plus = instantiator.plus!=null?instantiator.plus:"";
		System.out.println(instantiator.clazz.getSimpleName()+plus+": "+size+" bytes");
		return size;
	}

	private static void runGarbageCollection() {
		for (int r = 0; r < 4; ++r){
			long usedMem1 = usedMemory();
			long usedMem2 = Long.MAX_VALUE;
			for (int i = 0; (usedMem1 < usedMem2) && (i < 500); ++i) {
				runtime.runFinalization();
				runtime.gc();
				Thread.yield();
				usedMem2 = usedMem1;
				usedMem1 = usedMemory();
			}
		}
	}

	private static long usedMemory() {
		return runtime.totalMemory() - runtime.freeMemory();
	}
}

abstract class Instantiator<T> {

    public Class<T> clazz;
    
    public String plus;
    
    public Instantiator(Class<T> clazz) {
	this(clazz, null);
    }
    
    public Instantiator(Class<T> clazz, String plus) {
	this.clazz = clazz;
	this.plus = plus;
    }

    abstract T execute();
}
