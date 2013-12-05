
package ch.ethz.inf.vs.californium;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import ch.ethz.inf.vs.elements.Connector;

/**
 * CalifonriumLogger is a helper class for logging in Californium when
 * java.util.logging.config.file is not used. Make a static call to
 * CaliforniumLogger.initializeLogger() to configure the
 * {@link CaliforniumFormatter}. Make a call to
 * CaliforniumLogger.setLoggerLevel() to adjust the logging level for all or
 * specific classes.
 */
public class CaliforniumLogger {
	
	private static final Logger CALIFORNIUM_LOGGER = Logger.getLogger(CaliforniumLogger.class.getPackage().getName());
	private static final Logger CONNECTOR_LOGGER = Logger.getLogger(Connector.class.getPackage().getName());
	
	/**
	 * Initializes the logger. The resulting format of logged messages is
	 * 
	 * <pre>
	 * {@code
	 * | Thread ID | Level | Message | Class | Line No | Method | Thread name |
	 * }
	 * </pre>
	 * 
	 * where Level is the {@link Level} of the message, the <code>Class</code>
	 * is the class in which the log statement is located, the
	 * <code>Line No</code> is the line number of the logging statement, the
	 * <code>Method</code> is the method name in which the statement is located
	 * and the <code>Thread name</code> is the name of the thread that executed
	 * the logging statement.
	 */
	public static void initialize() {
		CALIFORNIUM_LOGGER.setUseParentHandlers(false);
		CALIFORNIUM_LOGGER.addHandler(new CaliforniumHandler());
		CONNECTOR_LOGGER.setUseParentHandlers(false);
		CONNECTOR_LOGGER.addHandler(new CaliforniumHandler());
	}
	
	/**
	 * Disables logging by setting the level of all loggers that have been
	 * requested over this class to OFF.
	 */
	public static void disableLogging() {
		CALIFORNIUM_LOGGER.setLevel(Level.OFF);
		CONNECTOR_LOGGER.setLevel(Level.OFF);
	}

	/**
	 * Sets the logger level of all loggers that have been requests over this
	 * class to the specified level and sets this level for all loggers that are
	 * going to be requested over this class in the future.
	 * 
	 * @param level
	 *            the new logger level
	 */
	public static void setLevel(Level level) {
		CALIFORNIUM_LOGGER.setLevel(level);
		CONNECTOR_LOGGER.setLevel(level);
	}

	private static class CaliforniumHandler extends StreamHandler {

		public CaliforniumHandler() {
			super(System.out, new CaliforniumFormatter());
			this.setLevel(Level.ALL);
		}

		@Override
		public synchronized void publish(LogRecord record) {
			super.publish(record);
			super.flush();
		}
	}
}
