package ch.zhaw.ficore.p2abc.services.issuance;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnection;
import ch.zhaw.ficore.p2abc.xml.AuthInfoSimple;
import ch.zhaw.ficore.p2abc.xml.AuthenticationInformation;

/**
 * An AuthenticationProvider for LDAP.
 * 
 * @author mroman
 */
public class LdapAuthenticationProvider extends AuthenticationProvider {

    private static final XLogger logger = new XLogger(
            LoggerFactory.getLogger(LdapAuthenticationProvider.class));
    private boolean authenticated = false;
    private String uid;

    /**
     * Constructor
     * 
     * @param configuration
     *            Configuration (Issuance)
     */
    public LdapAuthenticationProvider(IssuanceConfiguration configuration) {
        super(configuration);
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
     * @throws NamingException
     */
    public boolean authenticate(AuthenticationInformation authInfo)
            throws NamingException {
        boolean isGood = true;

        logger.entry();

        if (!(authInfo instanceof AuthInfoSimple)) {
            logger.warn("LDAP AuthenticationInformation is not simple, can't use username/password");
            isGood = false;
        }

        if (isGood) {
            AuthInfoSimple simpleAuth = (AuthInfoSimple) authInfo;
            IssuanceConfiguration configuration = ServicesConfiguration
                    .getIssuanceConfiguration();

            LdapConnection adminConnection = null;
            LdapConnection userConnection = null;

            try {
                String bindQuery = QueryHelper.buildQuery(
                        configuration.getBindQuery(),
                        QueryHelper.ldapSanitize(simpleAuth.username));

                ConnectionParameters adminCfg = configuration
                        .getAuthenticationConnectionParameters();
                adminConnection = new LdapConnection(adminCfg);

                NamingEnumeration<SearchResult> results = adminConnection
                        .newSearch().search("", bindQuery);
                String binddn = null;
                if (results.hasMore()) {
                    SearchResult sr = results.next();
                    binddn = sr.getName();
                }

                if (binddn == null) {
                    logger.warn("Couldn't find DN for user "
                            + simpleAuth.username);
                    isGood = false;
                }

                if (isGood) {
                    ConnectionParameters userCfg = new ConnectionParameters(
                            adminCfg.getServerName(), adminCfg.getServerPort(),
                            adminCfg.getServerPort(), adminCfg.getServerPort(),
                            binddn, simpleAuth.password, adminCfg.usesTls());
                    // Implicit authentication
                    userConnection = new LdapConnection(userCfg);

                    // If no exception thrown above, then user is authenticated
                    authenticated = true;
                    uid = simpleAuth.username;

                    userConnection.close();
                    adminConnection.close();
                }
            } catch (Exception e) {
                logger.catching(e);
                return logger.exit(false);
            } finally {
                try {
                    if (adminConnection != null)
                        adminConnection.close();
                } catch (Exception e) {
                    logger.catching(e);
                    isGood = false;
                }
                try {
                    if (userConnection != null)
                        userConnection.close();
                } catch (Exception e) {
                    logger.catching(e);
                    isGood = false;
                }
            }
        }

        /*
         * N.B.: !isGood && authenticated is possible, so we must return
         * authenticated here, not isGood.
         */
        return logger.exit(authenticated);
    }

    public String getUserID() {
        if (!authenticated)
            throw new IllegalStateException(
                    "Must successfully authenticate prior to calling this method!");

        return uid;
    }
}
