package ch.zhaw.ficore.p2abc.services;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ch.zhaw.ficore.p2abc.services.guice.SqliteStorageModule;

import com.google.inject.Module;

/**
 * Serves as a Factory for StorageModules where a StorageModule is an
 * AbstractModule from Guice with bindings.
 * 
 * @author mroman
 */
public class StorageModuleFactory {

	private static final XLogger logger = new XLogger(
	        LoggerFactory.getLogger(StorageModuleFactory.class.getName()));

	/**
	 * Factory method. Returns an array of Modules.
	 * 
	 * @param type
	 *            Type of the service.
	 * @return Array of Modules that shall be used to overwrite existing guice
	 *         bindings.
	 */
	public static synchronized Module[] getModulesForServiceConfiguration(
	        ServiceType type) {
		logger.entry();

		return logger.exit(new Module[] { new SqliteStorageModule(type) });
	}
}