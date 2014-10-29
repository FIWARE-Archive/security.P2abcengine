package ch.zhaw.ficore.p2abc.services.issuance;

import eu.abc4trust.xml.CredentialSpecification;
import java.util.List;


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
	 * 
	 * The query parameter tells the AttributeValueProvider how to fetch the
	 * values. E.g., for LDAP query might be an LDAP-Search query. 
	 * 
	 * @param configuration Configuration.
	 */
	public AttributeValueProvider(IssuanceConfigurationData configuration) {
		this.configuration = configuration;
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
		switch(configuration.getAttributeSource()) {
		case LDAP:
			return new LdapAttributeValueProvider(configuration);
		default:
	    return null;
		}
	}
	
	/**
	 * Called when this AttributeValueProvider is no longer required. 
	 * Providers should close open sockets/connections/files etc. on shutdown.
	 */
	public abstract void shutdown();
	
	
	
	/**
	 * Obtains the attributes to be embedded in the IssuancePolicyAndAttributes. 
	 * The query parameter tells the AttributeValueProvider how to fetch the values.
	 * E.g. for LDAP query might be an LDAP-Search query. The exact behaviour of <em>query</em> is provider
	 * specific. 
	 * 
	 * @param query Query (see description above) 
	 * @param credSpec CredentialSpecification to obtain attributes for. 
	 */
	public abstract List<eu.abc4trust.xml.Attribute> getAttributes(String query,
			CredentialSpecification credSpec) throws Exception;
}