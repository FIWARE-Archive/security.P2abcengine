package ch.zhaw.ficore.p2abc.xml;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Abstract class that contains the information required to authenticate a user.
 *
 * @author Roman M&uuml;ntener &lt;roman.muentener@zhaw.ch&gt;
 */
@XmlRootElement(name = "auth-info", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public abstract class AuthenticationInformation {

}
