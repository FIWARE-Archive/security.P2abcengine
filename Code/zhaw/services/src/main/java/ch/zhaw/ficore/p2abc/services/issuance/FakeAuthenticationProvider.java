package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.services.ConfigurationData;
import ch.zhaw.ficore.p2abc.services.issuance.xml.*;

/**
 * An AuthenticationProvider that is not coupled with any actual
 * identity source. Use this for testing or as a reference.
 * 
 * @author mroman
 */
public class FakeAuthenticationProvider extends AuthenticationProvider {
	
	/**
	 * Constructor
	 */
	public FakeAuthenticationProvider(ConfigurationData configuration) {
		super(configuration);
	}
	
	/**
	 * No operation.
	 */
	public void shutdown() {
		
	}
	
	/**
	 * Performs the authentication. Uses a dummy hardcoded combination
	 * of a username "CaroleKing" and "Jazzman" as the password.
	 */
	public boolean authenticate(AuthenticationInformation authInfo) {
		if(!(authInfo instanceof AuthInfoSimple))
			return false;
		
		AuthInfoSimple simpleAuth = (AuthInfoSimple) authInfo;
		
		if(simpleAuth.username.equals("CaroleKing") && simpleAuth.password.equals("Jazzman"))
			return true;
		
		return false;
	} 
}