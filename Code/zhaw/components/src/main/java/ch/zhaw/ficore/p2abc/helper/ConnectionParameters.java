package ch.zhaw.ficore.p2abc.helper;

import org.apache.commons.lang3.builder.ToStringBuilder;

/** Parameters for connections to attribute or authentication sources.
 * 
 * This class holds parameters to use when connecting to a standard
 * TCP or UDP service such as LDAP or a SQL database.  It is assumed
 * that the information in this class is sufficient to connect.
 * 
 * This class is <em>almost</em> immutable: once connectivity has been
 * established, users of this class should call
 * {@link #passwordNoLongerNeeded()} to erase the memory containing the
 * password.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
public class ConnectionParameters implements Cloneable {
  private static final int MAX_PORT = (1 << 16) - 1;
  private String serverName;
  private int serverPort;
  private String autheticationMethod;
  private String user;
  private char[] password;
  private boolean useTls;
  
  /** Creates connection parameters.
   * 
   * Uses authentication method "simple" by default.
   * 
   * @param serverName the server to connect to
   * @param serverPort the port number; may be 0 for default
   * @param normalDefaultPort the default port when not using TLS
   * @param tlsDefaultPort the default port when using TLS
   * @param user the user name to use when authenticating
   * @param password the password to use when authenticating
   * @param useTls whether or not to use TLS
   */
  public ConnectionParameters(String serverName, int serverPort,
      int normalDefaultPort, int tlsDefaultPort,
      String user, char[] password, boolean useTls) {
    super();
    this.serverName = serverName;
    if (serverPort > 0 && serverPort <= MAX_PORT)
      this.serverPort = serverPort;
    else
      this.serverPort = useTls ? tlsDefaultPort : normalDefaultPort;
    this.autheticationMethod = "simple";
    this.user = user;
    this.password = password;
    this.useTls = useTls;
  }

  public String getServerName() {
    return serverName;
  }

  public int getServerPort() {
    return serverPort;
  }

  public String getAuthenticationMethod() {
    return autheticationMethod;
  }

  public String getUser() {
    return user;
  }

  public char[] getPassword() {
    return password;
  }

  /** Erase memory containing the password. 
   * 
   * This method should be called once a connection to a server has been
   * successfully established. It erases the password by writing zero
   * characters over its storage. If connectivity is subsequently lost,
   * the password needs to be re-acquired before attempting to connect
   * again.  
   */
  public void passwordNoLongerNeeded() {
    if (password != null) {
      for (int i = 0; i < password.length; i++)
        password[i] = 0;
    }
  }
  
  public boolean usesTls() {
    return useTls;
  }
  
  @Override
  public ConnectionParameters clone() {
    return new ConnectionParameters(serverName, serverPort, serverPort,
        serverPort, user, password, useTls);
  }
  
  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("serverName", serverName)
      .append("serverPort", serverPort)
      .append("user", user)
      .append("password", "(withheld)")
      .append("useTls", useTls)
      .toString();
  }
}
