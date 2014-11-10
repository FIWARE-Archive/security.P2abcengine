package ch.zhaw.ficore.p2abc.services.issuance;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;

import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuancePolicyAndAttributes;
import eu.abc4trust.xml.ObjectFactory;

public class FakeAttributeValueProvider extends AttributeValueProvider {

    private ObjectFactory of;

    public FakeAttributeValueProvider(IssuanceConfiguration config) {
        super(config);
        of = new ObjectFactory();
    }

    public void shutdown() {

    }

    public List<eu.abc4trust.xml.Attribute> getAttributes(String query, String uid,
            CredentialSpecification credSpec) throws Exception {

        try {
            AttributeDescriptions attrDescs = credSpec.getAttributeDescriptions();
            List<AttributeDescription> descriptions = attrDescs.getAttributeDescription();
            IssuancePolicyAndAttributes ipa = of.createIssuancePolicyAndAttributes();
            List<eu.abc4trust.xml.Attribute> attributes = ipa.getAttribute();
            for(AttributeDescription attrDesc : descriptions) {
                Object value;
                
                if(attrDesc.getDataType().toString().equals("xs:integer") && attrDesc.getEncoding().toString().equals("urn:abc4trust:1.0:encoding:integer:signed")) {
                    value = BigInteger.valueOf(123);
                }
                else if(attrDesc.getDataType().toString().equals("xs:string") && attrDesc.getEncoding().toString().equals("urn:abc4trust:1.0:encoding:string:sha-256")) {
                    value = "FAKE";
                }
                else {
                    throw new RuntimeException("Unsupported combination of encoding and dataType: " + attrDesc.getEncoding().toString() + "," + attrDesc.getDataType().toString());
                }

                eu.abc4trust.xml.Attribute attrib = of.createAttribute();
                attrib.setAttributeDescription(attrDesc);
                attrib.setAttributeValue(value);
                attrib.setAttributeUID(new URI(ServicesConfiguration.getURIBase() + "ldap:" + attrDesc.getType().toString()));
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
