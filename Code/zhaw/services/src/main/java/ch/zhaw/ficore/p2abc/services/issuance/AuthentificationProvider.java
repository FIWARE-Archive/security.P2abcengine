package ch.zhaw.ficore.p2abc.services.issuance;

/** Serves as a Factory for AuthenticationProviders.
 * 
 * An AuthenticationProvider provides the Authentifaction
 * for an identity source. Services may use such providers
 * to authenticate users for initializing an issuance 
 * protocol.
 * 
 * @author mroman
 */
public abstract class AuthentificationProvider {
	
	/**
	 * Factory method to construct an AuthentificationProvider for a given
	 * ServiceConfiguration. The AuthentificationProvider will receive a copy
	 * of the ServiceConfiguration.
	 * 
	 * @param srvcCfg a ServiceConfiguration
	 * @return an implementation of an AuthentificationProvider
	 */
	public static AuthentificationProvider getAuthentificationProvider(ServiceConfiguration srvcCfg) {
		//TODO: Factory method
		return null;
	}
	
	/**
	 * This method allows to verify the AuthentificationInformation provided
	 * by a user.
	 * 
	 * @param authInfo AuthentificationInformation as given by a user.
	 * @return true if AuthentificationInformation is correct (e.g. username, password is correct)
	 */
	public abstract boolean authenticate(AuthentificationInformation authInfo);
	
	/**
	 * Called when this AuthentificationProvider is no longer required. 
	 * Providers should close open sockets/connections/files etc. on shutdown.
	 */
	public abstract void shutdown();
}