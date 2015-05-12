package ch.zhaw.ficore.p2abc.services.tests;

import static org.junit.Assert.*;

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
import ch.zhaw.ficore.p2abc.xml.Settings;

import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.TestConstants;

import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.ObjectFactory;

public class TestUserAPI extends JerseyTest {
	private String userServiceURL = "user/";
	private String userServiceURLUnprot = "user/";

	public TestUserAPI() throws Exception {
		super("ch.zhaw.ficore.p2abc.services");
		userServiceURL = getBaseURI() + userServiceURL;
		userServiceURLUnprot = getBaseURI() + userServiceURLUnprot;
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
		ic.bind("java:/comp/env/cfg/userServiceURL", "");
		ic.bind("java:/comp/env/cfg/verificationServiceURL", "");
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
		RESTHelper.postRequest(userServiceURL + "reset"); // make sure the
															// service is
															// *clean* before
															// each test.
	}

	@AfterClass
	public static void cleanup() throws Exception {
		storageFile.delete();
	}

	/**
	 * Tests user status.
	 * 
	 * @fiware-unit-test-feature FIWARE.Feature.Security.Privacy.User
	 * 
	 * @fiware-unit-test-initial-condition Service set up and running
	 * 
	 * @fiware-unit-test-test This test tests that the service is running.
	 * 
	 * @fiware-unit-test-expected-outcome HTTP 200.
	 */
	@Test
	public void testStatus() throws Exception {
		RESTHelper.getRequest(userServiceURL + "status");
	}

	/**
	 * Tests credential specification storage and retrieval.
	 * 
	 * @fiware-unit-test-feature FIWARE.Feature.Security.Privacy.User
	 * 
	 * @fiware-unit-test-initial-condition Service set up, no credential
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
				userServiceURL + "credentialSpecification/store/"
						+ URLEncoder.encode("urn:fiware:cred", "UTF-8"),
				RESTHelper.toXML(CredentialSpecification.class,
						of.createCredentialSpecification(orig)));

		RESTHelper.getRequest(userServiceURL + "credentialSpecification/get/"
				+ URLEncoder.encode("urn:fiware:cred", "UTF-8"));
	}

	/**
	 * Tests deletion of credential specifications.
	 * 
	 * @fiware-unit-test-feature FIWARE.Feature.Security.Privacy.User
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
		RESTHelper.deleteRequest(userServiceURL
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
				userServiceURL + "issuerParameters/store/"
						+ URLEncoder.encode("urn:ip", "UTF-8"),
				RESTHelper.toXML(IssuerParameters.class,
						of.createIssuerParameters(ip)));

		Settings settings = (Settings) RESTHelper.getRequest(userServiceURL
				+ "getSettings", Settings.class);
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
		RESTHelper.deleteRequest(userServiceURL + "issuerParameters/delete/"
				+ URLEncoder.encode("urn:ip", "UTF-8"));

		Settings settings = (Settings) RESTHelper.getRequest(userServiceURL
				+ "getSettings", Settings.class);
		assertEquals(settings.issuerParametersList.size(), 0);
	}
}
