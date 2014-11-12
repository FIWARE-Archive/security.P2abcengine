package ch.zhaw.ficore.p2abc.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.logging.log4j.LogManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

/** Tests basic functions of SQLite URI storage.
 * 
 * Be careful when extending this class with more tests: A new SQLite
 * database file is created for <em>every</em> test.
 * 
 * @author Stephan Neuhaus &lt;stephan.neuhaus@zhaw.ch&gt;
 * @version 1.0
 */
public class TestSqliteURIBytesStorage {
    private static final int MAX_I = 10;

    private static final int MAX_J = 10;

    private static final String table = "TestTable";

    private static SqliteURIBytesStorage storage;
    private static String dbName = "URIBytesStorage";
    private static File storageFile;
    private static URI myUri;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // rcarver - setup the jndi context and the datasource
        storageFile = new File(dbName);
        myUri = new URI("http://www.zhaw.ch");

        try {
            // Create initial context
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.naming.java.javaURLContextFactory");
            System.setProperty(Context.URL_PKG_PREFIXES, 
                "org.apache.naming");            
            InitialContext ic = new InitialContext();

            ic.createSubcontext("java:");
            ic.createSubcontext("java:/comp");
            ic.createSubcontext("java:/comp/env");
            ic.createSubcontext("java:/comp/env/jdbc");
           
            SQLiteDataSource ds = new SQLiteDataSource();
            ds.setUrl("jdbc:sqlite:" + storageFile.getPath());
            ic.bind("java:/comp/env/jdbc/" + dbName, ds);
        } catch (NamingException ex) {
            LogManager.getLogger().catching(ex);
        }
        
        storage = new SqliteURIBytesStorage(dbName, table);
    }

    @AfterClass
    public static void tearDownClass() {
        storageFile.delete();
    }

    @Before
    public void setUp() throws SQLException {
        storage.deleteAll();
    }
    
    @Test(expected=UnsafeTableNameException.class)
    public void testInvalidTableName() throws Exception {
        String tableName = "users; DROP TABLE customers";
        SqliteURIBytesStorage invalidStorage = new SqliteURIBytesStorage("hi", tableName);
        invalidStorage.get(myUri);
    }
    
    @Test
    public void testLargeBlob() throws Exception {
        byte[] data = new byte[1024*1024*16]; //==16MB
        for(int i= 0; i < data.length; i++)
            data[i] = (byte)(i % 256);
        storage.put("blob", data);
        byte[] ret = storage.get("blob");
        assertTrue(Arrays.equals(ret,data));
    }
    
    @Test
    public void testMultipleConnections() throws Exception {
        /**
         * Some testing with multiple connections to the same database
         * with multiple threads involved. 
         */
        
        SqliteURIBytesStorage storage2 = new SqliteURIBytesStorage(storageFile.getPath(), table);
        storage.put("zhaw.ch", "winterthur".getBytes());
        assertTrue(Arrays.equals(storage2.get("zhaw.ch"), "winterthur".getBytes()));
        List<Thread> threads = new ArrayList<Thread>();
        for(int i = 0; i < MAX_I; i++) {
            for(int j = 0; j < MAX_J; j++) {
                Thread thrd1 = new Thread() {
                    @Override
                    public void run() {
                        SqliteURIBytesStorage myStorage;
                        try {
                            myStorage = new SqliteURIBytesStorage(storageFile.getPath(), table);
                            myStorage.put("zhaw.ch", "123".getBytes());
                        } catch (ClassNotFoundException | SQLException
                                | UnsafeTableNameException | NamingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                };
                final int v = j;
                Thread thrd2 = new Thread() {
                    @Override
                    public void run() {
                        try {
                            SqliteURIBytesStorage myStorage = new SqliteURIBytesStorage(storageFile.getPath(), table);
                            myStorage.put("zhaw.ch/"+v, "234".getBytes());
                        } catch (ClassNotFoundException | SQLException
                                | UnsafeTableNameException | NamingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                };
                thrd1.start();
                thrd2.start();
                threads.add(thrd1);
                threads.add(thrd2);
            }
            //assertTrue(Arrays.equals(storage.get("zhaw.ch"), "123".getBytes()));
        }
        for(Thread thrd : threads) {
            thrd.join();
        }
        for(int j = 0; j < MAX_J; j++)
            assertTrue(Arrays.equals(storage.get("zhaw.ch/"+j), "234".getBytes()));
    }
    
    @Test
    public void testPutNew() throws Exception {
        assertTrue(storage.putNew("foobar", "barfoo".getBytes()));
        assertFalse(storage.putNew("foobar", "barfoo".getBytes()));
    }
    
    @Test
    public void testURIString() throws Exception {
        String uri1 = "http://zhaw.ch/foo bar";
        String uri2 = "http://zhaw.ch/foo%20bar";
        
        storage.delete(uri1);
        storage.delete(uri2);
        
        storage.put(uri1, new byte[]{1,2,3});
        storage.put(uri2, new byte[]{5,4,3});
        
        byte[] ret = storage.get(new URI("http://zhaw.ch/foo%20bar"));
        assertTrue(Arrays.equals(ret, new byte[]{5,4,3}));
        ret = storage.get("http://zhaw.ch/foo bar");
        assertTrue(Arrays.equals(ret, new byte[]{1,2,3}));
        List<URI> uris = storage.keys();
        assertTrue(uris.size() == 1);
        List<String> keys = storage.keysAsStrings();
        assertTrue(keys.size() == 2);
    }
    
    @Test
    public void testValuesAndKeys() throws Exception {
        String urn1 = "urn:foobar";
        String urn2 = "urn:barfoo";
        URI uri1 = new URI(urn1);
        URI uri2 = new URI(urn2);
        
        storage.put(uri1, new byte[]{1,9,9});
        storage.put(uri2, new byte[]{0,0,0});
        
        List<URI> keys = storage.keys();
        assertTrue(keys.size() == 2);
        
        assertTrue(keys.contains(uri1));
        assertTrue(keys.contains(uri2));
        
        assertTrue(Arrays.equals(new byte[]{1,9,9}, storage.get(uri1)));
        assertTrue(Arrays.equals(new byte[]{0,0,0}, storage.get(uri2)));
        
        List<byte[]> values = storage.values();
        assertTrue(values.size() == 2);
        
        assertTrue(Arrays.equals(new byte[]{1,9,9}, values.get(0)));
        assertTrue(Arrays.equals(new byte[]{0,0,0}, values.get(1)));
    }

    @Test
    public void testEmptyKeyOnEmptyStorage() throws Exception {
        assertFalse(storage.containsKey(new URI("")));
    }

    @Test
    public void testUrnKeyOnEmptyStorage() throws Exception {
        assertFalse(storage.containsKey(new URI("urn:abc4trust:1.0:encoding:integer:signed")));
    }

    @Test
    public void testHttpKeyOnEmptyStorage() throws Exception {
        assertFalse(storage.containsKey(myUri));
    }

    @Test
    public void testContainsSimpleKey() throws Exception {
        byte[] stored = new byte[] { 0x01 };

        storage.putNew(myUri, stored);
        assertTrue(storage.containsKey(myUri));
    }

    @Test
    public void testSimpleKeyValue() throws Exception {
        byte[] stored = new byte[] { 0x01 };

        storage.putNew(myUri, stored);
        byte[] value = storage.get(myUri);
        assertTrue(value != null);
        assertTrue(value.length == stored.length);
        assertTrue(Arrays.equals(value, stored));
    }

    @Test
    public void testSomeNonexistentKey() throws Exception {
        storage.putNew(myUri, new byte[] { 0x01 });
        assertFalse(storage.containsKey(new URI("http://www.apple.com")));
    }

    @Test
    public void testStringStorage() throws Exception {
        String testString = "Hello, world";

        storage.put(myUri, testString.getBytes());
        String retrievedString = new String(storage.get(myUri));

        assertTrue(retrievedString != null);
        assertTrue(retrievedString.equals(testString));
    }
}
