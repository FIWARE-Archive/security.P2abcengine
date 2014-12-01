package ch.zhaw.ficore.p2abc.ldap.helper;

import javax.naming.NamingException;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;

/**
 * Provides some of the helper functions as static methods.
 * 
 * @author mroman
 * 
 */
public class LdapStatic {
    // private static ConnectionParameters config;
    private static LdapConnection con;

    /**
     * Init LdapStatic with a LdapConnectionConfig. This also creates
     * LdapConnection
     * 
     * @param config
     *            an LdapConnectionConfig-Object
     * @throws NamingException
     *             on an LDAP error
     */
    public static void init(ConnectionParameters config) throws NamingException {
        // LdapStatic.config = config;
        LdapStatic.con = new LdapConnection(config);
    }

    /**
     * @return a new LdapSearch-Object
     */
    public static LdapSearch newSearch() {
        return con.newSearch();
    }
}
