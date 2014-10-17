package ch.zhaw.ficore.p2abc.services.issuance;

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
}