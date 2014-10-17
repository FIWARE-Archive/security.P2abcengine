package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.*;

@XmlRootElement(name="class")
public class AttributeInfoCollection {
	public String name;
	@XmlElementWrapper(name = "attributes")
	@XmlElement(name = "attribute")
	public List<AttributeInformation> attributes = new ArrayList<AttributeInformation>();

	public AttributeInfoCollection() {}
 
	public AttributeInfoCollection(String name) {
		this.name = name;
	}

	public void addAttribute(String name, String mapping, String encoding) {
		AttributeInformation attr = new AttributeInformation(name, mapping, encoding);
		attr.addFriendlyDescription("en", name + " attribute");
		attributes.add(attr);
	}
}