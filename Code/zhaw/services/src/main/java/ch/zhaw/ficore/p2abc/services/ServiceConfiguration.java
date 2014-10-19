package ch.zhaw.ficore.p2abc.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.ConfigurationData.IdentitySource;

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
 * in this class.  You will have to get and set entire configurations. See
 * {@link #setServiceConfiguration(ServiceConfiguration)}
 * and {@link #getServiceConfiguration()} to see how it's done.
 * 
 * At the moment, this class offers two identity sources, LDAP and Keyrock.
 * For LDAP, no transport protocol can be configured, so it's up to the
 * LDAP issuer to decide what transport protocol to use.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
public class ServiceConfiguration implements Cloneable {
  /** Default port for LDAP when TLS is used. */
  private static final int LDAP_TLS_DEFAULT_PORT = 636;

  /** Default port for LDAP when TLS is not used. */
  private static final int LDAP_DEFAULT_PORT = 389;
  
  private static ConfigurationData configuration = new ConfigurationData();
  
  private static Logger logger = LogManager.getLogger(ServiceConfiguration.class.getName());
  
  /**
   * Verifies the correctness of the magic cookie (i.e. if it matches
   * the one stored in this configuration.)
   * 
   * @param magicCookie The value to check against
   * @return true or false.
   */
  public static synchronized boolean isMagicCookieCorrect(String magicCookie) {
	  return configuration.magicCookie.equals(magicCookie);
  }
  
  /**
   * Sets the magic cookie to the given value.
   * 
   * @param magicCookie the new value of the magic cookie.
   */
  public static synchronized void setMagicCookie(String magicCookie) {
	  configuration.magicCookie = magicCookie;
  }
  
  public static synchronized IdentitySource getIdentitySource() {
	  return configuration.identitySource;
  }
  
  private ServiceConfiguration() {
  }
  
  /** Returns a copy of the current configuration parameters.
   * 
   * We return a <em>copy</em> of the configuration parameters
   * instead of a reference to the configuration parameters themselves
   * because we don't want to enable the caller to change these
   * parameters without our knowledge.
   *  
   * @return the current configuration parameters, or <code>null</code>
   *    if there was a problem cloning the current configuration. 
   */
  public static synchronized ConfigurationData getServiceConfiguration() {
    ServiceConfiguration.logger.entry();
    
    ConfigurationData ret = null;
    
    try {
      ret = ServiceConfiguration.configuration.clone();
    } catch (CloneNotSupportedException e) {
      ServiceConfiguration.logger.error("Service configuration can't be cloned: \""
           + e.getMessage() + "\". This is decidedly unexpected!");
    }
    
    return ServiceConfiguration.logger.exit(ret);
  }
  
  private static boolean isGoodConfiguration(ConfigurationData config) {
    boolean ret = true;
    
    if (config.ldapServerPort <= 0
        || config.ldapServerPort >= (1 << 16)) {
      if (config.ldapServerPort != 0)
        ServiceConfiguration.logger.warn("LDAP server port "
           + config.ldapServerPort + " out of range; "
           + "using " + config.ldapServerPort + " instead");
      config.ldapServerPort = config.ldapUseTls
          ? LDAP_TLS_DEFAULT_PORT : LDAP_DEFAULT_PORT;      
    }

    return ret;
  }
  
  /** Replaces the current configuration. 
   * 
   * The new configuration is scrutinised and, if all sanity checks are
   * passed, the old configuration is replaced with the new one.  If there
   * is something wrong with the configuration, the current configuration
   * is retained.
   *
   * It is possible to pass 0 for ldapServerPort.  In this case, the
   * correct default port is chosen, depending on whether TLS is to
   * be used or not.
   * 
   * @param newConfig the new configuration
   */
  public static synchronized void setServiceConfiguration(ConfigurationData newConfig) {
    ServiceConfiguration.logger.entry();

    ServiceConfiguration.logger.info("Old configuration: " + configuration);
    if (isGoodConfiguration(newConfig)) {
      try {
        configuration = newConfig.clone();
        ServiceConfiguration.logger.info("New configuration: " + configuration);
      } catch (CloneNotSupportedException e) {
        ServiceConfiguration.logger.error("Service configuration can't be cloned: \""
           + e.getMessage() + "\". This is decidedly unexpected!");
        ServiceConfiguration.logger.warn("Attempt to configure services unsuccessful");
      }
    } else {
      ServiceConfiguration.logger.warn("Problems detected with service"
         + " configuration; the configuration was NOT overwritten and"
         + " the old configuration is still in effect.");
    }
    ServiceConfiguration.logger.exit();
  }

  public static synchronized void setFakeParameters() {
    configuration.identitySource = IdentitySource.FAKE;
    // TODO: Set more parameters?
  }
}
