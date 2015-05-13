package ch.zhaw.ficore.p2abc.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/** The XML for simple authentication information.
 *
 * @author Roman M&uuml;ntener &lt;roman.muentener@zhaw.ch&gt;
 *
 */
@XmlRootElement(name = "auth-request", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class AuthenticationRequest {
    @XmlElements({ @XmlElement(type = AuthInfoSimple.class, name = "auth-info-simple", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0"),
        @XmlElement(type = AuthInfoKeyrock.class, name = "auth-info-keyrock", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")})
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
        value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
        justification = "Field is read from another project")
    public AuthenticationInformation authInfo;

    public AuthenticationRequest() {
    }

    public AuthenticationRequest(final AuthenticationInformation authInfo) {
        this.authInfo = authInfo;
    }
}
