package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.services.ConfigurationData;


/** Serves as a Factory for AttributeValueProviders.
 * 
 * An AttributeValueProvider is capable of providing the values of attributes
 * for a certain credential. In short: AttributeValueProviders provide
 * the IssuanceAttributes. An AttributeValueProvider may or may not work
 * together with an AuthenticationProvider. The same instance of an
 * AttributeValueProvider shall not be used twice. 
 * 
 * @author mroman
 */
public abstract class AttributeValueProvider {
	
	protected IssuanceConfigurationData configuration;
	
	/**
	 * Constructor of an AttributeValueProvider.
	 * The query parameter tells the AttributeValueProvider how to fetch the values.
	 * E.g. for LDAP query might be an LDAP-Search query. The exact behaviour of <em>query</em> is provider
	 * specific. This constructor calls the <em>protected loadAttributes</em> method. 
	 * 
	 * @param configuration Configuration.
	 * @param authInfo AuthenticationInformation of the user
	 * @param query Query (see description above)
	 */
	public AttributeValueProvider(IssuanceConfigurationData configuration, AuthenticationInformation authInfo, String query) {
		this.configuration = configuration;
		loadAttributes(authInfo, query);
	}
	
	/**
	 * Factory method to construct an AttributeValueProvider for a given
	 * ServiceConfiguration. The AttributeValueProvider will receive
	 * a reference to the configuration.
	 * 
	 * @param configuration Issuance configuration
	 * @return an implementation of an AttributeValueProvider
	 */
	public static AttributeValueProvider getAttributeValueProvider(IssuanceConfigurationData configuration) {
		switch(configuration.getIdentitySource()) {
		default:
		  //return null;
		}
		return null;
	}
	
	/**
	 * Called when this AttributeValueProvider is no longer required. 
	 * Providers should close open sockets/connections/files etc. on shutdown.
	 */
	public abstract void shutdown();
	
	/**
	 * Returns the String representation of an attribute's value for embedding into the IssuanceAttributes (XML).
	 * 
	 * @param name Name of the attribute
	 * @param type Type of the attribute as specified in the CredentialSpecification
	 * @param encoding Encoding of the attribute as specified in the CredentialSpecification
	 * @return String representation
	 */
	public abstract String getAttributeValue(String name, String type, String encoding);
	
	/**
	 * Called by the constructor. This method <b>MUST</b> perform the required initialization 
	 * for getAttributeValue. This method may or may not cache the information retreived from the
	 * IdentitySource. The query parameter tells the AttributeValueProvider how to fetch the values.
	 * E.g. for LDAP query might be an LDAP-Search query. The exact behaviour of <em>query</em> is provider
	 * specific. 
	 * 
	 * @param authInfo AuthenticationInformation of the user.
	 * @param query Query (see description above) 
	 */
	protected abstract void loadAttributes(AuthenticationInformation authInfo, String query);
}