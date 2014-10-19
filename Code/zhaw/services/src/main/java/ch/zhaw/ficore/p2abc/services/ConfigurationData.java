package ch.zhaw.ficore.p2abc.services;

import org.apache.commons.lang3.builder.ToStringBuilder;

/** Data container for the configuration. */
public class ConfigurationData implements Cloneable {
  /** What identity source will we be using for the issuer? */
  public enum IdentitySource {
    KEYROCK,  /** Use keyrock as the identity source. */
    LDAP,     /** Use an LDAP server as the identity source. */
    FAKE,     /** Use a fake identity source. */
  }

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
  
  /** Magic Cookie
   * The Magic Cookie is used for service administration such as changing
   * or reloading the configuration as well as storing or creating
   * CredentialSpecifications and more.
   * 
   * The default value is <b>*magic*</b>. It's <b>HIGHLY</b> recommended
   * to change it to a secure value and to change it frequently.
   * Please only communicate over secure channels and secure applications
   * when transmitting the magic cookie. 
   */
  public String magicCookie = "*magic*";
  

  @Override
  public ConfigurationData clone() throws CloneNotSupportedException {
    ConfigurationData ret = (ConfigurationData) super.clone();
    
    ret.identitySource = identitySource;
    ret.ldapUseTls = ldapUseTls;
    ret.ldapServerName = ldapServerName;
    ret.ldapServerPort = ldapServerPort;
    ret.ldapUser = ldapUser;
    ret.ldapPassword = ldapPassword;
    ret.magicCookie = magicCookie;
    
    return ret;
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

}

