package ch.zhaw.ficore.p2abc.storage;

import java.net.URI;
import java.util.List;

/**
 * Interface for storage with a KeyValue-Structure
 * where the Key is an URI and Value is byte[].
 * 
 * @author mroman
 */
public interface URIBytesStorage {
	/**
	 * Put data into storage possibly overwriting an existing entry
	 * in the storage.
	 * 
	 * @param URI uri (key)
	 * @param bytes bytes (value)
	 */
	public void put(URI uri, byte[] bytes) throws Exception;
	
	/**
	 * Put data into storage if and only if no such
	 * entry exists yet in the storage.
	 * 
	 * @param URI uri (key)
	 * @param bytes bytes (value)
	 * @return true if data was added, false otherwise
	 */
	public boolean putNew(URI uri, byte[] bytes) throws Exception;
	
	/**
	 * Retreive a value from the Storage.
	 * 
	 * @param URI uri (key)
	 * @return bytes (value)
	 */
	public byte[] get(URI uri) throws Exception;
	
	/**
	 * Return a list of all keys (URIs).
	 * 
	 * @return List of URIs.
	 */
	public List<URI> keys() throws Exception;
	
	/**
	 * Checks whether an entry with a given key exists in the storage.
	 * 
	 * @param URI uri (key)
	 * @return true if exists, false otherwise
	 */
	public boolean containsKey(URI uri) throws Exception;
	
	/**
	 * Deletes an entry with a given key from the storage.
	 * 
	 * @param URI uri (key)
	 */
	public void delete(URI uri) throws Exception;
}