package ch.zhaw.ficore.p2abc.storage;

import java.io.IOException;
import java.net.URI;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.keyManager.KeyStorage;

public class GenericKeyStorage implements KeyStorage {

    private final URIBytesStorage storage;

    @Inject
    public GenericKeyStorage(@Named("keyStorage") final URIBytesStorage storage) {
        this.storage = storage;
    }

    public void addValueAndOverwrite(final URI uri, final byte[] key) throws IOException {
        try {
            storage.put(uri, key);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void addValue(final URI uri, final byte[] key) throws IOException {
        try {
            storage.put(uri, key);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public byte[] getValue(final URI uri) throws IOException {
        try {
            return storage.get(uri);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public URI[] listUris() throws IOException {
        try {
            return storage.keys().toArray(new URI[] {});
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void delete(final URI uri) throws IOException {
        try {
            storage.delete(uri);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }
}
