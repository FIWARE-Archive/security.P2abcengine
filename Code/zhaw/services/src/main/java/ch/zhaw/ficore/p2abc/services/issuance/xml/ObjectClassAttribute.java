package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.*;

@XmlRootElement(name="attribute")
public class ObjectClassAttribute {
	public String name;
	public String mapping;
	public String encoding;
	public boolean include;
	@XmlElementWrapper(name = "friendlyDescriptions")
	@XmlElement(name = "friendlyDescriptions")
	public List<LanguageValuePair> friendlyDescriptions = new ArrayList<LanguageValuePair>();

	public ObjectClassAttribute() {}

	public ObjectClassAttribute(String name, String mapping, String encoding) {
		this.name = name;
		this.include = false;
		this.mapping = mapping;
		this.encoding = encoding;
	}

	public void addFriendlyDescription(String language, String value) {
		friendlyDescriptions.add(new LanguageValuePair(language, value));
	}
}