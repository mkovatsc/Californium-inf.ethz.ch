package ch.ethz.inf.vs.californium.resources.proxy;


import java.io.IOException;

/**
 * The Class InvalidFieldException.
 * 
 * @author Francesco Corazza
 */
public class InvalidFieldException extends TranslationException {
	private static final long serialVersionUID = 1L;

	public InvalidFieldException() {
		super();
	}

	public InvalidFieldException(String message) {
		super(message);
	}

	public InvalidFieldException(String string, IOException e) {
		super(string, e);
	}

	public InvalidFieldException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidFieldException(Throwable cause) {
		super(cause);
	}
}
