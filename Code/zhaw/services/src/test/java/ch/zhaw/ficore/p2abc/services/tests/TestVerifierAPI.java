package ch.zhaw.ficore.p2abc.services.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.xml.PresentationPolicyAlternativesCollection;
import ch.zhaw.ficore.p2abc.xml.Settings;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.TestConstants;

import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicyAlternatives;

public class TestVerifierAPI extends JerseyTest {

	private String verificationServiceURL = "verification/protected/";
	private String verificationServiceURLUnprot = "verification/";

	public TestVerifierAPI() throws Exception {
		super("ch.zhaw.ficore.p2abc.services");
		verificationServiceURL = getBaseURI() + verificationServiceURL;
		verificationServiceURLUnprot = getBaseURI()
		        + verificationServiceURLUnprot;
	}

	private static String getBaseURI() {
		return "http://localhost:" + TestConstants.JERSEY_HTTP_PORT + "/";
	}

	static File storageFile;
	static String dbName = "URIBytesStorage";
	ObjectFactory of = new ObjectFactory();

	@BeforeClass
	public static void initJNDI() throws Exception {
		System.out.println("init [TestVerificationAPI]");
		// Create initial context
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
		        "org.apache.naming.java.javaURLContextFactory");
		System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
		InitialContext ic = new InitialContext();

		try {
			ic.destroySubcontext("java:");
		} catch (Exception e) {
		}

		ic.createSubcontext("java:");
		ic.createSubcontext("java:/comp");
		ic.createSubcontext("java:/comp/env");
		ic.createSubcontext("java:/comp/env/jdbc");
		ic.createSubcontext("java:/comp/env/cfg");
		ic.createSubcontext("java:/comp/env/cfg/Source");
		ic.createSubcontext("java:/comp/env/cfg/ConnectionParameters");

		ConnectionParameters cp = new ConnectionParameters();
		ic.bind("java:/comp/env/cfg/ConnectionParameters/attributes", cp);
		ic.bind("java:/comp/env/cfg/ConnectionParameters/authentication", cp);
		ic.bind("java:/comp/env/cfg/Source/attributes", "FAKE");
		ic.bind("java:/comp/env/cfg/Source/authentication", "FAKE");
		ic.bind("java:/comp/env/cfg/bindQuery", "FAKE");
		ic.bind("java:/comp/env/cfg/restAuthPassword", "");
		ic.bind("java:/comp/env/cfg/restAuthUser", "issuerapi");
		ic.bind("java:/comp/env/cfg/verificationServiceURL", "");
		ic.bind("java:/comp/env/cfg/userServiceURL", "");
		ic.bind("java:/comp/env/cfg/issuanceServiceURL", "");
		ic.bind("java:/comp/env/cfg/verifierIdentity", "unknown");

		SQLiteDataSource ds = new SQLiteDataSource();

		storageFile = File.createTempFile("test", "sql");

		ds.setUrl("jdbc:sqlite:" + storageFile.getPath());
		System.out.println(ds.getUrl());
		ic.rebind("java:/comp/env/jdbc/" + dbName, ds);
		ic.bind("java:/comp/env/cfg/useDbLocking", new Boolean(true));

		ic.close();

	}

	@Before
	public void doReset() throws Exception {
		RESTHelper.postRequest(verificationServiceURL + "reset"); // make sure
		                                                          // the
		                                                          // service
		                                                          // is
		                                                          // *clean*
		                                                          // before
		                                                          // each
		                                                          // test.
	}

	@AfterClass
	public static void cleanup() throws Exception {
		storageFile.delete();
	}

	/**
	 * Tests verifier status.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition Verifier set up and running.
	 * 
	 * @fiware-unit-test-test This test tests that a running verifier responds
	 *                        to a status request.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200.
	 */
	@Test
	public void testStatus() throws Exception {
		RESTHelper.getRequest(verificationServiceURL + "status");
	}

	/**
	 * Tests creation of resources.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition Verifier set up and running, no
	 *                                     resource with name "test" registered.
	 * 
	 * @fiware-unit-test-test This test tests that resources can be correctly
	 *                        registered. In particular, this test creates a
	 *                        resource with name "test", retrieves it again and
	 *                        then tests if, for the retrieved resource
	 * 
	 *                        * The number of URIs is 1. * The URI is "test". *
	 *                        The number of redirect URLs is 1. * The redirect
	 *                        URL is "http://localhost/foo". * The number of
	 *                        policy alternatives is 1.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200 for all requests, correct
	 *                                    values for all object attributes.
	 */
	@Test
	public void testCreateResource() throws Exception {
		MultivaluedMapImpl params = new MultivaluedMapImpl();
		params.add("redirectURI", "http://localhost/foo");

		RESTHelper.putRequest(verificationServiceURL + "resource/create/test",
		        params);

		PresentationPolicyAlternativesCollection ppac = (PresentationPolicyAlternativesCollection) RESTHelper
		        .getRequest(verificationServiceURL
		                + "presentationPolicyAlternatives/list",
		                PresentationPolicyAlternativesCollection.class);

		assertEquals(ppac.uris.size(), 1);
		assertEquals(ppac.uris.get(0), "test");
		assertEquals(ppac.redirectURIs.size(), 1);
		assertEquals(ppac.redirectURIs.get(0), "http://localhost/foo");
		assertEquals(ppac.presentationPolicyAlternatives.size(), 1);
	}

	/**
	 * Tests addition of presentation policy alternatives.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition Verifier set up and running, a
	 *                                     resource with name "test" registered.
	 * 
	 * @fiware-unit-test-test This test tests that policy alternatives can be
	 *                        added for resources. In this case, we add a policy
	 *                        alternative named "urn:policy", retrieve it again
	 *                        and then check that
	 * 
	 *                        * The number of policy alternatives is 1. * The
	 *                        policy name is "urn:policy".
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200 for all requests, correct
	 *                                    values for all object attributes.
	 */
	@Test
	public void testAddPPA() throws Exception {
		testCreateResource();

		MultivaluedMapImpl params = new MultivaluedMapImpl();
		params.add("puid", "urn:policy");

		RESTHelper.postRequest(verificationServiceURL
		        + "presentationPolicyAlternatives/addPolicyAlternative/test",
		        params);

		PresentationPolicyAlternatives ppas = (PresentationPolicyAlternatives) RESTHelper
		        .getRequest(verificationServiceURL
		                + "presentationPolicyAlternatives/get/test",
		                PresentationPolicyAlternatives.class);

		assertEquals(ppas.getPresentationPolicy().size(), 1);
		assertEquals(ppas.getPresentationPolicy().get(0).getPolicyUID()
		        .toString(), "urn:policy");
	}

	/**
	 * Tests deletion of presentation policy alternatives.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition Verifier set up and running, a
	 *                                     resource with name "test" registered,
	 *                                     having policy alternative with name
	 *                                     "urn:policy".
	 * 
	 * @fiware-unit-test-test This test tests that policy alternatives can be
	 *                        deleted. In this case, we delete a policy
	 *                        alternative named "urn:policy".
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200 for all requests.
	 */
	@Test
	public void testDeletePPA() throws Exception {
		testAddPPA();

		MultivaluedMapImpl params = new MultivaluedMapImpl();
		params.add("puid", "urn:policy");

		RESTHelper
		        .postRequest(
		                verificationServiceURL
		                        + "presentationPolicyAlternatives/deletePolicyAlternative/test",
		                params);

		PresentationPolicyAlternatives ppas = (PresentationPolicyAlternatives) RESTHelper
		        .getRequest(verificationServiceURL
		                + "presentationPolicyAlternatives/get/test",
		                PresentationPolicyAlternatives.class);

		assertEquals(ppas.getPresentationPolicy().size(), 0);
	}

	/**
	 * Tests deletion of resources.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition Verifier set up and running, a
	 *                                     resource with name "test" registered.
	 * 
	 * @fiware-unit-test-test This test tests that resources can be deleted. In
	 *                        this case, we delete a resource named "test".
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200 for all requests, resource
	 *                                    "test" gone after deletion.
	 */
	@Test
	public void testDeleteResource() throws Exception {
		testCreateResource();

		RESTHelper.deleteRequest(verificationServiceURL
		        + "resource/delete/test");

		PresentationPolicyAlternativesCollection ppac = (PresentationPolicyAlternativesCollection) RESTHelper
		        .getRequest(verificationServiceURL
		                + "presentationPolicyAlternatives/list",
		                PresentationPolicyAlternativesCollection.class);

		assertEquals(ppac.uris.size(), 0);
	}

	/**
	 * Tests adding an alias to a PresentationPolicyAlternative of a resource.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition Requires a resource with name "test"
	 *                                     and a PresentationPolicyAlternative
	 *                                     with "urn:policy".
	 * 
	 * @fiware-unit-test-test This test tests that an alias can be added.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddAlias() throws Exception {
		testAddPPA();

		MultivaluedMapImpl params = new MultivaluedMapImpl();
		params.add("al", "somealias");

		RESTHelper.postRequest(
		        verificationServiceURL
		                + "presentationPolicyAlternatives/addAlias/test/"
		                + URLEncoder.encode("urn:policy", "UTF-8"), params);
	}

	/**
	 * Tests deleting an alias to a PresentationPolicyAlternative of a resource.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition depends on {@link testAddAlias}
	 * 
	 * @fiware-unit-test-test This test tests that an alias can be deleted.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteAlias() throws Exception {
		testAddAlias();

		MultivaluedMapImpl params = new MultivaluedMapImpl();
		params.add("al", "somealias");

		RESTHelper.postRequest(verificationServiceURL
		        + "presentationPolicyAlternatives/deleteAlias/test/"
		        + URLEncoder.encode("urn:policy", "UTF-8"), params);
	}

	/**
	 * Tests adding an issuer alternative.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition depends on {@link testAddAlias}.
	 * 
	 * @fiware-unit-test-test This test tests than an issuer alternative can be
	 *                        added.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddIssuerAlternative() throws Exception {
		testAddAlias();

		MultivaluedMapImpl params = new MultivaluedMapImpl();
		params.add("al", "somealias");
		params.add("ip", "urn:issuer-alt");

		RESTHelper.postRequest(verificationServiceURL
		        + "presentationPolicyAlternatives/addIssuerAlternative/test/"
		        + URLEncoder.encode("urn:policy", "UTF-8"), params);
	}

	/**
	 * Tests deleting an issuer alternative.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition depends on
	 *                                     {@link testAddIssuerAlternative}
	 * 
	 * @fiware-unit-test-test This test tests than an issuer alternative can be
	 *                        deleted.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteIssuerAlternative() throws Exception {
		testAddIssuerAlternative();

		MultivaluedMapImpl params = new MultivaluedMapImpl();
		params.add("al", "somealias");
		params.add("ip", "urn:issuer-alt");

		RESTHelper
		        .postRequest(
		                verificationServiceURL
		                        + "presentationPolicyAlternatives/deleteIssuerAlternative/test/"
		                        + URLEncoder.encode("urn:policy", "UTF-8"),
		                params);
	}

	/**
	 * Tests adding a credential specification alternative
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-inital-condition depends on {@link testAddAlias}
	 * 
	 * @fiware-unit-test-test This test tests than a credential specification
	 *                        alternative can be added.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddCredSpecAlternative() throws Exception {
		testAddAlias();

		MultivaluedMapImpl params = new MultivaluedMapImpl();
		params.add("al", "somealias");
		params.add("cs", "urn:cred-alt");

		RESTHelper
		        .postRequest(
		                verificationServiceURL
		                        + "presentationPolicyAlternatives/addCredentialSpecificationAlternative/test/"
		                        + URLEncoder.encode("urn:policy", "UTF-8"),
		                params);
	}

	/**
	 * Tests deleting a credential specification alternative.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Securyt.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-inital-condition depends on
	 *                                    {@link testAddCredSpeAlternative}
	 * 
	 * @fiware-unit-test-test This test tests than a credential specification
	 *                        alternative can be deleted.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteCredSpecAlternative() throws Exception {
		testAddCredSpecAlternative();

		MultivaluedMapImpl params = new MultivaluedMapImpl();
		params.add("al", "somealias");
		params.add("cs", "urn:cred-alt");

		RESTHelper
		        .postRequest(
		                verificationServiceURL
		                        + "presentationPolicyAlternatives/deleteCredentialSpecificationAlternative/test/"
		                        + URLEncoder.encode("urn:policy", "UTF-8"),
		                params);
	}

	/**
	 * Tests credential specification storage and retrieval.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition Verifier set up, no credential
	 *                                     specification with URN
	 *                                     "urn:fiware:cred" in the database.
	 * 
	 * @fiware-unit-test-test This test creates a new credential specification
	 *                        with one attribute called "someAttribute" of type
	 *                        "xs:integer" and encoding
	 *                        "urn:abc4trust:1.0:encoding:integer:signed". It
	 *                        also creates a description with language "en". It
	 *                        then stores the credential specification and
	 *                        retrieves it again.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200 for all operations. After
	 *                                    this test, there will be a credential
	 *                                    specification with URN
	 *                                    "urn:fiware:cred" in the database.
	 * 
	 */
	@Test
	public void testStoreGetCredSpec() throws Exception {
		CredentialSpecification orig = new CredentialSpecification();
		orig.setSpecificationUID(new URI("urn:fiware:cred"));
		AttributeDescriptions attrDescs = new AttributeDescriptions();
		List<AttributeDescription> lsAttrDesc = attrDescs
		        .getAttributeDescription();

		AttributeDescription ad = new AttributeDescription();
		ad.setDataType(new URI("xs:integer"));
		ad.setEncoding(new URI("urn:abc4trust:1.0:encoding:integer:signed"));
		ad.setType(new URI("someAttribute"));

		FriendlyDescription fd = new FriendlyDescription();
		fd.setLang("en");
		fd.setValue("huhu");

		ad.getFriendlyAttributeName().add(fd);

		lsAttrDesc.add(ad);

		orig.setAttributeDescriptions(attrDescs);

		RESTHelper.putRequest(
		        verificationServiceURL + "credentialSpecification/store/"
		                + URLEncoder.encode("urn:fiware:cred", "UTF-8"),
		        RESTHelper.toXML(CredentialSpecification.class,
		                of.createCredentialSpecification(orig)));

		RESTHelper.getRequest(verificationServiceURL
		        + "credentialSpecification/get/"
		        + URLEncoder.encode("urn:fiware:cred", "UTF-8"));
	}

	/**
	 * Tests deletion of credential specifications.
	 * 
	 * @fiware-unit-test-feature 
	 *                           FIWARE.Feature.Security.Privacy.Verification.Verification
	 * 
	 * @fiware-unit-test-initial-condition depends on
	 *                                     {@link testStoreGetCredSpec}
	 * 
	 * @fiware-unit-test-test This test tests deleting an existent credential
	 *                        specification.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200.
	 * 
	 */
	@Test
	public void testDeleteSpec() throws Exception {
		/* First we need to actually store one. So we call a test... */
		testStoreGetCredSpec();
		RESTHelper.deleteRequest(verificationServiceURL
		        + "credentialSpecification/delete/"
		        + URLEncoder.encode("urn:fiware:cred", "UTF-8"));
	}

	/**
	 * @fiware-unit-test-feature FIWARE.Feature.Security.Privacy.User
	 * 
	 * @fiware-unit-test-initial-condition service running.
	 * 
	 * @fiware-unit-test-test Tests storing issuer parameters
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testStoreIssuerParams() throws Exception {
		IssuerParameters ip = new IssuerParameters();
		ip.setParametersUID(new URI("urn:ip"));
		ip.setAlgorithmID(new URI("urn:algo"));
		ip.setCredentialSpecUID(new URI("urn:cs"));

		RESTHelper.putRequest(
		        verificationServiceURL + "issuerParameters/store/"
		                + URLEncoder.encode("urn:ip", "UTF-8"),
		        RESTHelper.toXML(IssuerParameters.class,
		                of.createIssuerParameters(ip)));

		Settings settings = (Settings) RESTHelper.getRequest(
		        verificationServiceURLUnprot + "getSettings", Settings.class);
		assertEquals(settings.issuerParametersList.size(), 1);
		assertEquals(settings.issuerParametersList.get(0).getParametersUID()
		        .toString(), "urn:ip");
	}

	/**
	 * @fiware-unit-test-feature FIWARE.Feature.Security.Privacy.User
	 * 
	 * @fiware-unit-test-initial-condition depends on {@link testDeleteSpec}
	 * 
	 * @fiware-unit-test-test Tests deleting issuer parameters.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteIssuerParams() throws Exception {
		testStoreIssuerParams();
		RESTHelper.deleteRequest(verificationServiceURL
		        + "issuerParameters/delete/"
		        + URLEncoder.encode("urn:ip", "UTF-8"));

		Settings settings = (Settings) RESTHelper.getRequest(
		        verificationServiceURLUnprot + "getSettings", Settings.class);
		assertEquals(settings.issuerParametersList.size(), 0);
	}

}