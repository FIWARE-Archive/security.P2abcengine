package ch.zhaw.ficore.p2abc.services.issuance;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;

import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;
import ch.zhaw.ficore.p2abc.xml.QueryRule;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.xml.IssuancePolicy;

public class GenericIssuanceStorage implements IssuanceStorage {

    private final URIBytesStorage issuancePolicyStorage;
    private final URIBytesStorage queryRuleStorage;

    @Inject
    public GenericIssuanceStorage(
            @Named("issuancePolicyStorage") final URIBytesStorage issuancePolicyStorage,
            @Named("queryRuleStorage") final URIBytesStorage queryRuleStorage) {

        this.issuancePolicyStorage = issuancePolicyStorage;
        this.queryRuleStorage = queryRuleStorage;
    }

    public void addQueryRule(final URI uri, final QueryRule rule)
            throws IOException {
        try {
            byte[] data = SerializationUtils.serialize(rule);
            queryRuleStorage.put(uri, data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void addIssuancePolicy(final URI uri, final IssuancePolicy policy)
            throws IOException {
        try {
            byte[] data = SerializationUtils.serialize(policy);
            issuancePolicyStorage.put(uri, data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public QueryRule getQueryRule(final URI uri) throws IOException {
        try {
            if (!queryRuleStorage.containsKey(uri)) {
                return null;
            }

            byte[] data = queryRuleStorage.get(uri);
            return (QueryRule) SerializationUtils.deserialize(data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public IssuancePolicy getIssuancePolicy(final URI uri) throws IOException {
        try {
            if (!issuancePolicyStorage.containsKey(uri)) {
                return null;
            }

            byte[] data = issuancePolicyStorage.get(uri);
            return (IssuancePolicy) SerializationUtils.deserialize(data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public List<URI> listQueryRules() throws Exception {
        return queryRuleStorage.keys();
    }

    public void deleteQueryRule(final URI uri) throws IOException {
        try {
            queryRuleStorage.delete(uri);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}