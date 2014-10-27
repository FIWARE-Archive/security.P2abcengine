package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.services.issuance.xml.QueryRule;
import eu.abc4trust.xml.IssuancePolicy;
import java.net.URI;

public interface IssuanceStorage {
	
	public void addIssuancePolicy(URI credSpecUid, IssuancePolicy policy) throws Exception;
	
	public void addQueryRule(URI credSpecUid, QueryRule rule) throws Exception;
	
	public QueryRule getQueryRule(URI credSpecUid) throws Exception;
	
	public IssuancePolicy getIssuancePolicy(URI credSpecUid) throws Exception;
}