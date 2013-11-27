package ch.ethz.inf.vs.californium;

import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * CalifonriumLogger is a helper class for the logging in Californium.
 * CaliforniumLogger makes sure that {@link #initializeLogger()} initializes the
 * loggers before use so that they print in the appropriate format.
 */
public class CalifonriumLogger {
	
	/**
	 * Gets the logger for the specified class. If the calling class is not the
	 * same as the class provided, an info is printed to the logger saying, that
	 * another class uses the logger of the specified class.
	 * 
	 * @param clazz
	 *            the class which's logger is desired
	 * @return the logger
	 * @deprecated Classes that need to log something should instead use the
	 *             standard JDK {@link LogManager#getLogger(String)} method to
	 *             obtain a logger. The JDK logging framework can then be
	 *             configured to use the {@link CaliforniumFormatter} (see JDK
	 *             JavaDocs of java.util.logging.LogManager and
	 *             java.util.logging.Handler for details)
	 */
	public static Logger getLogger(Class<?> clazz) {
		if (clazz == null) throw new NullPointerException();
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		Logger logger = Logger.getLogger(clazz.getName());
		String caller = trace[2].getClassName();
		if (!caller.equals(clazz.getName())) {
			logger.info(String.format("Note that class [%s] uses the logger of class [%s]", caller, clazz.getName()));
		}
		return logger;
	}
	
	public static void printLoggerFormat() {
		CalifonriumLogger.getLogger(CalifonriumLogger.class).info("Logging format: Thread-ID | Level | Message - Class | Line No. | Method name | Thread name");
	}
}
