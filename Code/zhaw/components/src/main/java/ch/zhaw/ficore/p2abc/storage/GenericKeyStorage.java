package ch.zhaw.ficore.p2abc.storage;

import java.io.IOException;
import java.net.URI;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.keyManager.KeyStorage;

/** A generic key storage.
 *
 * @author Roman M&uuml;ntener &lt;roman.muentener@zhaw.ch&gt;
 *
 */
public class GenericKeyStorage implements KeyStorage {

    private final URIBytesStorage storage;


    /** Creates a generic key storage.
     *
     * @param storage the storage to use
     */
    @Inject
    public GenericKeyStorage(@Named("keyStorage") final URIBytesStorage storage) {
        this.storage = storage;
    }

    public final void addValueAndOverwrite(final URI uri, final byte[] key) throws IOException {
        try {
            storage.put(uri, key);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public final void addValue(final URI uri, final byte[] key) throws IOException {
        try {
            storage.put(uri, key);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public final byte[] getValue(final URI uri) throws IOException {
        try {
            return storage.get(uri);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public final URI[] listUris() throws IOException {
        try {
            return storage.keys().toArray(new URI[] {});
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public final void delete(final URI uri) throws IOException {
        try {
            storage.delete(uri);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }
}
