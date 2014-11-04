package ch.zhaw.ficore.p2abc.ldap.sandbox;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapAttributes;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnection;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapException;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapSearch;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapStatic;

public class FindAttribute {
    public static void main(String... args) throws NamingException, LdapException {
        ConnectionParameters cfg = new ConnectionParameters("localhost",10389,10389,10389,"uid=admin, ou=system","secret",false);
        LdapConnection con = new LdapConnection(cfg);
        LdapSearch srch = con.newSearch().setName("dc=example, dc=com");

        System.out.println("testAttribut2 value");
        System.out.println(srch.getAttribute("(cn=munt)","testAttribut2"));

        System.out.println("bind...");
        //cfg.setAuth("uid=admin, ou=system","secret");
        //con.reloadConfig();
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
