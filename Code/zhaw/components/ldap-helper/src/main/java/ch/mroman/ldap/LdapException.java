package ch.mroman.ldap;

/**
 * An LdapException.
 * @author mroman
 *
 */
public class LdapException extends Exception {
  private static final long serialVersionUID = -8312060122280210119L;

  public LdapException(String msg) {
		super(msg);
	}
}
