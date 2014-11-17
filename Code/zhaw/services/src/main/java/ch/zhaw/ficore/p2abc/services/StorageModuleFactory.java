package ch.zhaw.ficore.p2abc.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.guice.SqliteStorageModule;

import com.google.inject.Module;

/**
 * Serves as a Factory for StorageModules where a StorageModule is an
 * AbstractModule from Guice with bindings.
 * 
 * @author mroman
 */
public class StorageModuleFactory {

    private static Logger logger = LogManager
            .getLogger(StorageModuleFactory.class.getName());

    /**
     * Factory method. Returns an array of Modules.
     * 
     * @return Array of Modules that shall be used to overwrite existing guice
     *         bindings.
     */
    public static synchronized Module[] getModulesForServiceConfiguration(
            ServiceType type) {
        logger.entry();

        return logger.exit(new Module[] { new SqliteStorageModule(type) });
    }
}