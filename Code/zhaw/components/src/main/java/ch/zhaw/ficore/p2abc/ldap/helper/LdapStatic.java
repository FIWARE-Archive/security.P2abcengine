package ch.zhaw.ficore.p2abc.ldap.helper;

import javax.naming.NamingException;

/**
 * Provides some of the helper functions as
 * static methods. 
 * @author mroman
 *
 */
public class LdapStatic {
	private static LdapConnectionConfig _config;
	private static LdapConnection _con;
	
	/**
	 * Init LdapStatic with a LdapConnectionConfig. This
	 * also creates LdapConnection
	 * @param config an LdapConnectionConfig-Object
	 * @throws NamingException
	 */
	public static void init(LdapConnectionConfig config) throws NamingException {
		_config = config;
		_con = config.newConnection();
	}
	
	/**
	 * @return a new LdapSearch-Object
	 */
	public static LdapSearch newSearch() {
		return _con.newSearch();
	}
}
