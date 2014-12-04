package ch.zhaw.ficore.p2abc.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import eu.abc4trust.xml.PresentationPolicyAlternatives;

@XmlRootElement(name = "presentation-policy-alternatives-collection", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class PresentationPolicyAlternativesCollection {

    @XmlElementWrapper(name = "presentation-policy-alternatives-list", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @XmlElement(name = "presentation-policy-alternatives", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    public List<PresentationPolicyAlternatives> presentationPolicyAlternatives = new ArrayList<PresentationPolicyAlternatives>();

    @XmlElement(name = "uris", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    public List<String> uris = new ArrayList<String>();
    
    public PresentationPolicyAlternativesCollection() {

    }
}