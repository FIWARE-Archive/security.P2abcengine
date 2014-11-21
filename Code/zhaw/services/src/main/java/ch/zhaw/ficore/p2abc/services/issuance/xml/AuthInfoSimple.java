package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "auth-info-simple", namespace="http://abc4trust.eu/wp2/abcschemav1.0")
public class AuthInfoSimple extends AuthenticationInformation {
    @XmlElement(name = "username", required = true, namespace="http://abc4trust.eu/wp2/abcschemav1.0")
    public String username;

    @XmlElement(name = "password", required = true, namespace="http://abc4trust.eu/wp2/abcschemav1.0")
    public String password;

    public AuthInfoSimple() {
    }

    public AuthInfoSimple(String username, String password) {
        this.username = username;
        this.password = password;
    }
}