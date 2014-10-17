package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.services.issuance.xml.*;

public class FakeAttributeInfoProvider extends AttributeInfoProvider {
	
	public FakeAttributeInfoProvider(ServiceConfiguration srvcCfg) {
		super(srvcCfg);
	}
	
	public void shutdown() {
		
	}
	
	public AttributeInfoCollection getAttributes(String name) {
		AttributeInfoCollection aiCol = new AttributeInfoCollection(name);
		aiCol.addAttribute("someAttribute", "xsi:integer", "urn:abc4trust:1.0:encoding:integer:signed");
		return aiCol;
	}
}