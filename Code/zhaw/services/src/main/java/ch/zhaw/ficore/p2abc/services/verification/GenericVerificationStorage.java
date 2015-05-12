package ch.zhaw.ficore.p2abc.services.verification;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;

import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.xml.PresentationPolicyAlternatives;

public class GenericVerificationStorage implements VerificationStorage {

    private final URIBytesStorage presentationPolicyStorage;
    private final URIBytesStorage redirectURIStorage;

    @Inject
    public GenericVerificationStorage(
            @Named("presentationPolicyStorage") final URIBytesStorage presentationPolicyStorage,
            @Named("redirectURIStorage") final URIBytesStorage redirectURIStorage) {

        this.presentationPolicyStorage = presentationPolicyStorage;
        this.redirectURIStorage = redirectURIStorage;
    }

    public void addPresentationPolicyAlternatives(final URI uri,
            final PresentationPolicyAlternatives ppa) throws IOException {
        try {
            byte[] data = SerializationUtils.serialize(ppa);
            presentationPolicyStorage.put(uri, data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void addRedirectURI(final URI key, final URI value)
            throws IOException {
        try {
            byte[] data = SerializationUtils.serialize(value);
            redirectURIStorage.put(key, data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public List<PresentationPolicyAlternatives> listPresentationPolicyAlternatives()
            throws IOException {
        try {
            List<PresentationPolicyAlternatives> ppas = new ArrayList<PresentationPolicyAlternatives>();
            for (URI uri : presentationPolicyStorage.keys()) {
                ppas.add(getPresentationPolicyAlternatives(uri));
            }
            return ppas;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public List<URI> listResourceURIs() throws IOException {
        try {
            return presentationPolicyStorage.keys();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public PresentationPolicyAlternatives getPresentationPolicyAlternatives(
            final URI uri) throws IOException {
        try {
            if (!presentationPolicyStorage.containsKey(uri)) {
                return null;
            }

            byte[] data = presentationPolicyStorage.get(uri);
            return (PresentationPolicyAlternatives) SerializationUtils
                    .deserialize(data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public URI getRedirectURI(final URI key) throws IOException {
        try {
            if (!presentationPolicyStorage.containsKey(key)) {
                return null;
            }

            byte[] data = redirectURIStorage.get(key);
            return (URI) SerializationUtils.deserialize(data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void deleteRedirectURI(final URI key) throws IOException {
        try {
            redirectURIStorage.delete(key);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void deletePresentationPolicyAlternatives(final URI key)
            throws IOException {
        try {
            presentationPolicyStorage.delete(key);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}