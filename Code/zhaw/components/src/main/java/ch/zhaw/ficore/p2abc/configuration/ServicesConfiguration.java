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
public final class ServicesConfiguration {
    private static XLogger logger = new XLogger(LoggerFactory.getLogger(ServicesConfiguration.class));

    /**
     * URI base The URI base is used as a prefix to URIs for example in the
     * generation of CredentialSpecifications.
     */
    private static String uriBase = "urn:fiware:privacy:";

    /** Private do-nothing constructor to prevent construction of instances. */
    private ServicesConfiguration() {

    }

    private static Context getEnvContext() throws NamingException {
        final Context initCtx = new InitialContext();
        final Context envCtx = (Context) initCtx.lookup("java:/comp/env");
        return envCtx;
    }

    /** Checks whether fake access tokens are allowed.
     *
     * @return whether fake access tokens are allowed
     */
    public static synchronized boolean getAllowFakeAccesstoken() {
        try {
            final Context envCtx = ServicesConfiguration.getEnvContext();

            try {
                return (Boolean) envCtx.lookup("cfg/allowFakeAccesstoken");
            } catch (final Exception ex) {
                return false;
            }
        } catch (final Exception ex) {
            return false;
        }
    }

    /** Retrieves the configured issuance service's URL.
     *
     * @return the configured issuance service's URL
     *
     * @throws NamingException when the URL isn't configured
     */
    public static synchronized String getIssuanceServiceURL() throws NamingException {
        final Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx
                .lookup("cfg/issuanceServiceURL");
    }

    /** Retrieves the configured user service's URL.
     *
     * @return the configured user service's URL
     *
     * @throws NamingException when the URL isn't configured
     */
    public static synchronized String getUserServiceURL() throws NamingException {
        final Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx
                .lookup("cfg/userServiceURL");
    }

    /** Retrieves the configured verification service's URL.
     *
     * @return the configured verification service's URL
     *
     * @throws NamingException when the URL isn't configured
     */
    public static synchronized String getVerificationServiceURL() throws NamingException {
        final Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx.lookup("cfg/verificationServiceURL");
    }

    /** Retrieves the REST authentication user.
     *
     * @return the REST authentication user
     * @throws NamingException when the user isn't configured
     */
    public static synchronized String getRestAuthUser() throws NamingException {
        final Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx.lookup("cfg/restAuthUser");
    }

    /** Retrieves the REST authentication password.
     *
     * @return the REST authentication password
     * @throws NamingException when the password isn't configured
     */
    public static synchronized String getRestAuthPassword() throws NamingException {
        final Context envCtx = ServicesConfiguration.getEnvContext();

        return (String) envCtx.lookup("cfg/restAuthPassword");
    }

    /** Retrieves the verifier identity.
     *
     * @return the verifier identity
     * @throws NamingException when the verifier identity isn't configured
     */
    public static synchronized String getVerifierIdentity() throws NamingException {
        final Context envCtx = ServicesConfiguration.getEnvContext();

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

    /**
     * Returns the current issuance configuration.
     *
     * @return the current issuance parameters.
     * @throws NamingException when the issuance service isn't fully configured
     */
    public static synchronized IssuanceConfiguration getIssuanceConfiguration() throws NamingException {
        logger.entry();
        final Context envCtx = ServicesConfiguration.getEnvContext();

        final ConnectionParameters cpAttributes = (ConnectionParameters) envCtx
                .lookup("cfg/ConnectionParameters/attributes");
        final ConnectionParameters cpAuthentication = (ConnectionParameters) envCtx
                .lookup("cfg/ConnectionParameters/authentication");
        final IssuanceConfiguration.IdentitySource sourceAttributes = IssuanceConfiguration.IdentitySource
                .valueOf((String) envCtx.lookup("cfg/Source/attributes"));
        final IssuanceConfiguration.IdentitySource sourceAuthentication = IssuanceConfiguration.IdentitySource
                .valueOf((String) envCtx
                        .lookup("cfg/Source/authentication"));

        final String bindQuery = (String) envCtx.lookup("cfg/bindQuery");

        final String restAuthUser = (String) envCtx.lookup("cfg/restAuthUser");

        System.out.println("restAuthUser :=" + restAuthUser);
        logger.info("restAuthUser := " + restAuthUser);

        return logger.exit(new IssuanceConfiguration(
                sourceAttributes, cpAttributes, sourceAuthentication,
                cpAuthentication, bindQuery));
    }
}
