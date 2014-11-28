import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.user.UserService;

import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.TestConstants;


public class TestService extends JerseyTest {

    public TestService() throws Exception {
        super("ch.zhaw.ficore.p2abc.services");
        initJNDI();
    }

    UserService userService;

    private static String getBaseURI() {
        return "http://localhost:" + TestConstants.JERSEY_HTTP_PORT + "/";
    }
    
    File storageFile;
    String dbName = "URIBytesStorage";
    
    public void initJNDI() throws Exception {
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
        ic.bind("java:/comp/env/jdbc/" + dbName, ds);
        ic.bind("java:/comp/env/cfg/useDbLocking", new Boolean(true));
    }
    

    @Test
    public void testUserServiceStatus() throws Exception {        
        RESTHelper.getRequest(getBaseURI() + "user/status");
        
    }
    
    public void assertOk(Response r) {
        assertEquals(r.getStatus(), 200);
    }
}