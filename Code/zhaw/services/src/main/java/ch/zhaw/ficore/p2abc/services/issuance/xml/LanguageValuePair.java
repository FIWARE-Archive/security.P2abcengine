package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "langValuePair")
public class LanguageValuePair {
    @XmlElement(name = "language", required = true)
    public String language;

    @XmlElement(name = "value", required = true)
    public String value;

    public LanguageValuePair() {
    }

    public LanguageValuePair(String l, String v) {
        this.language = l;
        this.value = v;
    }
}