package ch.zhaw.ficore.p2abc.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "auth-info-simple", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class AuthInfoSimple extends AuthenticationInformation {
    @XmlElement(name = "username", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification="Field is read from another project")
    public String username;

    @XmlElement(name = "password", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification="Field is read from another project")
    public String password;

    public AuthInfoSimple() {
    }

    public AuthInfoSimple(final String username, final String password) {
        this.username = username;
        this.password = password;
    }
}