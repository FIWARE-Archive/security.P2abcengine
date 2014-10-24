package ch.zhaw.ficore.p2abc.storage;

import eu.abc4trust.abce.internal.user.credentialManager.SecretStorage;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class GenericSecretStorage implements SecretStorage {
	
private URIBytesStorage storage;
	
	public GenericSecretStorage(URIBytesStorage storage) {
		this.storage = storage;
	}
	
	public void addSecret(URI key, byte[] bytes) throws IOException {
		try {
			storage.put(key, bytes);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public byte[] getSecret(URI key) throws IOException {
		try {
			return storage.get(key);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public List<URI> listSecrets() throws IOException {
		try {
			return storage.keys();
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public void deleteSecret(URI key) throws IOException {
		try {
			storage.delete(key);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
}