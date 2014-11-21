package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "issuance-request", namespace="http://abc4trust.eu/wp2/abcschemav1.0")
public class IssuanceRequest {

    @XmlElement(name = "auth-request", required = true, namespace="http://abc4trust.eu/wp2/abcschemav1.0")
    public AuthenticationRequest authRequest;

    @XmlElement(name = "credential-specification-uid", required = true, namespace="http://abc4trust.eu/wp2/abcschemav1.0")
    public String credentialSpecificationUid;
}