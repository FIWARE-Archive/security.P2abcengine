package ch.zhaw.ficore.p2abc.ldap.helper;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * Helper class for doing searches.
 * 
 * @author mroman
 * 
 */
public class LdapSearch {
    private LdapConnection connection;
    private String name;

    /**
     * Create a new LdapSearch-Object
     * 
     * @param connection
     *            An LdapConnection
     */
    public LdapSearch(LdapConnection connection) {
        this.connection = connection;
    }

    /**
     * Sets the default name.
     * 
     * @param name
     *            (Name of an ldap object/context)
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
     * 
     * @param filter
     *            the filter expression to use
     * @return NamingEnumeration
     * @throws NamingException
     *             on an LDAP error
     */
    public NamingEnumeration<SearchResult> search(String filter)
            throws NamingException {
        return search(name, filter);
    }

    /**
     * Ask for an attribute.
     * 
     * @param filter
     *            the filter to use
     * @param attr
     *            the attribute to search for
     * @return Object the object associated with that attribute
     * @throws LdapException
     *             on an LDAP error
     * @throws NamingException
     *             on an LDAP error
     */
    public Object getAttribute(String filter, String attr)
            throws LdapException, NamingException {
        return getAttribute(name, filter, attr);
    }

    /**
     * Returs true if an object matches the filter-Expression.
     * 
     * @param filter
     *            the filter to use
     * @return true if an object exists that matches the filter expression,
     *         false otherwise
     * @throws LdapException
     *             on an LDAP error
     * @throws NamingException
     *             on an LDAP error
     */
    public boolean doesExist(String filter) throws LdapException,
            NamingException {
        return doesExist(name, filter);
    }

    /**
     * Perform a search using a specified filter-Expression.
     * 
     * @param name
     *            (context)
     * @param filter
     *            the filter expression to use
     * @return a number of results
     * @throws NamingException
     *             on an LDAP error
     */
    public NamingEnumeration<SearchResult> search(String name, String filter)
            throws NamingException {
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        return connection.getInitialDirContext().search(name, filter, ctls);
    }

    /**
     * Ask for an attribute of an object that matches the specified
     * filter-Expression.
     * 
     * @param name
     *            the name of the object
     * @param filter
     *            the filter expression to use
     * @param attr
     *            ate attribute to look for
     * @return the matched object
     * @throws LdapException
     *             on an LDAP error
     * @throws NamingException
     *             on an LDAP error
     */
    public Object getAttribute(String name, String filter, String attr)
            throws LdapException, NamingException {
        NamingEnumeration<SearchResult> answer = this.search(name, filter);
        if (!answer.hasMore())
            throw new LdapException("Result set was empty!");
        Object val = ((SearchResult) answer.next()).getAttributes().get(attr)
                .get();
        answer.close();
        return val;
    }

    /**
     * Returns true if the specified filter-Expression matches something.
     * 
     * @param name
     *            the name of the object
     * @param filter
     *            the filter expression to use
     * @return true if the specified object exists, false otherwise
     * @throws LdapException
     *             on an LDAP error
     * @throws NamingException
     *             on an LDAP error
     */
    public boolean doesExist(String name, String filter) throws LdapException,
            NamingException {
        NamingEnumeration<SearchResult> answer = this.search(name, filter);
        if (!answer.hasMore())
            return false;
        answer.close();
        return true;
    }

    public void dumpSearch(String name, String filter) throws NamingException {
        NamingEnumeration<SearchResult> answer = this.search(name, filter);
        while (answer.hasMore()) {
            SearchResult sr = (SearchResult) answer.next();
            System.out.println(sr);
        }
        answer.close();
    }

    public void dumpAttributes(String name, String filter)
            throws NamingException {
        NamingEnumeration<SearchResult> answer = this.search(name, filter);
        while (answer.hasMore()) {
            SearchResult sr = (SearchResult) answer.next();
            NamingEnumeration<? extends Attribute> attrs = sr.getAttributes()
                    .getAll();
            while (attrs.hasMoreElements()) {
                System.out.println(attrs.next());
            }
        }
        answer.close();
    }
}
