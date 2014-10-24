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

//This code was copied from the Code/core-abce/service-helper tree

package ch.zhaw.ficore.p2abc.services.helpers.issuer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.abce.external.issuer.IssuerAbcEngine;
import eu.abc4trust.abce.external.issuer.SynchronizedIssuerAbcEngineImpl;
import eu.abc4trust.abce.internal.issuer.credentialManager.CredentialManager;
import eu.abc4trust.abce.internal.issuer.tokenManagerIssuer.TokenStorageIssuer;
import eu.abc4trust.guice.ProductionModule;
import eu.abc4trust.guice.ProductionModuleFactory;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.guice.configuration.AbceConfigurationImpl;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.xml.SystemParameters;

import ch.zhaw.ficore.p2abc.services.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.StorageModuleFactory;

public class IssuanceHelper {
	private static Logger logger = LogManager.getLogger(IssuanceHelper.class.getName());
	private static IssuanceHelper instance = null;
	
	private CryptoEngine cryptoEngine;
	private ServicesConfiguration serviceConfig;
	private final List<TokenStorageIssuer> issuerStorageManagerList = new ArrayList<TokenStorageIssuer>();
	private Random random;
	private KeyManager keyManager;
	private IssuerAbcEngine singleEngine;
	
	public static synchronized IssuanceHelper initInstanceForService(
			CryptoEngine cryptoEngine, ServicesConfiguration config) throws Exception {
		
		logger.entry();
		
        if (instance != null) {
            throw logger.throwing(new IllegalStateException(
                    "initInstance can only be called once!"));
        }

        instance = new IssuanceHelper(cryptoEngine, config);

        return logger.exit(instance);
    }
	
	private IssuanceHelper(CryptoEngine cryptoEngine, ServicesConfiguration config)
                    throws Exception {
        logger.info("IssuanceHelper : create instance for issuer service "
                + cryptoEngine);
        
        this.cryptoEngine = cryptoEngine;
        this.serviceConfig = config;

        this.setupSingleEngineForService(
                cryptoEngine, StorageModuleFactory.getModulesForServiceConfiguration(config));
    }
	
	private void setupSingleEngineForService(
            CryptoEngine cryptoEngine,
            Module... modules)
                    throws Exception {

        AbceConfigurationImpl configuration = new AbceConfigurationImpl();

        Module newModule = ProductionModuleFactory
                .newModule(configuration, cryptoEngine);
        Module combinedModule = Modules.override(newModule).with(modules);
        Injector injector = Guice.createInjector(combinedModule);

        IssuerAbcEngine engine = injector.getInstance(IssuerAbcEngine.class);

        this.singleEngine = new SynchronizedIssuerAbcEngineImpl(engine);

        this.keyManager = injector.getInstance(KeyManager.class);
        
        SystemParameters systemParameters = keyManager.getSystemParameters();

        if (systemParameters == null) {
            logger.warn("No system parameters loaded");
        }

        this.issuerStorageManagerList.add(injector
                .getInstance(TokenStorageIssuer.class));
        
        this.random = configuration.getPrng();
    }
}
