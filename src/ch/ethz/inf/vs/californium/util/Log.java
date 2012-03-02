/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.util;

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
