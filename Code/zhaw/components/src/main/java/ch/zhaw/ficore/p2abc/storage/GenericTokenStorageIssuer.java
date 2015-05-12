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
    private final URIBytesStorage tokensStorageIssuer;
    private final URIBytesStorage pseudonymsStorageIssuer;
    private final URIBytesStorage logStorageIssuer;

    @Inject
    public GenericTokenStorageIssuer(
            @Named("tokensStorageIssuer") final URIBytesStorage tokensStorageIssuer,
            @Named("pseudonymsStorageIssuer") final URIBytesStorage pseudonymsStorageIssuer,
            @Named("logStorageIssuer") final URIBytesStorage logStorageIssuer) {

        this.tokensStorageIssuer = tokensStorageIssuer;
        this.pseudonymsStorageIssuer = pseudonymsStorageIssuer;
        this.logStorageIssuer = logStorageIssuer;
    }

    public boolean checkForPseudonym(final String primaryKey) throws IOException {
        try {
            return pseudonymsStorageIssuer.containsKey(primaryKey);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void addPseudonymPrimaryKey(final String primaryKey) throws IOException {
        try {
            pseudonymsStorageIssuer.put(primaryKey, new byte[] { (byte) 0 });
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void deletePseudonym(final String primaryKey) throws Exception {
        pseudonymsStorageIssuer.delete(primaryKey);
    }

    public byte[] getToken(final URI tokenuid) throws Exception {
        return tokensStorageIssuer.get(tokenuid);
    }

    public void addToken(final URI tokenuid, final byte[] token) throws IOException {
        try {
            tokensStorageIssuer.put(tokenuid, token);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public boolean deleteToken(final URI tokenuid) throws Exception {

        // first remove stored pseudonyms for this token

        final byte[] result = getToken(tokenuid);

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                result);
        final ObjectInput objectInput = new ObjectInputStream(byteArrayInputStream);
        final IssuanceToken tokenResult = (IssuanceToken) objectInput.readObject();

        // Close the streams..
        objectInput.close();
        byteArrayInputStream.close();

        final List<PseudonymInToken> pseudonyms = tokenResult
                .getIssuanceTokenDescription()
                .getPresentationTokenDescription().getPseudonym();

        for (final PseudonymInToken p : pseudonyms) {
            final String primaryKey = DatatypeConverter.printBase64Binary(p
                    .getPseudonymValue());
            deletePseudonym(primaryKey);
        }

        // Delete the IssuanceToken
        tokensStorageIssuer.delete(tokenuid);

        return true;
    }

    public void addIssuanceLogEntry(final URI entryUID, final byte[] bytes)
            throws IOException {
        try {
            logStorageIssuer.put(entryUID, bytes);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public byte[] getIssuanceLogEntry(final URI entryUID) throws Exception {
        try {
            return logStorageIssuer.get(entryUID);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public boolean deleteIssuanceLogEntry(final URI entryUID) throws Exception {
        logStorageIssuer.delete(entryUID);
        return true;
    }
}