package ch.zhaw.ficore.p2abc.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "langValuePair", namespace="http://abc4trust.eu/wp2/abcschemav1.0")
public class LanguageValuePair {
    @XmlElement(name = "language", required = true, namespace="http://abc4trust.eu/wp2/abcschemav1.0")
    public String language;

    @XmlElement(name = "value", required = true, namespace="http://abc4trust.eu/wp2/abcschemav1.0")
    public String value;

    public LanguageValuePair() {
    }

    public LanguageValuePair(String l, String v) {
        this.language = l;
        this.value = v;
    }
}