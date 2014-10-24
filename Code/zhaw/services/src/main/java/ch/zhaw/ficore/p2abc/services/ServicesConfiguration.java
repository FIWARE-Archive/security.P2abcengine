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
		USER
	}

	/** Configuration data for issuance service. */
	private static IssuanceConfigurationData issuanceConfiguration = new IssuanceConfigurationData();

	/** Configuration data for issuance service. */
	private static VerificationConfigurationData verificationConfiguration = new VerificationConfigurationData();

	/** Configuration data for issuance service. */
	private static UserConfigurationData userConfiguration = new UserConfigurationData();
	
	/** Storage configuration **/
	private static StorageConfiguration storageConfiguration = new SqliteStorageConfiguration(); //TODO: make more generic -- munt

	private static Logger logger = LogManager.getLogger();

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
	public static synchronized IssuanceConfigurationData getIssuanceConfiguration() {
		return (IssuanceConfigurationData) getConfigurationFor(ServiceType.ISSUANCE);
	}

	public static synchronized VerificationConfigurationData getVerificationConfiguration() {
		return (VerificationConfigurationData) getConfigurationFor(ServiceType.VERIFICATION);
	}

	public static synchronized UserConfigurationData getUserConfiguration() {
		return (UserConfigurationData) getConfigurationFor(ServiceType.USER);
	}

	private static synchronized ConfigurationData getConfigurationFor(ServiceType type) {
		logger.entry();

		ConfigurationData ret = null;
		ConfigurationData src = getConfigurationReferenceFor(type);

		try {
			assert src != null;
			ret = src.clone();
		} catch (CloneNotSupportedException e) {
			logger.error("Service configuration can't be cloned: \""
					+ e.getMessage() + "\". This is decidedly unexpected!");
		}

		return logger.exit(ret);
	}

	private static ConfigurationData getConfigurationReferenceFor(ServiceType type) {
		ConfigurationData ret = null;

		switch (type) {
		case ISSUANCE: ret = issuanceConfiguration; break;
		case VERIFICATION: ret = verificationConfiguration; break;
		case USER: ret = userConfiguration; break;
		}

		return ret;
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
	 * @param newConfig the new configuration
	 */
	public static synchronized void setIssuanceConfiguration(IssuanceConfigurationData newConfig) {
		setConfigurationFor(ServiceType.ISSUANCE, newConfig);
	}

	public static synchronized void setVerificationConfiguration(ConfigurationData newConfig) {
		setConfigurationFor(ServiceType.VERIFICATION, newConfig);    
	}

	public static synchronized void setUserConfiguration(UserConfigurationData newConfig) {
		setConfigurationFor(ServiceType.USER, newConfig);
	}

	private static synchronized void setConfigurationFor(ServiceType type, ConfigurationData newConfig) {
		logger.entry();

		boolean nonNull = true;
		if (newConfig == null) {
			logger.warn("Trying to set configuration for " + type + " to null; ignored");
			nonNull = false;
		}

		boolean typesMatch = true;
		if (nonNull && !serviceTypeMatchesObjectType(type, newConfig)) {
			logger.error("Type mismatch: attemtpt to overwrite " + type 
					+ " configuration with object of type " + newConfig.getClass());
			typesMatch = false;
		}

		if (nonNull && typesMatch && newConfig.isGood()) {
			ConfigurationData configuration = getConfigurationReferenceFor(type);
			logger.info("Old configuration: " + configuration);

			try {
				ConfigurationData newData = newConfig.clone();
				switch (type) {
				case ISSUANCE: issuanceConfiguration = (IssuanceConfigurationData) newData; break;
				case VERIFICATION: verificationConfiguration = (VerificationConfigurationData) newData; break;
				case USER: userConfiguration = (UserConfigurationData) newData; break;
				}

				logger.info("New configuration: " + configuration);
				
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
		issuanceConfiguration.setIdentitySource(IdentitySource.FAKE);
		// TODO: Set more parameters?
	}
}
