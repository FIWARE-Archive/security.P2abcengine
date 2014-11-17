package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AttributeInfoCollection;

/**
 * An AttributeInfoProvider that is not coupled with any actual identity source.
 * Use in testing or as a reference.
 * 
 * @author mroman
 */
public class FakeAttributeInfoProvider extends AttributeInfoProvider {

    /**
     * Constructor
     */
    public FakeAttributeInfoProvider(IssuanceConfiguration configuration) {
        super(configuration);
    }

    /**
     * No Operation.
     */
    public void shutdown() {

    }

    /**
     * Returns a AttributeInfoCollection filled with dummy attributes.
     * 
     * @return an AttributeInfoCollection
     */
    public AttributeInfoCollection getAttributes(String name) {
        AttributeInfoCollection aiCol = new AttributeInfoCollection(name);
        aiCol.addAttribute("someAttribute", "xs:integer",
                "urn:abc4trust:1.0:encoding:integer:signed");
        return aiCol;
    }
}