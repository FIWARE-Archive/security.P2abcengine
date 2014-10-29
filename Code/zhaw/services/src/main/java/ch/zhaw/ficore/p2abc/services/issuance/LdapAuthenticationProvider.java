package ch.zhaw.ficore.p2abc.services.issuance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnection;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnectionConfig;
import ch.zhaw.ficore.p2abc.services.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthInfoSimple;

import javax.naming.*;
import javax.naming.directory.*;



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
		if(configuration.doesLdapUseTls())
			throw logger.throwing(new RuntimeException("TLS not supported yet :("));
		
		try {
			LdapConnectionConfig cfg = new LdapConnectionConfig(configuration.getLdapServerPort(), configuration.getLdapServerName());
			cfg.setAuth(configuration.getLdapUser(), configuration.getLdapPassword());
			ldapConnection = cfg.newConnection();
			
			String bindQuery = QueryHelper.buildQuery(configuration.getBindQuery(), 
					QueryHelper.ldapSanitize(simpleAuth.username));
			System.out.println("q:"+bindQuery);
			
			NamingEnumeration results = ldapConnection.newSearch().search("", bindQuery);
            String binddn = null;
            while (results.hasMore()) {
                SearchResult sr = (SearchResult) results.next();
                binddn = sr.getName();
            }
            System.out.println(binddn);
            
            if(binddn == null)
            	return logger.exit(false);
            
            cfg.setAuth(binddn, simpleAuth.password);
            ldapConnection.reloadConfig();
			
			authenticated = true;
			uid = simpleAuth.username;
			return logger.exit(true);
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(false);
		}
		finally {
			try {
				if(ldapConnection != null)
					ldapConnection.close();
			}
			catch(Exception e) {
				logger.catching(e);
				return logger.exit(false);
			}
		}
	}
	
	public String getUserID() {
		if(!authenticated)
			throw new IllegalStateException("Must successfully authenticate prior to calling this method!");
		
		return uid;
	}
}
