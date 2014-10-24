package ch.zhaw.ficore.p2abc.ldap.helper;

import javax.naming.NamingException;

/**
 * Provides some of the helper functions as
 * static methods. 
 * @author mroman
 *
 */
public class LdapStatic {
	private static LdapConnectionConfig config;
	private static LdapConnection con;
	
	/**
	 * Init LdapStatic with a LdapConnectionConfig. This
	 * also creates LdapConnection
	 * @param config an LdapConnectionConfig-Object
	 * @throws NamingException
	 */
	public static void init(LdapConnectionConfig config) throws NamingException {
		LdapStatic.config = config;
		LdapStatic.con = LdapStatic.config.newConnection();
	}
	
	/**
	 * @return a new LdapSearch-Object
	 */
	public static LdapSearch newSearch() {
		return con.newSearch();
	}
}
