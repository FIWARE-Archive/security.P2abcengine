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

    public IssuanceConfiguration() {
        super();
    }

    public IssuanceConfiguration(IdentitySource attributeSource,
            ConnectionParameters attributeConnectionParameters,
            IdentitySource authenticationSource,
            ConnectionParameters authenticationConnectionParameters,
            String bindQuery) {
        super();
        this.attributeSource = attributeSource;
        this.attributeConnectionParameters = attributeConnectionParameters;
        this.authenticationSource = authenticationSource;
        this.authenticationConnectionParameters = authenticationConnectionParameters;
        this.bindQuery = bindQuery;
    }

    public IdentitySource getAttributeSource() {
        return attributeSource;
    }

    public ConnectionParameters getAttributeConnectionParameters() {
        return attributeConnectionParameters;
    }

    public IdentitySource getAuthenticationSource() {
        return authenticationSource;
    }

    public ConnectionParameters getAuthenticationConnectionParameters() {
        return authenticationConnectionParameters;
    }

    public String getBindQuery() {
        return bindQuery;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("attributeSource", attributeSource)
                .append("attributeConnectionParameters",
                        attributeConnectionParameters)
                .append("authenticationSource", authenticationSource)
                .append("authenticationConnectionParameters",
                        authenticationConnectionParameters)
                .append("bindQuery", bindQuery).toString();
    }

    public void setFakeSources() {
        attributeSource = IdentitySource.FAKE;
        authenticationSource = IdentitySource.FAKE;
    }

    public boolean isPlausible() {
        // TODO Auto-generated method stub
        return true;
    }

}
