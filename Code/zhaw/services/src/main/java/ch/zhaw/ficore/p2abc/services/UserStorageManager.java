//INFO: This is a COPY of the UserStorageManager from the tree Code/core-abce/abce-services.
//TODO: With the planned storage abstraction (i.e. to allow storing stuff in databases
//      rather than files) look at this file again and decide whether to replace/rewrite it.

//* Licensed Materials - Property of IBM, Miracle A/S, and            *
//* Alexandra Instituttet A/S                                         *
//* eu.abc4trust.pabce.1.0                                            *
//* (C) Copyright IBM Corp. 2012. All Rights Reserved.                *
//* (C) Copyright Miracle A/S, Denmark. 2012. All Rights Reserved.    *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2012. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*                                                                   *
//* This file is licensed under the Apache License, Version 2.0 (the  *
//* "License"); you may not use this file except in compliance with   *
//* the License. You may obtain a copy of the License at:             *
//*   http://www.apache.org/licenses/LICENSE-2.0                      *
//* Unless required by applicable law or agreed to in writing,        *
//* software distributed under the License is distributed on an       *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY            *
//* KIND, either express or implied.  See the License for the         *
//* specific language governing permissions and limitations           *
//* under the License.                                                *
//*/**/****************************************************************

package ch.zhaw.ficore.p2abc.services;

import java.security.SecureRandom;
import java.util.Random;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import eu.abc4trust.guice.configuration.AbceConfigurationImpl;
import eu.abc4trust.guice.configuration.AbceKeyManagerConfigurationModule;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.ri.servicehelper.FileSystem;

public class UserStorageManager {

	private static final boolean WIPE_STOARAGE_FILES = false;

	public static KeyManager getKeyManager(String fileStoragePrefix)
	        throws Exception {

		AbceConfigurationImpl configuration = setupStorageFilesForConfiguration(
		        fileStoragePrefix, WIPE_STOARAGE_FILES);

		Module m = new AbceKeyManagerConfigurationModule(configuration);
		Injector injector = Guice.createInjector(m);
		return injector.getInstance(KeyManager.class);
	}

	protected static AbceConfigurationImpl setupStorageFilesForConfiguration(
	        String fileStoragePrefix, boolean wipe_existing_storage)
	        throws Exception {
		AbceConfigurationImpl configuration = new AbceConfigurationImpl();
		configuration.setKeyStorageFile(FileSystem.getFile(fileStoragePrefix
		        + "keystorage", wipe_existing_storage));
		Random random = new SecureRandom(); // new Random(1985)
		configuration.setPrng(random);

		return configuration;
	}

}
