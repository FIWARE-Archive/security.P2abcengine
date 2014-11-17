package ch.zhaw.ficore.p2abc.ldap.helper;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private Logger logger;

    /**
     * Create a Connection using a ConnectionParameters
     * 
     * @param cfg
     *            An ConnectionParameters
     * @throws NamingException
     */
    public LdapConnection(ConnectionParameters config) throws NamingException {
        logger = LogManager.getLogger();
        if (config.usesTls())
            throw logger.throwing(new RuntimeException(
                    "TLS not supported yet :("));
        this.config = config;
        reloadConfig();
    }

    /**
     * @return Returns the DirContext
     */
    public DirContext getInitialDirContext() {
        return initialDirContext;
    }

    /**
     * Replace the ConnectionParameters associated with this connection and load
     * it.
     * 
     * @param config
     *            A new ConnectionParameters
     * @throws NamingException
     */
    public void applyConfig(ConnectionParameters config) throws NamingException {
        this.config = config;
        reloadConfig();
    }

    /**
     * Reload the associated ConnectionParameters
     * 
     * @throws NamingException
     */
    private void reloadConfig() throws NamingException {
        if (initialDirContext != null)
            initialDirContext.close();
        initialDirContext = new InitialDirContext(makeEnvironment());
    }

    /**
     * Return the environment needed to construct a DirContext
     * 
     * @return Environment as a Hashtable
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
