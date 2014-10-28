package ch.zhaw.ficore.p2abc.services.issuance.xml;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import java.io.*;

@XmlRootElement(name="query-rule")
public class QueryRule implements Serializable {
	private static final long serialVersionUID = 9154661099255513606L;

	@XmlElement(name="query-string", required=true)
	public String queryString;

	public QueryRule() {}

	public QueryRule(String queryString) {
		this.queryString = queryString;
	}

	public static void dumpSchema() {
		try {
			JAXBContext jc = JAXBContext.newInstance( new Class[] { QueryRule.class,
					AuthenticationRequest.class, IssuanceRequest.class, AttributeInfoCollection.class,
					LanguageValuePair.class, AttributeInformation.class, AuthInfoSimple.class
			}
					);
			jc.generateSchema(new SchemaOutputResolver() {
				@Override
				public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
					File file = new File("/tmp/out.xsd");
					return new StreamResult(file);
				}
			});
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}