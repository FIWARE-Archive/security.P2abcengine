package ch.zhaw.ficore.p2abc.services.issuance;

import ch.zhaw.ficore.p2abc.services.issuance.xml.QueryRule;
import eu.abc4trust.xml.IssuancePolicy;
import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;

import java.net.URI;
import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.lang.SerializationUtils;

public class GenericIssuanceStorage implements IssuanceStorage {
	
	private URIBytesStorage issuancePolicyStorage;
	private URIBytesStorage queryRuleStorage;
	
	@Inject
	public GenericIssuanceStorage(@Named("issuancePolicyStorage") URIBytesStorage issuancePolicyStorage,
			@Named("queryRuleStorage") URIBytesStorage queryRuleStorage) {
		
		this.issuancePolicyStorage = issuancePolicyStorage;
		this.queryRuleStorage = queryRuleStorage;
	}
	
	public void addQueryRule(URI uri, QueryRule rule) throws IOException {
		try {
			byte[] data = SerializationUtils.serialize(rule);
			queryRuleStorage.put(uri, data);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public void addIssuancePolicy(URI uri, IssuancePolicy policy) throws IOException {
		try {
			byte[] data = SerializationUtils.serialize(policy);
			issuancePolicyStorage.put(uri, data);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public QueryRule getQueryRule(URI uri) throws IOException {
		try {
			if(!queryRuleStorage.containsKey(uri))
				return null;
			
			byte[] data = queryRuleStorage.get(uri);
			return (QueryRule) SerializationUtils.deserialize(data);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public IssuancePolicy getIssuancePolicy(URI uri) throws IOException {
		try {
			if(!issuancePolicyStorage.containsKey(uri))
				return null;
			
			byte[] data = issuancePolicyStorage.get(uri);
			return (IssuancePolicy) SerializationUtils.deserialize(data);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
}