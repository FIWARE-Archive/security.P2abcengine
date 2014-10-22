package ch.zhaw.ficore.p2abc.storage;

import eu.abc4trust.keyManager.KeyStorage;

import java.net.URI;
import java.io.IOException;

public class GenericKeyStorage implements KeyStorage {
	
	private URIBytesStorage storage;
	
	public GenericKeyStorage(URIBytesStorage storage) {
		this.storage = storage;
	}
	
	public void addValueAndOverwrite(URI uri, byte[] key) throws IOException {
		try {
			storage.put(uri, key);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public void addValue(URI uri, byte[] key) throws IOException {
		try {
			storage.putNew(uri, key);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public byte[] getValue(URI uri) throws IOException {
		try {
			return storage.get(uri);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public URI[] listUris() throws IOException {
		try {
			return storage.keys().toArray(new URI[]{});
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
}