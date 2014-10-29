package ch.zhaw.ficore.p2abc.storage;

import eu.abc4trust.abce.internal.issuer.credentialManager.CredentialStorage;
import java.net.URI;
import java.io.IOException;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class GenericIssuerCredentialStorage implements CredentialStorage {
	
	private URIBytesStorage storage;

	@Inject
	public GenericIssuerCredentialStorage(@Named("issuerSecretKeyStorage") URIBytesStorage storage) {
		this.storage = storage;
	}
	
	public void addIssuerSecret(URI issuerParamsUid, byte[] bytes) throws IOException {
		try {
			storage.put(issuerParamsUid, bytes);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public byte[] getIssuerSecretKey(URI issuerParamsUid) throws IOException {
		try {
			return storage.get(issuerParamsUid);
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public List<URI> listIssuerSecretKeys() throws IOException {
		try {
			return storage.keys();
		}
		catch(Exception e) {
			throw new IOException(e);
		}
	}
}