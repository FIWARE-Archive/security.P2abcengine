package ch.zhaw.ficore.p2abc.services.verification;

import java.net.URI;
import java.util.List;

import eu.abc4trust.xml.PresentationPolicyAlternatives;

public interface VerificationStorage {

    public void addPresentationPolicy(URI uri,
            PresentationPolicyAlternatives ppa) throws Exception;

    public PresentationPolicyAlternatives getPresentationPolicy(URI uri)
            throws Exception;
    
    public List<PresentationPolicyAlternatives> listPresentationPolicies() throws Exception;
    
    public List<URI> listPresentationPoliciesURIS() throws Exception;

    public void addRedirectURI(URI key, URI value) throws Exception;

    public URI getRedirectURI(URI key) throws Exception;
}
