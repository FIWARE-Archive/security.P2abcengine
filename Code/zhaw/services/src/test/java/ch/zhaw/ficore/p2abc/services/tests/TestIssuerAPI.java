package ch.zhaw.ficore.p2abc.services.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.core.Response;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.services.helpers.RESTException;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.user.UserService;
import ch.zhaw.ficore.p2abc.xml.AttributeInfoCollection;
import ch.zhaw.ficore.p2abc.xml.AuthInfoSimple;
import ch.zhaw.ficore.p2abc.xml.AuthenticationInformation;
import ch.zhaw.ficore.p2abc.xml.AuthenticationRequest;
import ch.zhaw.ficore.p2abc.xml.IssuanceRequest;
import ch.zhaw.ficore.p2abc.xml.QueryRule;
import ch.zhaw.ficore.p2abc.xml.QueryRuleCollection;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.TestConstants;

import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuancePolicy;
import eu.abc4trust.xml.ObjectFactory;

public class TestIssuerAPI extends JerseyTest {

    private String issuanceServiceURL = "issuance/protected/";
    private String issuanceServiceURLUnprot = "issuance/";

    public TestIssuerAPI() throws Exception {
        super("ch.zhaw.ficore.p2abc.services");
        issuanceServiceURL = getBaseURI() + issuanceServiceURL;
        issuanceServiceURLUnprot = getBaseURI() + issuanceServiceURLUnprot;
    }

    UserService userService;

    private static String getBaseURI() {
        return "http://localhost:" + TestConstants.JERSEY_HTTP_PORT + "/";
    }

    static File storageFile;
    static String dbName = "URIBytesStorage";
    ObjectFactory of = new ObjectFactory();

    @BeforeClass
    public static void initJNDI() throws Exception {
        System.out.println("init [TestIssuerAPI]");
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
        ic.bind("java:/comp/env/cfg/restAuthUser", "issuerapi");
        ic.bind("java:/comp/env/cfg/issuanceServiceURL", "");
        ic.bind("java:/comp/env/cfg/userServiceURL", "");
        ic.bind("java:/comp/env/cfg/verificationServiceURL", "");
        ic.bind("java:/comp/env/cfg/verifierIdentity", "unknown");

        final SQLiteDataSource ds = new SQLiteDataSource();

        storageFile = File.createTempFile("test", "sql");

        ds.setUrl("jdbc:sqlite:" + storageFile.getPath());
        System.out.println(ds.getUrl());
        ic.rebind("java:/comp/env/jdbc/" + dbName, ds);
        ic.bind("java:/comp/env/cfg/useDbLocking", new Boolean(true));

        ic.close();

    }

    @Before
    public void doReset() throws Exception {
        RESTHelper.postRequest(issuanceServiceURL + "reset"); // make sure the
                                                              // service is
                                                              // *clean*
                                                              // before each
                                                              // test.
    }

    @AfterClass
    public static void cleanup() throws Exception {
        storageFile.delete();
    }

    /**
     * Tests getSettings.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up.
     * 
     * @fiware-unit-test-test This test tests the getSettings method of the
     *                        issuer. It issues the request and checks that a
     *                        HTTP 200 answer is received. There is not much
     *                        that one can test here betond a HTTP 200 because
     *                        settings by nature involve much random (or at
     *                        least random-looking) data such as the system
     *                        parameters. Also the correct functioning of
     *                        getSettings should also be at least partly covered
     *                        by the flow tests.
     * 
     * @fiware-unit-test-expected-outcome HTTP 200
     * 
     * @throws Exception
     */
    @Test
    public void testGetSettings() throws Exception {
        /*
         * There isn't much we can test here yet I think because settings
         * involves system parameters and involves A LOT of random stuff. Also
         * the correct functioning of getSettings should also be at least partly
         * covered by the flow tests.
         */
        RESTHelper.getRequest(issuanceServiceURLUnprot + "getSettings");
    }

    /**
     * Tests query rules.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, no query rules stored
     *                                     in database.
     * 
     * @fiware-unit-test-test This test tests the correct functioning of the
     *                        query rule administration interface. The test
     *                        stores a small number of simple query strings,
     *                        retrieves them again, and checks if they are
     *                        correctly retrieved. Then it retrieves th entire
     *                        list of quer strings and checks that these (and
     *                        only these) query strings are present. Afterwards,
     *                        it deletes the query rules and checks that they
     *                        are no longer present.
     * 
     * @fiware-unit-test-expected-outcome HTTP 200 on storing the query rules,
     *                                    correct retrieval of stored query
     *                                    rules, HTTP 200 on deleting them and
     *                                    an empty query rule list after
     *                                    deletion.
     * 
     * @throws Exception
     */
    @Test
    public void testQueryRules() throws Exception {

        final QueryRule qr = new QueryRule();
        qr.queryString = "string1";

        RESTHelper.putRequest(
                issuanceServiceURL + "queryRule/store/urn%3Afoo1",
                RESTHelper.toXML(QueryRule.class, qr));

        qr.queryString = "string2";

        RESTHelper.putRequest(
                issuanceServiceURL + "queryRule/store/urn%3Afoo2",
                RESTHelper.toXML(QueryRule.class, qr));

        QueryRule qr_ = (QueryRule) RESTHelper.getRequest(issuanceServiceURL
                + "queryRule/get/urn%3Afoo1", QueryRule.class);
        assertEquals(qr_.queryString, "string1");

        qr_ = (QueryRule) RESTHelper.getRequest(issuanceServiceURL
                + "queryRule/get/urn%3Afoo2", QueryRule.class);
        assertEquals(qr_.queryString, "string2");

        QueryRuleCollection qrc = (QueryRuleCollection) RESTHelper.getRequest(
                issuanceServiceURL + "queryRule/list",
                QueryRuleCollection.class);
        assertEquals(qrc.queryRules.size(), qrc.uris.size());
        assertEquals(2, qrc.queryRules.size());

        for (final String s : new String[] { "urn:foo1", "urn:foo2" }) {
            assertEquals(qrc.uris.contains(s), true);
        }

        final Map<String, String> m = new HashMap<String, String>();
        m.put("string1", "urn:foo1");
        m.put("string2", "urn:foo2");

        for (int i = 0; i < qrc.queryRules.size(); i++) {
            final QueryRule q = qrc.queryRules.get(i);
            assertEquals(qrc.uris.get(i), m.get(q.queryString));
        }

        RESTHelper.deleteRequest(issuanceServiceURL
                + "queryRule/delete/urn:foo1");
        RESTHelper.deleteRequest(issuanceServiceURL
                + "queryRule/delete/urn:foo2");

        qrc = (QueryRuleCollection) RESTHelper.getRequest(issuanceServiceURL
                + "queryRule/list", QueryRuleCollection.class);
        assertEquals(qrc.queryRules.size(), qrc.uris.size());
        assertEquals(0, qrc.queryRules.size());
    }

    /**
     * Tests credential specification generation.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, attribute provider has
     *                                     attribute "someAttribute" pf type
     *                                     integer, no credential specification
     *                                     with name "test" in database.
     * 
     * @fiware-unit-test-test This test installs a credential specification with
     *                        name "test" containing the attribute
     *                        "someAttribute" in the Issuer, retrieves it again
     *                        and checks if all attributes have the correct
     *                        value.
     * 
     *                        In particular, this test checks:
     * 
     *                        * That a credential specification with the name
     *                        "test" exists * That it contains an attribute
     *                        named "someAttribute" * That this attribute is of
     *                        type "xs:integer" * That it is encoded as
     *                        "urn:abc4trust:1.0:encoding:integer:signed"
     * 
     * @fiware-unit-test-expected-outcome HTTP 200 on all requests, attribute
     *                                    and type and encoding as expected (see
     *                                    above).
     * 
     */
    @Test
    public void testGenCredSpec() throws Exception {
        final AttributeInfoCollection aic = (AttributeInfoCollection) RESTHelper
                .getRequest(
                        issuanceServiceURL + "attributeInfoCollection/test",
                        AttributeInfoCollection.class);

        assertEquals(aic.name, "test");
        assertTrue(aic.attributes.size() == 1);
        assertEquals(aic.attributes.get(0).name, "someAttribute");

        final CredentialSpecification credSpec = (CredentialSpecification) RESTHelper
                .postRequest(issuanceServiceURL
                        + "credentialSpecification/generate",
                        RESTHelper.toXML(AttributeInfoCollection.class, aic),
                        CredentialSpecification.class);

        assertEquals("urn:fiware:privacy:test", credSpec.getSpecificationUID()
                .toString());
        assertEquals(1, credSpec.getAttributeDescriptions()
                .getAttributeDescription().size());
        assertEquals("someAttribute", credSpec.getAttributeDescriptions()
                .getAttributeDescription().get(0).getType().toString());

        assertEquals("xs:integer", aic.attributes.get(0).mapping);
        assertEquals("urn:abc4trust:1.0:encoding:integer:signed",
                aic.attributes.get(0).encoding);

        final AttributeDescription ad = credSpec.getAttributeDescriptions()
                .getAttributeDescription().get(0);
        assertEquals("xs:integer", ad.getDataType().toString());
        assertEquals("urn:abc4trust:1.0:encoding:integer:signed", ad
                .getEncoding().toString());
        assertEquals("someAttribute attribute", ad.getFriendlyAttributeName()
                .get(0).getValue());
        assertEquals("en", ad.getFriendlyAttributeName().get(0).getLang());
    }

    /**
     * Tests credential specification storage and retrieval.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, no credential
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
        final CredentialSpecification orig = new CredentialSpecification();
        orig.setSpecificationUID(new URI("urn:fiware:cred"));
        final AttributeDescriptions attrDescs = new AttributeDescriptions();
        final List<AttributeDescription> lsAttrDesc = attrDescs
                .getAttributeDescription();

        final AttributeDescription ad = new AttributeDescription();
        ad.setDataType(new URI("xs:integer"));
        ad.setEncoding(new URI("urn:abc4trust:1.0:encoding:integer:signed"));
        ad.setType(new URI("someAttribute"));

        final FriendlyDescription fd = new FriendlyDescription();
        fd.setLang("en");
        fd.setValue("huhu");

        ad.getFriendlyAttributeName().add(fd);

        lsAttrDesc.add(ad);

        orig.setAttributeDescriptions(attrDescs);

        RESTHelper.putRequest(
                issuanceServiceURL + "credentialSpecification/store/"
                        + URLEncoder.encode("urn:fiware:cred", "UTF-8"),
                RESTHelper.toXML(CredentialSpecification.class,
                        of.createCredentialSpecification(orig)));

        RESTHelper.getRequest(issuanceServiceURL
                + "credentialSpecification/get/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"));
    }

    /**
     * Tests attribute deletion.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, a credential
     *                                     specification with URN
     *                                     "urn:fiware:cred" exists in the
     *                                     database, this credential
     *                                     specification has exactly one
     *                                     attribute.
     * 
     * @fiware-unit-test-test This test tests the deletion of attributes from
     *                        credential specifications. We delete the first
     *                        attribute from a credential "urn:fiware:cred".
     * 
     * @fiware-unit-test-expected-outcome HTTP 200 on all requests and zero
     *                                    attributes remaining in
     *                                    "urn:fiware"cred" afterwards.
     * 
     */
    @Test
    public void testDeleteAttribute() throws Exception {
        testStoreGetCredSpec();

        final MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("i", "0");

        RESTHelper
                .deleteRequest(issuanceServiceURL
                        + "credentialSpecification/deleteAttribute/"
                        + URLEncoder.encode("urn:fiware:cred", "UTF-8"), params);

        final CredentialSpecification credSpec = (CredentialSpecification) RESTHelper
                .getRequest(issuanceServiceURL + "credentialSpecification/get/"
                        + URLEncoder.encode("urn:fiware:cred", "UTF-8"),
                        CredentialSpecification.class);

        assertEquals(credSpec.getAttributeDescriptions()
                .getAttributeDescription().size(), 0);
    }

    /**
     * Tests wrong deletion of nonexistent attribute.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, a credential
     *                                     specification with URN
     *                                     "urn:fiware:cred" exists in the
     *                                     database, this credential
     *                                     specification has exactly one
     *                                     attribute.
     * 
     * @fiware-unit-test-test This test tests what happens when someone attempts
     *                        to delete an attribute that does not exist. It
     *                        uses the same credential as the
     *                        {@link #testDeleteAttribute() testDeleteAttribute}
     *                        test but tries to delete the (nonexistent) second
     *                        attribute instead of the first.
     * 
     * @fiware-unit-test-expected-outcome A HTTP 404 on the attempt to delete
     *                                    the (nonexistent) second attribute.
     * 
     */
    @Test
    public void testDeleteAttributeInvalid() throws Exception {
        testStoreGetCredSpec();

        try {
            final MultivaluedMapImpl params = new MultivaluedMapImpl();
            params.add("i", "2");

            RESTHelper.deleteRequest(
                    issuanceServiceURL
                            + "credentialSpecification/deleteAttribute/"
                            + URLEncoder.encode("urn:fiware:cred", "UTF-8"),
                    params);
            throw new RuntimeException("Expected exception!");
        } catch (final RESTException e) {
            assertEquals(e.getStatusCode(), 404);
        }
    }

    /**
     * Tests generation of issuer parameters.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, a credential
     *                                     specification with URN
     *                                     "urn:fiware:cred" exists in the
     *                                     database, this credential
     *                                     specification has exactly one
     *                                     attribute.
     * 
     * @fiware-unit-test-test This test tests the generation of issuer
     *                        parameters for a given credential specification.
     * 
     * @fiware-unit-test-expected-outcome HTTP 200.
     * 
     */
    @Test
    public void testGenerateIssuerParams() throws Exception {
        testStoreGetCredSpec();

        RESTHelper.postRequest(issuanceServiceURL
                + "issuerParameters/generate/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"));
    }

    /**
     * Tests generation of issuer parameters for nonexistent credential
     * specification.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, <em>no</em> credential
     *                                     specification with URN
     *                                     "urn:fiware:crad" (note the "a")
     *                                     exists in the database.
     * 
     * @fiware-unit-test-test This test tests the generation of issuer
     *                        parameters for a nonexistent credential
     *                        specification.
     * 
     * @fiware-unit-test-expected-outcome HTTP 404.
     * 
     */
    @Test
    public void testGenerateIssuerParamsInvalid() throws Exception {
        testStoreGetCredSpec();

        try {
            RESTHelper.postRequest(issuanceServiceURL
                    + "issuerParameters/generate/"
                    + URLEncoder.encode("urn:fiware:crad", "UTF-8"));
            throw new RuntimeException("Expected exception!");
        } catch (final RESTException e) {
            assertEquals(e.getStatusCode(), 404);
        }
    }

    /**
     * Tests deletion of issuer parameters.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, a credential
     *                                     specification with URN
     *                                     "urn:fiware:cred" exists in the
     *                                     database.
     * 
     * @fiware-unit-test-test This test tests the deletion of issuer parameters
     *                        for a given credential specification.
     * 
     * @fiware-unit-test-expected-outcome HTTP 200.
     * 
     */
    @Test
    public void testDeleteIssuerParams() throws Exception {
        testGenerateIssuerParams();

        RESTHelper.deleteRequest(issuanceServiceURL
                + "issuerParameters/delete/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"));
    }

    /**
     * Tests storing of issuance policies.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up.
     * 
     * @fiware-unit-test-test This test tests the storing of a default issuance
     *                        policy.
     * 
     * @fiware-unit-test-expected-outcome HTTP 200.
     * 
     */
    @Test
    public void testStoreIssuancePolicy() throws Exception {
        final IssuancePolicy ip = new IssuancePolicy();

        RESTHelper.putRequest(
                issuanceServiceURL + "issuancePolicy/store/ip",
                RESTHelper.toXML(IssuancePolicy.class,
                        of.createIssuancePolicy(ip)));
    }

    /**
     * Tests retrieval of issuance policies.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, some issuance policy
     *                                     stored in the database.
     * 
     * @fiware-unit-test-test This test tests the deletion of an issuance
     *                        policy.
     * 
     * @fiware-unit-test-expected-outcome HTTP 200.
     * 
     */
    @Test
    public void testGetIssuancePolicy() throws Exception {
        testStoreIssuancePolicy();

        RESTHelper.getRequest(issuanceServiceURL + "issuancePolicy/get/ip");
    }

    /**
     * Tests adding of descriptions.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, credential
     *                                     specification with name
     *                                     "urn:fiware:cred" stored in the
     *                                     database.
     * 
     * @fiware-unit-test-test This test tests adding "friendly descriptions" to
     *                        a credential specification. In this case, we also
     *                        test that languages that use UTF-8 characters not
     *                        present in ASCII are correctly supported.
     * 
     * @fiware-unit-test-expected-outcome HTTP 200 and parameters retrieved
     *                                    correctly in the correct encoding.
     * 
     */
    @Test
    public void testAddFriendlyDescription() throws Exception {
        testStoreGetCredSpec();

        final MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("language", "ch");
        params.add("value", "chuchichäschtli");
        params.add("i", "0");

        RESTHelper.putRequest(issuanceServiceURL
                + "credentialSpecification/addFriendlyDescriptionAttribute/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"), params);

        final CredentialSpecification credSpec = (CredentialSpecification) RESTHelper
                .getRequest(issuanceServiceURL + "credentialSpecification/get/"
                        + URLEncoder.encode("urn:fiware:cred", "UTF-8"),
                        CredentialSpecification.class);

        final List<FriendlyDescription> fds = credSpec
                .getAttributeDescriptions().getAttributeDescription().get(0)
                .getFriendlyAttributeName();
        assertEquals(fds.get(1).getLang(), "ch");
        assertEquals(fds.get(1).getValue(), "chuchichäschtli");
    }

    /**
     * Tests deletion of descriptions.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, credential
     *                                     specification with name
     *                                     "urn:fiware:cred" stored in the
     *                                     database, with exactly one friendly
     *                                     description.
     * 
     * @fiware-unit-test-test This test tests deleting "friendly descriptions"
     *                        to a credential specification.
     * 
     * @fiware-unit-test-expected-outcome HTTP 200 and there are no more
     *                                    friendly descriptions for that
     *                                    credential specification.
     * 
     */
    @Test
    public void testDeleteFriendlyDescription() throws Exception {
        testAddFriendlyDescription();

        final MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("language", "ch");
        params.add("i", "0");
        RESTHelper.deleteRequest(issuanceServiceURL
                + "credentialSpecification/deleteFriendlyDescriptionAttribute/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"), params);

        final CredentialSpecification credSpec = (CredentialSpecification) RESTHelper
                .getRequest(issuanceServiceURL + "credentialSpecification/get/"
                        + URLEncoder.encode("urn:fiware:cred", "UTF-8"),
                        CredentialSpecification.class);

        final List<FriendlyDescription> fds = credSpec
                .getAttributeDescriptions().getAttributeDescription().get(0)
                .getFriendlyAttributeName();

        assertEquals(fds.size(), 1);
    }

    /**
     * Tests invalid deletion of nonexistent descriptions.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, credential
     *                                     specification with name
     *                                     "urn:fiware:cred" stored in the
     *                                     database, with no friendly
     *                                     descriptions.
     * 
     * @fiware-unit-test-test This test tests invalidly deleting
     *                        "friendly descriptions" from a credential
     *                        specification that has no friendly descriptions.
     * 
     * @fiware-unit-test-expected-outcome HTTP 404.
     * 
     */
    @Test
    public void testDeleteFriendlyDescriptionInvalid() throws Exception {
        testAddFriendlyDescription();

        try {
            final MultivaluedMapImpl params = new MultivaluedMapImpl();
            params.add("language", "de");
            params.add("i", "0");
            RESTHelper
                    .deleteRequest(
                            issuanceServiceURL
                                    + "credentialSpecification/deleteFriendlyDescriptionAttribute/"
                                    + URLEncoder.encode("urn:fiware:cred",
                                            "UTF-8"), params);
            throw new RuntimeException("Expected exception!");

        } catch (final RESTException e) {
            assertEquals(e.getStatusCode(), 404);
        }
    }

    /**
     * Tests invalid adding of descriptions.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, credential
     *                                     specification with name
     *                                     "urn:fiware:cred" stored in the
     *                                     database, with no more than 4
     *                                     friendly descriptions.
     * 
     * @fiware-unit-test-test This test tests invalidly adding a "friendly
     *                        descriptions" as the sixth description for a
     *                        credential specification that has no more than
     *                        four friendly descriptions.
     * 
     * @fiware-unit-test-expected-outcome HTTP 404.
     * 
     */
    @Test
    public void testAddFriendlyDescriptionInvalid() throws Exception {
        testStoreGetCredSpec();

        final MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("language", "ch");
        params.add("value", "chuchichäschtli");
        params.add("i", "5");

        try {

            RESTHelper
                    .putRequest(
                            issuanceServiceURL
                                    + "credentialSpecification/addFriendlyDescriptionAttribute/"
                                    + URLEncoder.encode("urn:fiware:cred",
                                            "UTF-8"), params);

            throw new RuntimeException("Expected exception!");
        } catch (final RESTException e) {
            assertEquals(e.getStatusCode(), 404);
        }
    }

    /**
     * Tests invalid adding of descriptions.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, credential
     *                                     specification with name
     *                                     "urn:fiware:cred" stored in the
     *                                     database, with no more than 4
     *                                     friendly descriptions.
     * 
     * @fiware-unit-test-test This test tests invalidly adding a credential
     *                        specification which claims in its data to be
     *                        called "urn:fiware:cred", but which is referenced
     *                        in the URL as "urn:fiware:creed".
     * 
     * @fiware-unit-test-expected-outcome HTTP 409.
     * 
     */
    @Test
    public void testStoreSpecInvalid() throws Exception {
        final CredentialSpecification orig = new CredentialSpecification();
        orig.setSpecificationUID(new URI("urn:fiware:cred"));
        final AttributeDescriptions attrDescs = new AttributeDescriptions();
        final List<AttributeDescription> lsAttrDesc = attrDescs
                .getAttributeDescription();

        final AttributeDescription ad = new AttributeDescription();
        ad.setDataType(new URI("xs:integer"));
        ad.setEncoding(new URI("urn:abc4trust:1.0:encoding:integer:signed"));
        ad.setType(new URI("someAttribute"));

        final FriendlyDescription fd = new FriendlyDescription();
        fd.setLang("en");
        fd.setValue("huhu");

        ad.getFriendlyAttributeName().add(fd);

        lsAttrDesc.add(ad);

        orig.setAttributeDescriptions(attrDescs);

        try {
            RESTHelper.putRequest(
                    issuanceServiceURL + "credentialSpecification/store/"
                            + URLEncoder.encode("urn:fiware:creed", "UTF-8"),
                    RESTHelper.toXML(CredentialSpecification.class,
                            of.createCredentialSpecification(orig)));
            throw new RuntimeException("Expected exception!");
        } catch (final RESTException e) {
            assertEquals(e.getStatusCode(), 409);
        }
    }

    /**
     * Tests invalid deletion of nonexistentent descriptions.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, <em>no</em> credential
     *                                     specification with name
     *                                     "urn:non-existing" stored in the
     *                                     database.
     * 
     * @fiware-unit-test-test This test tests invalidly deleting a nonexistent
     *                        credential specification.
     * 
     * @fiware-unit-test-expected-outcome HTTP 404.
     * 
     */
    @Test
    public void testDeleteSpecInvalid() throws Exception {
        try {
            RESTHelper.deleteRequest(issuanceServiceURL
                    + "credentialSpecification/delete/"
                    + URLEncoder.encode("urn:non-existing", "UTF-8"));
            throw new RuntimeException("Expected exception!");
        } catch (final RESTException e) {
            assertEquals(e.getStatusCode(), 404);
        }
    }

    /**
     * Tests deletion of credential specifications.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, credential
     *                                     specification with name
     *                                     "urn:fiware:cred" stored in the
     *                                     database.
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
        RESTHelper.deleteRequest(issuanceServiceURL
                + "credentialSpecification/delete/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"));
    }

    /**
     * Tests correct authentication.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, user with user name
     *                                     "CaroleKing" and password "Jazzman"
     *                                     stored in the authentication
     *                                     provider.
     * 
     * @fiware-unit-test-test This test tests successful authentication.
     * 
     * @fiware-unit-test-expected-outcome HTTP 200.
     * 
     */
    @Test
    public void testTestAuthentication() throws Exception {
        final AuthenticationRequest authReq = new AuthenticationRequest();
        final AuthenticationInformation authInfo = new AuthInfoSimple(
                "CaroleKing", "Jazzman");
        authReq.authInfo = authInfo;
        RESTHelper.postRequest(issuanceServiceURLUnprot + "testAuthentication",
                RESTHelper.toXML(AuthenticationRequest.class, authReq));
    }

    /**
     * Tests authentication failure with invalid credentials.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, <em>no</em>with user
     *                                     name "CaröléKing" and password
     *                                     "Jazzman" stored in the
     *                                     authentication provider.
     * 
     * @fiware-unit-test-test This test tests authentication failure.
     * 
     * @fiware-unit-test-expected-outcome HTTP 403.
     * 
     */
    @Test
    public void testTestAuthenticationInvalid() throws Exception {
        final AuthenticationRequest authReq = new AuthenticationRequest();
        final AuthenticationInformation authInfo = new AuthInfoSimple(
                "CaröléKing", "Jazzman");
        authReq.authInfo = authInfo;
        try {
            RESTHelper.postRequest(issuanceServiceURLUnprot
                    + "testAuthentication",
                    RESTHelper.toXML(AuthenticationRequest.class, authReq));
            throw new RuntimeException("Expected exception!");
        } catch (final RESTException e) {
            assertEquals(e.getStatusCode(), 403);
        }
    }

    /**
     * Tests authentication failure with credential issuance.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, <em>no</em>with user
     *                                     name "CaröléKing" and password
     *                                     "Jazzman" stored in the
     *                                     authentication provider.
     * 
     * @fiware-unit-test-test This test tests authentication failure when a user
     *                        issues a credential issuance request with invalid
     *                        credentials.
     * 
     * @fiware-unit-test-expected-outcome HTTP 403.
     * 
     */
    @Test
    public void testIssuanceRequestInvalid() throws Exception {

        final AuthenticationRequest authReq = new AuthenticationRequest();
        final AuthenticationInformation authInfo = new AuthInfoSimple(
                "CaröléKing", "Jazzman");
        authReq.authInfo = authInfo;
        final IssuanceRequest isReq = new IssuanceRequest();
        isReq.authRequest = authReq;
        isReq.credentialSpecificationUid = "urn:fiware:cred";
        try {
            RESTHelper.postRequest(
                    issuanceServiceURLUnprot + "issuanceRequest",
                    RESTHelper.toXML(IssuanceRequest.class, isReq));
            throw new RuntimeException("Expected exception!");
        } catch (final RESTException e) {
            assertEquals(e.getStatusCode(), 403);
        }
    }

    /**
     * Tests failure with credential issuance request for nonexistent credential
     * specification.
     * 
     * @fiware-unit-test-feature 
     *                           FIWARE.Feature.Security.Privacy.Issuance.SimpleIssuance
     * 
     * @fiware-unit-test-initial-condition Issuer set up, with user name
     *                                     "CaröléKing" and password "Jazzman"
     *                                     stored in the authentication
     *                                     provider, <em>no</em> credential
     *                                     specification with URN
     *                                     "urn:fiware:cred" in the database.
     * 
     * @fiware-unit-test-test This test tests failure when a user issues a
     *                        credential issuance request with valid credentials
     *                        but for a nonexistent credential specification.
     * 
     * @fiware-unit-test-expected-outcome HTTP 404.
     */
    @Test
    public void testIssuanceRequestInvalid_NoCred() throws Exception {

        final AuthenticationRequest authReq = new AuthenticationRequest();
        final AuthenticationInformation authInfo = new AuthInfoSimple(
                "CaroleKing", "Jazzman");
        authReq.authInfo = authInfo;
        final IssuanceRequest isReq = new IssuanceRequest();
        isReq.authRequest = authReq;
        isReq.credentialSpecificationUid = "urn:fiware:cred";
        try {
            RESTHelper.postRequest(
                    issuanceServiceURLUnprot + "issuanceRequest",
                    RESTHelper.toXML(IssuanceRequest.class, isReq));
            throw new RuntimeException("Expected exception!");
        } catch (final RESTException e) {
            assertEquals(e.getStatusCode(), 404);
        }
    }

    public void assertOk(final Response r) {
        assertEquals(r.getStatus(), 200);
    }
}