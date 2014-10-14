package ch.mroman.ldap;
import javax.naming.*;
import javax.naming.directory.*;

/**
 * Contains an DirContext (LDAPConnection)
 * @author mroman
 *
 */
public class LdapConnection {
	private DirContext initialDirContext;
	private LdapConnectionConfig config;
	
	/**
	 * Create a Connection using a LdapConnectionConfig
	 * @param cfg An LdapConnectionConfig
	 * @throws NamingException
	 */
	public LdapConnection(LdapConnectionConfig cfg) throws NamingException {
		config = cfg;
		reloadConfig();
	}
	
	/**
	 * @return Returns the DirContext
	 */
	public DirContext getInitialDirContext() {
		return initialDirContext;
	}
	
	/**
	 * Replace the LdapConnectionConfig associated with
	 * this connection and load it.
	 * @param config A new LdapConnectionConfig
	 * @throws NamingException
	 */
	public void applyConfig(LdapConnectionConfig config) throws NamingException {
		this.config = config;
		reloadConfig();
	}
	
	/**
	 * Reload the associated LdapConnectionConfig
	 * @throws NamingException
	 */
	public void reloadConfig() throws NamingException {
		if(initialDirContext != null)
			initialDirContext.close();
		initialDirContext = new InitialDirContext(config.getEnvironment());
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
