package ch.zhaw.ficore.p2abc.storage;

import eu.abc4trust.abce.internal.user.credentialManager.CredentialStorage;
import java.net.URI;
import java.io.IOException;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class GenericUserCredentialStorage implements CredentialStorage {
    
    private URIBytesStorage credentialStorage;
    private URIBytesStorage pseudonymStorage;
    
    @Inject
    public GenericUserCredentialStorage(
            @Named("credentialStorage") URIBytesStorage credentialStorage,
            @Named("pseudonymStorage") URIBytesStorage pseudonymStorage
            ) {
        
        this.credentialStorage = credentialStorage;
        this.pseudonymStorage = pseudonymStorage;
    }
    
    public byte[] getCredential(URI creduid) throws Exception {
        return credentialStorage.get(creduid);
    }
    
    public byte[] getPseudonymWithData(URI pseudonymUid) throws Exception {
        return pseudonymStorage.get(pseudonymUid);
    }
    
    public List<URI> listCredentials() throws Exception {
        return credentialStorage.keys();
    }
    
    public void addCredential(URI creduid, byte[] data) throws IOException {
        try {
            credentialStorage.put(creduid, data);
        }
        catch(Exception e) {
            throw new IOException(e);
        }
    }
    
    public void addPseudonymWithMetadata(URI puid, byte[] data) throws IOException {
        try {
            pseudonymStorage.put(puid, data);
        }
        catch(Exception e) {
            throw new IOException(e);
        }
    }
    
    public void deletePseudonymWithMetadata(URI puid) throws Exception {
        pseudonymStorage.delete(puid);
    }
    
    public void deleteCredential(URI credui) throws Exception {
        credentialStorage.delete(credui);
    }
    
    public List<byte[]> listPseudonyms() throws Exception {
        return pseudonymStorage.values();
    }
}