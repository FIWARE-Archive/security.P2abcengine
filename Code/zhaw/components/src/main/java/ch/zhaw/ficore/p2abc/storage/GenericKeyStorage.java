package ch.zhaw.ficore.p2abc.storage;

import java.io.IOException;
import java.net.URI;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.keyManager.KeyStorage;

public class GenericKeyStorage implements KeyStorage {

    private URIBytesStorage storage;

    @Inject
    public GenericKeyStorage(@Named("keyStorage") URIBytesStorage storage) {
        this.storage = storage;
    }

    public void addValueAndOverwrite(URI uri, byte[] key) throws IOException {
        try {
            storage.put(uri, key);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void addValue(URI uri, byte[] key) throws IOException {
        try {
            storage.put(uri, key);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public byte[] getValue(URI uri) throws IOException {
        try {
            return storage.get(uri);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public URI[] listUris() throws IOException {
        try {
            return storage.keys().toArray(new URI[] {});
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void delete(URI uri) throws IOException {
        try {
            storage.delete(uri);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
