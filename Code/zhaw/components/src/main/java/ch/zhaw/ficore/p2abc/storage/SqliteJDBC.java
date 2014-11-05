package ch.zhaw.ficore.p2abc.storage;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.sqlite.SQLiteJDBCLoader;

/** Copied from SQLite driver, adapted for multi-threaded use. */
public class SqliteJDBC implements Driver
{
    public static final String PREFIX = "jdbc:zsqlite:";

    public static final Map<String, Connection> urlToConnection = new HashMap<>();
    
    static {
        try {
            Class.forName("org.sqlite.JDBC");
            DriverManager.registerDriver(new SqliteJDBC());
        }
        catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see java.sql.Driver#getMajorVersion()
     */
    public int getMajorVersion() {
        return SQLiteJDBCLoader.getMajorVersion();
    }

    /**
     * @see java.sql.Driver#getMinorVersion()
     */
    public int getMinorVersion() {
        return SQLiteJDBCLoader.getMinorVersion();
    }

    /**
     * @see java.sql.Driver#jdbcCompliant()
     */
    public boolean jdbcCompliant() {
        return false;
    }

    /**
     * @see java.sql.Driver#acceptsURL(java.lang.String)
     */
    public boolean acceptsURL(String url) {
        return isValidURL(url);
    }

    /**
     * Validates a URL
     * @param url
     * @return true if the URL is valid, false otherwise
     */
    public static boolean isValidURL(String url) {
        return url != null && url.toLowerCase().startsWith(PREFIX);
    }

    /**
     * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
     */
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return null;
        // not visible: return SQLiteConfig.getDriverPropertyInfo();
    }

    /**
     * @see java.sql.Driver#connect(java.lang.String, java.util.Properties)
     */
    public Connection connect(String url, Properties info) throws SQLException {
        return createConnection(url, info);
    }

    /**
     * Gets the location to the database from a given URL.
     * @param url The URL to extract the location from.
     * @return The location to the database.
     */
    static String extractAddress(String url) {
        // if no file name is given use a memory database
        return PREFIX.equalsIgnoreCase(url) ? ":memory:" : url.substring(PREFIX.length());
    }

    /**
     * Creates a new database connection to a given URL.
     * @param url the URL
     * @param prop the properties
     * @return a Connection object that represents a connection to the URL
     * @throws SQLException
     * @see java.sql.Driver#connect(java.lang.String, java.util.Properties)
     */
    public static Connection createConnection(String url, Properties prop) throws SQLException {
        if (!isValidURL(url))
            throw new SQLException("invalid database address: " + url);

        url = url.trim();
        Connection ret = urlToConnection.get(url);
        
        if (ret == null) {
            ret = DriverManager.getConnection("jdbc:sqlite:" + url.substring(PREFIX.length()));
            urlToConnection.put(url, ret);
        }
        
        return ret;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }
}