package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElements;

import ch.zhaw.ficore.p2abc.services.issuance.*;

@XmlRootElement(name="auth-request")
public class AuthentificationRequest {
	@XmlElements({
		@XmlElement(type=AuthInfoSimple.class, name="auth-info-simple")
	})
	public AuthentificationInformation authInfo;
	
	public AuthentificationRequest() {}
	
	public AuthentificationRequest(AuthentificationInformation authInfo) {
		this.authInfo = authInfo;
	}
}