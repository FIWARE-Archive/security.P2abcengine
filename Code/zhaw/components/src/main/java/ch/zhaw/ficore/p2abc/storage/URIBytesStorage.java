package ch.zhaw.ficore.p2abc.storage;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface for storage with a KeyValue-Structure where the Key is an URI (or a
 * String) and Value is byte[].
 *
 * @author mroman
 */
public abstract class URIBytesStorage {


    private static List<URIBytesStorage> instances = new ArrayList<URIBytesStorage>();

    public URIBytesStorage() {
        instances.add(this);
    }

    public static void clearEverything() throws Exception {
        for(final URIBytesStorage instance : instances) {
            instance.deleteAll();
        }
    }

    /**
     * Put data into storage possibly overwriting an existing entry in the
     * storage.
     *
     * @param uri
     *            the uri (key)
     * @param bytes
     *            the bytes (value)
     * @throws Exception
     *             on any error
     */
    public void put(final URI uri, final byte[] bytes) throws Exception {
        put(uri.toString(), bytes);
    }

    public abstract void put(String key, byte[] bytes) throws Exception;

    /**
     * Put data into storage if and only if no such entry exists yet in the
     * storage.
     *
     * @param uri
     *            the uri (key)
     * @param bytes
     *            the bytes (value)
     * @return true if data was added, false otherwise
     * @throws Exception
     *             on any error
     */
    public boolean putNew(final URI uri, final byte[] bytes) throws Exception {
        return putNew(uri.toString(), bytes);
    }

    public abstract boolean putNew(String key, byte[] bytes) throws Exception;

    /**
     * Retreive a value from the Storage.
     *
     * @param uri
     *            the uri (key)
     * @return the bytes (value)
     * @throws Exception
     *             on any error
     */
    public byte[] get(final URI uri) throws Exception {
        return get(uri.toString());
    }

    public abstract byte[] get(String key) throws Exception;

    /**
     * Return a list of all keys (URIs). Please use keysAsStrings if you plan on
     * using raw strings as keys.
     *
     * @return List of URIs.
     * @throws Exception
     *             on any error
     */
    public List<URI> keys() throws Exception {
        final List<URI> uris = new ArrayList<URI>();
        for (final String key : keysAsStrings()) {
            try {
                uris.add(new URI(key));
            } catch (final Exception e) {
                /*
                 * key wasn't an URI. Since this storage is also capable of
                 * using raw Strings as key (due to compatibility with the rest
                 * of the engine) it can happen that something isn't a correct
                 * URI. In such a case we just don't list it here. Storages
                 * using raw strings must use keysAsStrings(). -- munt
                 */
            }
        }
        return uris;
    }

    /**
     * Returns a list of all values.
     *
     * @return list of all values
     * @throws Exception
     *             on any error
     *
     */
    public List<byte[]> values() throws Exception {
        final List<String> keys = keysAsStrings();
        final List<byte[]> values = new ArrayList<byte[]>();
        for (final String key : keys) {
            values.add(get(key));
        }
        return values;
    }

    public abstract List<String> keysAsStrings() throws Exception;

    /**
     * Checks whether an entry with a given key exists in the storage.
     *
     * @param uri
     *            the uri (key)
     * @return true if exists, false otherwise
     * @throws Exception
     *             on any error
     */
    public boolean containsKey(final URI uri) throws Exception {
        return containsKey(uri.toString());
    }

    public abstract boolean containsKey(String key) throws Exception;

    /**
     * Deletes an entry with a given key from the storage.
     *
     * @param uri
     *            the uri (key)
     * @throws Exception
     *             on any error
     */
    public void delete(final URI uri) throws Exception {
        delete(uri.toString());
    }

    public abstract void delete(String key) throws Exception;

    /**
     * Deletes all entries in the storage.
     *
     * @throws Exception
     *             on any error
     * @throws Exception
     *             on any error
     */
    public abstract void deleteAll() throws Exception;
}