package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="query-rule")
public class QueryRule {
	@XmlElement(name="credential-specification-uid")
	public String credentialSpecificationUID;
	@XmlElement(name="query-string")
	public String queryString;

	public QueryRule() {}

	public QueryRule(String credentialSpecificationUID, String queryString) {
		this.credentialSpecificationUID = credentialSpecificationUID;
		this.queryString = queryString;
	}
}