package ch.zhaw.ficore.p2abc.ldap.helper;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

/**
 * Helper class for managing attributes.
 * 
 * @author mroman
 * 
 */
public class LdapAttributes {

    /**
     * <p>
     * Replace attributes of an ldap object.
     * </p>
     * <p>
     * Example:
     * </p>
     * 
     * <pre>
     * LdapAttributes.replaceAttribute(&quot;cn=munt, dc=example, dc=com&quot;,
     *         new BasicAttribute(&quot;testAttribut2&quot;, &quot;12345&quot;), con);
     * </pre>
     * 
     * @param name
     *            Name of the ldap object
     * @param attribute
     *            Attribute to replace
     * @param con
     *            An LdapConnection
     * @throws NamingException
     */
    public static void replaceAttribute(String name, Attribute attribute,
            LdapConnection con) throws NamingException {
        DirContext context = con.getInitialDirContext();
        // Attributes attributes = context.getAttributes(name);
        ModificationItem[] mods = new ModificationItem[] { new ModificationItem(
                DirContext.REPLACE_ATTRIBUTE, attribute) };
        context.modifyAttributes(name, mods);
    }
}
