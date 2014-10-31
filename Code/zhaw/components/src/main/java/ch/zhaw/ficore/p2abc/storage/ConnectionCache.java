package ch.zhaw.ficore.p2abc.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class ConnectionCache {
    private static ConnectionCache instance = new ConnectionCache();
    
    private class CountedConnection {
        private Connection connection;
        private int count;
        
        public CountedConnection(Connection connection) {
            this.connection = connection;
            this.count = 1;
        }

        public Connection getConnection() {
            return connection;
        }

        public int getCount() {
            return count;
        }

        public void incCount() {
            count++;
        }

        public void decCount() {
            count--;
        }
    }
    
    private Map<String, CountedConnection> connections;
    private Logger logger;
    
    private ConnectionCache() {
        logger = LogManager.getLogger();
        connections = new HashMap<>();
    }
    
    /** Returns the instance for this connection cache.
     * 
     * @return this connection cache
     */
    public static ConnectionCache instance() {
        return ConnectionCache.instance;
    }

    public Connection get(String filePath) throws SQLException {
        logger.entry(filePath);
        
        CountedConnection con = connections.get(filePath);
        if(con != null)
            con.incCount();
        else {
            Connection c = DriverManager.getConnection("jdbc:sqlite:" + filePath);
            con = new CountedConnection(c);
            connections.put(filePath, con);
        }
        
        return logger.exit(con.getConnection());
    }

    public void release(String filePath) throws SQLException {
        logger.entry(filePath);
        
        CountedConnection con = connections.get(filePath);
        if(con != null) {
            con.decCount();
            if (con.getCount() == 0)
                con.getConnection().close();
        } else
            logger.warn("Trying to release unknown connection for \"" + filePath + "\"");
        
        logger.exit();        
    }
}
