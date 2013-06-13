package ch.inf.vs.californium.resources;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public class CalifonriumLogger {

	static {
		initializeLogger();
	}
	
	public static Logger getLogger(Class<?> clazz) {
		if (clazz == null) throw new NullPointerException();
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		Logger logger = Logger.getLogger(clazz.getName());
		String caller = trace[2].getClassName();
		if (!caller.equals(clazz.getName()))
			logger.info("Note that class "+caller+" uses the logger of class "+clazz.getName());
		return logger;
	}
	
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
