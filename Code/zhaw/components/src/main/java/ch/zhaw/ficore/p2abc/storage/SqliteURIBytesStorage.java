package ch.zhaw.ficore.p2abc.storage;

import java.net.URI;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements the URIBytesStorage interface. This class
 * uses a sqlite database.
 * 
 * This class will only create one connection for the same
 * filePath to prevent concurrency issues. This is due to SQLite
 * writing to files which means SQLite relies on filesystem locks and the like.
 * SQLite has some known issues under certain circumstances with concurrent 
 * modifications: http://www.sqlite.org/howtocorrupt.html.
 * All methods synchronize through the connection object.
 * 
 * @author mroman
 */
public class SqliteURIBytesStorage implements URIBytesStorage {
  private Connection con;
	private String table;
	
	private Logger logger;
	
	private static Map<String, Connection> connections = new HashMap<String, Connection>();
	
	/**
	 * Constructor. This will open and create the database as well as the tables
	 * if neccessary. 
	 * 
	 * @param filePath Path to the database file
	 * @param table Name of the table to use
	 * @throws UnsafeTableNameException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public SqliteURIBytesStorage(String filePath, String table) throws ClassNotFoundException, SQLException, UnsafeTableNameException {
		logger = LogManager.getLogger(SqliteURIBytesStorage.class.getName());
		init(filePath, table);
	}
	
	/**
	 * Performs the "connection sharing" logic. 
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 * @throws DataFormatException 
	 * @throws UnsafeTableNameException 
	 */
	private synchronized void init(String filePath, String table) throws ClassNotFoundException, SQLException, UnsafeTableNameException {
	  logger.entry();
	  Class.forName("org.sqlite.JDBC");

	  String key = filePath;

	  if(connections.containsKey(key))
	    con = connections.get(key);
	  else {
	    con = DriverManager.getConnection("jdbc:sqlite:" + filePath);
	    connections.put(key, con);
	  }

	  synchronized(con) {
	    Statement stmt = null;
	    try {
	      checkIfSafeTableName(table);
	      stmt = con.createStatement();
	      String sql = "CREATE TABLE IF NOT EXISTS " + table +
	          "(hash          VARCHAR(40) PRIMARY KEY     NOT NULL," +
	          " uri           TEXT    NOT NULL, " + 
	          " value         BLOB    NOT NULL)";
	      stmt.executeUpdate(sql);
	      this.table = table;
	    }
	    catch(SQLException e) {
	       logger.catching(e);
	        throw logger.throwing(e);
	    }
	    catch (UnsafeTableNameException e) {
        logger.catching(e);
        throw logger.throwing(e);	      
	    }
	    finally {
	      if(stmt != null)
	        stmt.close();
	    }
	  }
    logger.exit();
	}
	
	/** Checks if a table name is safe to use in a SQL CREATE TABLE statement.
	 * 
	 * This function applies a rather drastic whitelist of characters that
	 * are allowed in a SQL CREATE TABLE statement.  This is to prevent
	 * SQL injection at the "create table" stage.  For example, <code>users</code>
	 * is a valid table name, whereas <code>a; DROP TABLE users</code> is not.
	 * 
	 * This function might reject perfectly safe names out of paranoia, but names
	 * consisting only of ASCII letters are always safe.
	 * 
	 * @param tableName the table name to check
	 * 
	 * @throws DataFormatException if the table name is not deemed to be safe
	 */
  private static void checkIfSafeTableName(String tableName) throws  UnsafeTableNameException {
    CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();

    if (!asciiEncoder.canEncode(tableName)) {
      throw new UnsafeTableNameException(tableName);
    }
    
    for (char c : tableName.toCharArray()) {
      int codePoint = c;
      
      /* At this point, codePoint is an ASCII character, hence the
       * comparisons below are safe. */
      if ((codePoint >= 'A' && codePoint <= 'Z')
          || (codePoint >= 'a' && codePoint <= 'z'))
        ; // empty
      else {
        throw new UnsafeTableNameException(tableName);
      }
    }
  }

  /**
	 * Lists all keys
	 */
	public List<URI> keys() throws SQLException {
		logger.entry();
		synchronized(con) {
			PreparedStatement pStmt = null;
			ResultSet rst = null;
			List<URI> uris = new ArrayList<URI>();
			
			try {
				pStmt = con.prepareStatement("SELECT uri FROM " + table);
				rst = pStmt.executeQuery();
				while(rst.next()) {
					try {
						uris.add(new URI(rst.getString(1)));
					}
					catch(Exception e) {
						e.printStackTrace();
						//We can't do much here. This means that somebody managed to store
						//an invalid URI in the storage. 
					}
				}
				return logger.exit(uris);
			}
			catch(Exception e) {
				logger.catching(e);
				throw logger.throwing(new RuntimeException("Storage failure!"));
			}
			finally {
				if(pStmt != null) pStmt.close();
				if(rst != null) rst.close();
			}
		}
	}
	
	/**
	 * Get an entry from the storage
	 */
	public byte[] get(URI uri) throws SQLException {
		logger.entry();
		synchronized(con) {
			PreparedStatement pStmt = null;
			ResultSet rst = null;
			
			try {
				pStmt = con.prepareStatement("SELECT value FROM " + table + " WHERE hash = ?");
				String hash = DigestUtils.sha1Hex(uri.toString());
				pStmt.setString(1, hash);
				rst = pStmt.executeQuery();
				while(rst.next()) {
					return logger.exit(rst.getBytes(1));
				}
				return logger.exit(null);
			}
			catch(Exception e) {
				logger.catching(e);
				throw logger.throwing(new RuntimeException("Storage failure!"));
			}
			finally {
				if(pStmt != null) pStmt.close();
				if(rst != null) rst.close();
			}
		}
	}
	
	/**
	 * Delete an entry from the storage
	 */
	public void delete(URI uri) throws SQLException {
		logger.entry();
		synchronized(con) {
			PreparedStatement pStmt = null;
			
			try {
				pStmt = con.prepareStatement("DELETE FROM " + table + " WHERE hash = ?");
				String hash = DigestUtils.sha1Hex(uri.toString());
				pStmt.setString(1, hash);
				pStmt.executeUpdate();
			}
			catch(Exception e) {
				logger.catching(e);
				throw logger.throwing(new RuntimeException("Storage failure!"));
			}
		}
		logger.exit();
	}
	
	/**
	 * Put (and possibly replace) an entry to the storage
	 */
	public void put(URI uri, byte[] bytes) throws SQLException {
		logger.entry();
		synchronized(con) {
			if(putNew(uri, bytes)) { //putNew returns true if it added something
				logger.exit();
				return;
			}
	
			//Entry exists, so we need to do an UPDATE instead of an INSERT
			
			PreparedStatement pStmt = null;
			try {
				pStmt = con.prepareStatement("UPDATE " + table + " SET uri = ?, value = ? WHERE " +
								" hash = ?");
				
				String hash = DigestUtils.sha1Hex(uri.toString());
				
				pStmt.setString(3, hash);
				pStmt.setString(1, uri.toString());
				pStmt.setBytes(2, bytes);
				
				pStmt.executeUpdate();
			}
			catch(Exception e) {
				logger.catching(e);
				throw logger.throwing(new RuntimeException("Storage failure!"));
			}
			finally {
				if(pStmt != null) pStmt.close();
			}
		}
		logger.exit();
	}
	
	/**
	 * Add an entry to the storage if and only if it did not exist yet
	 */
	public synchronized boolean putNew(URI uri, byte[] bytes) throws SQLException {
		logger.entry();
		synchronized(con) {
			if(containsKey(uri))
				return logger.exit(false);
			
			PreparedStatement pStmt = null;
			try {
				pStmt = con.prepareStatement("INSERT INTO " + table + "(hash, uri, value) " +
								"VALUES(?, ?, ?)");
				
				String hash = DigestUtils.sha1Hex(uri.toString());
				
				pStmt.setString(1, hash);
				pStmt.setString(2, uri.toString());
				pStmt.setBytes(3, bytes);
				
				pStmt.executeUpdate();
				return logger.exit(true);
			}
			catch(Exception e) {
				logger.catching(e);
				throw logger.throwing(new RuntimeException("Storage failure!"));
			}
			finally {
				if(pStmt != null) pStmt.close();
			}
		}
	}
	
	/**
	 * Checks if an entry exists in the storage
	 */
	public boolean containsKey(URI uri) throws SQLException {
		logger.entry();
		synchronized(con) {
			PreparedStatement pStmt = null;
			ResultSet rst = null;
			try {
				pStmt = con.prepareStatement("SELECT EXISTS(SELECT 1 FROM " + table + " WHERE " +
							" hash = ? LIMIT 1)");
				String hash = DigestUtils.sha1Hex(uri.toString());
				pStmt.setString(1, hash);
				rst = pStmt.executeQuery();
				
				if(!rst.next())
					return logger.exit(false);
				
				int result = rst.getInt(1);
				
				if(result == 1)
					return logger.exit(true);
				else
					return logger.exit(false);
			}
			catch(Exception e) {
				logger.catching(e);
				throw logger.throwing(new RuntimeException("Storage failure!"));
			}
			finally {
				if(pStmt != null) pStmt.close();
				if(rst != null) rst.close();
			}
		}
	}
}