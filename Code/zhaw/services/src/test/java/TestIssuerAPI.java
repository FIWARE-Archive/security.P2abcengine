import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.sqlite.SQLiteDataSource;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.user.UserService;
import ch.zhaw.ficore.p2abc.xml.AttributeInfoCollection;
import ch.zhaw.ficore.p2abc.xml.QueryRule;
import ch.zhaw.ficore.p2abc.xml.QueryRuleCollection;

import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.TestConstants;


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
    
    @Before
    public void initJNDI() throws Exception {
        System.out.println("init [TestIssuerAPI]");
     // Create initial context
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.naming.java.javaURLContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
        InitialContext ic = new InitialContext();

        try {
            ic.destroySubcontext("java:");
        }
        catch(Exception e) {
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
        ic.bind("java:/comp/env/cfg/restAuthPassword","");
        ic.bind("java:/comp/env/cfg/restAuthUser", "");
        ic.bind("java:/comp/env/cfg/issuanceServiceURL","");
        ic.bind("java:/comp/env/cfg/userServiceURL","");
        
        SQLiteDataSource ds = new SQLiteDataSource();
        
        storageFile = File.createTempFile("test", "sql");
        
        ds.setUrl("jdbc:sqlite:" + storageFile.getPath());
        System.out.println(ds.getUrl());
        ic.rebind("java:/comp/env/jdbc/" + dbName, ds);
        ic.bind("java:/comp/env/cfg/useDbLocking", new Boolean(true));
    }
    
    @After
    public void cleanup() throws Exception {
        System.out.println("cleanup [TestIssuerAPI] " + storageFile.getAbsolutePath());
        InitialContext ic = new InitialContext();
        ic.destroySubcontext("java:");
        storageFile.delete();
        this.tearDown();
    }
    

    @Ignore
    public void testQueryRules() throws Exception {        
        QueryRule qr = new QueryRule();
        qr.queryString = "string1";
        
        RESTHelper.putRequest(issuanceServiceURL+"queryRule/store/urn%3Afoo1", 
                RESTHelper.toXML(QueryRule.class, qr));
        
        qr.queryString = "string2";
        

        RESTHelper.putRequest(issuanceServiceURL+"queryRule/store/urn%3Afoo2", 
                RESTHelper.toXML(QueryRule.class, qr));
        
        QueryRule qr_ = (QueryRule) RESTHelper.getRequest(issuanceServiceURL+"queryRule/get/urn%3Afoo1", QueryRule.class);
        assertEquals(qr_.queryString, "string1");
        
        qr_ = (QueryRule) RESTHelper.getRequest(issuanceServiceURL+"queryRule/get/urn%3Afoo2", QueryRule.class);
        assertEquals(qr_.queryString, "string2");
        
        QueryRuleCollection qrc = (QueryRuleCollection) RESTHelper.getRequest(issuanceServiceURL+"queryRule/list", QueryRuleCollection.class);
        assertEquals(qrc.queryRules.size(), qrc.uris.size());
        assertEquals(2, qrc.queryRules.size());
        
        for(String s : new String[]{"urn:foo1","urn:foo2"}) {
            assertEquals(qrc.uris.contains(s), true);
        }
      
        
        Map<String, String> m = new HashMap<String,String>();
        m.put("string1","urn:foo1");
        m.put("string2","urn:foo2");
        
        for(int i = 0; i < qrc.queryRules.size(); i++) {
            QueryRule q = qrc.queryRules.get(i);
            assertEquals(qrc.uris.get(i), m.get(q.queryString));
        }
    }
    
    @Ignore
    public void testGenCredSpecAndGenIssuerParameters() throws Exception {
        AttributeInfoCollection aic = (AttributeInfoCollection) RESTHelper.getRequest(issuanceServiceURL + "attributeInfoCollection/test", 
                AttributeInfoCollection.class);
        
        assertEquals(aic.name, "test");
        assertTrue(aic.attributes.size() == 1);
        
    }
    
    public void assertOk(Response r) {
        assertEquals(r.getStatus(), 200);
    }
}