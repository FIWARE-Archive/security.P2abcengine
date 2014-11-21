package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Abstract class that contains the information required to authenticate a user.
 * 
 * @author mroman
 */
@XmlRootElement(name = "auth-info", namespace="http://abc4trust.eu/wp2/abcschemav1.0")
public abstract class AuthenticationInformation {

}