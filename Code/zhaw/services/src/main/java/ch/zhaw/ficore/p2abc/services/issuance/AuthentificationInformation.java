package ch.zhaw.ficore.p2abc.services.issuance;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Abstract class that contains the information
 * required to authenticate a user.
 * 
 * @author mroman
 */
@XmlRootElement(name="auth-info")
public abstract class AuthentificationInformation {
	
}