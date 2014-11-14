package ch.zhaw.ficore.p2abc.storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcMeta {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        String tempFilePath = "/home/mroman/test.db";
        String tableName = "test2";
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempFilePath);  
        
        Statement stmt = conn.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName +
                "(hash          VARCHAR(40) PRIMARY KEY     NOT NULL," +
                " uri           TEXT    NOT NULL, " + 
                " value         BLOB    NOT NULL," +
                " miau          INT     NOT NULL)";
        stmt.executeUpdate(sql);
        
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getColumns(null, null, "test2", null);
        while(rs.next()) {
            System.out.println("Name: " + rs.getString(4));
            System.out.println("Type (as per java.sql.Types): " + rs.getString(5));
            System.out.println("Type (as per TYPE_NAME): " + rs.getString(6));
            System.out.println();
        }
        System.out.println("done");
    }
}
