package ch.zhaw.ficore.p2abc.services.issuance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.helper.ConnectionParameters;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnection;
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
	private boolean authenticated = false;
	private String uid;
	
	/**
	 * Constructor
	 * @throws NamingException 
	 */
	public LdapAuthenticationProvider(IssuanceConfigurationData configuration)  {
		super(configuration);
		logger = LogManager.getLogger();
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
	  boolean isGood = true;

	  logger.entry();

	  if(!(authInfo instanceof AuthInfoSimple)) {
	    logger.warn("LDAP AuthenticationInformation is not simple, can't use username/password");
	    isGood = false;
	  }

	  if (isGood) {
	    AuthInfoSimple simpleAuth = (AuthInfoSimple) authInfo;
	    IssuanceConfigurationData configuration = ServicesConfiguration.getIssuanceConfiguration();
	    if(configuration.getAuthenticationConnectionParameters().usesTls())
	      throw logger.throwing(new RuntimeException("TLS not supported yet :("));

	    LdapConnection adminConnection = null;
	    LdapConnection userConnection = null;

	    try {
	      String bindQuery = QueryHelper.buildQuery(configuration.getBindQuery(), 
	          QueryHelper.ldapSanitize(simpleAuth.username));
	      System.out.println("q:"+bindQuery);

	      ConnectionParameters adminCfg = configuration.getAuthenticationConnectionParameters();
	      adminConnection = new LdapConnection(adminCfg);

	      NamingEnumeration<SearchResult> results = adminConnection.newSearch().search("", bindQuery);
	      String binddn = null;
	      if (results.hasMore()) {
	        SearchResult sr = (SearchResult) results.next();
	        binddn = sr.getName();
	      }
	      System.out.println(binddn);

	      if (binddn == null) {
	        logger.warn ("Couldn't find DN for user " + simpleAuth.username);
	        isGood = false;
	      }

	      if (isGood) {
	        ConnectionParameters userCfg = new ConnectionParameters(adminCfg.getServerName(), adminCfg.getServerPort(), adminCfg.getServerPort(), adminCfg.getServerPort(), binddn, simpleAuth.password.toCharArray(), adminCfg.usesTls());
	        // Implicit authentication
	        userConnection = new LdapConnection(userCfg);

	        // If no exception thrown above, then user is authenticated
	        authenticated = true;
	        uid = simpleAuth.username;

	        userConnection.close();
	        adminConnection.close();
	      }
	    }
	    catch(Exception e) {
	      logger.catching(e);
	      return logger.exit(false);
	    }
	    finally {
	      try {
	        if (adminConnection != null)
	          adminConnection.close();
	      }
	      catch(Exception e) {
	        logger.catching(e);
	        isGood = false;
	      }      
	      try {
	        if (userConnection != null)
	          userConnection.close();
	      }
	      catch(Exception e) {
	        logger.catching(e);
	        isGood = false;
	      }
	    }
	  }
	  
	  /* N.B.: !isGood && authenticated is possible, so we must return
	   * authenticated here, not isGood. */
	  return logger.exit(authenticated);
	}
	
	public String getUserID() {
		if(!authenticated)
			throw new IllegalStateException("Must successfully authenticate prior to calling this method!");
		
		return uid;
	}
}
