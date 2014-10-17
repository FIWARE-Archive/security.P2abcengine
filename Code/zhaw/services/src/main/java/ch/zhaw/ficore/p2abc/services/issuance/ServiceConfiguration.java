package ch.zhaw.ficore.p2abc.services.issuance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Holds the configuration for all the services.
 * 
 * This class, implemented as a singleton, holds the configuration for
 * all the services defined here.  This class can be loaded from and
 * stored to a file, and parts of the configuration can be selectively
 * retrieved and updated.
 * 
 * Note for implementers and other people who add to this class: You cannot
 * employ simple getters and setters on fields, for the simple reason that this
 * class is being used from within a servlet container, which means that there
 * may be several threads trying to get information from or change information
 * in this class.  You will have to get and set parameters in blocks, and to
 * synchronise them, too.  You <em>must</em> make sure that after <em>any</em>
 * call to a set... method, the configuration is left in a consistent state,
 * and you em>must</em> make sure that <em>any</em> call to a get... method
 * returns a <em>consistent</em> view of the configuration.  Take a look at
 * {@link #setLdapParameters(boolean, String, int, String, String)}
 * and {@link #getLdapParameters()} to see how it's done.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
public class ServiceConfiguration {
  /** What identity source will we be using for the issuer? */
  public enum IdentitySource {
    KEYROCK,  /** Use keyrock as the identity source. */
    LDAP,     /** Use an LDAP server as the identity source. */
    FAKE,	  /** Use a fake identity source. */
  }
  
  /** This class is implemented as a singleton. */
  private static ServiceConfiguration configuration = new ServiceConfiguration();
  
  /** Configuration parameters for LDAP. */
  public class LdapParameters implements Cloneable {
    /** Use TLS with LDAP? */
    public boolean ldapUseTls;

    /** Name of LDAP server. Used only if identitySource == LDAP. */
    public String ldapServerName;
    
    /** LDAP server port. Defaults are 636 if using TLS, 389 if not. */
    public int ldapServerPort;
    
    /** LDAP user that can access all the identity information. */
    public String ldapUser;

    /** Password for LDAP user. */
    public String ldapPassword;
    
    @Override
    public LdapParameters clone() throws CloneNotSupportedException {
      LdapParameters ret = (LdapParameters) super.clone();
      
      ret.ldapUseTls = ldapUseTls;
      ret.ldapServerName = ldapServerName;
      ret.ldapServerPort = ldapServerPort;
      ret.ldapUser = ldapUser;
      ret.ldapPassword = null;
      
      return ret;
    }
  }
  
  private LdapParameters ldapParameters;
  
  private Logger logger;
  
  /** What identity source should we use? */
  private IdentitySource identitySource;
  
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
  private String magicCookie = "*magic*";
  
  /**
   * Verifies the correctness of the magic cookie (i.e. if it matches
   * the one stored in this configuration.)
   * 
   * @param magicCookie The value to check against
   * @return true or false.
   */
  public boolean isMagicCookieCorrect(String magicCookie) {
	  return this.magicCookie.equals(magicCookie);
  }
  
  /**
   * Sets the magic cookie to the given value.
   * 
   * @param magicCookie the new value of the magic cookie.
   */
  public void setMagicCookie(String magicCookie) {
	  this.magicCookie = magicCookie;
  }
  
  public IdentitySource getIdentitySource() {
	  return identitySource;
  }
  
  private ServiceConfiguration() {
    logger = LogManager.getLogger(ServiceConfiguration.class.getName());
    ldapParameters = new LdapParameters();
  }
  
  public synchronized static void defaultConfiguration() {
    configuration.logger.entry();

    configuration.identitySource = IdentitySource.LDAP;
    configuration.ldapParameters.ldapUseTls = true;
    configuration.ldapParameters.ldapServerName = "localhost";
    configuration.ldapParameters.ldapServerPort
      = configuration.ldapParameters.ldapUseTls ? 636 : 389;
    configuration.ldapParameters.ldapUser = "";
    configuration.ldapParameters.ldapPassword = "";

    configuration.logger.info("Setting default configuration: " + configuration);

    configuration.logger.exit();
  }
  
  public synchronized void setFakeParameters() {
	  this.identitySource = IdentitySource.FAKE;
  }
  

  /** Sets LDAP parameters and switches to LDAP as the identity source. 
   * 
   * @param ldapUseTls true if TLS should be used (recommended)
   * @param ldapServerName name of LDAP server
   * @param ldapServerPort port number for LDAP server or 0 for default
   * @param ldapUser LDAP user's name, must have access to all attributes of interest
   * @param ldapPassword LDAP user's password with which to authenticate
   */
  public synchronized void setLdapParameters(boolean ldapUseTls,
        String ldapServerName,
        int ldapServerPort,
        String ldapUser,
        String ldapPassword) {
    logger.entry();
    
    configuration.identitySource = IdentitySource.LDAP;
    configuration.ldapParameters.ldapUseTls = ldapUseTls;
    configuration.ldapParameters.ldapServerName = ldapServerName;
    
    if (ldapServerPort <= 0 || ldapServerPort >= (1 << 16)) {
      configuration.ldapParameters.ldapServerPort = configuration.ldapParameters.ldapUseTls ? 636 : 389;
      
      if (ldapServerPort != 0)
        logger.warn("LDAP server port " + ldapServerPort + " out of range; "
            + "using " + configuration.ldapParameters.ldapServerPort + " instead");
    } else {
    	configuration.ldapParameters.ldapServerPort = ldapServerPort; 
    }
    
    configuration.ldapParameters.ldapUser = ldapUser;
    configuration.ldapParameters.ldapPassword = ldapPassword;
    
    logger.info("Switched to LDAP: " + this);

    logger.exit();
  }
  
  /** Returns a copy of the LDAP configuration parameters.
   * 
   * We return a <em>copy</em> of the configuration parameters
   * instead of a reference to the configuration parameters themselves
   * because we don't want to enable the caller to change these
   * parameters without our knowledge.
   *  
   * @return the current LDAP configuration parameters, if the current
   *   {@link #identitySource} is LDAP, or <code>null</code> otherwise. 
   */
  public synchronized LdapParameters getLdapParameters() {
    logger.entry();
    
    LdapParameters ret = null;
    
    if (identitySource == IdentitySource.LDAP) {
      try {
        ret = ldapParameters.clone();
      } catch (CloneNotSupportedException e) {
        logger.error("LdapParameters can't be cloned: \"" + e.getMessage() 
            + "\". This is decidedly unexpected!");
        ret = null;
      }
    }
    
    return logger.exit(ret);
  }
  
  public static ServiceConfiguration getInstance() {
    return configuration;
  }
  
  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this).
      append("identitySource", identitySource);
    
    if (identitySource == IdentitySource.LDAP) {
      builder = builder
          .append("ldapUseTls", ldapParameters.ldapUseTls)
          .append("ldapServerName", ldapParameters.ldapServerName)
          .append("ldapServerPort", ldapParameters.ldapServerPort)
          .append("ldapUser", ldapParameters.ldapUser)
          .append("ldapPassword", "(not disclosed)");
    } else {
      // Do nothing for now, since we don't know what the configuration
      // parameters for Keyrock are.
    }

    return builder.toString();
  }
}
