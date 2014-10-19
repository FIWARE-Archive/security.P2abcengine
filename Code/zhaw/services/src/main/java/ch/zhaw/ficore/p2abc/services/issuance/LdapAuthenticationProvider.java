package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.services.ConfigurationData;
import ch.zhaw.ficore.p2abc.services.issuance.xml.*;
import ch.zhaw.ficore.p2abc.ldap.helper.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An AuthenticationProvider for LDAP.
 * 
 * @author mroman
 */
public class LdapAuthenticationProvider extends AuthenticationProvider {
	
	private Logger logger;
	
	/**
	 * Constructor
	 */
	public LdapAuthenticationProvider(ConfigurationData configuration) {
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
		ConfigurationData configuration = ServiceConfiguration.getServiceConfiguration();
		if(configuration.ldapUseTls)
			throw logger.throwing(new RuntimeException("TLS not supported yet :("));
		
		try {
			LdapConnectionConfig cfg = new LdapConnectionConfig(configuration.ldapServerPort, configuration.ldapServerName);
			cfg.setAuth(simpleAuth.username, simpleAuth.password);
			LdapConnection con = cfg.newConnection();
			return logger.exit(true);
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(false);
		}
	} 
}