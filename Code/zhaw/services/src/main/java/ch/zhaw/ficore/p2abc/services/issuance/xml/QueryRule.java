package ch.zhaw.ficore.p2abc.services.issuance.xml;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="query-rule")
public class QueryRule implements Serializable {
	private static final long serialVersionUID = 9154661099255513606L;

	@XmlElement(name="query-string", required=true)
	public String queryString;

	public QueryRule() {}

	public QueryRule(String queryString) {
		this.queryString = queryString;
	}
}