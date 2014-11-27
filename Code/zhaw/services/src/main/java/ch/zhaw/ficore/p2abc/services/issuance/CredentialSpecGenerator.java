package ch.zhaw.ficore.p2abc.services.issuance;

import java.net.URI;
import java.util.List;

import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.xml.AttributeInfoCollection;
import ch.zhaw.ficore.p2abc.xml.AttributeInformation;
import ch.zhaw.ficore.p2abc.xml.LanguageValuePair;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.ObjectFactory;

/**
 * Class providing functionality to generate a CredentialSpecification for an
 * AttributeInfoCollection.
 * 
 * @author mroman
 */
public class CredentialSpecGenerator {

    /**
     * Creates a CredentialSpecification from an AttributeInfoCollection. As of
     * now this method DOES NOT perform any sanity check and assumes that all
     * mappings, encodings etc. of attributes in the AttributeInfoCollection are
     * sane and correct.
     * 
     * @param attrInfoCol
     *            an AttributeInfoCollectin
     * @return corresponding CredentialSpecification
     */
    public CredentialSpecification generateCredentialSpecification(
            AttributeInfoCollection attrInfoCol) {
        ObjectFactory of = new ObjectFactory();
        try {
            CredentialSpecification credSpec = of
                    .createCredentialSpecification();
            credSpec.setSpecificationUID(new URI(ServicesConfiguration
                    .getURIBase() + attrInfoCol.name));

            credSpec.setVersion("1.0");
            credSpec.setKeyBinding(true);
            credSpec.setRevocable(false);

            AttributeDescriptions attrDescs = of.createAttributeDescriptions();
            attrDescs.setMaxLength(256);
            List<AttributeDescription> descriptions = attrDescs
                    .getAttributeDescription();

            for (AttributeInformation attrInfo : attrInfoCol.attributes) {

                AttributeDescription attr = of.createAttributeDescription();
                attr.setType(new URI(attrInfo.name));
                attr.setDataType(new URI(attrInfo.mapping));
                attr.setEncoding(new URI(attrInfo.encoding));
                descriptions.add(attr);

                List<FriendlyDescription> friendlies = attr
                        .getFriendlyAttributeName();
                for (LanguageValuePair lvp : attrInfo.friendlyDescriptions) {
                    FriendlyDescription friendlyDesc = of
                            .createFriendlyDescription();
                    friendlyDesc.setLang(lvp.language);
                    friendlyDesc.setValue(lvp.value);
                    friendlies.add(friendlyDesc);
                }

            }

            credSpec.setAttributeDescriptions(attrDescs);

            return credSpec;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}