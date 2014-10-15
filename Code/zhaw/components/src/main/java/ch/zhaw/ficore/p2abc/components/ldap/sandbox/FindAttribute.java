package ch.zhaw.ficore.p2abc.ldap.sandbox;

import javax.naming.*;
import javax.naming.directory.*;

import ch.zhaw.ficore.p2abc.ldap.helper.*;

public class FindAttribute {
	public static void main(String... args) throws NamingException, LdapException {
		LdapConnectionConfig cfg = new LdapConnectionConfig(10389,"localhost");
		LdapConnection con = cfg.newConnection();
		LdapSearch srch = con.newSearch().setName("dc=example, dc=com");
		
		System.out.println("testAttribut2 value");
		System.out.println(srch.getAttribute("(cn=munt)","testAttribut2"));
	
		System.out.println("bind...");
		cfg.setAuth("uid=admin, ou=system","secret");
		con.reloadConfig();
		srch.dumpAttributes(srch.getName(), "(cn=munt)");
	
		System.out.println(srch.doesExist("(&(cn=munt)(testAttribut2=23423))"));
		
		System.out.println("static search..");
		LdapStatic.init(cfg);
		LdapStatic.newSearch().dumpSearch("dc=example, dc=com","(cn=munt)");
		
		System.out.println("modify attribute");
		LdapAttributes.replaceAttribute("cn=munt, dc=example, dc=com", new BasicAttribute("testAttribut2","12345"), con);
	
		System.out.println("Schema?");
		
		dumpSchema(con, "ClassDefinition/person");
	}
	
	public static void dumpSchema(LdapConnection con, String classDef) throws NamingException {
		DirContext ctx = con.getInitialDirContext();
		DirContext schema = ctx.getSchema("ou=schema");
		
		Attributes answer = schema.getAttributes("ClassDefinition/abcPerson");
		Attribute must = answer.get("must");
		Attribute may = answer.get("may");
		
		for(int i = 0; i < must.size(); i++) {
			System.out.println((must.get(i)));
			Attributes attrAnswer = schema.getAttributes("AttributeDefinition/" + must.get(i));
			System.out.println(" " + attrAnswer.get("syntax"));
		}
	
		for(int i = 0; i < may.size(); i++) {
			System.out.println((may.get(i)));
			Attributes attrAnswer = schema.getAttributes("AttributeDefinition/" + may.get(i));
			System.out.println(" " + attrAnswer.get("syntax"));
		}
	}
}
