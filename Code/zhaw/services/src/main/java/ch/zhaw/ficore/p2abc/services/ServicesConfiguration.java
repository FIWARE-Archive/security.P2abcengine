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
 * in this class.  You will have to get and set entire configurations.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
public class ServicesConfiguration {
	/** The services for which we hold the configuration. */
	public enum ServiceType {
		ISSUANCE,
		VERIFICATION,
		USER
	}

	/** Configuration data for issuance service. */
	private static IssuanceConfigurationData issuanceConfiguration;

	/** Configuration data for issuance service. */
	private static VerificationConfigurationData verificationConfiguration;

	/** Configuration data for issuance service. */
	private static UserConfigurationData userConfiguration;
	
	/** Storage configuration **/
	private static StorageConfiguration storageConfiguration = new SqliteStorageConfiguration(); //TODO: make more generic -- munt

	private static Logger logger = LogManager.getLogger();

	/** Magic Cookie.
	 * 
	 * The Magic Cookie is used for service administration such as changing
	 * or reloading the configuration as well as storing or creating
	 * CredentialSpecifications and more.
	 * 
	 * The default value is <b>*magic*</b>. It's <em>HIGHLY</em> recommended
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
	 * @return true iff the parameter is the same as the stored cookie.
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
	
	
	/**
	 * Returns a copy of the current storage configurotion.
	 * 
	 * @return copy (StorageConfiguration)
	 */
	public static synchronized StorageConfiguration getStorageConfiguration() {
		logger.entry();

		StorageConfiguration ret = null;
		
		try {
			ret = (StorageConfiguration) storageConfiguration.clone();
		} catch (CloneNotSupportedException e) {
			logger.error("Storage configuration can't be cloned: \""
					+ e.getMessage() + "\". This is decidedly unexpected!");
		}

		return logger.exit(ret);
	}

	/** Returns the current issuance configuration.
	 *  
	 * @return the current issuance parameters. 
	 */
	public static synchronized IssuanceConfigurationData getIssuanceConfiguration() {
		return issuanceConfiguration;
	}

	public static synchronized VerificationConfigurationData getVerificationConfiguration() {
    return verificationConfiguration;
	}

	public static synchronized UserConfigurationData getUserConfiguration() {
    return userConfiguration;
	}


	/** Replaces the current issuance configuration. 
	 * 
	 * The new configuration is scrutinised and, if all sanity checks are
	 * passed, the old configuration is replaced with the new one.  If there
	 * is something wrong with the configuration, the current configuration
	 * is retained.
	 * 
	 * @param newConfig the new configuration
	 */
	public static synchronized void setIssuanceConfiguration(IssuanceConfigurationData newConfig) {
	  logger.entry();
	  
	  if (newConfig.isPlausible())
	    issuanceConfiguration = newConfig;
	  else
	    logger.warn("Issuance configuration not plausible, retaining old one");

	  logger.exit();
	}

	public static synchronized void setVerificationConfiguration(VerificationConfigurationData newConfig) {
    verificationConfiguration = newConfig;
	}

	public static synchronized void setUserConfiguration(UserConfigurationData newConfig) {
    userConfiguration = newConfig;
	}

	public static synchronized void setFakeIssuanceParameters() {
    issuanceConfiguration.setFakeSources();
		// TODO: Set more parameters?
	}
}
