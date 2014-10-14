package ch.mroman.ldap;

import java.util.Hashtable;
import javax.naming.directory.*;
import javax.naming.*;

/**
 * Contains the configuration of a connection.
 * Connections can reload the configuration.
 * @author mroman
 *
 */
public class LdapConnectionConfig {
	private int port;
	private String host;
	private String authId;
	private String authPw;
	private String authMethod;
	
	/**
	 * New LdapConnectionConfig
	 * @param port
	 * @param host
	 */
	public LdapConnectionConfig(int port, String host) {
		this.port = port;
		this.host = host;
	}
	
	/**
	 * New LdapConnectionConfig with auth-Settings.
	 * Will use the Simple Authentication Method.
	 * @param port
	 * @param host
	 * @param authId (username)
	 * @param authPw (password)
	 */
	public LdapConnectionConfig(int port, String host,
			String authId, String authPw) {
		this(port, host);
		this.authId = authId;
		this.authPw = authPw;
		this.authMethod = "simple";
	}
	
	/**
	 * New LdapConnectionConfig but allow also
	 * to specify the Authentication Method.
	 * @param port
	 * @param host
	 * @param authId
	 * @param authPw
	 * @param authMethod
	 */
	public LdapConnectionConfig(int port, String host,
			String authId, String authPw, String authMethod) {
		this(port, host, authId, authPw);
		this.authMethod = authMethod;
	}
	
	/**
	 * Return the environment needed to construct a DirContext
	 * @return Environment as a Hashtable
	 */
	public Hashtable<String, String> getEnvironment() {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://" + host + ":" + port);
		if(authMethod != null)
			env.put(Context.SECURITY_AUTHENTICATION, authMethod);
		if(authId != null)
			env.put(Context.SECURITY_PRINCIPAL, authId);
		if(authPw != null)
			env.put(Context.SECURITY_CREDENTIALS, authPw);
		return env;
	}
	
	public void setAuth(String authId, String authPw) {
		authMethod = "simple";
		this.authId = authId;
		this.authPw = authPw;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getAuthId() {
		return authId;
	}

	public void setAuthId(String authId) {
		this.authId = authId;
	}

	public String getAuthPw() {
		return authPw;
	}

	public void setAuthPw(String authPw) {
		this.authPw = authPw;
	}

	public String getAuthMethod() {
		return authMethod;
	}

	public void setAuthMethod(String authMethod) {
		this.authMethod = authMethod;
	}
	
	/**
	 * Create a LdapConnection using this LdapConnectionConfig
	 * @return A new LdapConnection associated with this LdapConnectionConfig 
	 * @throws NamingException
	 */
	public LdapConnection newConnection() throws NamingException {
		return new LdapConnection(this);
	}
}
