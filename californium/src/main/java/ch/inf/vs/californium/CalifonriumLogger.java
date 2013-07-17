package ch.inf.vs.californium;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * CalifonriumLogger is a helper class for the logging in Californium.
 * CaliforniumLogger makes sure that {@link #initializeLogger()} initializes the
 * loggers before use so that they print in the appropriate format.
 */
public class CalifonriumLogger {

	static {
		initializeLogger();
	}
	
	/**
	 * Gets the logger for the specified class. If the calling class is not the
	 * same as the class provided, an info is printed to the logger saying, that
	 * another class uses the logger of the specified class.
	 * 
	 * @param clazz
	 *            the class which's logger is desired
	 * @return the logger
	 */
	public static Logger getLogger(Class<?> clazz) {
		if (clazz == null) throw new NullPointerException();
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		Logger logger = Logger.getLogger(clazz.getName());
		String caller = trace[2].getClassName();
		if (!caller.equals(clazz.getName()))
			logger.info("Note that class "+caller+" uses the logger of class "+clazz.getName());
		return logger;
	}
	
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
	private static void initializeLogger() {
		try {
			LogManager.getLogManager().reset();
			Logger logger = Logger.getLogger("");
			logger.addHandler(new StreamHandler(System.out, new Formatter() {
			    @Override
			    public synchronized String format(LogRecord record) {
			    	String stackTrace = "";
			    	Throwable throwable = record.getThrown();
			    	if (throwable != null) {
			    		StringWriter sw = new StringWriter();
			    		throwable.printStackTrace(new PrintWriter(sw));
			    		stackTrace = sw.toString();
			    	}
			    	
			    	int lineNo;
			    	StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			    	if (throwable != null && stack.length > 7)
			    		lineNo = stack[7].getLineNumber();
			    	else if (stack.length > 8)
			    		lineNo = stack[8].getLineNumber();
			    	else lineNo = -1;
			    	
			        return String.format("%2d", record.getThreadID()) + " " + record.getLevel()+": "
			        		+ record.getMessage()
			        		+ " - ("+record.getSourceClassName()+".java:"+lineNo+") "
			                + record.getSourceMethodName()+"()"
			                + " in thread " + Thread.currentThread().getName()+"\n"
			                + stackTrace;
			    }
			}) {
				@Override
				public synchronized void publish(LogRecord record) {
					super.publish(record);
					super.flush();
				}
				}
			);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
