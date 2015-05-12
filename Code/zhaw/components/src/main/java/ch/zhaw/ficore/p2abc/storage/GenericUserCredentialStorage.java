package ch.zhaw.ficore.p2abc.storage;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.abce.internal.user.credentialManager.CredentialStorage;

public class GenericUserCredentialStorage implements CredentialStorage {

    private final URIBytesStorage credentialStorage;
    private final URIBytesStorage pseudonymStorage;

    @Inject
    public GenericUserCredentialStorage(
            @Named("credentialStorage") final URIBytesStorage credentialStorage,
            @Named("pseudonymStorage") final URIBytesStorage pseudonymStorage) {

        this.credentialStorage = credentialStorage;
        this.pseudonymStorage = pseudonymStorage;
    }

    public byte[] getCredential(final URI creduid) throws Exception {
        return credentialStorage.get(creduid);
    }

    public byte[] getPseudonymWithData(final URI pseudonymUid) throws Exception {
        return pseudonymStorage.get(pseudonymUid);
    }

    public List<URI> listCredentials() throws Exception {
        return credentialStorage.keys();
    }

    public void addCredential(final URI creduid, final byte[] data) throws IOException {
        try {
            credentialStorage.put(creduid, data);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void addPseudonymWithMetadata(final URI puid, final byte[] data)
            throws IOException {
        try {
            pseudonymStorage.put(puid, data);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void deletePseudonymWithMetadata(final URI puid) throws Exception {
        pseudonymStorage.delete(puid);
    }

    public void deleteCredential(final URI credui) throws Exception {
        credentialStorage.delete(credui);
    }

    public List<byte[]> listPseudonyms() throws Exception {
        return pseudonymStorage.values();
    }
}