package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.services.issuance.xml.QueryRule;
import eu.abc4trust.xml.IssuancePolicy;
import java.net.URI;

public interface IssuanceStorage {
	
	/**
	 * Add (and possibly overwrite an existing) IssuancePolicy bound to a CredentialSpecification
	 * as denoted by the Uid.
	 * 
	 * @param credSpecUid Uid of the CredentialSpecification
	 * @param policy IssuancePolicy to store.
	 */
	public void addIssuancePolicy(URI credSpecUid, IssuancePolicy policy) throws Exception;
	
	/**
	 * Add (and possibly overwrite an existing) QueryRule bound to a CredentialSpecification
	 * as denoted by the Uid.
	 * 
	 * @param credSpecUid Uid of the CredentialSpecification
	 * @param rule QueryRule to store
	 */
	public void addQueryRule(URI credSpecUid, QueryRule rule) throws Exception;
	
	/**
	 * Get the QueryRule bound to a CredentialSpecification as denoted by the Uid.
	 * 
	 * @param credSpecUid Uid of the CredentialSpecification
	 * @return the QueryRule bound to the given CredentialSpecification
	 */
	public QueryRule getQueryRule(URI credSpecUid) throws Exception;
	
	/**
	 * Get the IssuancePolicy bound to a CredentialSpecification as denoted by the Uid.
	 * 
	 * @param credSpecUid Uid of the CredentialSpecification
	 * @return the IssuancePolicy bound to the givin CredentialSpecification
	 */
	public IssuancePolicy getIssuancePolicy(URI credSpecUid) throws Exception;
}