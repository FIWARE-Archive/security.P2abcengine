package ch.zhaw.ficore.p2abc.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.issuance.IssuanceConfigurationData;
import ch.zhaw.ficore.p2abc.services.issuance.IssuanceConfigurationData.IdentitySource;
import ch.zhaw.ficore.p2abc.services.user.UserConfigurationData;
import ch.zhaw.ficore.p2abc.services.verification.VerificationConfigurationData;

/** Holds the configuration for all the services.
 * 
 * This class, implemented only through static methods, holds the configuration
 * for all the services defined here.  This class can be loaded from and
 * stored to a file, and parts of the configuration can be selectively
 * retrieved and updated.
 * 
 * Note for implementers and other people who add to this class: You cannot
 * employ simple getters and setters on fields, for the simple reason that this
 * class is being used from within a servlet container, which means that there
 * may be several threads trying to get information from or change information
 * in this class.  You will have to get and set entire configurations. See
 * {@link #setConfigurationFor(ServiceType)}
 * and {@link #getConfigurationFor(ServiceType)} to see how it's done.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
public class ServicesConfiguration {
  /** The services for which we hold the configuration. */
  public enum ServiceType {
    ISSUANCE,
    VERIFICATION,
    USER,
  }
  
  /** Configuration data for issuance service. */
  private static IssuanceConfigurationData issuanceConfiguration = new IssuanceConfigurationData();
  
  /** Configuration data for issuance service. */
  private static VerificationConfigurationData verificationConfiguration = new VerificationConfigurationData();
  
  /** Configuration data for issuance service. */
  private static UserConfigurationData userConfiguration = new UserConfigurationData();
  
  private static Logger logger = LogManager.getLogger(ServicesConfiguration.class);

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
  public static String magicCookie = "*magic*";

  /**
   * Verifies the correctness of the magic cookie (i.e. if it matches
   * the one stored in this configuration.)
   * 
   * @param magicCookie The value to check against
   * @return true or false.
   */
  public static synchronized boolean isMagicCookieCorrect(String magicCookie) {
	  return ServicesConfiguration.magicCookie.equals(magicCookie);
  }
  
  /**
   * Sets the magic cookie to the given value.
   * 
   * @param magicCookie the new value of the magic cookie.
   */
  public static synchronized void setMagicCookie(String magicCookie) {
    ServicesConfiguration.magicCookie = magicCookie;
  }
  
  /** Private do-nothing constructor to prevent construction of instances. */
  private ServicesConfiguration () {
  }
  
  /** Returns a copy of the current issuance configuration.
   * 
   * We return a <em>copy</em> of the issuance parameters
   * instead of a reference to the issuance parameters themselves
   * because we don't want to enable the caller to change these
   * parameters without our knowledge.
   *  
   * @return the current issuance parameters, or <code>null</code>
   *    if there was a problem cloning the current configuration. 
   */
  public static synchronized ConfigurationData getConfigurationFor(ServiceType type) {
    logger.entry();
    
    ConfigurationData ret = null;
    
    try {
      switch (type) {
      case ISSUANCE: ret = issuanceConfiguration.clone(); break;
      case VERIFICATION: ret = verificationConfiguration.clone(); break;
      case USER: ret = userConfiguration.clone(); break;
      }
    } catch (CloneNotSupportedException e) {
      logger.error("Service configuration can't be cloned: \""
           + e.getMessage() + "\". This is decidedly unexpected!");
    }
    
    return logger.exit(ret);
  }
  
  
  private static boolean serviceTypeMatchesObjectType(ServiceType type, ConfigurationData newConfig) {
    return type == ServiceType.ISSUANCE && newConfig instanceof IssuanceConfigurationData
        || type == ServiceType.VERIFICATION && newConfig instanceof VerificationConfigurationData
        || type == ServiceType.USER && newConfig instanceof UserConfigurationData;
  }
  
  /** Replaces the current issuance configuration. 
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
  public static synchronized void setConfigurationFor(ServiceType type, ConfigurationData newConfig) {
    logger.entry();
    boolean typesMatch = true;
    
    if (!serviceTypeMatchesObjectType(type, newConfig)) {
      logger.error("Type mismatch: attemtpt to overwrite " + type 
          + " configuration with object of type " + newConfig.getClass());
      typesMatch = false;
    }

    if (typesMatch && newConfig.isGood()) {
      ConfigurationData configuration = null;
      
      switch (type) {
      case ISSUANCE: configuration = issuanceConfiguration; break;
      case VERIFICATION: configuration = verificationConfiguration; break;
      case USER: configuration = userConfiguration; break;
      }

      logger.info("Old configuration: " + configuration);
      
      try {
        issuanceConfiguration = newConfig.clone();
        logger.info("New configuration: " + issuanceConfiguration);
      } catch (CloneNotSupportedException e) {
        logger.error("Service configuration can't be cloned: \""
           + e.getMessage() + "\". This is decidedly unexpected!");
        logger.warn("Attempt to configure services unsuccessful");
      }
    } else {
      logger.warn("Problems detected with configuration; the configuration"
         + " was NOT overwritten and the old configuration is still in"
         + " effect.");
    }
    logger.exit();
  }

  public static synchronized void setFakeIssuanceParameters() {
    issuanceConfiguration.identitySource = IdentitySource.FAKE;
    // TODO: Set more parameters?
  }

  /** Returns a copy of the current verification parameters.
   * 
   * We return a <em>copy</em> of the configuration parameters
   * instead of a reference to the configuration parameters themselves
   * because we don't want to enable the caller to change these
   * parameters without our knowledge.
   *  
   * @return the current configuration parameters, or <code>null</code>
   *    if there was a problem cloning the current configuration. 
   */
  public static synchronized VerificationConfigurationData getVerificationConfiguration() {
    logger.entry();
    
    VerificationConfigurationData ret = null;
    
    try {
      ret = verificationConfiguration.clone();
    } catch (CloneNotSupportedException e) {
      logger.error("Service configuration can't be cloned: \""
           + e.getMessage() + "\". This is decidedly unexpected!");
    }
    
    return logger.exit(ret);
  }

  /** Replaces the current issuance configuration. 
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
  public static synchronized void setVerificationConfiguration(VerificationConfigurationData newConfig) {
    logger.entry();
  
    logger.info("Old configuration: " + verificationConfiguration);
    if (VerificationConfigurationData.isGoodVerificationConfiguration(newConfig)) {
      try {
        verificationConfiguration = newConfig.clone();
        logger.info("New configuration: " + verificationConfiguration);
      } catch (CloneNotSupportedException e) {
        logger.error("Service configuration can't be cloned: \""
           + e.getMessage() + "\". This is decidedly unexpected!");
        logger.warn("Attempt to configure services unsuccessful");
      }
    } else {
      logger.warn("Problems detected with service"
         + " configuration; the configuration was NOT overwritten and"
         + " the old configuration is still in effect.");
    }
    logger.exit();
  }
}
