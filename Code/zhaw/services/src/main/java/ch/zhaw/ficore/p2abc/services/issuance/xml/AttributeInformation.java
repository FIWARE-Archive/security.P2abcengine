package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.*;

@XmlRootElement(name="attribute")
public class AttributeInformation {
	
	@XmlElement(name="name", required=true)
	public String name;
	
	@XmlElement(name="mapping", required=true)
	public String mapping;
	
	@XmlElement(name="encoding", required=true)
	public String encoding;
	
	@XmlElementWrapper(name = "friendly-descriptions")
	@XmlElement(name = "friendly-description", required=true)
	public List<LanguageValuePair> friendlyDescriptions = new ArrayList<LanguageValuePair>();

	public AttributeInformation() {}

	public AttributeInformation(String name, String mapping, String encoding) {
		this.name = name;
		this.mapping = mapping;
		this.encoding = encoding;
	}

	public void addFriendlyDescription(String language, String value) {
		friendlyDescriptions.add(new LanguageValuePair(language, value));
	}
}