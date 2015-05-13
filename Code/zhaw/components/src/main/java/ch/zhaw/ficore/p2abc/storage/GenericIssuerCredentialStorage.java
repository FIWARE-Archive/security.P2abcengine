package ch.zhaw.ficore.p2abc.storage;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.abce.internal.issuer.credentialManager.CredentialStorage;

/** A generic storage class for issuer credentials.
 *
 * @author Roman M&uuml;ntener &lt;roman.muentener@zhaw.ch&gt;
 *
 */
public class GenericIssuerCredentialStorage implements CredentialStorage {

    private final URIBytesStorage storage;

    /** Creates a credential storage.
     *
     * @param storage the storage to use
     */
    @Inject
    public GenericIssuerCredentialStorage(
            @Named("issuerSecretKeyStorage") final URIBytesStorage storage) {
        this.storage = storage;
    }

    public final void addIssuerSecret(final URI issuerParamsUid, final byte[] bytes)
            throws IOException {
        try {
            storage.put(issuerParamsUid, bytes);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public final byte[] getIssuerSecretKey(final URI issuerParamsUid) throws IOException {
        try {
            return storage.get(issuerParamsUid);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public final List<URI> listIssuerSecretKeys() throws IOException {
        try {
            return storage.keys();
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }
}
