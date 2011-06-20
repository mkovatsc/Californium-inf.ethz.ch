package util;

/*
 * This class implements a simple way for logging events in the CoAP library.
 * It can be used to redirect console output and provide uniform error
 * messages.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */

public class Log {

	/*
	 * Logs an error event with the specified message.
	 * 
	 * @param sender The object the event originated from
	 * @param msg A string describing the event
	 * @param args Arguments referenced by the format specifiers in 
	 *        the message string
	 */
	public static void error(Object sender, String msg, Object... args ) {
		
		String format = String.format("ERROR - %s\n", msg);
		if (sender != null) {
			format = "[" + sender.getClass().getName() + "] " + format;
		}
		
		System.err.printf(format, args);
	}

	/*
	 * Logs a warning event with the specified message.
	 * 
	 * @param sender The object the event originated from
	 * @param msg A string describing the event
	 * @param args Arguments referenced by the format specifiers in 
	 *        the message string
	 */
	public static void warning(Object sender, String msg, Object... args ) {
		
		String format = String.format("WARNING - %s\n", msg);
		if (sender != null) {
			format = "[" + sender.getClass().getName() + "] " + format;
		}
		
		System.err.printf(format, args);
	}

	/*
	 * Logs an info event with the specified message.
	 * 
	 * @param sender The object the event originated from
	 * @param msg A string describing the event
	 * @param args Arguments referenced by the format specifiers in 
	 *        the message string
	 */
	public static void info(Object sender, String msg, Object... args ) {
		
		String format = String.format("INFO - %s\n", msg);
		if (sender != null) {
			format = "[" + sender.getClass().getName() + "] " + format;
		}
		
		System.err.printf(format, args);
	}
}
