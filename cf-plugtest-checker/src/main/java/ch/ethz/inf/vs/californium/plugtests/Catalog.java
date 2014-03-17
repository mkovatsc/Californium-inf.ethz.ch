package ch.ethz.inf.vs.californium.plugtests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.reflections.Reflections;

import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * A catalog with all tests
 */
public class Catalog {
	

	public static final Class<?> PLUGTEST_2_SUPERCLASS = TestClientAbstract.class;
	
	private HashMap<String, Class<?>> catalog;
	
	public Catalog() {
		this.catalog = new HashMap<String, Class<?>>();
		loadSubclasses(PLUGTEST_2_SUPERCLASS);
	}
	
	public void loadSubclasses(Class<?> superclass) {
		Reflections reflections = new Reflections("ch.ethz.inf.vs.californium");
		for (Class<?> clazz:reflections.getSubTypesOf(superclass))
			loadClass(clazz);
	}
	
	public void loadClass(Class<?> clazz) {
		catalog.put(clazz.getSimpleName(), clazz);
	}
	
	public Class<?> getTestClass(String name) {
		return catalog.get(name);
	}
	
	public List<Class<?>> getTestsClasses(String... names) {
		if (names.length==0) names = new String[] {".*"};
		
		List<Class<?>> list = new ArrayList<Class<?>>();
		for (Entry<String, Class<?>> entry:catalog.entrySet()) {
			for (String name:names) {
				String regex = name.replace("*", ".*");
				if (entry.getKey().matches(regex))
					list.add(entry.getValue());
			}
		}
		return list;
	}
	
	public List<String> getAllTestNames() {
		ArrayList<String> list = new ArrayList<String>(catalog.keySet());
		Collections.sort(list);
		return list;
	}
	
	// Old stuff, TODO to remove it when really no longer used
//	public static final String JAVA_CLASS_SUFFIX = ".class";
//	
//	public static final String PATH = "target/classes/";
//	public static final String PLUGTEST_2 = "ch.ethz.inf.vs.californium.examples.plugtest2";
//	public static final String PLUGTEST_3 = "ch.ethz.inf.vs.californium.examples.plugtest3";
//	/*
//	 * This method does not work when packed in a jar.
//	 * TODO: Make it work in jars.
//	 */
//	public void loadClasses(String path, String pckg) {
//		System.out.println("Catalog load package "+pckg);
//		try {
//			URL url = getClass().getResource("/"+pckg.replace(".", "/"));
//			if (url == null) {
//				System.out.println("No classes in package "+pckg);
//				return;
//			}
//			File file = new File(url.toURI());
//			System.out.println("Load file "+file.getAbsolutePath()+", exists: "+file.exists());
//			for (File f:file.listFiles()) {
//				String filename = f.getName();
//				// If file is proper top level class
//				if (filename.endsWith(JAVA_CLASS_SUFFIX) && !filename.contains("$")) {
//					try {
//						String name = filename.replace(JAVA_CLASS_SUFFIX, "");
//						Class<?> clazz = Class.forName(pckg+"."+name);
//						catalog.put(name, clazz);
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
