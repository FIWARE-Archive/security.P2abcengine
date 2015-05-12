package ch.zhaw.ficore.p2abc.storage;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.abce.internal.issuer.credentialManager.CredentialStorage;

public class GenericIssuerCredentialStorage implements CredentialStorage {

    private final URIBytesStorage storage;

    @Inject
    public GenericIssuerCredentialStorage(
            @Named("issuerSecretKeyStorage") final URIBytesStorage storage) {
        this.storage = storage;
    }

    public void addIssuerSecret(final URI issuerParamsUid, final byte[] bytes)
            throws IOException {
        try {
            storage.put(issuerParamsUid, bytes);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public byte[] getIssuerSecretKey(final URI issuerParamsUid) throws IOException {
        try {
            return storage.get(issuerParamsUid);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public List<URI> listIssuerSecretKeys() throws IOException {
        try {
            return storage.keys();
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }
}