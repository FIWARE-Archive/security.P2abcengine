package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import ch.zhaw.ficore.p2abc.services.issuance.*;

@XmlRootElement(name="auth-info-simple")
public class AuthInfoSimple extends AuthentificationInformation {
	public String username;
	public String password;
	
	public AuthInfoSimple(){}
	
	public AuthInfoSimple(String username, String password) {
		this.username = username;
		this.password = password;
	}
}