package ch.zhaw.ficore.p2abc.services.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.xml.CredentialCollection;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.TestConstants;

import eu.abc4trust.xml.ApplicationData;
import eu.abc4trust.xml.Credential;
import eu.abc4trust.xml.PresentationPolicyAlternatives;

public class TestFlow extends JerseyTest {

    private String userServiceURL = "user/";
    private String verificationServiceURL = "verification/protected/";
    private String verificationServiceURLUnprot = "verification/";
    private String issuanceServiceURL = "issuance/protected/";
    private String issuanceServiceURLUnprot = "issuance/";
    private static String credSpecName = "test";
    private static String credSpecURI = "urn%3Afiware%3Aprivacy%3Atest";
    private static String issuanceURI = "urn%3Afiware%3Aprivacy%3Aissuance%3Aidemix";

    public TestFlow() throws Exception {
        super("ch.zhaw.ficore.p2abc");
        userServiceURL = getBaseURI() + userServiceURL;
        verificationServiceURL = getBaseURI() + verificationServiceURL;
        verificationServiceURLUnprot = getBaseURI()
                + verificationServiceURLUnprot;
        issuanceServiceURL = getBaseURI() + issuanceServiceURL;
        issuanceServiceURLUnprot = getBaseURI() + issuanceServiceURLUnprot;
    }

    private static String getBaseURI() {
        // return "http://srv-lab-t-425.zhaw.ch:8080/zhaw-p2abc-webservices/";
        return "http://localhost:" + TestConstants.JERSEY_HTTP_PORT + "/";
    }

    File storageFile;
    String dbName = "URIBytesStorage";

    @Before
    public void initJNDI() throws Exception {
        System.out.println("init [TestFlow]");
        this.setUp();
        // Create initial context
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.naming.java.javaURLContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
        final InitialContext ic = new InitialContext();

        try {
            ic.destroySubcontext("java:");
        } catch (final Exception e) {
        }

        ic.createSubcontext("java:");
        ic.createSubcontext("java:/comp");
        ic.createSubcontext("java:/comp/env");
        ic.createSubcontext("java:/comp/env/jdbc");
        ic.createSubcontext("java:/comp/env/cfg");
        ic.createSubcontext("java:/comp/env/cfg/Source");
        ic.createSubcontext("java:/comp/env/cfg/ConnectionParameters");

        final ConnectionParameters cp = new ConnectionParameters();
        ic.bind("java:/comp/env/cfg/ConnectionParameters/attributes", cp);
        ic.bind("java:/comp/env/cfg/ConnectionParameters/authentication", cp);

        ic.bind("java:/comp/env/cfg/Source/attributes", "FAKE");
        ic.bind("java:/comp/env/cfg/Source/authentication", "FAKE");
        ic.bind("java:/comp/env/cfg/bindQuery", "FAKE");
        ic.bind("java:/comp/env/cfg/restAuthPassword", "");
        ic.bind("java:/comp/env/cfg/restAuthUser", "flow");
        ic.bind("java:/comp/env/cfg/issuanceServiceURL", getBaseURI()
                + "issuance/");
        ic.bind("java:/comp/env/cfg/userServiceURL", getBaseURI() + "user/");
        ic.bind("java:/comp/env/cfg/verificationServiceURL", getBaseURI()
                + "verification/");
        ic.bind("java:/comp/env/cfg/verifierIdentity", "unknown");

        final SQLiteDataSource ds = new SQLiteDataSource();

        storageFile = File.createTempFile("test", "sql");

        ds.setUrl("jdbc:sqlite:" + storageFile.getPath());
        System.out.println(ds.getUrl());
        ic.rebind("java:/comp/env/jdbc/" + dbName, ds);
        ic.bind("java:/comp/env/cfg/useDbLocking", new Boolean(true));

        ic.close();

        RESTHelper.postRequest(issuanceServiceURL + "reset");
        RESTHelper.postRequest(verificationServiceURL + "reset");
        RESTHelper.postRequest(userServiceURL + "reset");

        // System.exit(1);
    }

    @After
    public void cleanup() throws Exception {
    }

    /**
     * Performs the whole more or less adopted from the ancient tutorial.
     * 
     * Please don't change the order of the calls as it is relevant! To run this
     * test you have to: - have the services running (on tomcat) - have write
     * access to the location where the STORAGE resides - the STORAGE must be
     * EMPTIED/CLEARED before running this. - the configuration needs to be
     * set-up to use FAKE identity and FAKE attribute source (because we check
     * against hardcoded values used in the Fake*Providers. (Please don't change
     * the values in Fake*Providers without reflecting the changes here and vice
     * versa)).
     * 
     * Notes: This integration test tests the whole flow from setup to
     * generation of CredentialSpecification to IssuanceRequest to Verification.
     * However, this test does not check any intermediate results (other than
     * ensuring that the webservices responded with the correct status code)
     * because this test assumes that if the final Verification process
     * succeeds, the test was successful. In other words: This test will obtain
     * a Credential from the Issuance service and verifies the obtained
     * Credential against a PresentationPolicy at the Verification service.
     * 
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     * @throws JAXBException
     * @throws NamingException
     * @throws UniformInterfaceException
     * @throws ClientHandlerException
     */
    @Test
    public void flowTest() throws UnsupportedEncodingException,
            InterruptedException, JAXBException, ClientHandlerException,
            UniformInterfaceException, NamingException {
        System.out.println("hi there");

        /*
         * Test if all three services are running by calling /status/ on each
         * service and expecting a 200 response.
         */
        testUserStatus();
        testIssuanceStatus();
        testVerificationStatus();

        /*
         * Ok, if we are here all services are at least running
         */

        /* Test authentication */
        testAuthentication(readTextFile("/simpleAuth.xml"));

        /*
         * Get an attributeInfoCollection and convert it to a
         * credentialSpecification
         */
        final String attributeInfoCollection = testAttributeInfoCollection();
        final String credSpec = testGenCredSpec(attributeInfoCollection);

        /* Store/Get credentialSpecification at issuer */
        testStoreCredSpecAtIssuer(credSpec);
        testGetCredSpecFromIssuer();

        /* Store/Get queryRule at issuer */
        testStoreQueryRuleAtIssuer(readTextFile("/queryRule.xml"));
        testGetQueryRuleFromIssuer();

        /* Store/Get IssuancePolicy at issuer */
        testStoreIssuancePolicyAtIssuer(readTextFile("/issuancePolicy.xml"));
        testGetIssuancePolicyFromIssuer();

        /*
         * Ok, if we are here the first phase of setup is done.
         */

        /* Generate the SystemParameters */
        final String systemParameters = testSetupSystemParametersIssuer();

        /* Store CredentialSpecification at User and Verifier */
        testStoreCredSpecAtUser(credSpec);
        testStoreCredSpecAtVerifier(credSpec);

        /* Store SystemParameters at User and Verifier */
        testStoreSysParamsAtUser(systemParameters);
        testStoreSysParamsAtVerifier(systemParameters);

        /* Setup IssuerParameters */
        final String issuerParameters = testSetupIssuerParametersIssuer(readTextFile("/issuerParametersInput.xml"));
        System.out.println("--- issuerParameters");
        System.out.println(issuerParameters);

        /* Store IssuerParameters at User and Verifier */
        testStoreIssParamsAtUser(issuerParameters);
        testStoreIssParamsAtVerifier(issuerParameters);

        /*
         * Ok, phase two of setup is done (which means setup is done). Now the
         * actual issuance protocol can take place.
         */

        for (int i = 0; i < 3; i++) {
            final String issuanceMessageAndBoolean = testIssuanceRequest(readTextFile("/issuanceRequest.xml"));

            /* Extract issuance message */
            final String firstIssuanceMessage = testExtractIssuanceMessage(issuanceMessageAndBoolean);
            System.out.println("--- firstIssuanceMessage");
            System.out.println(firstIssuanceMessage);

            /* Issuance steps in the protocol */
            final String issuanceReturn = testIssuanceStepUser1(firstIssuanceMessage);
            String contextString = getContextString(issuanceReturn);
            System.out.println("--- issuanceReturn");
            System.out.println(issuanceReturn);
            System.out.println(contextString);

            String uiIssuanceReturn = readTextFile("/uiIssuanceReturn.xml");
            uiIssuanceReturn = replaceContextString(uiIssuanceReturn,
                    contextString);
            System.out.println("--- uiIssuanceReturn");
            System.out.println(uiIssuanceReturn);

            final String secondIssuanceMessage = testIssuanceStepUserUi1(uiIssuanceReturn);
            System.out.println("--- secondIssuanceMessage");
            System.out.println(secondIssuanceMessage);

            final String thirdIssuanceMessageAndBoolean = testIssuanceStepIssuer1(secondIssuanceMessage);
            final String thirdIssuanceMessage = testExtractIssuanceMessage(thirdIssuanceMessageAndBoolean);

            @SuppressWarnings("unused")
            final String fourthIssuanceMessageAndBoolean = testIssuanceStepUser2(thirdIssuanceMessage);

            /* Verification stuff */
            final String presentationPolicyAlternatives = testCreatePresentationPolicy(readTextFile("/presentationPolicyAlternatives.xml"));
            testCreatePresentationPolicy(readTextFile("/presentationPolicyAlternatives.xml"));

            final String presentationReturn = testCreatePresentationToken(presentationPolicyAlternatives);
            contextString = getContextString(presentationReturn);
            System.out.println(contextString);

            String uiPresentationReturn = readTextFile("/uiPresentationReturn.xml");
            uiPresentationReturn = replaceContextString(uiPresentationReturn,
                    contextString);

            final String presentationToken = testCreatePresentationTokenUi(uiPresentationReturn);

            final String rPresentationToken = presentationToken.replaceAll(
                    "<\\?xml(.*)\\?>", "");
            final String rPresentationPolicyAlternatives = presentationPolicyAlternatives
                    .replaceAll("<\\?xml(.*)\\?>", "");

            String ppapt = "";
            ppapt += "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
            ppapt += "<PresentationPolicyAlternativesAndPresentationToken xmlns=\"http://abc4trust.eu/wp2/abcschemav1.0\" Version=\"1.0\">";
            ppapt += rPresentationPolicyAlternatives;
            ppapt += rPresentationToken;
            ppapt += "</PresentationPolicyAlternativesAndPresentationToken>";

            final String presentationTokenDescription = testVerifyTokenAgainstPolicy(ppapt);
            System.out.println(presentationTokenDescription);

            /* Verification stuff 2 */
            System.out.println("***********");
            System.out.println("***********");
            System.out.println("***********");
            System.out.println("***********");
            System.out.println("***********");

            testStorePresentationPolicyAlternatives(presentationPolicyAlternatives);

            System.out.println("!!!!!!!");

            for (int j = 0; j < 3; j++) {
                testStoreRedirectURI("http://srv-lab-t-425.zhaw.ch:8080/zhaw-p2abc-webservices/demo-resource/page");
                final String presentationPolicyAlternatives_ = testRequestResource();

                final PresentationPolicyAlternatives ppa = (PresentationPolicyAlternatives) RESTHelper
                        .fromXML(PresentationPolicyAlternatives.class,
                                presentationPolicyAlternatives_);

                final ApplicationData apd = ppa.getPresentationPolicy().get(0)
                        .getMessage().getApplicationData();
                System.out.println("APD: " + apd.getContent().get(0));

                final String presentationReturn_ = testCreatePresentationToken(presentationPolicyAlternatives_);
                final String contextString_ = getContextString(presentationReturn_);
                System.out.println(contextString_);

                String uiPresentationReturn_ = readTextFile("/uiPresentationReturn.xml");
                uiPresentationReturn_ = replaceContextString(
                        uiPresentationReturn_, contextString_);

                final String presentationToken_ = testCreatePresentationTokenUi(uiPresentationReturn_);
                /*
                 * PresentationToken presentationToken2 = (PresentationToken)
                 * RESTHelper.fromXML(PresentationToken.class,
                 * presentationToken_); System.out.println(";VI 0 is " +
                 * presentationToken2
                 * .getPresentationTokenDescription().getMessage
                 * ().getVerifierIdentity().getContent().get(0));
                 * presentationToken2
                 * .getPresentationTokenDescription().getMessage
                 * ().getVerifierIdentity().getContent().clear();
                 * presentationToken2
                 * .getPresentationTokenDescription().getMessage
                 * ().getVerifierIdentity().getContent().add("urn:verifier:1");
                 * presentationToken_ =
                 * RESTHelper.toXML(PresentationToken.class,
                 * of.createPresentationToken(presentationToken2));
                 */

                final String presentationTokenDescription_ = testRequestResource2(presentationToken_);
                System.out.println("**#*#*#*#*#**#*#");
                System.out.println(presentationTokenDescription_);
            }

            testLoadSettingsVerification();
            testLoadSettingsUser();

            System.gc();
        }

        /* Test user credentials */
        CredentialCollection credCol = (CredentialCollection) RESTHelper
                .getRequest(userServiceURL + "credential/list",
                        CredentialCollection.class);
        System.out.println("Found " + credCol.credentials.size()
                + " credentials!");
        List<Credential> creds = credCol.credentials;
        assertEquals(creds.size(), 3);
        for (final Credential c : creds) {
            String credUID = c.getCredentialDescription().getCredentialUID()
                    .toString();
            System.out.println(credUID);
            credUID = credUID.split("/")[1];
            final Credential c1 = (Credential) RESTHelper.getRequest(
                    userServiceURL + "credential/get/" + credUID,
                    Credential.class);
            assertEquals(c1.getCredentialDescription().getCredentialUID()
                    .toString().endsWith(credUID), true);
            RESTHelper.deleteRequest(userServiceURL + "credential/delete/"
                    + credUID);
        }

        credCol = (CredentialCollection) RESTHelper.getRequest(userServiceURL
                + "credential/list", CredentialCollection.class);
        System.out.println("Found " + credCol.credentials.size()
                + " credentials!");
        creds = credCol.credentials;
        assertEquals(creds.size(), 0);

        // while(true) {
        // Thread.sleep(100);
        // }
    }

    public String readTextFile(final String path) {
        try {
            final InputStream is = TestFlow.class.getResourceAsStream(path);
            final BufferedReader br = new BufferedReader(new InputStreamReader(
                    is));
            String lines = "";
            String line = "";
            while ((line = br.readLine()) != null) {
                lines += line + "\n";
            }
            br.close();
            System.out.println("*** " + path);
            System.out.println(lines);
            return lines;
        } catch (final Exception e) {
            e.printStackTrace();
            throw new RuntimeException("readTextFile(" + path + ") failed!", e);
        }
    }

    public String getContextString(final String input) {
        final Pattern pattern = Pattern.compile("<uiContext>(.*)</uiContext>");
        final Matcher m = pattern.matcher(input);
        m.find();
        return m.group(1);
    }

    public String replaceContextString(final String input,
            final String contextString) {
        return input.replaceAll("REPLACE-THIS-CONTEXT", contextString);
    }

    public Client getClient() {
        final Client c = Client.create();
        c.addFilter(new HTTPBasicAuthFilter("api", "jura"));
        return c;
    }

    public void testLoadSettingsVerification()
            throws UnsupportedEncodingException {
        final Client client = getClient();

        final WebResource webResource = client.resource(verificationServiceURL
                + "loadSettings?url="
                + URLEncoder.encode(issuanceServiceURLUnprot + "getSettings",
                        "UTF-8"));

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class);
        assertOk(response);
    }

    public void testLoadSettingsUser() throws UnsupportedEncodingException {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "loadSettings?url="
                + URLEncoder.encode(issuanceServiceURLUnprot + "getSettings",
                        "UTF-8"));

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class);
        assertOk(response);
    }

    public String testRequestResource2(final String pt) {
        final Client client = getClient();

        final WebResource webResource = client
                .resource(verificationServiceURLUnprot
                        + "requestResource2/resource");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, pt);
        assertOk(response);

        return response.getEntity(String.class);
    }

    public String testRequestResource() {
        final Client client = getClient();

        final WebResource webResource = client
                .resource(verificationServiceURLUnprot
                        + "requestResource/resource");

        final ClientResponse response = webResource.type("application/xml")
                .get(ClientResponse.class);
        assertOk(response);

        return response.getEntity(String.class);
    }

    public void testStorePresentationPolicyAlternatives(final String ppa) {
        final Client client = getClient();

        final WebResource webResource = client.resource(verificationServiceURL
                + "presentationPolicyAlternatives/store/resource");

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, ppa);
        assertOk(response);
    }

    public void testStoreRedirectURI(final String uri)
            throws UnsupportedEncodingException {
        final Client client = getClient();

        final WebResource webResource = client.resource(verificationServiceURL
                + "redirectURI/store/resource");

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, uri);
        assertOk(response);
    }

    public String testVerifyTokenAgainstPolicy(final String ppapt) {
        final Client client = getClient();

        final WebResource webResource = client
                .resource(verificationServiceURLUnprot
                        + "verifyTokenAgainstPolicy");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, ppapt);
        assertOk(response);
        return response.getEntity(String.class);
    }

    public String testCreatePresentationTokenUi(final String pr) {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "createPresentationTokenUi");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, pr);
        assertOk(response);
        return response.getEntity(String.class);
    }

    public String testCreatePresentationToken(final String ppa) {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "createPresentationToken");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, ppa);
        assertOk(response);
        return response.getEntity(String.class);
    }

    public String testCreatePresentationPolicy(final String ppa) {
        final Client client = getClient();

        final WebResource webResource = client
                .resource(verificationServiceURLUnprot
                        + "createPresentationPolicy");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, ppa);
        assertOk(response);
        return response.getEntity(String.class);
    }

    public String testIssuanceStepUser2(final String im) {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "issuanceProtocolStep");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, im);
        assertOk(response);
        return response.getEntity(String.class);
    }

    public String testIssuanceStepIssuer1(final String im) {
        final Client client = getClient();

        final WebResource webResource = client
                .resource(issuanceServiceURLUnprot + "issuanceProtocolStep");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, im);
        assertOk(response);
        return response.getEntity(String.class);
    }

    public String testIssuanceStepUserUi1(final String uir) {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "issuanceProtocolStepUi");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, uir);
        assertOk(response);
        return response.getEntity(String.class);
    }

    public String testIssuanceStepUser1(final String im) {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "issuanceProtocolStep");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, im);
        assertOk(response);
        return response.getEntity(String.class);
    }

    public String testExtractIssuanceMessage(final String imab) {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "extractIssuanceMessage");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, imab);
        assertOk(response);
        return response.getEntity(String.class);
    }

    public String testIssuanceRequest(final String ir) {
        final Client client = getClient();

        final WebResource webResource = client
                .resource(issuanceServiceURLUnprot + "issuanceRequest");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, ir);
        assertOk(response);
        return response.getEntity(String.class);
    }

    public void testStoreIssParamsAtUser(final String p) {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "issuerParameters/store/" + issuanceURI);

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, p);
        assertOk(response);
    }

    public void testStoreIssParamsAtVerifier(final String p) {
        final Client client = getClient();

        final WebResource webResource = client.resource(verificationServiceURL
                + "issuerParameters/store/" + issuanceURI);

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, p);
        assertOk(response);
    }

    public String testSetupIssuerParametersIssuer(final String input) {
        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + "setupIssuerParameters/");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, input);
        assertOk(response);

        return response.getEntity(String.class);
    }

    public void testStoreSysParamsAtUser(final String sp) {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "systemParameters/store");

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, sp);
        assertOk(response);
    }

    public void testStoreSysParamsAtVerifier(final String sp) {
        final Client client = getClient();

        final WebResource webResource = client.resource(verificationServiceURL
                + "systemParameters/store");

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, sp);
        assertOk(response);
    }

    public void testStoreCredSpecAtUser(final String credSpec) {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "credentialSpecification/store/" + credSpecURI);

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, credSpec);
        assertOk(response);
    }

    public void testStoreCredSpecAtVerifier(final String credSpec) {
        final Client client = getClient();

        final WebResource webResource = client.resource(verificationServiceURL
                + "credentialSpecification/store/" + credSpecURI);

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, credSpec);
        assertOk(response);
    }

    public String testSetupSystemParametersIssuer() {
        final String uri = "setupSystemParameters/?securityLevel=80&cryptoMechanism=urn:abc4trust:1.0:algorithm:idemix";

        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + uri);

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class);
        assertOk(response);

        return response.getEntity(String.class);
    }

    public String testGetIssuancePolicyFromIssuer() {
        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + "issuancePolicy/get/" + credSpecURI);

        final ClientResponse response = webResource.get(ClientResponse.class);

        assertOk(response);

        return response.getEntity(String.class);
    }

    public void testStoreIssuancePolicyAtIssuer(final String ip) {
        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + "issuancePolicy/store/" + credSpecURI);

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, ip);
        assertOk(response);
    }

    public String testGetQueryRuleFromIssuer() {
        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + "queryRule/get/" + credSpecURI);

        final ClientResponse response = webResource.get(ClientResponse.class);

        assertOk(response);

        return response.getEntity(String.class);
    }

    public void testStoreQueryRuleAtIssuer(final String queryRule) {
        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + "queryRule/store/" + credSpecURI);

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, queryRule);
        assertOk(response);
    }

    public String testGetCredSpecFromIssuer() {
        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + "credentialSpecification/get/" + credSpecURI);

        final ClientResponse response = webResource.get(ClientResponse.class);

        assertOk(response);

        return response.getEntity(String.class);
    }

    public void testStoreCredSpecAtIssuer(final String credSpec) {
        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + "credentialSpecification/store/" + credSpecURI);

        final ClientResponse response = webResource.type("application/xml")
                .put(ClientResponse.class, credSpec);
        assertOk(response);
    }

    public String testAuthentication(final String authRequest) {
        final Client client = getClient();

        final WebResource webResource = client
                .resource(issuanceServiceURLUnprot + "testAuthentication");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, authRequest);

        assertOk(response);

        return response.getEntity(String.class);
    }

    public String testGenCredSpec(final String attributeInfoCollection) {
        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + "credentialSpecification/generate");

        final ClientResponse response = webResource.type("application/xml")
                .post(ClientResponse.class, attributeInfoCollection);

        assertOk(response);

        return response.getEntity(String.class);
    }

    public String testAttributeInfoCollection() {
        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + "attributeInfoCollection/" + credSpecName);

        final ClientResponse response = webResource.get(ClientResponse.class);

        assertOk(response);

        return response.getEntity(String.class);
    }

    public void testUserStatus() {
        final Client client = getClient();

        final WebResource webResource = client.resource(userServiceURL
                + "status");

        final ClientResponse response = webResource.get(ClientResponse.class);

        assertOk(response);
    }

    public void testIssuanceStatus() {
        final Client client = getClient();

        final WebResource webResource = client.resource(issuanceServiceURL
                + "status");

        final ClientResponse response = webResource.get(ClientResponse.class);

        assertOk(response);
    }

    public void testVerificationStatus() {
        final Client client = getClient();

        final WebResource webResource = client.resource(verificationServiceURL
                + "status");

        final ClientResponse response = webResource.get(ClientResponse.class);

        assertOk(response);
    }

    public void assertOk(final ClientResponse response) {
        if (response.getStatus() != 200) {
            System.out.println("-- NOT OK --");
            System.out.println(response.getStatus());
            System.out.println(response.getEntity(String.class));
        }
        assertTrue(response.getStatus() == 200);
    }

}
