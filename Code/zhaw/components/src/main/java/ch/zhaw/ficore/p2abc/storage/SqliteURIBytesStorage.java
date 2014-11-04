package ch.zhaw.ficore.p2abc.storage;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Implements the URIBytesStorage interface with a SQLite database.
 * 
 * This class will only create one connection for the same
 * filePath to prevent concurrency issues. This is due to SQLite
 * writing to files which means SQLite relies on file-system locks and the like.
 * SQLite has some known issues under certain circumstances with concurrent 
 * modifications: http://www.sqlite.org/howtocorrupt.html.
 * All methods synchronize through the connection object.
 * 
 * @author mroman
 */
public class SqliteURIBytesStorage extends URIBytesStorage {
	private Connection databaseConnection;
	private String tableName;
	
    private PreparedStatement keysAsStringsStatement;
    private PreparedStatement getStatement;
    private PreparedStatement deleteStatement;
    private PreparedStatement putStatement;
    private PreparedStatement putNewStatement;
    private PreparedStatement containsKeyStatement;

    private Logger logger;

	/**
	 * Constructor.
	 * 
	 * This will open and create the database as well as the tables
	 * if necessary. 
	 * 
	 * @param filePath Path to the database file
	 * @param table Name of the table to use
	 * 
	 * @throws UnsafeTableNameException if the table name is deemed unsafe to
	 *     use in a <code>CREATE TABLE</code> statement 
	 * @throws SQLException if a SQL error occurs, such as database error,
	 *     file-based errors, and so on
	 * @throws ClassNotFoundException when the SQLite JDBC driver can't be found
	 */
	@Inject
	public SqliteURIBytesStorage(@Named("sqliteDBPath") String filePath, @Named("sqliteTblName") String table) throws ClassNotFoundException, SQLException, UnsafeTableNameException {
		logger = LogManager.getLogger();
		init(filePath, table);
	}

	/**
	 * Performs the "connection sharing" logic. 
	 * 
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 * @throws UnsafeTableNameException 
	 */
	private synchronized void init(String filePath, String table) throws ClassNotFoundException, SQLException, UnsafeTableNameException {
		logger.entry();
        
		checkIfSafeTableName(table);
		
		PoolProperties p = new PoolProperties();
		p.setUrl("jdbc:sqlite:" + filePath);
		p.setDriverClassName("org.sqlite.JDBC");
		DataSource datasource = new org.apache.tomcat.jdbc.pool.DataSource(p);
		
        this.tableName = table;
        
		synchronized(databaseConnection) {
			Statement stmt = null;
			try {
			    databaseConnection = datasource.getConnection();
				stmt = databaseConnection.createStatement();
				String sql = "CREATE TABLE IF NOT EXISTS " + table +
						"(hash          VARCHAR(40) PRIMARY KEY     NOT NULL," +
						" uri           TEXT    NOT NULL, " + 
						" value         BLOB    NOT NULL)";
				stmt.executeUpdate(sql);
				this.tableName = table;
				
                keysAsStringsStatement = null;
                getStatement = null;
                deleteStatement = null;
                putStatement = null;
                putNewStatement = null;
                containsKeyStatement = null;
			}
			catch(SQLException e) {
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

	/** Closes this MYSQLite database.
	 * 
	 * This method performs any actions that need to be done in order to 
	 * release this object.
	 */
	public synchronized void close() {
	    logger.entry();
	    
        closePreparedStatement(keysAsStringsStatement);
        closePreparedStatement(getStatement);
        closePreparedStatement(deleteStatement);
        closePreparedStatement(putNewStatement);
        closePreparedStatement(putStatement);
        closePreparedStatement(containsKeyStatement);

        // Do this only after closing the statements.
        try {
            databaseConnection.close();
        } catch (SQLException e) {
            logger.catching(e);
        }
        
        logger.exit();
	}

    private void closePreparedStatement(PreparedStatement statement) {
        logger.entry();

        if (statement != null) {
	        try {
                statement.close();
            } catch (SQLException e) {
                logger.catching(e);
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
	 * @throws UnsafeTableNameException if the table name is not deemed to be safe
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
					|| (codePoint >= 'a' && codePoint <= 'z')
					|| (codePoint == '_'))
				; // empty
			else {
				throw new UnsafeTableNameException(tableName);
			}
		}
	}

	/**
	 * Lists all keys
	 */
	public List<String> keysAsStrings() throws SQLException {
		logger.entry();
		synchronized(databaseConnection) {
			ResultSet rst = null;
			List<String> uris = new ArrayList<String>();

			try {
			    if (keysAsStringsStatement == null)
			        keysAsStringsStatement = databaseConnection.prepareStatement("SELECT uri FROM " + tableName);
			    
				rst = keysAsStringsStatement.executeQuery();
				while(rst.next()) {
					try {
						uris.add(rst.getString(1));
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
				if(rst != null)
				    rst.close();
			}
		}
	}

	/**
	 * Get an entry from the storage
	 */
	public byte[] get(String key) throws SQLException {
		logger.entry(key);
		
		synchronized(databaseConnection) {
			ResultSet rst = null;

			try {
			    if (getStatement == null)
			        getStatement = databaseConnection.prepareStatement("SELECT value FROM " + tableName + " WHERE hash = ?");
			    
				String hash = hashKey(key);
				getStatement.setString(1, hash);
				rst = getStatement.executeQuery();
				while(rst.next()) {
				    byte[] ret = rst.getBytes(1);
				    if (logger.isTraceEnabled()) {
				        hexdump(logger, ret);
				    }
					return logger.exit(ret);
				}
				return logger.exit(null);
			}
			catch(Exception e) {
				logger.catching(e);
				throw logger.throwing(new RuntimeException("Storage failure!"));
			}
			finally {
				if(rst != null)
				    rst.close();
			}
		}
	}

    private static void hexdump(Logger logger, byte[] bytes) {
        logger.trace("Dumping byte array of size " + bytes.length);
        logger.trace(" Offset   0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f");
        for (int offset = 0; offset < bytes.length; offset += 16) {
            dumpLine(logger, bytes, offset);
        }
    }

    private static void dumpLine(Logger logger, byte[] bytes, int offset) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.ROOT);
        formatter.format("%1$08x", offset);
        
        int limit = Math.min(bytes.length, offset + 16);
        for (int i = offset; i < limit; i++) {
            formatter.format(" %1$02x", bytes[i]);
        }
        logger.trace(sb.toString());
        formatter.close();
    }
    
    /**
	 * Delete an entry from the storage
	 */
	public void delete(String key) throws SQLException {
		logger.entry();
		synchronized(databaseConnection) {
			try {
			    if (deleteStatement == null)
			        deleteStatement = databaseConnection.prepareStatement("DELETE FROM " + tableName + " WHERE hash = ?");
				String hash = hashKey(key);
				deleteStatement.setString(1, hash);
				deleteStatement.executeUpdate();
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
	public void put(String key, byte[] bytes) throws SQLException {
		logger.entry(key, bytes);
		
		synchronized(databaseConnection) {
			if(putNew(key, bytes)) { //putNew returns true if it added something
				logger.exit();
				return;
			}

			//Entry exists, so we need to do an UPDATE instead of an INSERT

			try {
			    if (putStatement == null)
			        putStatement = databaseConnection.prepareStatement("UPDATE " + tableName + " SET uri = ?, value = ? WHERE " +
			                " hash = ?");

				String hash = hashKey(key);

				putStatement.setString(3, hash);
				putStatement.setString(1, key);
				putStatement.setBytes(2, bytes);

				putStatement.executeUpdate();
			}
			catch(Exception e) {
				logger.catching(e);
				throw logger.throwing(new RuntimeException("Storage failure!"));
			}
		}
		logger.exit();
	}

	/**
	 * Add an entry to the storage if and only if it did not exist yet
	 */
	public synchronized boolean putNew(String key, byte[] bytes) throws SQLException {
		logger.entry(key, bytes);
		
		synchronized(databaseConnection) {
			if(containsKey(key))
				return logger.exit(false);

			try {
			    if (putNewStatement == null)
			        putNewStatement = databaseConnection.prepareStatement("INSERT INTO " + tableName + "(hash, uri, value) " +
			                "VALUES(?, ?, ?)");

				String hash = hashKey(key);

				putNewStatement.setString(1, hash);
				putNewStatement.setString(2, key);
				putNewStatement.setBytes(3, bytes);

				putNewStatement.executeUpdate();
				return logger.exit(true);
			}
			catch(Exception e) {
				logger.catching(e);
				throw logger.throwing(new RuntimeException("Storage failure!"));
			}
		}
	}

	/**
	 * Checks if an entry exists in the storage
	 */
	public boolean containsKey(String key) throws SQLException {
		logger.entry();
		synchronized(databaseConnection) {
			ResultSet rst = null;
			try {
				containsKeyStatement = databaseConnection.prepareStatement("SELECT EXISTS(SELECT 1 FROM " + tableName + " WHERE " +
						" hash = ? LIMIT 1)");
				String hash = hashKey(key);
				containsKeyStatement.setString(1, hash);
				rst = containsKeyStatement.executeQuery();

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
				if(rst != null)
				    rst.close();
			}
		}
	}

    private static String hashKey(String key) {
        return DigestUtils.sha1Hex(key);
    }
}