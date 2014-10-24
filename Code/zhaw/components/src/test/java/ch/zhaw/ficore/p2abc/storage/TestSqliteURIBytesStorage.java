package ch.zhaw.ficore.p2abc.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSqliteURIBytesStorage {
  private static final String table = "TestTable";
  private SqliteURIBytesStorage storage;
  private File storageFile;
  
  @Before
  public void setUp() throws Exception {
    storageFile = File.createTempFile("test", "sql", new File("."));
    storage = new SqliteURIBytesStorage(storageFile.getPath(), table);
  }

  @After
  public void tearDown() throws Exception {
    storageFile.delete();
  }

  @Test
  public void testSomeKeyOnEmptyStorage() throws SQLException, URISyntaxException {
    assertFalse(storage.containsKey(new URI("http://www.google.com/")));
  }
  
  @Test
  public void testContainsSimpleKey() throws SQLException, URISyntaxException {
    byte[] stored = new byte[] { 0x01 };
    URI myUri = new URI("http://www.google.com");
    
    storage.putNew(myUri, stored);
    assertTrue(storage.containsKey(myUri));
  }
  
  @Test
  public void testSimpleKeyValue() throws SQLException, URISyntaxException {
    byte[] stored = new byte[] { 0x01 };
    URI myUri = new URI("http://www.google.com");
    
    storage.putNew(myUri, stored);
    byte[] value = storage.get(myUri);
    assertTrue(value != null);
    assertTrue(value.length == stored.length);
    assertTrue(Arrays.equals(value, stored));
  }
  
  @Test
  public void testSomeNonexistentKey() throws SQLException, URISyntaxException {
    storage.putNew(new URI("http://www.google.com"), new byte[] { 0x01 });
    assertFalse(storage.containsKey(new URI("http://www.apple.com")));
  }
  
  @Test
  public void testStringStorage() throws SQLException, URISyntaxException {
    URI myUri = new URI("http://www.google.com");
    String testString = "Hello, world";
    
    storage.put(myUri, testString.getBytes());
    String retrievedString = new String(storage.get(myUri));
    
    assertTrue(retrievedString != null);
    assertTrue(retrievedString.equals(testString));
  }
}
