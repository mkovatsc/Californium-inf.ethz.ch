package ch.ethz.inf.vs.californium.resources.proxy;



import java.io.IOException;

/**
 * The Class InvalidMethodException.
 * 
 * @author Francesco Corazza
 */
public class InvalidMethodException extends TranslationException {
	private static final long serialVersionUID = 1L;

	public InvalidMethodException() {
		super();
	}

	public InvalidMethodException(String message) {
		super(message);
	}

	public InvalidMethodException(String string, IOException e) {
		super(string, e);
	}

	public InvalidMethodException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidMethodException(Throwable cause) {
		super(cause);
	}
}
