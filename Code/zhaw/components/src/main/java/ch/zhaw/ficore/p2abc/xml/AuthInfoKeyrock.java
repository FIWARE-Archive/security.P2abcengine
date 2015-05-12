package ch.zhaw.ficore.p2abc.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "auth-info-keyrock", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class AuthInfoKeyrock extends AuthenticationInformation {
    @XmlElement(name = "accessToken", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    public String accessToken;

    public AuthInfoKeyrock() {
    }

    public AuthInfoKeyrock(String accessToken) {
        this.accessToken = accessToken;
    }
}