package ch.zhaw.ficore.p2abc.storage;

import java.io.File;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * All methods synchronize through a lock object which is bound to the DBName.
 * 
 * @author mroman
 */
public class SqliteURIBytesStorage extends URIBytesStorage {
    private static final int DUMP_LIMIT = 1024;
    private DataSource dataSource;
    private String tableName;
    private String dbName;

    private static Map<String,Object> locks = new HashMap<String,Object>();
    
    /*
     * If usePool is set to false rather than using ConnectionPooling and DataSource
     * configured in Context.xml this class wil use temporary files to store the data.
     */
    private static boolean usePool = true;
    private static Map<String, File> tempFiles = new HashMap<String, File>();

    private Logger logger;

    /**
     * Constructor.
     * 
     * This will open and create the database as well as the tables
     * if necessary. 
     * 
     * @param dbName JNDI name of the database
     * @param table Name of the table to use
     * 
     * @throws UnsafeTableNameException if the table name is deemed unsafe to
     *     use in a <code>CREATE TABLE</code> statement 
     * @throws SQLException if a SQL error occurs, such as database error,
     *     file-based errors, and so on
     * @throws ClassNotFoundException when the SQLite JDBC driver can't be found
     */
    @Inject
    public SqliteURIBytesStorage(@Named("sqliteDBPath") String dbName, @Named("sqliteTblName") String table)
            throws ClassNotFoundException, SQLException,
                UnsafeTableNameException, NamingException {
        logger = LogManager.getLogger();

        init(dbName, table);
    }


    private Connection getConnection() {
        try {
            if(usePool) {
                return dataSource.getConnection();
            }
            else {
                synchronized(tempFiles) {
                    if(tempFiles.get(dbName) == null) {
                        tempFiles.put(dbName,File.createTempFile("storage", "tmp"));
                    }
                    String tempFilePath = tempFiles.get(dbName).getAbsolutePath();
                    Class.forName("org.sqlite.JDBC");
                    logger.info("Temp file is: " + tempFilePath);
                    return DriverManager.getConnection("jdbc:sqlite:" + tempFilePath);  
                }
            }
        }
        catch(Exception e) {
            logger.catching(e);
            throw logger.throwing(new RuntimeException(e));
        }
    }

    /**
     * Performs the "connection sharing" logic. 
     * 
     * @throws ClassNotFoundException 
     * @throws SQLException 
     * @throws UnsafeTableNameException 
     */
    private void init(String dbName, String tableName)
            throws ClassNotFoundException, SQLException,
                UnsafeTableNameException, NamingException {
        logger.entry(dbName, tableName);

        checkIfSafeTableName(tableName);

        this.tableName = tableName;
        this.dbName = dbName;

        Statement stmt = null;

        try {
			Connection databaseConnection = null;
			
			synchronized(getLock(this)) {

    			if(usePool) {
                    Context initCtx = new InitialContext();
                    Context envCtx = (Context) initCtx.lookup("java:/comp/env");
                    dataSource = (DataSource) envCtx.lookup("jdbc/" + dbName);
    
                    assert dataSource != null;
                }
    			
                databaseConnection = getConnection();
                stmt = databaseConnection.createStatement();
                String sql = "CREATE TABLE IF NOT EXISTS " + tableName +
                        "(hash          VARCHAR(40) PRIMARY KEY     NOT NULL," +
                        " uri           TEXT    NOT NULL, " + 
                        " value         BLOB    NOT NULL)";
                stmt.executeUpdate(sql);
                this.tableName = tableName;
            }
        }
        catch(SQLException e) {
            logger.catching(e);
            throw logger.throwing(e);
        }
        catch(NamingException e) {
            logger.catching(e);
            throw logger.throwing(e);
        }
        finally {
            if(stmt != null)
                stmt.close();
        }

        logger.exit();
    }

    /**
     * Returns a lock based on the filePath of the SqliteURIBytesStorage
     * through which it will synchronize access to databases. 
     * 
     * @param storage An SqliteURIBytesStorage object
     * @return an Object (usable for locking)
     */
    private static synchronized Object getLock(SqliteURIBytesStorage storage) {
        String key = storage.getDBName();
        if(locks.get(key) == null)
            locks.put(key, new Object());
        return locks.get(key);
    }

    /**
     * Return the file path of the underlying database.
     * 
     * @return path to database file
     */
    public String getDBName() {
        return dbName;
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

        synchronized(getLock(this)) {
            ResultSet rst = null;
            List<String> uris = new ArrayList<String>();
            Connection databaseConnection = null;
            PreparedStatement keysAsStringsStatement = null;
            
            try {
                databaseConnection = getConnection();

                keysAsStringsStatement = databaseConnection.prepareStatement("SELECT uri FROM " + tableName);

                rst = keysAsStringsStatement.executeQuery();
                while(rst.next()) {
                    try {
                        logger.info(" KEY:=" + rst.getString(1));
                        uris.add(rst.getString(1));
                    }
                    catch(Exception e) {
                        //We can't do much here. This means that somebody
                        // managed to store an invalid URI in the storage.
                        // We do *not* break off the loop here, since we'll
                        // try to get more URIs out.
                        logger.catching(e);
                    }
                }
                return logger.exit(uris);
            } catch (SQLException e) {
                logger.catching(e);
                throw logger.throwing(new RuntimeException("Storage failure!"));
            }
            finally {
                if(rst != null)
                    rst.close();
                if (keysAsStringsStatement!= null)
                    keysAsStringsStatement.close();
                if (databaseConnection != null)
                    databaseConnection.close();
            }
        }
    }

    /**
     * Get an entry from the storage
     */
    public byte[] get(String key) throws SQLException {
        logger.entry(key);

        synchronized(getLock(this)) {
            ResultSet rst = null;
            PreparedStatement getStatement = null;
            Connection databaseConnection = null;

            try {
                databaseConnection = getConnection();
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
                if (rst != null)
                    rst.close();
                if (getStatement != null)
                    getStatement.close();
                if (databaseConnection != null)
                    databaseConnection.close();
            }
        }
    }

    private static void hexdump(Logger logger, byte[] bytes) {
        logger.trace("Dumping byte array of size " + bytes.length + " (max. " + DUMP_LIMIT + " bytes)");
        logger.trace(" Offset   0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f   0123456789abcdef");

        int limit = Math.min(bytes.length,  DUMP_LIMIT);
        for (int offset = 0; offset < limit; offset += 16) {
            dumpLine(logger, bytes, offset);
        }
    }

    private static void dumpLine(Logger logger, byte[] bytes, int offset) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.ROOT);
        formatter.format("%1$08x", offset);

        int limit = Math.min(bytes.length, offset + 16);
        for (int i = offset; i < limit; i++)
            formatter.format(" %1$02x", bytes[i]);

        for (int i = limit; i < offset + 16; i++)
            formatter.format("   ");

        formatter.format("  ");

        for (int i = offset; i < limit; i++)
            formatter.format("%1$c", makeAscii(bytes[i]));

        logger.trace(sb.toString());
        formatter.close();
    }

    private static char makeAscii(byte b) {
        return (b >= ' ' && b <= '~') ? (char) b : '.';
    }

    /**
     * Delete an entry from the storage
     */
    public void delete(String key) throws SQLException {
        logger.entry();
        synchronized(getLock(this)) {
            Connection databaseConnection = null;
            PreparedStatement deleteStatement = null;
            try {
                databaseConnection = getConnection();
                deleteStatement = databaseConnection.prepareStatement("DELETE FROM " + tableName + " WHERE hash = ?");
                String hash = hashKey(key);
                deleteStatement.setString(1, hash);
                deleteStatement.executeUpdate();
            }
            catch(Exception e) {
                logger.catching(e);
                throw logger.throwing(new RuntimeException("Storage failure!"));
            }
            finally {
                if (deleteStatement != null)
                    deleteStatement.close();
                if (databaseConnection != null)
                    databaseConnection.close();
            }
        }
        logger.exit();
    }

    /**
     * Put (and possibly replace) an entry to the storage
     */


    public void put(String key, byte[] bytes) throws SQLException {
        logger.entry(key, bytes);

        synchronized(getLock(this)) {

            if(putNew(key, bytes)) { //putNew returns true if it added something
                logger.exit();
                return;
            }

            //Entry exists, so we need to do an UPDATE instead of an INSERT

            Connection databaseConnection = null;
            PreparedStatement putStatement = null;

            try {
                databaseConnection = getConnection();
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
            finally {
                if (putStatement != null)
                    putStatement.close();
                if (databaseConnection != null)
                    databaseConnection.close();
            }
        }
        logger.exit();
    }

    /**
     * Add an entry to the storage if and only if it did not exist yet
     */
    public boolean putNew(String key, byte[] bytes) throws SQLException {
        logger.entry(key, bytes);

        synchronized(getLock(this)) {
            if(containsKey(key))
                return logger.exit(false);

            Connection databaseConnection = null;
            PreparedStatement putNewStatement = null;

            try {
                databaseConnection = getConnection();
                putNewStatement = databaseConnection.prepareStatement("INSERT INTO " + tableName + "(hash, uri, value) " +
                            "VALUES(?, ?, ?)");

                String hash = hashKey(key);

                putNewStatement.setString(1, hash);
                putNewStatement.setString(2, key);
                putNewStatement.setBytes(3, bytes);

                putNewStatement.executeUpdate();
                return logger.exit(true);
            }
            catch(SQLException e) {
                logger.catching(e);
                throw logger.throwing(new RuntimeException("Storage failure: " + e.getMessage()));
            }
            finally {
                if (putNewStatement != null)
                    putNewStatement.close();
                if (databaseConnection != null)
                    databaseConnection.close();
            }
        }
    }

    /**
     * Checks if an entry exists in the storage
     */
    public boolean containsKey(String key) throws SQLException {
        logger.entry();
        synchronized(getLock(this)) {
            Connection databaseConnection = null;
            PreparedStatement containsKeyStatement = null;
            ResultSet rst = null;

            try {
                databaseConnection = getConnection();
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
            catch(SQLException e) {
                logger.catching(e);
                throw logger.throwing(new RuntimeException("Storage failure: " + e.getMessage()));
            }
            finally {
                if(rst != null)
                    rst.close();
                if (containsKeyStatement != null)
                    containsKeyStatement.close();
                if (databaseConnection != null)
                    databaseConnection.close();
            }
        }
    }

    private static String hashKey(String key) {
        return DigestUtils.sha1Hex(key);
    }


    public void deleteAll() throws SQLException {
        logger.entry();
        
        Connection databaseConnection = null;
        PreparedStatement deleteAllStatement = null;

        try {
            databaseConnection = getConnection();
            deleteAllStatement = databaseConnection.prepareStatement("DELETE FROM " + tableName);
            deleteAllStatement.execute();
        }
        catch(SQLException e) {
            logger.catching(e);
            throw logger.throwing(new RuntimeException("Storage failure: " + e.getMessage()));
        }
        finally {
            if (deleteAllStatement != null)
                deleteAllStatement.close();
            if (databaseConnection != null)
                databaseConnection.close();
        }
        logger.exit();
    }
}
