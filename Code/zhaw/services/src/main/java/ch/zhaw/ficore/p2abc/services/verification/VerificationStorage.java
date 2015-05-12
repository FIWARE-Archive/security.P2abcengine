package ch.zhaw.ficore.p2abc.services.verification;

import java.net.URI;
import java.util.List;

import eu.abc4trust.xml.PresentationPolicyAlternatives;

public interface VerificationStorage {

	public void addPresentationPolicyAlternatives(URI uri,
	        PresentationPolicyAlternatives ppa) throws Exception;

	public PresentationPolicyAlternatives getPresentationPolicyAlternatives(
	        URI uri) throws Exception;

	public List<PresentationPolicyAlternatives> listPresentationPolicyAlternatives()
	        throws Exception;

	public List<URI> listResourceURIs() throws Exception;

	public void addRedirectURI(URI key, URI value) throws Exception;

	public URI getRedirectURI(URI key) throws Exception;

	public void deleteRedirectURI(URI key) throws Exception;

	public void deletePresentationPolicyAlternatives(URI key) throws Exception;
}
