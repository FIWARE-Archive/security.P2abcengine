import java.io.File;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.mortbay.jetty.Server;
import org.sqlite.SQLiteDataSource;

import ch.zhaw.ficore.p2abc.configuration.ConnectionParameters;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.user.UserService;


public class TestService {

    UserService userService;

    private static String getBaseURI() {
        return "http://localhost:8989/zhaw-p2abc-webservices/";
    }
    
    File storageFile;
    String dbName = "URIBytesStorage";
    
    public void launchServer() throws Exception {
     // Create initial context
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.naming.java.javaURLContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
        InitialContext ic = new InitialContext();

        ic.createSubcontext("java:");
        ic.createSubcontext("java:/comp");
        ic.createSubcontext("java:/comp/env");
        ic.createSubcontext("java:/comp/env/jdbc");
        ic.createSubcontext("java:/comp/env/cfg");
        ic.createSubcontext("java:/comp/env/cfg/Source");
        ic.createSubcontext("java:/comp/env/cfg/ConnectionParameters");

        SQLiteDataSource ds = new SQLiteDataSource();
        
        storageFile = File.createTempFile("test", "sql");
        
        ds.setUrl("jdbc:sqlite:" + storageFile.getPath());
        ic.bind("java:/comp/env/jdbc/" + dbName, ds);
        ic.bind("java:/comp/env/cfg/useDbLocking", new Boolean(true));
        
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
        
        userService = new UserService();
    }

    @Before
    public void startServer() throws Exception {
        System.out.println("Starting server...");

        launchServer();
    }

    @After
    public void stopServer() throws Exception {
        storageFile.delete();
    }

    @Test
    public void testUserServiceStatus() throws Exception {
        Response r = userService.status();
        assertOk(r);
    }
    
    public void assertOk(Response r) {
        assertEquals(r.getStatus(), 200);
    }
}