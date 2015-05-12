package ch.zhaw.ficore.p2abc.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "attribute", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class AttributeInformation {

    @XmlElement(name = "name", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification="Field is read from another project")
    public String name;

    @XmlElement(name = "mapping", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification="Field is read from another project")
    public String mapping;

    @XmlElement(name = "encoding", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification="Field is read from another project")
    public String encoding;

    @XmlElementWrapper(name = "friendly-descriptions", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @XmlElement(name = "friendly-description", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    public List<LanguageValuePair> friendlyDescriptions = new ArrayList<LanguageValuePair>();

    public AttributeInformation() {
    }

    public AttributeInformation(final String name, final String mapping, final String encoding) {
        this.name = name;
        this.mapping = mapping;
        this.encoding = encoding;
    }

    public void addFriendlyDescription(final String language, final String value) {
        friendlyDescriptions.add(new LanguageValuePair(language, value));
    }
}