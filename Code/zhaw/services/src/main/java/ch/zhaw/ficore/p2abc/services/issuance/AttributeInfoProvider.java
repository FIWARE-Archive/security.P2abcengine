package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.services.issuance.xml.*;


/** Serves as a Factory for AttributeInfoProviders.
 * 
 * An AttributeInfoProvider is capable of providing meta-information
 * about Attributes from an identity source. This meta-information
 * is required to create corresponding CredentialSpecifications. 
 * 
 * @author mroman
 */
public abstract class AttributeInfoProvider {
	
	/**
	 * Factory method to construct an AttributeInfoProvider for a given
	 * ServiceConfiguration. The AttributeInfoProvider will receive
	 * a copy of the ServiceConfiguration.
	 * 
	 * @param srvcCfg a ServiceConfiguration
	 * @return an implementation of an AttributeInfoProvider
	 */
	public static AttributeInfoProvider getAttributeInfoProvider(ServiceConfiguration srvcCfg) {
		//TODO: Factory method
		return null;
	}
	
	/**
	 * Called when this AttributeInfoProvider is no longer required. 
	 * Providers should close open sockets/connections/files etc. on shutdown.
	 */
	public abstract void shutdown();
	
	/**
	 * Returns the collected meta-information about attributes of <em>name</em>.
	 * <em>Name</em> may and should be used to distinguish between different objects/credentials
	 * a user can obtain. I.e. <em>name</em> may refer to an objectClass in an LDAP identity source.
	 * The exact behaviour of <em>name</em> is provider specific.
	 * 
	 * @param name <em>name</em>
	 * @return An AttributeInfoCollection
	 */
	public abstract AttributeInfoCollection getAttributes(String name);
}