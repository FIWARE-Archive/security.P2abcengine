@XmlSchema(
xmlns = {
@XmlNs(prefix = "abc", namespaceURI ="http://abc4trust.eu/wp2/abcschemav1.0"),
},
elementFormDefault = XmlNsForm.QUALIFIED
) 
package ch.zhaw.ficore.p2abc.services.issuance.xml;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;