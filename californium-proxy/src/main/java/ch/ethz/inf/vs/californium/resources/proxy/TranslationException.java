package ch.ethz.inf.vs.californium.resources.proxy;


import java.io.IOException;

/**
 * The Class TranslationException.
 * 
 * @author Francesco Corazza
 */
public class TranslationException extends Exception {
	private static final long serialVersionUID = 1L;

	public TranslationException() {
		super();
	}

	public TranslationException(String message) {
		super(message);
	}

	public TranslationException(String string, IOException e) {
		super(string, e);
	}

	public TranslationException(String message, Throwable cause) {
		super(message, cause);
	}

	public TranslationException(Throwable cause) {
		super(cause);
	}
}