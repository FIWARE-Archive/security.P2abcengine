package ch.zhaw.ficore.p2abc.services.issuance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnection;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnectionConfig;
import ch.zhaw.ficore.p2abc.services.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthInfoSimple;

/**
 * An AuthenticationProvider for LDAP.
 * 
 * @author mroman
 */
public class LdapAuthenticationProvider extends AuthenticationProvider {
	
	private Logger logger;
	private LdapConnection ldapConnection;
	
	/**
	 * Constructor
	 */
	public LdapAuthenticationProvider(IssuanceConfigurationData configuration) {
		super(configuration);
		logger = LogManager.getLogger(LdapAuthenticationProvider.class.getName());
	}
	
	/**
	 * No operation.
	 */
	public void shutdown() {
		
	}
	
	/**
	 * Performs the authentication through Ldap.
	 * 
	 * @return true if successful, false otherwise.
	 */
	public boolean authenticate(AuthenticationInformation authInfo) {
		logger.entry();
		
		if(!(authInfo instanceof AuthInfoSimple))
			return logger.exit(false);
		
		AuthInfoSimple simpleAuth = (AuthInfoSimple) authInfo;
		IssuanceConfigurationData configuration = ServicesConfiguration.getIssuanceConfiguration();
		if(configuration.doesLdapUseTls())
			throw logger.throwing(new RuntimeException("TLS not supported yet :("));
		
		try {
			LdapConnectionConfig cfg = new LdapConnectionConfig(configuration.getLdapServerPort(), configuration.getLdapServerName());
			cfg.setAuth(simpleAuth.username, simpleAuth.password);
			ldapConnection = cfg.newConnection();
			return logger.exit(true);
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(false);
		}
	}
	
	/**
	 * Returns the LdapConnection after a successful authenticate().
	 * 
	 * @param authInfo AuthenticationInformation
	 * @return an LdapConnection if successfully authenticated, Exception otherwise
	 */
	public LdapConnection getConnection(AuthenticationInformation authInfo) {
		if(authenticate(authInfo))
			return ldapConnection;
		throw new RuntimeException("Authentication failed!");
	}
}
