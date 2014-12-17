package ch.zhaw.ficore.p2abc.configuration;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    /** Configuration data for issuance service. */
    private IssuanceConfiguration issuanceConfiguration;

    /** Configuration data for verification service. */
    private VerificationConfiguration verificationConfiguration;

    /** Configuration data for user service. */
    private UserConfiguration userConfiguration;

    private static Logger logger = LogManager.getLogger();

    private static ServicesConfiguration instance = new ServicesConfiguration();

    private static String issuanceServiceURL = "http://localhost/";

    private static String userServiceURL = "http://localhost/";
    
    private static String verificationServiceURL = "http://localhost/";

    private static String restAuthUser = "user";

    private static String restAuthPassword = "password";
    
    private static String verifierIdentity = "unknown";

    static {
        staticInit();
    }
    
    public static synchronized void staticInit() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:/comp/env");

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

            issuanceServiceURL = (String) envCtx
                    .lookup("cfg/issuanceServiceURL");
            userServiceURL = (String) envCtx.lookup("cfg/userServiceURL");
            verificationServiceURL = (String) envCtx.lookup("cfg/verificationServiceURL");
            restAuthPassword = (String) envCtx.lookup("cfg/restAuthPassword");
            restAuthUser = (String) envCtx.lookup("cfg/restAuthUser");
            verifierIdentity = (String) envCtx.lookup("cfg/verifierIdentity");
            
            System.out.println("restAuthUser :=" + restAuthUser);
            logger.info("restAuthUser := " + restAuthUser);

            IssuanceConfiguration cfgData = new IssuanceConfiguration(
                    sourceAttributes, cpAttributes, sourceAuthentication,
                    cpAuthentication, bindQuery);
            ServicesConfiguration.setIssuanceConfiguration(cfgData);
        } catch (Exception e) {
            logger.catching(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * URI base The URI base is used as a prefix to URIs for example in the
     * generation of CredentialSpecifications.
     */
    private static String uriBase = "urn:fiware:privacy:";

    public static synchronized String getIssuanceServiceURL() {
        return issuanceServiceURL;
    }

    public static synchronized String getUserServiceURL() {
        return userServiceURL;
    }
    
    public static synchronized String getVerificationServiceURL() {
        return verificationServiceURL;
    }

    public static synchronized String getRestAuthUser() {
        return restAuthUser;
    }

    public static synchronized String getRestAuthPassword() {
        return restAuthPassword;
    }
    
    public static synchronized String getVerifierIdentity() {
        return verifierIdentity;
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
     */
    public static synchronized IssuanceConfiguration getIssuanceConfiguration() {
        logger.entry();
        return logger.exit(instance.issuanceConfiguration);
    }

    public static synchronized VerificationConfiguration getVerificationConfiguration() {
        logger.entry();
        return logger.exit(instance.verificationConfiguration);
    }

    public static synchronized UserConfiguration getUserConfiguration() {
        logger.entry();
        return logger.exit(instance.userConfiguration);
    }

    /**
     * Replaces the current issuance configuration.
     * 
     * The new configuration is scrutinised and, if all sanity checks are
     * passed, the old configuration is replaced with the new one. If there is
     * something wrong with the configuration, the current configuration is
     * retained.
     * 
     * @param newConfig
     *            the new configuration
     */
    public static synchronized void setIssuanceConfiguration(
            IssuanceConfiguration newConfig) {
        logger.entry();

        if (newConfig.isPlausible())
            instance.issuanceConfiguration = newConfig;
        else
            logger.warn("Issuance configuration not plausible, retaining old one");

        logger.exit();
    }

    public static synchronized void setVerificationConfiguration(
            VerificationConfiguration newConfig) {
        logger.entry();
        instance.verificationConfiguration = newConfig;
        logger.exit();
    }

    public static synchronized void setUserConfiguration(
            UserConfiguration newConfig) {
        logger.entry();
        instance.userConfiguration = newConfig;
        logger.exit();
    }

    public static synchronized void setFakeIssuanceParameters() {
        logger.entry();
        instance.issuanceConfiguration.setFakeSources();
        // TODO: Set more parameters?
        logger.exit();
    }

    public boolean isPlausible() {
        return true;
    }
}
