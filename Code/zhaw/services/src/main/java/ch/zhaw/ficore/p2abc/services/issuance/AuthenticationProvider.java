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
public abstract class AuthenticationProvider {
	
	protected ServiceConfiguration srvcCfg;
	
	public AuthenticationProvider(ServiceConfiguration srvcCfg) {
		this.srvcCfg = srvcCfg;
	}
	
	/**
	 * Factory method to construct an AuthenticationProvider for a given
	 * ServiceConfiguration. The AuthenticationProvider will receive a copy
	 * of the ServiceConfiguration.
	 * 
	 * @param srvcCfg a ServiceConfiguration
	 * @return an implementation of an AuthenticationProvider
	 */
	public static AuthenticationProvider getAuthenticationProvider(ServiceConfiguration srvcCfg) {
		switch(srvcCfg.getIdentitySource()) {
		case FAKE:
			return new FakeAuthenticationProvider(srvcCfg);
		case LDAP:
			return new LdapAuthenticationProvider(srvcCfg);
		}
		throw new RuntimeException(srvcCfg.getIdentitySource().toString() + " is not a supported AuthenticationProvider!");
	}
	
	/**
	 * This method allows to verify the AuthenticationInformation provided
	 * by a user.
	 * 
	 * @param authInfo AuthenticationInformation as given by a user.
	 * @return true if AuthenticationInformation is correct (e.g. username, password is correct)
	 */
	public abstract boolean authenticate(AuthenticationInformation authInfo);
	
	/**
	 * Called when this AuthenticationProvider is no longer required. 
	 * Providers should close open sockets/connections/files etc. on shutdown.
	 */
	public abstract void shutdown();
}