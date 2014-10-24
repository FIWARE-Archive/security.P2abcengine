package ch.zhaw.ficore.p2abc.services;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class StorageModuleFactory {
	
	public static Module[] getModulesForServiceConfiguration(ServicesConfiguration config) {
		return new Module[]{};
	}
}