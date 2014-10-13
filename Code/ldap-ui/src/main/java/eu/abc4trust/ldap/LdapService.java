//* Licensed Materials - Property of IBM, Miracle A/S, and            *
//* Alexandra Instituttet A/S                                         *
//* eu.abc4trust.pabce.1.0                                            *
//* (C) Copyright IBM Corp. 2012. All Rights Reserved.                *
//* (C) Copyright Miracle A/S, Denmark. 2012. All Rights Reserved.    *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2012. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*/**/****************************************************************

package eu.abc4trust.ui.idselectservice;


import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import ch.mroman.ldap.*;
import javax.naming.*;
import javax.naming.directory.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.charset.*;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.CredentialSpecificationAndSystemParameters;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.Attribute;
import eu.abc4trust.xml.IssuancePolicyAndAttributes;
import java.net.*;
import javax.xml.bind.JAXBElement;
import java.util.*;
import java.math.BigInteger;

@javax.ws.rs.Path("/")
public class UserService {
	@javax.ws.rs.core.Context
	ServletContext context;

	ObjectFactory of = new ObjectFactory();


	/* Syntax mappings determine to which xml-Datatype we map an ldap-Datatype */
	public static Map<String, List<String>> syntaxMappings = new HashMap<String, List<String>>();
	/* Mapping encodings determine how we encode the values (for the syntax mapping) in the credential */
	public static Map<String, List<String>> mappingEncodings = new HashMap<String, List<String>>();

	static {
		syntaxMappings.put("1.3.6.1.4.1.1466.115.121.1.15", new ArrayList<String>());
		syntaxMappings.put("1.3.6.1.4.1.1466.115.121.1.50", new ArrayList<String>());
		syntaxMappings.put("1.3.6.1.4.1.1466.115.121.1.27", new ArrayList<String>());


		syntaxMappings.get("1.3.6.1.4.1.1466.115.121.1.15").add("xs:string");
		syntaxMappings.get("1.3.6.1.4.1.1466.115.121.1.50").add("xs:string");
		syntaxMappings.get("1.3.6.1.4.1.1466.115.121.1.27").add("xs:integer");

		mappingEncodings.put("xs:string", new ArrayList<String>());
		mappingEncodings.put("xs:integer", new ArrayList<String>());

		mappingEncodings.get("xs:string").add("urn:abc4trust:1.0:encoding:string:sha-256");
		mappingEncodings.get("xs:integer").add("urn:abc4trust:1.0:encoding:integer:signed");
	}

	public UserService() {
	}


   	@GET()
	@javax.ws.rs.Path("/test")
	@Produces("text/html")
	public String test() {
		try {
			Path path = Paths.get("/var/www/index.html");
			String contents = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
			return contents;
		}
		catch(Exception e) {
			return "error";
		}
	}

	@POST()
	@javax.ws.rs.Path("/genIssuanceAttributes")
	@Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    @Produces(MediaType.TEXT_XML)
	public JAXBElement<IssuancePolicyAndAttributes> generateIssuanceAttributes(CredentialSpecificationAndSystemParameters credSpecParams, @QueryParam("srch") String query) {
		try {
			LdapConnectionConfig cfg = new LdapConnectionConfig(10389,"localhost");
			LdapConnection con = cfg.newConnection();
			LdapSearch srch = con.newSearch().setName("dc=example, dc=com");
			
			CredentialSpecification credSpec = credSpecParams.getCredentialSpecification();

			AttributeDescriptions attrDescs = credSpec.getAttributeDescriptions();
			List<AttributeDescription> descriptions = attrDescs.getAttributeDescription();
			IssuancePolicyAndAttributes ipa = of.createIssuancePolicyAndAttributes();
			List<eu.abc4trust.xml.Attribute> attributes = ipa.getAttribute();
			for(AttributeDescription attrDesc : descriptions) {
				Object value = srch.getAttribute("(cn=munt)", attrDesc.getType().toString());
			
				/* TODO: We can't support arbitrary types here (yet). Currently only integer/string are supported */
				if(attrDesc.getDataType().toString().equals("xs:integer") && attrDesc.getEncoding().equals("urn:abc4trust:1.0:encoding:integer:signed")) {
					value = BigInteger.valueOf((Integer.parseInt(((String)value))));
				}
				else if(attrDesc.getDataType().toString().equals("xs:string") && attrDesc.getEncoding().equals("urn:abc4trust:1.0:encoding:string:sha-256")) {
					value = (String)value;
				}
				else {
					throw new RuntimeException("Unsupported combination of encoding and dataType!");
				}

				eu.abc4trust.xml.Attribute attrib = of.createAttribute();
				attrib.setAttributeDescription(attrDesc);
				attrib.setAttributeValue(value);
				attributes.add(attrib);
			}
			return of.createIssuancePolicyAndAttributes(ipa);
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	

	@POST()
	@javax.ws.rs.Path("/genCredSpec")
	@Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    @Produces(MediaType.TEXT_XML)
	public JAXBElement<CredentialSpecificationAndSystemParameters> generateCredentialSpecification(ObjectClass oc) {
		try {
			CredentialSpecification credSpec = of.createCredentialSpecification();
			credSpec.setSpecificationUID(new URI("abc4trust:ldap:" + oc.name));
			CredentialSpecificationAndSystemParameters credSysParams = of.createCredentialSpecificationAndSystemParameters();
			credSysParams.setCredentialSpecification(credSpec);

			credSpec.setVersion("1.0");
			credSpec.setKeyBinding(false);
			credSpec.setRevocable(false);

			AttributeDescriptions attrDescs = of.createAttributeDescriptions();
			attrDescs.setMaxLength(256);
			List<AttributeDescription> descriptions = attrDescs.getAttributeDescription();

			for(ObjectClassAttribute oca : oc.attributes) {

				AttributeDescription attr = of.createAttributeDescription();
				attr.setType(new URI(oca.name));
				attr.setDataType(new URI(oca.mapping));
				attr.setEncoding(new URI(oca.encoding));
				descriptions.add(attr);

				List<FriendlyDescription> friendlies = attr.getFriendlyAttributeName();
				for(LanguageValuePair lvp : oca.friendlyDescriptions) {
					FriendlyDescription friendlyDesc = of.createFriendlyDescription();
					friendlyDesc.setLang(lvp.language);
					friendlyDesc.setValue(lvp.value);
					friendlies.add(friendlyDesc);
				}

			}

			credSpec.setAttributeDescriptions(attrDescs);

			return of.createCredentialSpecificationAndSystemParameters(credSysParams);
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@GET()
	@javax.ws.rs.Path("/schemaDump")
	@Produces("application/xml")
	public ObjectClass schemaDump(@QueryParam("oc") String objectClass) throws NamingException {
		LdapConnectionConfig cfg = new LdapConnectionConfig(10389,"localhost");
		LdapConnection con = cfg.newConnection();
		LdapSearch srch = con.newSearch().setName("dc=example, dc=com");
		
		DirContext ctx = con.getInitialDirContext();
		DirContext schema = ctx.getSchema("ou=schema");
		
		Attributes answer = schema.getAttributes("ClassDefinition/" + objectClass);
		javax.naming.directory.Attribute must = answer.get("must");
		javax.naming.directory.Attribute may = answer.get("may");

		ObjectClass oc = new ObjectClass(objectClass);
		
		for(int i = 0; i < must.size(); i++) {
			Attributes attrAnswer = schema.getAttributes("AttributeDefinition/" + must.get(i));
			oc.addAttribute(must.get(i).toString(), attrAnswer.get("syntax").get(0).toString());
		}
	
		for(int i = 0; i < may.size(); i++) {
			Attributes attrAnswer = schema.getAttributes("AttributeDefinition/" + may.get(i));
			oc.addAttribute(may.get(i).toString(), attrAnswer.get("syntax").get(0).toString());
		}

		return oc;
	}
}

@XmlRootElement(name="langValuePair")
class LanguageValuePair {
	public String language;
	public String value;

	public LanguageValuePair() {}

	public LanguageValuePair(String l, String v) {
		this.language = l;
		this.value = v;
	}
}

@XmlRootElement(name="attribute")
class ObjectClassAttribute {
	public String name;
	public String syntax;
	public String mapping;
	public String encoding;
	public boolean include;
	@XmlElementWrapper(name = "friendlyDescriptions")
	@XmlElement(name = "friendlyDescriptions")
	public List<LanguageValuePair> friendlyDescriptions = new ArrayList<LanguageValuePair>();

	public ObjectClassAttribute() {}

	public ObjectClassAttribute(String name, String syntax) {
		this.name = name;
		this.syntax = syntax;
		this.include = false;
		if(UserService.syntaxMappings.containsKey(syntax))
			this.mapping = UserService.syntaxMappings.get(syntax).get(0);
		else
			this.mapping = "string"; //use string as default mapping
		if(UserService.mappingEncodings.containsKey(this.mapping))
			this.encoding = UserService.mappingEncodings.get(this.mapping).get(0);
		else
			throw new RuntimeException("ObjectClassAttribute. Can't determine encoding for mapping!");
	}

	public void addFriendlyDescription(String language, String value) {
		friendlyDescriptions.add(new LanguageValuePair(language, value));
	}
}

@XmlRootElement(name="class")
class ObjectClass {
	public String name;
	@XmlElementWrapper(name = "attributes")
	@XmlElement(name = "attribute")
	public List<ObjectClassAttribute> attributes = new ArrayList<ObjectClassAttribute>();

	public ObjectClass() { 
	} // JAXB needs this
 
	public ObjectClass(String name) {
		this.name = name;
	}

	public void addAttribute(String name, String syntax) {
		/* Filter {n} out */
		syntax = syntax.replaceAll("\\{\\d+\\}$","");
		ObjectClassAttribute attr = new ObjectClassAttribute(name, syntax);
		attr.addFriendlyDescription("en", name + " attribute");
		attributes.add(attr);
	}
}
