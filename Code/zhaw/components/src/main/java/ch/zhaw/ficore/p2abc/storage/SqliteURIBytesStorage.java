package ch.zhaw.ficore.p2abc.storage;

import java.net.URI;
import java.util.List;
import java.sql.*;

public class SqliteURIBytesStorage {
	private Connection con;
	private String table;
	
	public SqliteURIBytesStorage(String filePath, String table) {
		try {
			Class.forName("org.sqlite.JDBC");
			con = DriverManager.getConnection("jdbc:sqlite:" + filePath);
			
			Statement stmt = con.createStatement();
		    String sql = "CREATE TABLE IF NOT EXISTS " + table +
		                   "(hash          VARCHAR(40) PRIMARY KEY     NOT NULL," +
		                   " uri           TEXT    NOT NULL, " + 
		                   " value         BLOB     NOT NULL)";
		    stmt.executeUpdate(sql);
		    stmt.close();
		    this.table = table;
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to open storage!");
		}
	}
	
	public void putNew(URI uri, byte[] bytes) {
		try {
			PreparedStatement pStmt = con.prepareStatement("INSERT INTO " + table + "(hash, uri, value) " +
							"VALUES(?, ?, ?)");
			
			String hash = "foo"; //TODO: Calc hash here :D
			
			pStmt.setString(1, hash);
			pStmt.setString(2, uri.toString());
			pStmt.setBytes(3, bytes);
			
			pStmt.executeUpdate();
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Storage failure!");
		}
	}
}