package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.services.issuance.xml.*;

import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.ObjectFactory;

import java.net.URI;
import java.util.*;

/**
 * Class providing functionality to generate a
 * CredentialSpecification for an AttributeInfoCollection.
 * 
 * @author mroman
 */
public class CredentialSpecGenerator {
	
	/**
	 * Creates a CredentialSpecification from an AttributeInfoCollection.
	 * As of now this method DOES NOT perform any sanity check and assumes
	 * that all mappings, encodings etc. of attributes in the AttributeInfoCollection
	 * are sane and correct. 
	 * 
	 * @param attrInfoCol an AttributeInfoCollectin
	 * @return corresponding CredentialSpecification
	 */
	public CredentialSpecification generateCredentialSpecification(AttributeInfoCollection attrInfoCol) {
		ObjectFactory of = new ObjectFactory();
		try {
			CredentialSpecification credSpec = of.createCredentialSpecification();
			credSpec.setSpecificationUID(new URI("urn:abc4trust:credspec:ldap:" + attrInfoCol.name));

			credSpec.setVersion("1.0");
			credSpec.setKeyBinding(false);
			credSpec.setRevocable(false);

			AttributeDescriptions attrDescs = of.createAttributeDescriptions();
			attrDescs.setMaxLength(256);
			List<AttributeDescription> descriptions = attrDescs.getAttributeDescription();

			for(AttributeInformation attrInfo : attrInfoCol.attributes) {

				AttributeDescription attr = of.createAttributeDescription();
				attr.setType(new URI(attrInfo.name));
				attr.setDataType(new URI(attrInfo.mapping));
				attr.setEncoding(new URI(attrInfo.encoding));
				descriptions.add(attr);

				List<FriendlyDescription> friendlies = attr.getFriendlyAttributeName();
				for(LanguageValuePair lvp : attrInfo.friendlyDescriptions) {
					FriendlyDescription friendlyDesc = of.createFriendlyDescription();
					friendlyDesc.setLang(lvp.language);
					friendlyDesc.setValue(lvp.value);
					friendlies.add(friendlyDesc);
				}

			}

			credSpec.setAttributeDescriptions(attrDescs);

			return credSpec;
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}