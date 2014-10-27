package ch.zhaw.ficore.p2abc.services;

import java.io.File;

/**
 * StorageConfiguration for an sqlite backend.
 * 
 * @author mroman
 */
public class SqliteStorageConfiguration extends StorageConfiguration implements Cloneable {
	private String dbFilePath;

	public SqliteStorageConfiguration() {
		dbFilePath = "/tmp" + File.separator + "p2abc.db";
	}

	public String getDatabaseFilePath() {
		return dbFilePath;
	}

	@Override
	public SqliteStorageConfiguration clone() throws CloneNotSupportedException {
		

		SqliteStorageConfiguration ret = (SqliteStorageConfiguration) super.clone();
		ret.dbFilePath = this.dbFilePath;

		return ret;
	}
}