package ch.zhaw.ficore.p2abc.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import eu.abc4trust.xml.Credential;

@XmlRootElement(name = "credential-collection", namespace="http://abc4trust.eu/wp2/abcschemav1.0")
public class CredentialCollection {

    @XmlElementWrapper(name = "credentials", required = true, namespace="http://abc4trust.eu/wp2/abcschemav1.0")
    @XmlElement(name = "credential", namespace="http://abc4trust.eu/wp2/abcschemav1.0")
    public List<Credential> credentials = new ArrayList<Credential>();
    
  
    
    public CredentialCollection() {
        
    }
}