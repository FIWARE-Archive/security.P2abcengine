package ch.zhaw.ficore.p2abc.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.SystemParameters;

/** The XML for settings.
 *
 * @author Roman M&uuml;ntener &lt;roman.muentener@zhaw.ch&gt;
 *
 */
@XmlRootElement(name = "settings", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class Settings {

    @XmlElementWrapper(name = "credential-specification-list", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @XmlElement(name = "credential-specification", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    public List<CredentialSpecification> credentialSpecifications;

    @XmlElementWrapper(name = "issuer-parameters-list", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @XmlElement(name = "issuer-parameters", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    public List<IssuerParameters> issuerParametersList;

    @XmlElement(name = "system-parameters")
    public SystemParameters systemParameters;

    public Settings() { }

}
