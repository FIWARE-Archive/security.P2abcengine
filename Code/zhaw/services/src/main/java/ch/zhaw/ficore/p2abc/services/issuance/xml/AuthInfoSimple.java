package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import ch.zhaw.ficore.p2abc.services.issuance.AuthenticationInformation;

@XmlRootElement(name="auth-info-simple")
public class AuthInfoSimple extends AuthenticationInformation {
	@XmlElement(name="username", required=true)
	public String username;
	
	@XmlElement(name="password", required=true)
	public String password;
	
	public AuthInfoSimple(){}
	
	public AuthInfoSimple(String username, String password) {
		this.username = username;
		this.password = password;
	}
}