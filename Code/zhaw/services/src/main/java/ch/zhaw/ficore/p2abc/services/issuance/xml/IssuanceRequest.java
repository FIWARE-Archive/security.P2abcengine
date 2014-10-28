package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="issuance-request")
public class IssuanceRequest {
	
	@XmlElement(name="auth-request", required=true)
	public AuthenticationRequest authRequest;
	
	@XmlElement(name="credential-specification-uid", required=true)
	public String credentialSpecificationUid;
}