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
    // Hidden default constructor. Do not use an instance of this class; use
    // the static methods instead.
    private LdapAttributes() {
    }

    /**
     * Replaces attributes of an LDAP object.
     *
     * Example:
     *
     * <pre>
     * LdapAttributes.replaceAttribute(&quot;cn=munt, dc=example, dc=com&quot;,
     *         new BasicAttribute(&quot;testAttribut2&quot;, &quot;12345&quot;), con);
     * </pre>
     *
     * @param name
     *            Name of the LDAP object
     * @param attribute
     *            Attribute to replace
     * @param con
     *            An LdapConnection
     * @throws NamingException
     *             if the initial context can't be retrieved or the replacement
     *             didn't work
     */
    public static final void replaceAttribute(
            final String name, final Attribute attribute,
            final LdapConnection con) throws NamingException {
        final DirContext context = con.getInitialDirContext();
        // Attributes attributes = context.getAttributes(name);
        final ModificationItem[] mods = new ModificationItem[] { new ModificationItem(
                DirContext.REPLACE_ATTRIBUTE, attribute) };
        context.modifyAttributes(name, mods);
    }
}
