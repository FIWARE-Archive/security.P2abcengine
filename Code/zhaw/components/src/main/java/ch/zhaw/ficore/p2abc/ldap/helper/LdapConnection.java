package ch.zhaw.ficore.p2abc.ldap.helper;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;

/**
 * Contains an DirContext (LDAPConnection)
 * 
 * @author mroman
 * 
 */
public class LdapConnection {
    private DirContext initialDirContext;
    private ConnectionParameters config;
    private static final XLogger logger = new XLogger(LoggerFactory.getLogger(LdapConnection.class));

    /**
     * Creates a Connection using a ConnectionParameters.
     * 
     * @param config
     *            the connection parameters
     * @throws NamingException
     *             on an LDAP error
     */
    public LdapConnection(ConnectionParameters config) throws NamingException {
        if (config.usesTls())
            throw logger.throwing(new RuntimeException(
                    "TLS not supported yet :("));
        this.config = config;
        reloadConfig();
    }

    /**
     * @return Returns the DirContext.
     * 
     * @return the directory context
     */
    public DirContext getInitialDirContext() {
        return initialDirContext;
    }

    /**
     * Replaces the ConnectionParameters associated with this connection and
     * loads it.
     * 
     * @param config
     *            A new ConnectionParameters
     * @throws NamingException
     *             on an LDAP error
     */
    public void applyConfig(ConnectionParameters config) throws NamingException {
        this.config = config;
        reloadConfig();
    }

    /**
     * Reloads the associated connection parameters.
     * 
     * @throws NamingException
     *             on an LDAP error
     */
    private void reloadConfig() throws NamingException {
        if (initialDirContext != null)
            initialDirContext.close();
        initialDirContext = new InitialDirContext(makeEnvironment());
    }

    /**
     * Return the environment needed to construct a DirContext
     * 
     * @return environment as a Hashtable
     */
    private Hashtable<String, String> makeEnvironment() {
        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + config.getServerName() + ":"
                + config.getServerPort());
        if (config.getAuthenticationMethod() != null)
            env.put(Context.SECURITY_AUTHENTICATION,
                    config.getAuthenticationMethod());
        if (config.getUser() != null)
            env.put(Context.SECURITY_PRINCIPAL, config.getUser());
        if (config.getPassword() != null)
            env.put(Context.SECURITY_CREDENTIALS, config.getPassword()
                    .toString());

        return env;
    }

    /**
     * Closes the DirContext
     */
    protected void finalize() {
        try {
            initialDirContext.close();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the DirContext
     * 
     * @throws NamingException
     *             on an LDAP error
     */
    public void close() throws NamingException {
        initialDirContext.close();
    }

    /**
     * @return Returns a new LdapSearch-Object using this LdapConnection
     */
    public LdapSearch newSearch() {
        return new LdapSearch(this);
    }
}
