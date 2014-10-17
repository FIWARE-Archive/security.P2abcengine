package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.*;

@XmlRootElement(name="class")
public class ObjectClass {
	public String name;
	@XmlElementWrapper(name = "attributes")
	@XmlElement(name = "attribute")
	public List<ObjectClassAttribute> attributes = new ArrayList<ObjectClassAttribute>();

	public ObjectClass() { 
	} // JAXB needs this
 
	public ObjectClass(String name) {
		this.name = name;
	}

	public void addAttribute(String name, String mapping, String encoding) {
		ObjectClassAttribute attr = new ObjectClassAttribute(name, mapping, encoding);
		attr.addFriendlyDescription("en", name + " attribute");
		attributes.add(attr);
	}
}