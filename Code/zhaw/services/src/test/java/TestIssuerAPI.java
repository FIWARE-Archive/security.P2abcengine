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

import org.junit.After;
import org.junit.Before;
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

    File storageFile;
    String dbName = "URIBytesStorage";
    ObjectFactory of = new ObjectFactory();

    @Before
    public void initJNDI() throws Exception {        
        System.out.println("init [TestIssuerAPI]");
        this.setUp();
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
        ic.bind("java:/comp/env/cfg/issuanceServiceURL", "");
        ic.bind("java:/comp/env/cfg/userServiceURL", "");
        ic.bind("java:/comp/env/cfg/verificationServiceURL", "");
        ic.bind("java:/comp/env/cfg/verifierIdentity", "unknown");

        SQLiteDataSource ds = new SQLiteDataSource();

        storageFile = File.createTempFile("test", "sql");

        ds.setUrl("jdbc:sqlite:" + storageFile.getPath());
        System.out.println(ds.getUrl());
        ic.rebind("java:/comp/env/jdbc/" + dbName, ds);
        ic.bind("java:/comp/env/cfg/useDbLocking", new Boolean(true));
        
        ic.close();

        RESTHelper.postRequest(issuanceServiceURL + "reset"); //make sure the service is *clean* before each test.

    }

    @After
    public void cleanup() throws Exception {
    }
    
    
    /** Tests getSettings.
     * 
     * <b>Test</b>: getSettings
     * 
     * <b>Description</b>: This method tests the getSettings method of the
     * issuer. It issues the request and checks that a HTTP 200 answer is
     * received. 
     * 
     * @throws Exception
     */
    @Test
    public void testGetSettings() throws Exception {
        /*
         * There isn't much we can test here yet I think because settings involves
         * system parameters and involves A LOT of random stuff. Also the correct
         * functioning of getSettings should also be at least partly covered by the flow
         * tests.
         */
        RESTHelper.getRequest(issuanceServiceURLUnprot +"getSettings");
    }

    @Test
    public void testQueryRules() throws Exception {
        
        
        QueryRule qr = new QueryRule();
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

        for (String s : new String[] { "urn:foo1", "urn:foo2" }) {
            assertEquals(qrc.uris.contains(s), true);
        }

        Map<String, String> m = new HashMap<String, String>();
        m.put("string1", "urn:foo1");
        m.put("string2", "urn:foo2");

        for (int i = 0; i < qrc.queryRules.size(); i++) {
            QueryRule q = qrc.queryRules.get(i);
            assertEquals(qrc.uris.get(i), m.get(q.queryString));
        }
        
        RESTHelper.deleteRequest(issuanceServiceURL+"queryRule/delete/urn:foo1");
        RESTHelper.deleteRequest(issuanceServiceURL+"queryRule/delete/urn:foo2");
        
        qrc = (QueryRuleCollection) RESTHelper.getRequest(
                issuanceServiceURL + "queryRule/list",
                QueryRuleCollection.class);
        assertEquals(qrc.queryRules.size(), qrc.uris.size());
        assertEquals(0, qrc.queryRules.size());
    }

    @Test
    public void testGenCredSpec() throws Exception {
        AttributeInfoCollection aic = (AttributeInfoCollection) RESTHelper
                .getRequest(
                        issuanceServiceURL + "attributeInfoCollection/test",
                        AttributeInfoCollection.class);

        assertEquals(aic.name, "test");
        assertTrue(aic.attributes.size() == 1);
        assertEquals(aic.attributes.get(0).name, "someAttribute");
        
        CredentialSpecification credSpec = (CredentialSpecification) RESTHelper.postRequest(
                issuanceServiceURL + "credentialSpecification/generate",
                RESTHelper.toXML(AttributeInfoCollection.class, aic),
                CredentialSpecification.class);
        
        assertEquals("urn:fiware:privacy:test",credSpec.getSpecificationUID().toString());
        assertEquals(1,credSpec.getAttributeDescriptions().getAttributeDescription().size());
        assertEquals("someAttribute", 
                credSpec.getAttributeDescriptions().getAttributeDescription().get(0).getType().toString());
        
        assertEquals("xs:integer",aic.attributes.get(0).mapping);
        assertEquals("urn:abc4trust:1.0:encoding:integer:signed", aic.attributes.get(0).encoding);
        
        AttributeDescription ad = credSpec.getAttributeDescriptions().getAttributeDescription().get(0);
        assertEquals("xs:integer",ad.getDataType().toString());
        assertEquals("urn:abc4trust:1.0:encoding:integer:signed", ad.getEncoding().toString());
        assertEquals("someAttribute attribute", ad.getFriendlyAttributeName().get(0).getValue());
        assertEquals("en", ad.getFriendlyAttributeName().get(0).getLang());
    }
    
    @Test
    public void testStoreGetCredSpec() throws Exception {
        CredentialSpecification orig = new CredentialSpecification();
        orig.setSpecificationUID(new URI("urn:fiware:cred"));
        AttributeDescriptions attrDescs = new AttributeDescriptions();
        List<AttributeDescription> lsAttrDesc = attrDescs.getAttributeDescription();
        
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
        
        RESTHelper.putRequest(issuanceServiceURL + "credentialSpecification/store/" 
                    + URLEncoder.encode("urn:fiware:cred", "UTF-8"), 
                RESTHelper.toXML(CredentialSpecification.class, 
                        of.createCredentialSpecification(orig)));
        
        RESTHelper.getRequest(issuanceServiceURL + "credentialSpecification/get/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"));
    }
    
    @Test
    public void testDeleteAttribute() throws Exception {
        testStoreGetCredSpec();
        
        
        MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("i", "0");
        
        RESTHelper.deleteRequest(issuanceServiceURL + "credentialSpecification/deleteAttribute/"
                + URLEncoder.encode("urn:fiware:cred","UTF-8"), params);
        
        CredentialSpecification credSpec = (CredentialSpecification) RESTHelper.getRequest(issuanceServiceURL + "credentialSpecification/get/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"), CredentialSpecification.class);
        
        assertEquals(credSpec.getAttributeDescriptions().getAttributeDescription().size(), 0);
    }
    
    @Test
    public void testDeleteAttributeInvalid() throws Exception {
        testStoreGetCredSpec();
        
        try {
            MultivaluedMapImpl params = new MultivaluedMapImpl();
            params.add("i", "2");
            
            RESTHelper.deleteRequest(issuanceServiceURL + "credentialSpecification/deleteAttribute/"
                    + URLEncoder.encode("urn:fiware:cred","UTF-8"), params);
            throw new RuntimeException("Expected exception!");
        }
        catch(RESTException e) {
            assertEquals(e.getStatusCode(), 404);
        }
    }
    
    @Test
    public void testGenerateIssuerParams() throws Exception {
        testStoreGetCredSpec();
        
        RESTHelper.postRequest(issuanceServiceURL + "issuerParameters/generate/"
                + URLEncoder.encode("urn:fiware:cred","UTF-8"));
    }
    
    @Test
    public void testGenerateIssuerParamsInvalid() throws Exception {
        testStoreGetCredSpec();
        
        try {
            RESTHelper.postRequest(issuanceServiceURL + "issuerParameters/generate/"
                    + URLEncoder.encode("urn:fiware:crad","UTF-8"));
            throw new RuntimeException("Expected exception!");
        }
        catch(RESTException e) {
            assertEquals(e.getStatusCode(), 500);
        }
    }
    
    @Test
    public void testDeleteIssuerParams() throws Exception {
        testGenerateIssuerParams();
        
        RESTHelper.deleteRequest(issuanceServiceURL + "issuerParameters/delete/"
                + URLEncoder.encode("urn:fiware:cred","UTF-8"));
    }
    
    @Test
    public void testStoreIssuancePolicy() throws Exception {
        IssuancePolicy ip = new IssuancePolicy();
        
        RESTHelper.putRequest(issuanceServiceURL + "issuancePolicy/store/ip",
                RESTHelper.toXML(IssuancePolicy.class, of.createIssuancePolicy(ip)));
    }
    
    @Test
    public void testGetIssuancePolicy() throws Exception {
        testStoreIssuancePolicy();
        
        RESTHelper.getRequest(issuanceServiceURL + "issuancePolicy/get/ip");
    }
    
    @Test
    public void testAddFriendlyDescription() throws Exception {
        testStoreGetCredSpec();
        
        MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("language", "ch");
        params.add("value", "chuchichäschtli");
        params.add("i", "0");
        
        RESTHelper.putRequest(issuanceServiceURL + "credentialSpecification/addFriendlyDescriptionAttribute/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"),
                params);
        
        CredentialSpecification credSpec = (CredentialSpecification) RESTHelper.getRequest(issuanceServiceURL + "credentialSpecification/get/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"), CredentialSpecification.class);
        
        List<FriendlyDescription> fds = credSpec.getAttributeDescriptions().
                getAttributeDescription().get(0).getFriendlyAttributeName();
        assertEquals(fds.get(1).getLang(),"ch");
        assertEquals(fds.get(1).getValue(),"chuchichäschtli");
    }
    
    @Test
    public void testDeleteFriendlyDescription() throws Exception {
        testAddFriendlyDescription();
        
        MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("language", "ch");
        params.add("i", "0");
        RESTHelper.deleteRequest(issuanceServiceURL + "credentialSpecification/deleteFriendlyDescriptionAttribute/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"),
                params);
        
        CredentialSpecification credSpec = (CredentialSpecification) RESTHelper.getRequest(issuanceServiceURL + "credentialSpecification/get/"
                + URLEncoder.encode("urn:fiware:cred", "UTF-8"), CredentialSpecification.class);
        
        List<FriendlyDescription> fds = credSpec.getAttributeDescriptions().
                getAttributeDescription().get(0).getFriendlyAttributeName();
        
        assertEquals(fds.size(),1);
    }
    
    @Test
    public void testDeleteFriendlyDescriptionInvalid() throws Exception {
        testAddFriendlyDescription();
        
        try {
            MultivaluedMapImpl params = new MultivaluedMapImpl();
            params.add("language", "de");
            params.add("i", "0");
            RESTHelper.deleteRequest(issuanceServiceURL + "credentialSpecification/deleteFriendlyDescriptionAttribute/"
                    + URLEncoder.encode("urn:fiware:cred", "UTF-8"),
                    params);
            throw new RuntimeException("Expected exception!");
        
        }
        catch(RESTException e) {
            assertEquals(e.getStatusCode(),404);
        }
    }
    
    @Test
    public void testAddFriendlyDescriptionInvalid() throws Exception {
        testStoreGetCredSpec();
        
        MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("language", "ch");
        params.add("value", "chuchichäschtli");
        params.add("i", "5");
        
        try {
        
            RESTHelper.putRequest(issuanceServiceURL + "credentialSpecification/addFriendlyDescriptionAttribute/"
                    + URLEncoder.encode("urn:fiware:cred", "UTF-8"),
                    params);
            
            throw new RuntimeException("Expected exception!");
        }
        catch(RESTException e) {
            assertEquals(e.getStatusCode(), 404);
        }
    }
    
    @Test
    public void testStoreSpecInvalid() throws Exception {
        CredentialSpecification orig = new CredentialSpecification();
        orig.setSpecificationUID(new URI("urn:fiware:cred"));
        AttributeDescriptions attrDescs = new AttributeDescriptions();
        List<AttributeDescription> lsAttrDesc = attrDescs.getAttributeDescription();
        
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
        
        try {
            RESTHelper.putRequest(issuanceServiceURL + "credentialSpecification/store/" 
                        + URLEncoder.encode("urn:fiware:creed", "UTF-8"), 
                    RESTHelper.toXML(CredentialSpecification.class, 
                            of.createCredentialSpecification(orig)));
            throw new RuntimeException("Expected exception!");
        }
        catch(RESTException e) {
            assertEquals(e.getStatusCode(), 409);
        }
    }
    
    @Test
    public void testDeleteSpecInvalid() throws Exception {
        try {
            RESTHelper.deleteRequest(issuanceServiceURL + "credentialSpecification/delete/" +
                    URLEncoder.encode("urn:non-existing","UTF-8"));
            throw new RuntimeException("Expected exception!");
        }
        catch(RESTException e) {
            assertEquals(e.getStatusCode(), 404);
        }
    }
    
    @Test
    public void testDeleteSpec() throws Exception {
        /* First we need to actually store one. So we call a test... */
        testStoreGetCredSpec();
        RESTHelper.deleteRequest(issuanceServiceURL + "credentialSpecification/delete/" +
                URLEncoder.encode("urn:fiware:cred","UTF-8"));
    }
    
    @Test
    public void testTestAuthentication() throws Exception {
        AuthenticationRequest authReq = new AuthenticationRequest();
        AuthenticationInformation authInfo = new AuthInfoSimple("CaroleKing","Jazzman");
        authReq.authInfo = authInfo;
        RESTHelper.postRequest(issuanceServiceURLUnprot + "testAuthentication",
                RESTHelper.toXML(AuthenticationRequest.class, authReq));
    }
    
    @Test
    public void testTestAuthenticationInvalid() throws Exception {
        AuthenticationRequest authReq = new AuthenticationRequest();
        AuthenticationInformation authInfo = new AuthInfoSimple("CaröléKing","Jazzman");
        authReq.authInfo = authInfo;
        try {
            RESTHelper.postRequest(issuanceServiceURLUnprot + "testAuthentication",
                    RESTHelper.toXML(AuthenticationRequest.class, authReq));
            throw new RuntimeException("Expected exception!");
        }
        catch(RESTException e) {
            assertEquals(e.getStatusCode(), 403);
        }
    }
    
    @Test
    public void testIssuanceRequestInvalid() throws Exception {
        
        
        AuthenticationRequest authReq = new AuthenticationRequest();
        AuthenticationInformation authInfo = new AuthInfoSimple("CaröléKing","Jazzman");
        authReq.authInfo = authInfo;
        IssuanceRequest isReq = new IssuanceRequest();
        isReq.authRequest = authReq;
        isReq.credentialSpecificationUid = "urn:fiware:cred";
        try {
            RESTHelper.postRequest(issuanceServiceURLUnprot + "issuanceRequest",
                    RESTHelper.toXML(IssuanceRequest.class, isReq));
            throw new RuntimeException("Expected exception!");
        }
        catch(RESTException e) {
            assertEquals(e.getStatusCode(), 403);
        }
    }
    
    @Test
    public void testIssuanceRequestInvalid_NoCred() throws Exception {
        
        
        AuthenticationRequest authReq = new AuthenticationRequest();
        AuthenticationInformation authInfo = new AuthInfoSimple("CaroleKing","Jazzman");
        authReq.authInfo = authInfo;
        IssuanceRequest isReq = new IssuanceRequest();
        isReq.authRequest = authReq;
        isReq.credentialSpecificationUid = "urn:fiware:cred";
        try {
            RESTHelper.postRequest(issuanceServiceURLUnprot + "issuanceRequest",
                    RESTHelper.toXML(IssuanceRequest.class, isReq));
            throw new RuntimeException("Expected exception!");
        }
        catch(RESTException e) {
            assertEquals(e.getStatusCode(), 404);
        }
    }

    public void assertOk(Response r) {
        assertEquals(r.getStatus(), 200);
    }
}