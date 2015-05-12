package ch.zhaw.ficore.p2abc.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "attribute-info-collection", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class AttributeInfoCollection {

    @XmlElement(name = "name", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            justification="Field is read from another project")
    public String name;

    @XmlElementWrapper(name = "attributes", required = true, namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    @XmlElement(name = "attribute", namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
    public List<AttributeInformation> attributes = new ArrayList<AttributeInformation>();

    public AttributeInfoCollection() {
    }

    public AttributeInfoCollection(final String name) {
        this.name = name;
    }

    public void addAttribute(final String name, final String mapping, final String encoding) {
        final AttributeInformation attr = new AttributeInformation(name, mapping,
                encoding);
        attr.addFriendlyDescription("en", name + " attribute");
        attributes.add(attr);
    }
}