package ch.ethz.inf.vs.californium;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
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
	
	// The policy what to print out when logging
	private static LogPolicy logPolicy = new LogPolicy().dateFormat(null);
	
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
			
			Handler handler = new StreamHandler(System.out, new Formatter() {
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
			    	
			    	LogPolicy p = logPolicy;
			        return iftrue(p.showTheadID, String.format("%2d", record.getThreadID()) + " ")
			        		+ iftrue(p.showLevel, record.getLevel()+" ")
			        		+ iftrue(p.showClass, "[" + getSimpleClassName(record.getSourceClassName()) + "]: ")
			        		+ iftrue(p.showMessage, record.getMessage())
			        		+ iftrue(p.showSource, " - ("+record.getSourceClassName()+".java:"+lineNo+") ")
			        		+ iftrue(p.showMethod, record.getSourceMethodName()+"()")
			                + iftrue(p.showThread, " in thread " + Thread.currentThread().getName())
			                + (p.dateFormat != null
			                	? " at (" + p.dateFormat.format( new Date(record.getMillis()) ) +")"
			                	: "")
			                +"\n" + stackTrace;
			    }
			}) {
				@Override
				public synchronized void publish(LogRecord record) {
					super.publish(record);
					super.flush();
				}
			};
			handler.setLevel(Level.ALL);
			logger.addHandler(handler);
			logger.info("Logging format: Thread-ID | Level | Message - Class | Line No. | Method name | Thread name");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private static String iftrue(boolean b, String s) {
		return b ? s : "";
	}
	
	private static String getSimpleClassName(String absolute) {
		String[] parts = absolute.split("\\.");
		return parts[parts.length -1];
	}
	
	public static void disableLogging() {
		Logger.getLogger("").setLevel(Level.OFF);
	}
	
	public static void setLoggerLevel(Level level) {
		Logger.getLogger("").setLevel(level);
	}

	public static LogPolicy getLogPolicy() {
		return logPolicy;
	}

	public static void setLogPolicy(LogPolicy logPolicy) {
		CalifonriumLogger.logPolicy = logPolicy;
	}
	
	/**
	 * This class represents the properties how logging records are represented. 
	 */
	// Defining logging properties in the NetworkConfig leads to a bootstrap
	// problem: The NetworkConfig wants to write a log when loading the
	// properties and the log wants to know the logging properties when writing
	// that log.
	public static class LogPolicy {
		public boolean showTheadID = true;
		public boolean showLevel = true;
		public boolean showClass = true;
		public boolean showMessage = true;
		public boolean showSource = true;
		public boolean showMethod = true;
		public boolean showThread = true;
		public Format dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		public LogPolicy() { }
		
		public LogPolicy(
				boolean showTheadID, 
				boolean showLevel,
				boolean showClass, 
				boolean showMessage, 
				boolean showSource,
				boolean showMethod,
				boolean showThread, 
				Format dateFormat) {
			this.showTheadID = showTheadID;
			this.showLevel = showLevel;
			this.showClass = showClass;
			this.showMessage = showMessage;
			this.showSource = showSource;
			this.showMethod = showMethod;
			this.showThread = showThread;
			this.dateFormat = dateFormat;
		}

		public LogPolicy showThreadID() { showTheadID = true; return this; }
		public LogPolicy showLevel() { showLevel = true; return this; }
		public LogPolicy showClass() { showClass = true; return this; }
		public LogPolicy showMessage() { showMessage = true; return this; }
		public LogPolicy showSource() { showSource = true; return this; }
		public LogPolicy showMethod() { showMethod = true; return this; }
		public LogPolicy showThread() { showThread = true; return this; }
		
		public LogPolicy hideThreadID() { showTheadID = false; return this; }
		public LogPolicy hideLevel() { showLevel = false; return this; }
		public LogPolicy hideClass() { showClass = false; return this; }
		public LogPolicy hideMessage() { showMessage = false; return this; }
		public LogPolicy hideSource() { showSource = false; return this; }
		public LogPolicy hideMethod() { showMethod = false; return this; }
		public LogPolicy hideThread() { showThread = false; return this; }
		
		public LogPolicy dateFormat(Format format) {
			this.dateFormat = format; return this;
		}
	}
}
