package ch.zhaw.ficore.p2abc.services.issuance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.helper.ConnectionParameters;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnection;
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
	private boolean authenticated = false;
	private String uid;
	
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
		if(configuration.getAuthenticationConnectionParameters().usesTls())
			throw logger.throwing(new RuntimeException("TLS not supported yet :("));
		
		try {
			ConnectionParameters cfg = configuration.getAuthenticationConnectionParameters();
			ldapConnection = new LdapConnection(cfg);
			authenticated = true;
			uid = simpleAuth.username;
			return logger.exit(true);
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(false);
		}
	}
	
	public String getUserID() {
		if(!authenticated)
			throw new IllegalStateException("Must successfully authenticate prior to calling this method!");
		
		return uid;
	}
}
