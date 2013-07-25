package ch.inf.vs.californium.perf.throughput;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class Test {
//
//    public static void main(String[] args) {
//	
//	
//	ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
//	
//	map.put("one", "one");
//	map.put("two", "two");
//	map.put("three", "three");
//	
////	for (String str:map.values())
////	    map.remove(str);
//	
//	System.out.println("done: "+map.toString());
//	
//    }
	public static void main(String[] args) throws Exception {
		
		URL url = new URL("http://localhost:80/test-app/hello");
		URLConnection con = url.openConnection();
		InputStream stream = con.getInputStream();
		try (Scanner s = new Scanner(stream).useDelimiter("\\A")) {
			String res = s.next();
			System.out.println(res);
		}
	}
}
