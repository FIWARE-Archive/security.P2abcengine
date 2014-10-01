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


@Path("/")
public class UserService {
	@javax.ws.rs.core.Context
	ServletContext context;


   	@GET()
	@Path("/test")
	@Produces(MediaType.APPLICATION_XML)
	public MyJaxbBean test(@QueryParam("name") String name, @QueryParam("age") int age) {
		return new MyJaxbBean(name, age);
	}

	@GET()
	@Path("/schemaDump")
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

@XmlRootElement
class ObjectClassAttribute {
	public String name;
	public String syntax;

	public ObjectClassAttribute() {}

	public ObjectClassAttribute(String name, String syntax) {
		this.name = name;
		this.syntax = syntax;
	}
}

@XmlRootElement
class ObjectClass {
	public String name;
	public List<ObjectClassAttribute> attributes = new ArrayList<ObjectClassAttribute>();

	public ObjectClass() { 
	} // JAXB needs this
 
	public ObjectClass(String name) {
		this.name = name;
	}

	public void addAttribute(String name, String syntax) {
		attributes.add(new ObjectClassAttribute(name, syntax));
	}
}
