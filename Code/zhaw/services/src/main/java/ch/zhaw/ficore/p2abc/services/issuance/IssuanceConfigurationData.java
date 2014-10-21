package ch.zhaw.ficore.p2abc.services.issuance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.ConfigurationData;

/** Data container for issuance service configuration.
 * 
 * At the moment, the issuance service offers two identity sources, LDAP and
 * Keyrock. For LDAP, no transport protocol can be configured, so it's up to
 * the LDAP issuer to decide what transport protocol to use.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 */
public class IssuanceConfigurationData implements ConfigurationData {
  /** Default port for LDAP when TLS is used. */
  private static final int LDAP_TLS_DEFAULT_PORT = 636;

  /** Default port for LDAP when TLS is not used. */
  private static final int LDAP_DEFAULT_PORT = 389;
  
  /** What identity source will we be using for the issuer? */
  public enum IdentitySource {
    KEYROCK,  /** Use keyrock as the identity source. */
    LDAP,     /** Use an LDAP server as the identity source. */
    FAKE,     /** Use a fake identity source. */
  }

  private Logger logger;

  /** What identity source should we use? */
  public IdentitySource identitySource;
  
  /** Use TLS with LDAP? */
  public boolean ldapUseTls;

  /** Name of LDAP server. Used only if identitySource == LDAP. */
  public String ldapServerName;

  /** LDAP server port. Defaults are 636 if using TLS, 389 if not. 
   * Used only if identitySource == LDAP. */
  public int ldapServerPort;

  /** LDAP user that can access all the identity information. Used only if
   * identitySource == LDAP. */
  public String ldapUser;

  /** Password for LDAP user. Used only if identitySource == LDAP. */
  public String ldapPassword;
    
  public IssuanceConfigurationData() {
    logger = LogManager.getLogger();
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
    } else {
      // TODO: Default message for now, since we don't know what the
      // configuration parameters for Keyrock are.
      builder = builder
          .append("noKeyrockConfiguration", true);
    }

    return builder.toString();
  }


  @Override
  public boolean isGood() {
    logger.entry();
    boolean ret = true;
    
    if (ldapServerPort <= 0
        || ldapServerPort >= (1 << 16)) {
      if (ldapServerPort != 0)
        logger.warn("LDAP server port "
           + ldapServerPort + " out of range; "
           + "using " + ldapServerPort + " instead");
      ldapServerPort = ldapUseTls
          ? LDAP_TLS_DEFAULT_PORT : LDAP_DEFAULT_PORT;      
    }

    return logger.exit(ret);
  }
  
  @Override
  public IssuanceConfigurationData clone() throws CloneNotSupportedException {
    IssuanceConfigurationData ret = (IssuanceConfigurationData) super.clone();
    return ret;
  }

}

