package ch.zhaw.ficore.p2abc.storage;

import java.net.URI;
import java.util.List;
import java.sql.*;
import org.apache.commons.codec.digest.DigestUtils;

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
	
	public void putNew(URI uri, byte[] bytes) throws SQLException {
		PreparedStatement pStmt = null;
		try {
			pStmt = con.prepareStatement("INSERT INTO " + table + "(hash, uri, value) " +
							"VALUES(?, ?, ?)");
			
			String hash = DigestUtils.sha1Hex(uri.toString());
			
			pStmt.setString(1, hash);
			pStmt.setString(2, uri.toString());
			pStmt.setBytes(3, bytes);
			
			pStmt.executeUpdate();
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Storage failure!");
		}
		finally {
			if(pStmt != null) pStmt.close();
		}
	}
	
	public boolean containsKey(URI uri) throws SQLException {
		PreparedStatement pStmt = null;
		ResultSet rst = null;
		try {
			pStmt = con.prepareStatement("SELECT EXISTS(SELECT 1 FROM " + table + " WHERE " +
						" hash = ? LIMIT 1)");
			String hash = DigestUtils.sha1Hex(uri.toString());
			pStmt.setString(1, hash);
			rst = pStmt.executeQuery();
			int result = rst.getInt(1);
			
			if(result == 1)
				return true;
			else
				return false;
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Storage failure!");
		}
		finally {
			if(pStmt != null) pStmt.close();
			if(rst != null) rst.close();
		}
	}
}