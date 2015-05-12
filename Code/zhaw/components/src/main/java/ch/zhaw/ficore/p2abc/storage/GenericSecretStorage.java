package ch.zhaw.ficore.p2abc.storage;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.abce.internal.user.credentialManager.SecretStorage;

public class GenericSecretStorage implements SecretStorage {

    private final URIBytesStorage storage;

    @Inject
    public GenericSecretStorage(@Named("secretStorage") final URIBytesStorage storage) {
        this.storage = storage;
    }

    public void addSecret(final URI key, final byte[] bytes) throws IOException {
        try {
            storage.put(key, bytes);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public byte[] getSecret(final URI key) throws IOException {
        try {
            return storage.get(key);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public List<URI> listSecrets() throws IOException {
        try {
            return storage.keys();
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void deleteSecret(final URI key) throws IOException {
        try {
            storage.delete(key);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }
}