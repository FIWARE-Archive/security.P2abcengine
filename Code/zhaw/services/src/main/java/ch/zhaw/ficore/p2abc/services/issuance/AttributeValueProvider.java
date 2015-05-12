package ch.zhaw.ficore.p2abc.services.issuance;

import java.util.List;

import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import eu.abc4trust.xml.CredentialSpecification;

/**
 * Serves as a Factory for AttributeValueProviders.
 * 
 * An AttributeValueProvider is capable of providing the values of attributes
 * for a certain credential. In short: AttributeValueProviders provide the
 * IssuanceAttributes. An AttributeValueProvider may or may not work together
 * with an AuthenticationProvider. The same instance of an
 * AttributeValueProvider shall not be used twice.
 * 
 * @author mroman
 */
public abstract class AttributeValueProvider {

    protected IssuanceConfiguration configuration;

    /**
     * Constructor of an AttributeValueProvider.
     * 
     * The query parameter tells the AttributeValueProvider how to fetch the
     * values. E.g., for LDAP query might be an LDAP-Search query.
     * 
     * @param configuration
     *            Configuration.
     */
    public AttributeValueProvider(IssuanceConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Factory method to construct an AttributeValueProvider for a given
     * ServiceConfiguration. The AttributeValueProvider will receive a reference
     * to the configuration.
     * 
     * @param configuration
     *            Issuance configuration
     * @return an implementation of an AttributeValueProvider
     */
    public static AttributeValueProvider getAttributeValueProvider(
            IssuanceConfiguration configuration) {
        switch (configuration.getAttributeSource()) {
        case LDAP:
            return new LdapAttributeValueProvider(configuration);
        case FAKE:
            return new FakeAttributeValueProvider(configuration);
        case JDBC:
            return new JdbcAttributeValueProvider(configuration);
        default:
            return null;
        }
    }

    /**
     * Called when this AttributeValueProvider is no longer required. Providers
     * should close open sockets/connections/files etc. on shutdown.
     */
    public abstract void shutdown();

    /**
     * Obtains the attributes to be embedded in the IssuancePolicyAndAttributes.
     * The query parameter tells the AttributeValueProvider how to fetch the
     * values. E.g. for LDAP query might be an LDAP-Search query. The exact
     * behaviour of <em>query</em> is provider specific.
     * 
     * @param query
     *            Query (see description above)
     * @param uid
     *            UserID
     * @param credSpec
     *            CredentialSpecification to obtain attributes for.
     * @return list of attributes.
     * @throws Exception
     *             when something went wrong.
     */
    public abstract List<eu.abc4trust.xml.Attribute> getAttributes(
            String query, String uid, CredentialSpecification credSpec)
            throws Exception;
}
