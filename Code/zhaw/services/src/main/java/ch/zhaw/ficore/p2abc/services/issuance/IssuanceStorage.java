package ch.zhaw.ficore.p2abc.services.issuance;

import java.net.URI;
import java.util.List;

import ch.zhaw.ficore.p2abc.xml.QueryRule;
import eu.abc4trust.xml.IssuancePolicy;

public interface IssuanceStorage {

    /**
     * Add (and possibly overwrite an existing) IssuancePolicy bound to a
     * CredentialSpecification as denoted by the Uid.
     * 
     * @param credSpecUid
     *            Uid of the CredentialSpecification
     * @param policy
     *            IssuancePolicy to store.
     * @throws Exception
     *             when something went wrong.
     */
    public void addIssuancePolicy(URI credSpecUid, IssuancePolicy policy)
            throws Exception;

    /**
     * Add (and possibly overwrite an existing) QueryRule bound to a
     * CredentialSpecification as denoted by the Uid.
     * 
     * @param credSpecUid
     *            Uid of the CredentialSpecification
     * @param rule
     *            QueryRule to store
     * @throws Exception
     *             when something went wrong.
     */
    public void addQueryRule(URI credSpecUid, QueryRule rule) throws Exception;

    /**
     * Get the QueryRule bound to a CredentialSpecification as denoted by the
     * Uid.
     * 
     * @param credSpecUid
     *            Uid of the CredentialSpecification
     * @return the QueryRule bound to the given CredentialSpecification
     * @throws Exception
     *             when something went wrong.
     */
    public QueryRule getQueryRule(URI credSpecUid) throws Exception;

    /**
     * Returns a list of URIs of QueryRules.
     * 
     * @return List of URIs
     * @throws Exception
     *             when something went wrong.
     */
    public List<URI> listQueryRules() throws Exception;

    /**
     * Get the IssuancePolicy bound to a CredentialSpecification as denoted by
     * the Uid.
     * 
     * @param credSpecUid
     *            Uid of the CredentialSpecification
     * @return the IssuancePolicy bound to the givin CredentialSpecification
     * @throws Exception
     *             when something went wrong.
     */
    public IssuancePolicy getIssuancePolicy(URI credSpecUid) throws Exception;

}