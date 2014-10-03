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


@javax.ws.rs.Path("/")
public class UserService {
	@javax.ws.rs.core.Context
	ServletContext context;

	/* Syntax mappings determine to which xml-Datatype we map an ldap-Datatype */
	public static Map<String, List<String>> syntaxMappings = new HashMap<String, List<String>>();
	/* Mapping encodings determine how we encode the values (for the syntax mapping) in the credential */
	public static Map<String, List<String>> mappingEncodings = new HashMap<String, List<String>>();

	static {
		syntaxMappings.put("1.3.6.1.4.1.1466.115.121.1.15", new ArrayList<String>());
		syntaxMappings.put("1.3.6.1.4.1.1466.115.121.1.50", new ArrayList<String>());
		syntaxMappings.put("1.3.6.1.4.1.1466.115.121.1.27", new ArrayList<String>());


		syntaxMappings.get("1.3.6.1.4.1.1466.115.121.1.15").add("string");
                syntaxMappings.get("1.3.6.1.4.1.1466.115.121.1.50").add("string");
		syntaxMappings.get("1.3.6.1.4.1.1466.115.121.1.27").add("integer");

		mappingEncodings.put("string", new ArrayList<String>());
		mappingEncodings.put("integer", new ArrayList<String>());

		mappingEncodings.get("string").add("urn:abc4trust:1.0:encoding:string:sha-256");
		mappingEncodings.get("integer").add("urn:abc4trust:1.0:encoding:integer:signed");
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
		Attribute must = answer.get("must");
		Attribute may = answer.get("may");

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

@XmlRootElement(name="attribute")
class ObjectClassAttribute {
	public String name;
	public String syntax;
	public String mapping;
	public String encoding;
	public boolean include;

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
		attributes.add(new ObjectClassAttribute(name, syntax));
	}
}
