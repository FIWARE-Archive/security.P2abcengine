import static org.junit.Assert.assertEquals;

import java.io.File;

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

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.TestConstants;

import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicyAlternatives;

public class TestVerifierAPI extends JerseyTest {

    private String verificationServiceURL = "verification/protected/";
    private String verificationServiceURLUnprot = "verification/";

    public TestVerifierAPI() throws Exception {
        super("ch.zhaw.ficore.p2abc.services");
        verificationServiceURL = getBaseURI() + verificationServiceURL;
        verificationServiceURLUnprot = getBaseURI() + verificationServiceURLUnprot;
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
        RESTHelper.postRequest(verificationServiceURL + "reset"); //make sure the service is *clean* before each test.
    }

    @AfterClass
    public static void cleanup() throws Exception {
        storageFile.delete();
    }
    
    
  
    @Test
    public void testStatus() throws Exception {
        RESTHelper.getRequest(verificationServiceURL +"status");
    }
    
    @Test
    public void testCreateResource() throws Exception {
        MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("redirectURI", "http://localhost/foo");
        
        RESTHelper.putRequest(verificationServiceURL + "resource/create/test", params);
        
        PresentationPolicyAlternativesCollection ppac = 
                (PresentationPolicyAlternativesCollection) RESTHelper.getRequest(verificationServiceURL + "presentationPolicyAlternatives/list",
                PresentationPolicyAlternativesCollection.class);
        
        assertEquals(ppac.uris.size(), 1);
        assertEquals(ppac.uris.get(0),"test");
        assertEquals(ppac.redirectURIs.size(), 1);
        assertEquals(ppac.redirectURIs.get(0),"http://localhost/foo");
        assertEquals(ppac.presentationPolicyAlternatives.size(), 1);
    }
    
    @Test
    public void testAddPPA() throws Exception {
        testCreateResource();
        
        MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add("puid", "urn:policy");
        
        
        RESTHelper.postRequest(verificationServiceURL + "presentationPolicyAlternatives/addPolicyAlternative/test", params);
        
        PresentationPolicyAlternatives ppas = (PresentationPolicyAlternatives) RESTHelper.getRequest(
                verificationServiceURL + "presentationPolicyAlternatives/get/test",
                PresentationPolicyAlternatives.class);
        
        assertEquals(ppas.getPresentationPolicy().size(), 1);
        assertEquals(ppas.getPresentationPolicy().get(0).getPolicyUID().toString(),"urn:policy");
    }
}