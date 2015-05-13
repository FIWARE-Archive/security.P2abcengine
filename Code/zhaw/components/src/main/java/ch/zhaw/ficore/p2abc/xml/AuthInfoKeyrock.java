package ch.zhaw.ficore.p2abc.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/** The XML for keyrock authentication information.
 *
 * @author Roman M&uuml;ntener &lt;roman.muentener@zhaw.ch&gt;
 *
 */
@XmlRootElement(name = "auth-info-keyrock", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class AuthInfoKeyrock extends AuthenticationInformation {
    @XmlElement(name = "accessToken", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    public String accessToken;

    public AuthInfoKeyrock() {
    }

    public AuthInfoKeyrock(final String accessToken) {
        this.accessToken = accessToken;
    }
}
