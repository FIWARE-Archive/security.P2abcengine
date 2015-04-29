package ch.zhaw.ficore.p2abc.configuration;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;


/**
 * Holds the configuration for all the services.
 * 
 * This class, implemented only through static methods, holds the configuration
 * for all the services defined here. This class can be loaded from and stored
 * to a file, and parts of the configuration can be selectively retrieved and
 * updated.
 * 
 * Note for implementers and other people who add to this class: You cannot
 * employ simple getters and setters on fields, for the simple reason that this
 * class is being used from within a servlet container, which means that there
 * may be several threads trying to get information from or change information
 * in this class. You will have to get and set entire configurations.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
public class ServicesConfiguration {

    /** Configuration data for verification service. */
    private VerificationConfiguration verificationConfiguration;

    /** Configuration data for user service. */
    private UserConfiguration userConfiguration;

    private static XLogger logger = new XLogger(LoggerFactory.getLogger(ServicesConfiguration.class));

    private static ServicesConfiguration instance = new ServicesConfiguration();

    private static Context getEnvContext() throws NamingException {
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:/comp/env");
        return envCtx;
    }

    /**
     * URI base The URI base is used as a prefix to URIs for example in the
     * generation of CredentialSpecifications.
     */
    private static String uriBase = "urn:fiware:privacy:";
    
    public static synchronized Boolean getAllowFakeAccesstoken() {
    	try {
	    	Context envCtx = ServicesConfiguration.getEnvContext();
	    	
	    	try {
	    		return (Boolean) envCtx.lookup("cfg/allowFakeAccesstoken");
	    	}
	    	catch(Exception ex) {
	    		return false;
	    	}
    	}
    	catch(Exception ex) {
    		return false;
    	}
    }

    public static synchronized String getIssuanceServiceURL() throws NamingException {
        Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx
                .lookup("cfg/issuanceServiceURL");
    }

    public static synchronized String getUserServiceURL() throws NamingException {
        Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx
                .lookup("cfg/userServiceURL");
    }
    
    public static synchronized String getVerificationServiceURL() throws NamingException {
        Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx.lookup("cfg/verificationServiceURL");
    }

    public static synchronized String getRestAuthUser() throws NamingException {
        Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx.lookup("cfg/restAuthUser");
    }

    public static synchronized String getRestAuthPassword() throws NamingException {
        Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx.lookup("cfg/restAuthPassword");
    }
    
    public static synchronized String getVerifierIdentity() throws NamingException {
        Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx.lookup("cfg/verifierIdentity");
    }

    /**
     * Returns the URI base.
     * 
     * @return the URI base
     */
    public static synchronized String getURIBase() {
        return uriBase;
    }

    /** Private do-nothing constructor to prevent construction of instances. */
    public ServicesConfiguration() {
    }

    /**
     * Returns the current issuance configuration.
     * 
     * @return the current issuance parameters.
     * @throws NamingException 
     */
    public static synchronized IssuanceConfiguration getIssuanceConfiguration() throws NamingException {
        logger.entry();
        Context envCtx = ServicesConfiguration.getEnvContext();
        
        ConnectionParameters cpAttributes = (ConnectionParameters) envCtx
                .lookup("cfg/ConnectionParameters/attributes");
        ConnectionParameters cpAuthentication = (ConnectionParameters) envCtx
                .lookup("cfg/ConnectionParameters/authentication");
        IssuanceConfiguration.IdentitySource sourceAttributes = IssuanceConfiguration.IdentitySource
                .valueOf((String) envCtx.lookup("cfg/Source/attributes"));
        IssuanceConfiguration.IdentitySource sourceAuthentication = IssuanceConfiguration.IdentitySource
                .valueOf((String) envCtx
                        .lookup("cfg/Source/authentication"));

        String bindQuery = (String) envCtx.lookup("cfg/bindQuery");

        String restAuthUser = (String) envCtx.lookup("cfg/restAuthUser");
        
        System.out.println("restAuthUser :=" + restAuthUser);
        logger.info("restAuthUser := " + restAuthUser);

        return logger.exit(new IssuanceConfiguration(
                sourceAttributes, cpAttributes, sourceAuthentication,
                cpAuthentication, bindQuery));
    }

    public static synchronized VerificationConfiguration getVerificationConfiguration() {
        logger.entry();
        return logger.exit(instance.verificationConfiguration);
    }

    public static synchronized UserConfiguration getUserConfiguration() {
        logger.entry();
        return logger.exit(instance.userConfiguration);
    }
}
