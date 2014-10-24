package ch.zhaw.ficore.p2abc.services.issuance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.ConfigurationData;
import ch.zhaw.ficore.p2abc.services.ConfigurationException;

/** Data container for issuance service configuration.
 * 
 * At the moment, the issuance service offers two identity sources, LDAP and
 * Keyrock. For LDAP, no transport protocol can be configured, so it's up to
 * the LDAP issuer to decide what transport protocol to use.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
public class IssuanceConfigurationData implements ConfigurationData, Cloneable {
  /** Default port for LDAP when TLS is used. */
  private static final int LDAP_TLS_DEFAULT_PORT = 636;

  /** Default port for LDAP when TLS is not used. */
  private static final int LDAP_DEFAULT_PORT = 389;
  
  /** Maximum legal TCP/UDP port number. */
  private static final int LDAP_MAX_PORT = (1 << 16) - 1;
  
  /** What identity source will we be using for the issuer? */
  public enum IdentitySource {
    KEYROCK,  /** Use keyrock as the identity source. */
    LDAP,     /** Use an LDAP server as the identity source. */
    FAKE,     /** Use a fake identity source. */
  }

  private Logger logger;

  /** What identity source should we use? */
  private IdentitySource identitySource;
  
  /** Use TLS with LDAP? */
  private boolean ldapUseTls;

  /** Name of LDAP server. Used only if identitySource == LDAP. */
  private String ldapServerName;

  /** LDAP server port. Defaults are 636 if using TLS, 389 if not. 
   * Used only if identitySource == LDAP. */
  private int ldapServerPort;

  /** LDAP user that can access all the identity information. Used only if
   * identitySource == LDAP. */
  private String ldapUser;

  /** Password for LDAP user. Used only if identitySource == LDAP. */
  private String ldapPassword;
    

  /** Constructs an empty issuance configuration.
   * 
   *  @warning: {#isGood()} will return <code>false</code> when using
   *  this constructor.
   */
  public IssuanceConfigurationData() {
    logger = LogManager.getLogger();
  }
  
  /** Constructs issuance configuration data for LDAP.
   * 
   * @param ldapUseTls whether to use TLS or not
   * @param ldapServerName the LDAP srever's name
   * @param ldapServerPort the port number to use on the LDAP server. This
   *   port number may be zero, in which case an appropriate default is chosen.
   * @param ldapUser the user name that has access to all the attributes
   * @param ldapPassword the user's password
   */
  public IssuanceConfigurationData(boolean ldapUseTls, String ldapServerName,
      int ldapServerPort, String ldapUser, String ldapPassword) 
          throws ConfigurationException {
    this();
  
    this.identitySource = IdentitySource.LDAP;
    
    this.ldapUseTls = ldapUseTls;
    this.ldapServerName = ldapServerName;
    this.ldapServerPort = ldapServerPort;
    this.ldapUser = ldapUser;
    this.ldapPassword = ldapPassword;
    
    if (ldapServerPort == 0) {
      this.ldapServerPort = ldapUseTls ? LDAP_TLS_DEFAULT_PORT : LDAP_DEFAULT_PORT;
    }
  }
  
  /** Constructs issuance configuration data for the fake provider.
   * 
   * @param fake any boolean
   */
  public IssuanceConfigurationData(boolean fake) {
    this();
    
    this.identitySource = IdentitySource.FAKE;
  }
  
  
  public IdentitySource getIdentitySource() {
    return identitySource;
  }

  public void setIdentitySource(IdentitySource identitySource) {
    this.identitySource = identitySource;
  }

  public boolean doesLdapUseTls() {
    return ldapUseTls;
  }

  public void setLdapUseTls(boolean ldapUseTls) {
    this.ldapUseTls = ldapUseTls;
  }

  public String getLdapServerName() {
    return ldapServerName;
  }

  public void setLdapServerName(String ldapServerName) {
    this.ldapServerName = ldapServerName;
  }

  public int getLdapServerPort() {
    return ldapServerPort;
  }

  /** Sets the LDAP server port.
  *
  * It is possible to pass 0 for ldapServerPort.  In this case, the
  * correct default port is chosen, depending on whether TLS is to
  * be used or not.
  */
  public void setLdapServerPort(int ldapServerPort) {
    logger.entry();
    
    if (ldapServerPort == 0) {
      this.ldapServerPort = ldapUseTls ? LDAP_TLS_DEFAULT_PORT : LDAP_DEFAULT_PORT;
    } else if (ldapServerPort > 0 && ldapServerPort <= LDAP_MAX_PORT) {
      this.ldapServerPort = ldapServerPort;
    } else {
      logger.warn("Trying to set LDAP server port to illegal value "
          + ldapServerPort + "; keeping old value " + this.ldapServerPort);
    }
  }

  public String getLdapUser() {
    return ldapUser;
  }

  public void setLdapUser(String ldapUser) {
    this.ldapUser = ldapUser;
  }

  public String getLdapPassword() {
    return ldapPassword;
  }

  public void setLdapPassword(String ldapPassword) {
    this.ldapPassword = ldapPassword;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this).
      append("identitySource", identitySource);
    
    if (identitySource == IdentitySource.LDAP) {
      builder = builder
          .append("ldapUseTls", ldapUseTls)
          .append("ldapServerName", ldapServerName)
          .append("ldapServerPort", ldapServerPort)
          .append("ldapUser", ldapUser)
          .append("ldapPassword", "(not disclosed)");
    } else if (identitySource == IdentitySource.KEYROCK) {
      // TODO: Default message for now, since we don't know what the
      // configuration parameters for Keyrock are.
      builder = builder
          .append("noKeyrockConfiguration", true);
    } else if (identitySource == IdentitySource.FAKE) {
      builder = builder
          .append("fake", true);
    } else {
      builder = builder
          .append("unhandled identity source", true);
    }

    return builder.toString();
  }

 @Override
  public boolean isGood() {
    boolean ret = true;
    logger.entry();

    if (ldapServerPort < 0
        || ldapServerPort >= (1 << 16)) {
      logger.error("LDAP server port "
          + ldapServerPort + " out of range");
      }

    return logger.exit(ret);    
  }
  
  @Override
  public IssuanceConfigurationData clone() throws CloneNotSupportedException {
    logger.entry();
    
    IssuanceConfigurationData ret = (IssuanceConfigurationData) super.clone();
    ret.identitySource = identitySource;
    ret.ldapUseTls = ldapUseTls;
    ret.ldapServerName = ldapServerName;
    ret.ldapServerPort = ldapServerPort;
    ret.ldapUser = ldapUser;
    ret.ldapPassword = ldapPassword;
    
    return logger.exit(ret);
  }

}

