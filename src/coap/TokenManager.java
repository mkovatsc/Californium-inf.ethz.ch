package coap;

import java.util.HashSet;
import java.util.Set;

import util.Log;

/*
 * This class describes the functionality of a Token Manager.
 * 
 * Its purpose is to manage tokens used for keeping state of
 * transactions and block-wise transfers. Communication layers use
 * a TokenManager to acquire token where needed and release
 * them after completion of the task.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class TokenManager {

	// Static Attributes ///////////////////////////////////////////////////////
	
	// the empty token, used as default value
	public static final Option emptyToken
		= new Option(new byte[0], OptionNumberRegistry.TOKEN);
	
	// Constructors ////////////////////////////////////////////////////////////
	
	/*
	 * Default constructor for a new TokenManager
	 */
	public TokenManager() {
		// TODO randomize initial token?
		this.nextValue = 0;
	}
	
	// Methods /////////////////////////////////////////////////////////////////
	
	/*
	 * Returns an unique token.
	 * 
	 * @param preferEmptyToken If set to true, the caller will receive
	 * the empty token if it is available. This is useful for reducing
	 * datagram sizes in transactions that are expected to complete
	 * in short time. On the other hand, empty tokens are not preferred
	 * in block-wise transfers, as the empty token is then not available
	 * for concurrent transactions.
	 * 
	 */
	public Option acquireToken(boolean preferEmptyToken) {
		
		Option token = null;
		if (preferEmptyToken && !isAcquired(emptyToken)) {
			token = emptyToken;
		} else {
			token = new Option(nextValue++, OptionNumberRegistry.TOKEN);
		}
		
		if (!acquiredTokens.add(token)) {
			Log.warning(this, "Token already acquired: %s\n", token.getDisplayValue());
		}
		
		return token;
	}
	
	public Option acquireToken() {
		return acquireToken(false);
	}
	
	/*
	 * Releases an acquired token and makes it available for reuse.
	 * 
	 * @param token The token to release
	 */
	public void releaseToken(Option token) {
		if (!acquiredTokens.remove(token)) {
			Log.warning(this, "Token to release is not acquired: %s\n", token.getDisplayValue());
		}
	}
	
	/*
	 * Checks if a token is acquired by this manager.
	 * 
	 * @param token The token to check
	 * @return True iff the token is currently in use
	 */
	public boolean isAcquired(Option token) {
		return acquiredTokens.contains(token);
	}
	
	
	// Attributes //////////////////////////////////////////////////////////////
	
	private Set<Option> acquiredTokens
		= new HashSet<Option>();

	private int nextValue;
}
