package ch.zhaw.ficore.p2abc.services.issuance.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;

@XmlRootElement(name="query-rule")
public class QueryRule implements Serializable {
	@XmlElement(name="query-string")
	public String queryString;

	public QueryRule() {}

	public QueryRule(String queryString) {
		this.queryString = queryString;
	}
}