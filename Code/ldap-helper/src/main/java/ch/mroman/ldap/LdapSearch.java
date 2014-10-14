package ch.mroman.ldap;
import javax.naming.*;
import javax.naming.directory.*;

/**
 * Helper class for doing searches. 
 * @author mroman
 *
 */
public class LdapSearch {
	private LdapConnection connection;
	private String name;
	
	/**
	 * Create a new LdapSearch-Object
	 * @param connection An LdapConnection
	 */
	public LdapSearch(LdapConnection connection) {
		this.connection = connection;
	}
	
	/**
	 * Sets the default name. 
	 * @param name (Name of an ldap object/context)
	 * @return an LdapSearch
	 */
	public LdapSearch setName(String name) {
		this.name = name;
		return this;
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * Perform a search using a specified filter-Expression
	 * @param filter
	 * @return NamingEnumeration
	 * @throws NamingException
	 */
	public NamingEnumeration<SearchResult> search(String filter) throws NamingException {
		return search(name, filter);
	}
	
	/**
	 * Ask for an attribute.
	 * @param filter
	 * @param attr
	 * @return Object
	 * @throws LdapException
	 * @throws NamingException
	 */
	public Object getAttribute(String filter, String attr) throws LdapException, NamingException {
		return getAttribute(name, filter, attr);
	}
	
	/**
	 * Returs true if an object matches the filter-Expression.
	 * @param filter
	 * @return true or false
	 * @throws LdapException
	 * @throws NamingException
	 */
	public boolean doesExist(String filter) throws LdapException, NamingException {
		return doesExist(name, filter);
	}
	
	/**
	 * Perform a search using a specified filter-Expression.
	 * @param name (context)
	 * @param filter
	 * @return
	 * @throws NamingException
	 */
	public NamingEnumeration<SearchResult> search(String name, String filter) throws NamingException {
		SearchControls ctls = new SearchControls();
		return connection.getInitialDirContext().search(name, filter, 
				ctls);
	}
	
	/**
	 * Ask for an attribute of an object that matches the
	 * specified filter-Expression.
	 * @param name
	 * @param filter
	 * @param attr
	 * @return
	 * @throws LdapException
	 * @throws NamingException
	 */
	public Object getAttribute(String name, String filter, String attr) throws LdapException, NamingException {
		NamingEnumeration<SearchResult> answer = this.search(name, filter);
		if(!answer.hasMore())
			throw new LdapException("Result set was empty!");
		Object val = ((SearchResult)answer.next()).getAttributes().get(attr).get();
		answer.close();
		return val;
	}
	
	/**
	 * Returns true if the specified filter-Expression matches something.
	 * @param name
	 * @param filter
	 * @return
	 * @throws LdapException
	 * @throws NamingException
	 */
	public boolean doesExist(String name, String filter) throws LdapException, NamingException {
		NamingEnumeration<SearchResult> answer = this.search(name, filter);
		if(!answer.hasMore())
			return false;
		answer.close();
		return true;
	}
	
	public void dumpSearch(String name, String filter) throws NamingException {
		NamingEnumeration<SearchResult> answer = this.search(name, filter);
		while(answer.hasMore()) {
			SearchResult sr = (SearchResult)answer.next();
			System.out.println(sr);
		}
		answer.close();
	}
	
	public void dumpAttributes(String name, String filter) throws NamingException {
		NamingEnumeration<SearchResult> answer = this.search(name, filter);
		while(answer.hasMore()) {
			SearchResult sr = (SearchResult)answer.next();
			NamingEnumeration<? extends Attribute> attrs = sr.getAttributes().getAll();
			while(attrs.hasMoreElements()) {
				System.out.println(attrs.next());
			}
		}
		answer.close();
	}
}
