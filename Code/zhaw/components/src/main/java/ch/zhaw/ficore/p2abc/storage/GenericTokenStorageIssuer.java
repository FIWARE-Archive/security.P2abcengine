package ch.zhaw.ficore.p2abc.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.URI;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.abce.internal.issuer.tokenManagerIssuer.TokenStorageIssuer;
import eu.abc4trust.xml.IssuanceToken;
import eu.abc4trust.xml.PseudonymInToken;

public class GenericTokenStorageIssuer implements TokenStorageIssuer {
    private URIBytesStorage tokensStorageIssuer;
    private URIBytesStorage pseudonymsStorageIssuer;
    private URIBytesStorage logStorageIssuer;

    @Inject
    public GenericTokenStorageIssuer(
            @Named("tokensStorageIssuer") URIBytesStorage tokensStorageIssuer,
            @Named("pseudonymsStorageIssuer") URIBytesStorage pseudonymsStorageIssuer,
            @Named("logStorageIssuer") URIBytesStorage logStorageIssuer) {

        this.tokensStorageIssuer = tokensStorageIssuer;
        this.pseudonymsStorageIssuer = pseudonymsStorageIssuer;
        this.logStorageIssuer = logStorageIssuer;
    }

    public boolean checkForPseudonym(String primaryKey) throws IOException {
        try {
            return pseudonymsStorageIssuer.containsKey(primaryKey);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void addPseudonymPrimaryKey(String primaryKey) throws IOException {
        try {
            pseudonymsStorageIssuer.put(primaryKey, new byte[] { (byte) 0 });
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void deletePseudonym(String primaryKey) throws Exception {
        pseudonymsStorageIssuer.delete(primaryKey);
    }

    public byte[] getToken(URI tokenuid) throws Exception {
        return tokensStorageIssuer.get(tokenuid);
    }

    public void addToken(URI tokenuid, byte[] token) throws IOException {
        try {
            tokensStorageIssuer.put(tokenuid, token);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public boolean deleteToken(URI tokenuid) throws Exception {

        // first remove stored pseudonyms for this token

        byte[] result = getToken(tokenuid);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                result);
        ObjectInput objectInput = new ObjectInputStream(byteArrayInputStream);
        IssuanceToken tokenResult = (IssuanceToken) objectInput.readObject();

        // Close the streams..
        objectInput.close();
        byteArrayInputStream.close();

        List<PseudonymInToken> pseudonyms = tokenResult
                .getIssuanceTokenDescription()
                .getPresentationTokenDescription().getPseudonym();

        for (PseudonymInToken p : pseudonyms) {
            String primaryKey = DatatypeConverter.printBase64Binary(p
                    .getPseudonymValue());
            deletePseudonym(primaryKey);
        }

        // Delete the IssuanceToken
        tokensStorageIssuer.delete(tokenuid);

        return true;
    }

    public void addIssuanceLogEntry(URI entryUID, byte[] bytes)
            throws IOException {
        try {
            logStorageIssuer.put(entryUID, bytes);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public byte[] getIssuanceLogEntry(URI entryUID) throws Exception {
        try {
            return logStorageIssuer.get(entryUID);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public boolean deleteIssuanceLogEntry(URI entryUID) throws Exception {
        logStorageIssuer.delete(entryUID);
        return true;
    }
}