package ch.zhaw.ficore.p2abc.configuration;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Data container for issuance service configuration.
 *
 * At the moment, the issuance service offers two identity sources, LDAP and
 * Keyrock. For LDAP, no transport protocol can be configured, so it's up to the
 * LDAP issuer to decide what transport protocol to use.
 *
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
@XmlRootElement(name = "issuance-configuration")
public class IssuanceConfiguration {
    /** What source will we be using for attributes or authentication? */
    public enum IdentitySource {
        KEYROCK, /** Use keyrock. */
        LDAP, /** Use LDAP. */
        FAKE, /** Use a fake (testing only). */
        JDBC
        /** Use JDBC **/
    }

    /** What attribute source should we use? */
    @XmlElement(name = "attribute-source")
    private IdentitySource attributeSource;

    /** How to connect to the attribute provider? */
    @XmlElement(name = "attribute-connection")
    private ConnectionParameters attributeConnectionParameters;

    /** What authentication source should we use? */
    @XmlElement(name = "identity-source")
    private IdentitySource authenticationSource;

    /** How to connect to the authentication provider? */
    @XmlElement(name = "identity-connection")
    private ConnectionParameters authenticationConnectionParameters;

    /** The binding query. */
    @XmlElement(name = "bind-query")
    private String bindQuery;

    /** Creates an empty issuance configuration. */
    public IssuanceConfiguration() {
        super();
    }

    /** Creates an issuance configuration.
     *
     * @param attributeSource the attribute source to use
     * @param attributeConnectionParameters what connection parameters to use
     *     for the attribute source
     * @param authenticationSource what authentication source to use
     * @param authenticationConnectionParameters what connection parameters
     *     to use for the authentication source
     * @param bindQuery the bind query to use
     */
    public IssuanceConfiguration(final IdentitySource attributeSource,
            final ConnectionParameters attributeConnectionParameters,
            final IdentitySource authenticationSource,
            final ConnectionParameters authenticationConnectionParameters,
            final String bindQuery) {
        super();
        this.attributeSource = attributeSource;
        this.attributeConnectionParameters = attributeConnectionParameters;
        this.authenticationSource = authenticationSource;
        this.authenticationConnectionParameters
            = authenticationConnectionParameters;
        this.bindQuery = bindQuery;
    }

    public final IdentitySource getAttributeSource() {
        return attributeSource;
    }

    public final ConnectionParameters getAttributeConnectionParameters() {
        return attributeConnectionParameters;
    }

    public final IdentitySource getAuthenticationSource() {
        return authenticationSource;
    }

    public final ConnectionParameters getAuthenticationConnectionParameters() {
        return authenticationConnectionParameters;
    }

    public final String getBindQuery() {
        return bindQuery;
    }

    @Override
    public final String toString() {
        return new ToStringBuilder(this)
                .append("attributeSource", attributeSource)
                .append("attributeConnectionParameters",
                        attributeConnectionParameters)
                .append("authenticationSource", authenticationSource)
                .append("authenticationConnectionParameters",
                        authenticationConnectionParameters)
                .append("bindQuery", bindQuery).toString();
    }

    /** Makes this configuration use a fake source for attributes and
     * authentication.
     *
     * WARNING: Do not use this in production.
     */
    public final void setFakeSources() {
        attributeSource = IdentitySource.FAKE;
        authenticationSource = IdentitySource.FAKE;
    }

}
