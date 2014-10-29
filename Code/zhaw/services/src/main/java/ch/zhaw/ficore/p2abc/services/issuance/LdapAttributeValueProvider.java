package ch.zhaw.ficore.p2abc.services.issuance;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;

import ch.zhaw.ficore.p2abc.helper.ConnectionParameters;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnection;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapSearch;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuancePolicyAndAttributes;
import eu.abc4trust.xml.ObjectFactory;

public class LdapAttributeValueProvider extends AttributeValueProvider {

	private ObjectFactory of;

	public LdapAttributeValueProvider(IssuanceConfigurationData config) {
		super(config);
		of = new ObjectFactory();
	}

	public void shutdown() {

	}

	public List<eu.abc4trust.xml.Attribute> getAttributes(String query, String uid,
			CredentialSpecification credSpec) throws Exception {

		try {
      query = QueryHelper.buildQuery(query, QueryHelper.ldapSanitize(uid));
			ConnectionParameters cfg = configuration.getAttributeConnectionParameters();
			LdapConnection connection = new LdapConnection(cfg);

			LdapSearch srch = connection.newSearch();
			//srch.setName("dc=example, dc=com");

			AttributeDescriptions attrDescs = credSpec.getAttributeDescriptions();
			List<AttributeDescription> descriptions = attrDescs.getAttributeDescription();
			IssuancePolicyAndAttributes ipa = of.createIssuancePolicyAndAttributes();
			List<eu.abc4trust.xml.Attribute> attributes = ipa.getAttribute();
			for(AttributeDescription attrDesc : descriptions) {
				System.out.println("attrDesc.getType().toString() = " + attrDesc.getType().toString());

				Object value = srch.getAttribute(query, attrDesc.getType().toString());

				/* TODO: We can't support arbitrary types here (yet). Currently only integer/string are supported */
				if(attrDesc.getDataType().toString().equals("xs:integer") && attrDesc.getEncoding().toString().equals("urn:abc4trust:1.0:encoding:integer:signed")) {
					value = BigInteger.valueOf((Integer.parseInt(((String)value))));
				}
				else if(attrDesc.getDataType().toString().equals("xs:string") && attrDesc.getEncoding().toString().equals("urn:abc4trust:1.0:encoding:string:sha-256")) {
					value = (String)value;
				}
				else {
					throw new RuntimeException("Unsupported combination of encoding and dataType!");
				}

				eu.abc4trust.xml.Attribute attrib = of.createAttribute();
				attrib.setAttributeDescription(attrDesc);
				attrib.setAttributeValue(value);
				attrib.setAttributeUID(new URI("urn:abc4trust:attributeuid:ldap:" + attrDesc.getType().toString()));
				attributes.add(attrib);
			}
			return attributes;
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}
}
