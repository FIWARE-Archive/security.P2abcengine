package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="langValuePair")
public class LanguageValuePair {
	public String language;
	public String value;

	public LanguageValuePair() {}

	public LanguageValuePair(String l, String v) {
		this.language = l;
		this.value = v;
	}
}