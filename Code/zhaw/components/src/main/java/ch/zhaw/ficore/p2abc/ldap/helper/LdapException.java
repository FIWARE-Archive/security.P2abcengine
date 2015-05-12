package ch.zhaw.ficore.p2abc.ldap.helper;

/**
 * An LdapException.
 *
 * @author mroman
 *
 */
public class LdapException extends Exception {
    private static final long serialVersionUID = -8312060122280210119L;

    /** Creates an LDAP exception.
     *
     * @param msg the message
     */
    public LdapException(final String msg) {
        super(msg);
    }
}
