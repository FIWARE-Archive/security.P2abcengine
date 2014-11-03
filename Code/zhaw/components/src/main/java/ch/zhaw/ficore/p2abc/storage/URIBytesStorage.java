package ch.zhaw.ficore.p2abc.storage;

import java.net.URI;
import java.util.List;
import java.util.ArrayList;

/**
 * Interface for storage with a KeyValue-Structure
 * where the Key is an URI and Value is byte[].
 * 
 * @author mroman
 */
public abstract class URIBytesStorage {
	/**
	 * Put data into storage possibly overwriting an existing entry
	 * in the storage.
	 * 
	 * @param URI uri (key)
	 * @param bytes bytes (value)
	 */
	public void put(URI uri, byte[] bytes) throws Exception {
	    put(uri.toString(), bytes);
	}
	
	public abstract void put(String key, byte[] bytes) throws Exception;
	
	/**
	 * Put data into storage if and only if no such
	 * entry exists yet in the storage.
	 * 
	 * @param URI uri (key)
	 * @param bytes bytes (value)
	 * @return true if data was added, false otherwise
	 */
	public boolean putNew(URI uri, byte[] bytes) throws Exception {
	    return putNew(uri.toString(), bytes);
	}
	
	public abstract boolean putNew(String key, byte[] bytes) throws Exception;
	
	/**
	 * Retreive a value from the Storage.
	 * 
	 * @param URI uri (key)
	 * @return bytes (value)
	 */
	public byte[] get(URI uri) throws Exception {
	    return get(uri.toString());
	}
	
	public abstract byte[] get(String key) throws Exception;
	
	/**
	 * Return a list of all keys (URIs).
	 * 
	 * @return List of URIs.
	 */
	public List<URI> keys() throws Exception {
	    List<URI> uris = new ArrayList<URI>();
	    for(String key : keysAsStrings()) {
	        uris.add(new URI(key));
	    }
	    return uris;
	}
	
	/**
	 * Return a list of all values (byte[]).
	 * 
	 */
	public List<byte[]> values() throws Exception {
	    List<String> keys = keysAsStrings();
	    List<byte[]> values = new ArrayList<byte[]>();
	    for(String key : keys) {
	        values.add(get(key));
	    }
	    return values;
	}
	
	public abstract List<String> keysAsStrings() throws Exception;
	
	/**
	 * Checks whether an entry with a given key exists in the storage.
	 * 
	 * @param URI uri (key)
	 * @return true if exists, false otherwise
	 */
	public boolean containsKey(URI uri) throws Exception {
	    return containsKey(uri.toString());
	}
	
	public abstract boolean containsKey(String key) throws Exception;
	
	/**
	 * Deletes an entry with a given key from the storage.
	 * 
	 * @param URI uri (key)
	 */
	public void delete(URI uri) throws Exception {
	    delete(uri.toString());
	}
	
	public abstract void delete(String key) throws Exception;
}