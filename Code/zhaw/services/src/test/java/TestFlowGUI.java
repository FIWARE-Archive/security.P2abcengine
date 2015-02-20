import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.TestConstants;

public class TestFlowGUI extends JerseyTest {

    private String userServiceURL = "user/";
    private String userGUI = "user-gui/";
    private String verificationServiceURL = "verification/protected/";
    private String verificationGUI = "verification-gui/";
    private String verificationServiceURLUnprot = "verification/";
    private String issuanceGUI = "issuance-gui/";
    private String issuanceServiceURL = "issuance/protected/";
    private String issuanceServiceURLUnprot = "issuance/";
    
    public TestFlowGUI() throws Exception {
        super("ch.zhaw.ficore.p2abc");
        userServiceURL = getBaseURI() + userServiceURL;
        verificationServiceURL = getBaseURI() + verificationServiceURL;
        verificationServiceURLUnprot = getBaseURI()
                + verificationServiceURLUnprot;
        issuanceServiceURL = getBaseURI() + issuanceServiceURL;
        issuanceServiceURLUnprot = getBaseURI() + issuanceServiceURLUnprot;
        userGUI = getBaseURI() + userGUI;
        verificationGUI = getBaseURI() + verificationGUI;
        issuanceGUI = getBaseURI() + issuanceGUI;
    }

    private static String getBaseURI() {
        //return "http://srv-lab-t-425.zhaw.ch:8080/zhaw-p2abc-webservices/";
        return "http://localhost:" + TestConstants.JERSEY_HTTP_PORT + "/";
    }

    File storageFile;
    String dbName = "URIBytesStorage";

    @Before
    public void initJNDI() throws Exception {
        System.out.println("init [TestFlowGUI]");
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
        ic.bind("java:/comp/env/cfg/restAuthUser", "flow");
        ic.bind("java:/comp/env/cfg/issuanceServiceURL", getBaseURI() + "issuance/");
        ic.bind("java:/comp/env/cfg/userServiceURL", getBaseURI() + "user/");
        ic.bind("java:/comp/env/cfg/verificationServiceURL", getBaseURI() + "verification/");
        ic.bind("java:/comp/env/cfg/verifierIdentity", "unknown");

        SQLiteDataSource ds = new SQLiteDataSource();

        storageFile = File.createTempFile("test", "sql");

        ds.setUrl("jdbc:sqlite:" + storageFile.getPath());
        System.out.println(ds.getUrl());
        ic.rebind("java:/comp/env/jdbc/" + dbName, ds);
        ic.bind("java:/comp/env/cfg/useDbLocking", new Boolean(true));
        
        ic.close();
        
        RESTHelper.postRequest(issuanceServiceURL + "reset");
        RESTHelper.postRequest(verificationServiceURL + "reset");
        RESTHelper.postRequest(userServiceURL + "reset");
        
        //System.exit(1);
    }

    @After
    public void cleanup() throws Exception {
    }
    
    @Test
    public void flow() throws InterruptedException, ClientHandlerException, UniformInterfaceException, JAXBException, UnsupportedEncodingException, NamingException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        
        params.add("n", "test");
        
        /* Obtain a credential specification through the issuance-gui and call the issuance-service
         * and check if it was created
         */
        RESTHelper.postRequest(issuanceGUI + "protected/obtainCredentialSpecification2", params);
        RESTHelper.getRequest(issuanceServiceURL + "credentialSpecification/get/" + URLEncoder.encode("urn:fiware:privacy:test", "utf-8"));
        
        /*
         * Generate issuer parameters for the credential specification
         */
        params = new MultivaluedMapImpl();
        params.add("cs","urn:fiware:privacy:test");
        
        RESTHelper.postRequest(issuanceGUI + "protected/generateIssuerParameters", params);
        
        /*
         * Add a dummy query rule
         */
        params = new MultivaluedMapImpl();
        params.add("cs", "urn:fiware:privacy:test");
        params.add("qr", "demo");
        
        RESTHelper.postRequest(issuanceGUI + "protected/addQueryRule", params);
        
        /*
         * Load settings from issuer onto the other services
         */
        String url = issuanceServiceURLUnprot + "getSettings";
        params = new MultivaluedMapImpl();
        params.add("url", url);
        RESTHelper.postRequest(userGUI + "loadSettings2", params);
        RESTHelper.postRequest(verificationGUI + "protected/loadSettings2", params);
        
        /*
         * Obtain a credential from the issuer
         */
        params = new MultivaluedMapImpl();
        params.add("un", "CaroleKing");
        params.add("pw", "Jazzman");
        params.add("is", issuanceServiceURLUnprot.substring(0, issuanceServiceURLUnprot.lastIndexOf("/")));
        params.add("cs", "urn:fiware:privacy:test");
        String result = (String)RESTHelper.postRequest(userGUI + "obtainCredential2", params);
        System.out.println("CTX: " + getContextString(result));
        
        params = new MultivaluedMapImpl();
        params.add("policyId","0");
        params.add("candidateId","0");
        params.add("pseudonymId","0");
        params.add("uic", getContextString(result));
        RESTHelper.postRequest(userGUI + "obtainCredential3", params);
        
        /*
         * Create a resource at the verifier
         */
        params = new MultivaluedMapImpl();
        params.add("rs", "resource");
        params.add("ru", "http://google.com");
        RESTHelper.postRequest(verificationGUI + "protected/createResource", params);
        
        /*
         * Add a policy alternative
         */
        params = new MultivaluedMapImpl();
        params.add("resource", "resource");
        params.add("puid", "urn:policy");
        RESTHelper.postRequest(verificationGUI + "protected/addPolicyAlt", params);
        
        /*
         * Add a new alias
         */
        params = new MultivaluedMapImpl();
        params.add("puid", "urn:policy");
        params.add("al", "test");
        params.add("resource", "resource");
        RESTHelper.postRequest(verificationGUI + "protected/addAlias", params);
        
        //while(true)
        //    Thread.sleep(1000);
    }
    
    public String getContextString(String input) {
        Pattern pattern = Pattern.compile("ui-context-(.*?)\">");
        Matcher m = pattern.matcher(input);
        m.find();
        return "ui-context-"+m.group(1);
    }
}