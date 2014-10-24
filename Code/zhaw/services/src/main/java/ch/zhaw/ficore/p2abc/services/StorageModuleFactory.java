package ch.zhaw.ficore.p2abc.services;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import ch.zhaw.ficore.p2abc.services.guice.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Serves as a Factory for StorageModules where a StorageModule
 * is an AbstractModule from Guice with bindings.
 * 
 * @author mroman
 */
public class StorageModuleFactory {
	
	private static Logger logger = LogManager.getLogger(StorageModuleFactory.class.getName());
	
	/**
	 * Factory method. Returns an array of Modules.
	 * 
	 * @return Array of Modules that shall be used to overwrite existing guice bindings.
	 */
	public static synchronized Module[] getModulesForServiceConfiguration(ServicesConfiguration config, ServicesConfiguration.ServiceType type) {
		logger.entry();
		
		StorageConfiguration storageConfig = config.getStorageConfiguration();
		
		if(storageConfig instanceof SqliteStorageConfiguration) {
			return logger.exit(new Module[]{new SqliteStorageModule((SqliteStorageConfiguration) storageConfig, type)});
		}
		
		logger.info("no corresponding modules found!");
		
		return logger.exit(new Module[]{});
	}
}