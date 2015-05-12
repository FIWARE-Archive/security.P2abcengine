package ch.zhaw.ficore.p2abc.ldap.helper;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * Helper class for doing searches.
 *
 * @author mroman
 *
 */
public class LdapSearch {
    /** The connection to the LDAP server. */
    private final LdapConnection connection;

    /** The default LDAP object to use in a search. */
    private String name;

    /**
     * Create a new LdapSearch-Object.
     *
     * @param connection
     *            An LdapConnection
     */
    public LdapSearch(final LdapConnection connection) {
        this.connection = connection;
    }

    /**
     * Sets the default name.
     *
     * @param name
     *            (Name of an ldap object/context)
     * @return an LdapSearch
     */
    public final LdapSearch setName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the default name.
     *
     * @return name of default LDAP object
     */
    public final String getName() {
        return name;
    }

    /**
     * Performs a search using a specified filter-Expression.
     *
     * @param filter
     *            the filter expression to use
     * @return NamingEnumeration
     * @throws NamingException
     *             on an LDAP error
     */
    public final NamingEnumeration<SearchResult> search(final String filter)
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
    public final Object getAttribute(final String filter, final String attr)
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
    public final boolean doesExist(final String filter) throws LdapException,
            NamingException {
        return doesExist(name, filter);
    }

    /**
     * Perform a search using a specified filter-Expression.
     *
     * @param contextName
     *            (context)
     * @param filter
     *            the filter expression to use
     * @return a number of results
     * @throws NamingException
     *             on an LDAP error
     */
    public final NamingEnumeration<SearchResult> search(
                final String contextName,
                final String filter)
            throws NamingException {
        final SearchControls ctls = new SearchControls();
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        return connection
                .getInitialDirContext()
                .search(contextName, filter, ctls);
    }

    /**
     * Ask for an attribute of an object that matches the specified
     * filter-Expression.
     *
     * @param objectName
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
    public final Object getAttribute(
                final String objectName,
                final String filter,
                final String attr)
            throws LdapException, NamingException {
        final NamingEnumeration<SearchResult> answer
            = search(objectName, filter);
        if (!answer.hasMore()) {
            throw new LdapException("Result set was empty!");
        }
        final Object val = ((SearchResult) answer.next())
                .getAttributes()
                .get(attr)
                .get();
        answer.close();
        return val;
    }

    /**
     * Returns true if the specified filter-Expression matches something.
     *
     * @param objectName
     *            the name of the object
     * @param filter
     *            the filter expression to use
     * @return true if the specified object exists, false otherwise
     * @throws LdapException
     *             on an LDAP error
     * @throws NamingException
     *             on an LDAP error
     */
    public final boolean doesExist(final String objectName, final String filter)
            throws LdapException, NamingException {
        final NamingEnumeration<SearchResult> answer
            = search(objectName, filter);
        if (!answer.hasMore()) {
            return false;
        }
        answer.close();
        return true;
    }
}
