package ch.zhaw.ficore.p2abc.services;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import ch.zhaw.ficore.p2abc.services.guice.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StorageModuleFactory {
	
	private static Logger logger = LogManager.getLogger(StorageModuleFactory.class.getName());
	
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