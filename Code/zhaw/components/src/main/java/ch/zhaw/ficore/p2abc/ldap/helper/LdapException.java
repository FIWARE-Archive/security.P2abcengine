package ch.zhaw.ficore.p2abc.ldap.helper;

/**
 * An LdapException.
 * 
 * @author mroman
 * 
 */
public class LdapException extends Exception {
    private static final long serialVersionUID = -8312060122280210119L;

    public LdapException(String msg) {
        super(msg);
    }
}
