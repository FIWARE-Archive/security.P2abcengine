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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Implements the URIBytesStorage interface with a SQLite database.
 *
 * This class will only create one connection for the same filePath to prevent
 * concurrency issues. This is due to SQLite writing to files which means SQLite
 * relies on file-system locks and the like. SQLite has some known issues under
 * certain circumstances with concurrent modifications:
 * http://www.sqlite.org/howtocorrupt.html. All methods synchronize through a
 * lock object which is bound to the DBName.
 *
 * @author mroman
 */
public class JdbcURIBytesStorage extends URIBytesStorage {
    /** The base of the hexadecimal number system. I need this so that
     * checkstyle shuts up. */
    private static final int HEX_BASE = 16;

    /** When dumping values for debugging purposes, how many
     * bytes to dump at most. */
    private static final int DUMP_LIMIT = 1024;

    /** The JDBC data source. */
    private DataSource dataSource;

    /** The JDBC data source's table name. */
    private String tableName;

    /** The JDBC data source's database name. */
    private String dbName;

    /** SQLits table locks. */
    private static Map<String, ReentrantLock> locks
        = new HashMap<String, ReentrantLock>();

    /** Use connection pooling?
     *
     * If this is set to false rather than using ConnectionPooling and
     * DataSource configured in Context.xml this class will use temporary files
     * to store the data.
     */
    private static boolean usePool = true;

    /** Temporary files for data storage. */
    private static Map<String, File> tempFiles = new HashMap<String, File>();

    /** Use locking on temporary files? */
    private static boolean useLocking = false;

    /** The logger. */
    private static final XLogger LOGGER
        = new XLogger(LoggerFactory.getLogger(JdbcURIBytesStorage.class));

    /**
     * Constructor.
     *
     * This will open and create the database as well as the tables if
     * necessary.
     *
     * @param dbName
     *            JNDI name of the database
     * @param tableName
     *            Name of the table to use
     *
     * @throws UnsafeTableNameException
     *             if the table name is deemed unsafe to use in a
     *             <code>CREATE TABLE</code> statement
     * @throws SQLException
     *             if a SQL error occurs, such as database error, file-based
     *             errors, and so on
     * @throws ClassNotFoundException
     *             when the SQLite JDBC driver can't be found
     * @throws NamingException
     *             when the naming context lookup fails
     */
    @Inject
    public JdbcURIBytesStorage(@Named("sqliteDBPath") final String dbName,
            @Named("sqliteTblName") final String tableName)
            throws ClassNotFoundException, SQLException,
            UnsafeTableNameException, NamingException {

        init(dbName, tableName);
    }

    private Connection getConnection() {
        try {
            if (usePool) {
                return dataSource.getConnection();
            } else {
                synchronized (tempFiles) {
                    if (tempFiles.get(dbName) == null) {
                        tempFiles.put(dbName,
                                File.createTempFile("storage", "tmp"));
                    }
                    final String tempFilePath = tempFiles.get(dbName)
                            .getAbsolutePath();
                    Class.forName("org.sqlite.JDBC");
                    LOGGER.info("Temp file is: " + tempFilePath);
                    return DriverManager.getConnection("jdbc:sqlite:"
                            + tempFilePath);
                }
            }
        } catch (final Exception e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new RuntimeException(e));
        }
    }

    /**
     * Performs the "connection sharing" logic.
     *
     * @param dbName the database name
     * @param tableName the table name inside the database
     *
     * @throws ClassNotFoundException when the SQLite driver cannot be loaded
     * @throws SQLException when the SQL to create the table fails
     * @throws UnsafeTableNameException when the table name given is considered
     *              unsafe (e.g., contains characters other than simple ASCII
     *              letters)
     * @throws NamingException when the JDBC context lookup fails
     */
    private void init(final String dbName, final String tableName)
            throws ClassNotFoundException, SQLException,
            UnsafeTableNameException, NamingException {
        LOGGER.entry(dbName, tableName);

        checkIfSafeTableName(tableName);

        this.tableName = tableName;
        this.dbName = dbName;

        final Statement stmt = null;

        try {
            Connection databaseConnection = null;

            final Context initCtx = new InitialContext();
            final Context envCtx = (Context) initCtx.lookup("java:/comp/env");

            if (usePool) {
                dataSource = (DataSource) envCtx.lookup("jdbc/" + dbName);
                assert dataSource != null;
            }

            useLocking = (Boolean) envCtx.lookup("cfg/useDbLocking");
            LOGGER.info("useLocking := " + useLocking);

            lock(this, LOGGER);

            databaseConnection = getConnection();
            createTable(tableName, databaseConnection);

            this.tableName = tableName;

        } catch (final SQLException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(e);
        } catch (final NamingException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(e);
        } finally {
            unlock(this, LOGGER);
            if (stmt != null) {
                stmt.close();
            }
        }

        LOGGER.exit();
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
            justification = "Table name is checked in checkIfSafeTableName()")
    private void createTable(final String tableName,
            final Connection databaseConnection) throws SQLException {
        Statement stmt = null;
        final String sql = "CREATE TABLE IF NOT EXISTS " + tableName
                + "(hash          VARCHAR(40) PRIMARY KEY     NOT NULL,"
                + " uri           TEXT    NOT NULL, "
                + " value         BLOB    NOT NULL)";

        try {
            stmt = databaseConnection.createStatement();
            stmt.executeUpdate(sql);
        } catch (final SQLException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (final SQLException ex) {
                    LOGGER.catching(ex);
                }
            }
        }
    }


    /**
     * Returns a lock based on the filePath of the SqliteURIBytesStorage
     * through which it will synchronize access to databases.
     *
     * @param storage
     *            An SqliteURIBytesStorage object
     * @return an Object (usable for locking)
     */
    private static synchronized ReentrantLock getLock(
            final JdbcURIBytesStorage storage) {
        final String key = storage.getDBName();
        if (locks.get(key) == null) {
            locks.put(key, new ReentrantLock());
        }
        return locks.get(key);
    }

    private static void lock(
            final JdbcURIBytesStorage storage,
            final XLogger logger) {
        logger.entry();

        if (!useLocking) {
            logger.exit();
            return;
        }

        final ReentrantLock rLock = getLock(storage);
        rLock.lock();

        logger.exit();
    }

    private static void unlock(
            final JdbcURIBytesStorage storage,
            final XLogger logger) {
        logger.entry();

        if (!useLocking) {
            logger.exit();
            return;
        }

        final ReentrantLock rLock = getLock(storage);
        rLock.unlock();

        logger.exit();
    }

    /**
     * Return the file path of the underlying database.
     *
     * @return path to database file
     */
    public final String getDBName() {
        return dbName;
    }

    /**
     * Checks if a table name is safe to use in a SQL CREATE TABLE statement.
     *
     * This function applies a rather drastic whitelist of characters that are
     * allowed in a SQL CREATE TABLE statement. This is to prevent SQL
     * injection at the "create table" stage. For example,
     * <code>users</code> is a valid table name, whereas <code>a; DROP
     * TABLE users</code> is not.
     *
     * This function might reject perfectly safe names out of paranoia, but
     * names consisting only of ASCII letters are always safe.
     *
     * @param tableName
     *            the table name to check
     *
     * @throws UnsafeTableNameException
     *             if the table name is not deemed to be safe
     */
    private static void checkIfSafeTableName(final String tableName)
            throws UnsafeTableNameException {
        final CharsetEncoder asciiEncoder
            = StandardCharsets.US_ASCII.newEncoder();

        if (!asciiEncoder.canEncode(tableName)) {
            throw new UnsafeTableNameException(tableName);
        }

        for (final char c : tableName.toCharArray()) {
            final int codePoint = c;

            /*
             * At this point, codePoint is an ASCII character, hence the
             * comparisons below are safe.
             */
            if ((codePoint >= 'A' && codePoint <= 'Z')
                    || (codePoint >= 'a' && codePoint <= 'z')
                    || (codePoint == '_')) {
                ; // empty
            } else {
                throw new UnsafeTableNameException(tableName);
            }
        }
    }

    /**
     * Lists all keys.
     */
    public final List<String> keysAsStrings()  {
        LOGGER.entry();

        lock(this, LOGGER);
        ResultSet rst = null;
        final List<String> uris = new ArrayList<String>();
        Connection databaseConnection = null;
        PreparedStatement keysAsStringsStatement = null;

        try {
            databaseConnection = getConnection();

            keysAsStringsStatement = databaseConnection
                    .prepareStatement("SELECT uri FROM " + tableName);

            rst = keysAsStringsStatement.executeQuery();
            while (rst.next()) {
                try {
                    LOGGER.info(" KEY:=" + rst.getString(1));
                    uris.add(rst.getString(1));
                } catch (final Exception e) {
                    // We can't do much here. This means that somebody
                    // managed to store an invalid URI in the storage.
                    // We do *not* break off the loop here, since we'll
                    // try to get more URIs out.
                    LOGGER.catching(e);
                }
            }
            return LOGGER.exit(uris);
        } catch (final SQLException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new RuntimeException("Storage failure!"));
        } finally {
            unlock(this, LOGGER);
            if (rst != null) {
                try {
                    rst.close();
                } catch (final SQLException e) {
                    LOGGER.catching(e);
                }
            }
            if (keysAsStringsStatement != null) {
                try {
                    keysAsStringsStatement.close();
                } catch (final SQLException e) {
                    LOGGER.catching(e);
                }
            }
            if (databaseConnection != null) {
                try {
                    databaseConnection.close();
                } catch (final SQLException e) {
                    LOGGER.catching(e);
                }
            }
        }

    }

    /**
     * Gets an entry from the storage.
     *
     * @param key the key to look up
     *
     * @return the bytes stored under that key
     */
    public final byte[] get(final String key) {
        LOGGER.entry(key);

        lock(this, LOGGER);
        ResultSet rst = null;
        PreparedStatement getStatement = null;
        Connection databaseConnection = null;

        try {
            databaseConnection = getConnection();
            getStatement = databaseConnection
                    .prepareStatement("SELECT value FROM " + tableName
                            + " WHERE hash = ?");

            final String hash = hashKey(key);
            getStatement.setString(1, hash);
            rst = getStatement.executeQuery();
            while (rst.next()) {
                final byte[] ret = rst.getBytes(1);
                if (LOGGER.isTraceEnabled()) {
                    hexdump(LOGGER, ret);
                }
                return LOGGER.exit(ret);
            }
            return LOGGER.exit(null);
        } catch (final Exception e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new RuntimeException("Storage failure!"));
        } finally {
            unlock(this, LOGGER);
            if (rst != null) {
                try {
                    rst.close();
                } catch (final SQLException e) {
                    LOGGER.catching(e);
                }
            }
            if (getStatement != null) {
                try {
                    getStatement.close();
                } catch (final SQLException e) {
                    LOGGER.catching(e);
                }
            }
            if (databaseConnection != null) {
                try {
                    databaseConnection.close();
                } catch (final SQLException e) {
                    LOGGER.catching(e);
                }
            }
        }
    }

    private static void hexdump(final XLogger logger, final byte[] bytes) {
        logger.trace("Dumping byte array of size " + bytes.length + " (max. "
                + DUMP_LIMIT + " bytes)");
        logger.trace(" Offset   0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f"
                + "   0123456789abcdef");

        final int limit = Math.min(bytes.length, DUMP_LIMIT);
        for (int offset = 0; offset < limit; offset += HEX_BASE) {
            dumpLine(logger, bytes, offset);
        }
    }

    private static void dumpLine(
            final XLogger logger,
            final byte[] bytes,
            final int offset) {
        final StringBuilder sb = new StringBuilder();
        final Formatter formatter = new Formatter(sb, Locale.ROOT);
        formatter.format("%1$08x", offset);

        final int limit = Math.min(bytes.length, offset + HEX_BASE);
        for (int i = offset; i < limit; i++) {
            formatter.format(" %1$02x", bytes[i]);
        }

        for (int i = limit; i < offset + HEX_BASE; i++) {
            formatter.format("   ");
        }

        formatter.format("  ");

        for (int i = offset; i < limit; i++) {
            formatter.format("%1$c", makeAscii(bytes[i]));
        }

        logger.trace(sb.toString());
        formatter.close();
    }

    private static char makeAscii(final byte b) {
        return (b >= ' ' && b <= '~') ? (char) b : '.';
    }

    /**
     * Deletes an entry from the storage.
     *
     * @param key the key to delete
     *
     * @throws SQLException when a database operation fails
     */
    public final void delete(final String key) throws SQLException {
        LOGGER.entry();

        lock(this, LOGGER);
        Connection databaseConnection = null;
        PreparedStatement deleteStatement = null;
        try {
            databaseConnection = getConnection();
            deleteStatement = databaseConnection
                    .prepareStatement("DELETE FROM " + tableName
                            + " WHERE hash = ?");
            final String hash = hashKey(key);
            deleteStatement.setString(1, hash);
            deleteStatement.executeUpdate();
        } catch (final Exception e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new RuntimeException("Storage failure!"));
        } finally {
            unlock(this, LOGGER);
            if (deleteStatement != null) {
                deleteStatement.close();
            }
            if (databaseConnection != null) {
                databaseConnection.close();
            }
        }

        LOGGER.exit();
    }

    /**
     * Puts (and possibly replaces) an entry in the storage.
     *
     * @param key the key to the new entry
     * @param bytes the bytes to be associated with that key
     *
     * @throws SQLException when a database operation fails
     */
    public final void put(final String key, final byte[] bytes)
            throws SQLException {
        LOGGER.entry(key, bytes);
        LOGGER.info(" - put " + key + "-" + bytes.length + " bytes");

        lock(this, LOGGER);

        if (putNew(key, bytes)) { // putNew returns true if it added something
            LOGGER.exit();
            unlock(this, LOGGER);
            return;
        }

        // Entry exists, so we need to do an UPDATE instead of an INSERT

        Connection databaseConnection = null;
        PreparedStatement putStatement = null;

        try {
            databaseConnection = getConnection();
            putStatement = databaseConnection.prepareStatement("UPDATE "
                    + tableName + " SET uri = ?, value = ? WHERE "
                    + " hash = ?");

            final String hash = hashKey(key);

            /* This nonsense is needed in order to shut checkstyule up.
             * If we did this with constants 1, 2, 3, checkstyle would
             * complain that "3 is a magic number". */
            int index = 1;
            putStatement.setString(index++, key);
            putStatement.setBytes(index++, bytes);
            putStatement.setString(index++, hash);

            putStatement.executeUpdate();
        } catch (final Exception e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new RuntimeException("Storage failure!"));
        } finally {
            unlock(this, LOGGER);
            if (putStatement != null) {
                putStatement.close();
            }
            if (databaseConnection != null) {
                databaseConnection.close();
            }
        }

        LOGGER.exit();
    }

    /**
     * Adds an entry to the storage if and only if it did not exist yet.
     *
     * @param key the key to insert
     * @param bytes the bytes to associate with that key
     *
     * @return true if the entry was successfully inserted, false
     *      if the key existed already
     *
     * @throws SQLException when a database operation fails
     */
    public final boolean putNew(
            final String key,
            final byte[] bytes) throws SQLException {
        LOGGER.entry(key, bytes);

        lock(this, LOGGER);
        if (containsKey(key)) {
            unlock(this, LOGGER);
            return LOGGER.exit(false);
        }

        Connection databaseConnection = null;
        PreparedStatement putNewStatement = null;

        try {
            databaseConnection = getConnection();
            putNewStatement = databaseConnection
                    .prepareStatement("INSERT INTO " + tableName
                            + "(hash, uri, value) " + "VALUES(?, ?, ?)");

            final String hash = hashKey(key);

            /* This stupid trick is so that checkstyle shuts up. */
            int index = 1;
            putNewStatement.setString(index++, hash);
            putNewStatement.setString(index++, key);
            putNewStatement.setBytes(index++, bytes);

            putNewStatement.executeUpdate();
            return LOGGER.exit(true);
        } catch (final SQLException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new RuntimeException("Storage failure: "
                    + e.getMessage()));
        } finally {
            unlock(this, LOGGER);
            if (putNewStatement != null) {
                putNewStatement.close();
            }
            if (databaseConnection != null) {
                databaseConnection.close();
            }
        }
    }

    /**
     * Checks if an entry exists in the storage.
     *
     * @param key the key to check
     *
     * @return true of the key is in the storage, false otherwise
     */
    public final boolean containsKey(final String key) {
        LOGGER.entry();
        lock(this, LOGGER);
        Connection databaseConnection = null;
        PreparedStatement containsKeyStatement = null;
        ResultSet rst = null;

        try {
            databaseConnection = getConnection();
            containsKeyStatement = databaseConnection
                    .prepareStatement("SELECT EXISTS(SELECT 1 FROM "
                            + tableName + " WHERE " + " hash = ? LIMIT 1)");

            final String hash = hashKey(key);
            containsKeyStatement.setString(1, hash);
            rst = containsKeyStatement.executeQuery();

            if (!rst.next()) {
                return LOGGER.exit(false);
            }

            final int result = rst.getInt(1);

            if (result == 1) {
                return LOGGER.exit(true);
            } else {
                return LOGGER.exit(false);
            }
        } catch (final SQLException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new RuntimeException("Storage failure: "
                    + e.getMessage()));
        } finally {
            unlock(this, LOGGER);
            if (rst != null) {
                try {
                    rst.close();
                } catch (final SQLException e) {
                    LOGGER.catching(e);
                }
            }
            if (containsKeyStatement != null) {
                try {
                    containsKeyStatement.close();
                } catch (final SQLException e) {
                    LOGGER.catching(e);
                }
            }
            if (databaseConnection != null) {
                try {
                    databaseConnection.close();
                } catch (final SQLException e) {
                    LOGGER.catching(e);
                }
            }
        }

    }

    private static String hashKey(final String key) {
        return DigestUtils.sha1Hex(key);
    }

    /** Deletes all entries.
     *
     * @throws SQLException when something goes wrong with the deletion
     */
    public final void deleteAll() throws SQLException {
        LOGGER.entry();

        Connection databaseConnection = null;
        PreparedStatement deleteAllStatement = null;

        lock(this, LOGGER);

        try {
            databaseConnection = getConnection();
            deleteAllStatement = databaseConnection
                    .prepareStatement("DELETE FROM " + tableName);
            deleteAllStatement.execute();
        } catch (final SQLException e) {
            LOGGER.catching(e);
            throw LOGGER.throwing(new RuntimeException("Storage failure: "
                    + e.getMessage()));
        } finally {
            unlock(this, LOGGER);
            if (deleteAllStatement != null) {
                deleteAllStatement.close();
            }
            if (databaseConnection != null) {
                databaseConnection.close();
            }
        }
        LOGGER.exit();
    }
}
