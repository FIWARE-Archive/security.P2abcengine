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

//This is a copy of the original UserHelper in the tree Code/core-abce/service-helpers

package ch.zhaw.ficore.p2abc.services.helpers.user;

import java.net.URISyntaxException;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import eu.abc4trust.abce.external.user.SynchronizedUserAbcEngineImpl;
import eu.abc4trust.abce.external.user.UserAbcEngine;
import eu.abc4trust.abce.internal.user.credentialManager.CredentialManager;
import eu.abc4trust.cryptoEngine.uprove.user.ReloadTokensCommunicationStrategy;
import eu.abc4trust.guice.ProductionModuleFactory;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.keyManager.KeyStorage;
import eu.abc4trust.ri.servicehelper.AbstractHelper;
import eu.abc4trust.smartcard.CardStorage;
import eu.abc4trust.smartcardManager.AbcSmartcardManager;

public class UserHelper extends AbstractHelper {

    private static final XLogger logger = new XLogger(
            LoggerFactory.getLogger(UserHelper.class));

    public ReloadTokensCommunicationStrategy reloadTokens = null;
    static UserHelper instance;
    public KeyStorage keyStorage;

    public static synchronized UserHelper initInstanceForService(
            final CryptoEngine cryptoEngine, final String fileStoragePrefix,
            final Module... modules) throws URISyntaxException {

        initializeInstanceField(cryptoEngine, fileStoragePrefix, modules);

        logger.info("UserHelper.initInstance : DONE");

        return instance;
    }

    public static synchronized void loadSystemParameters() {
        instance.checkIfSystemParametersAreLoaded();
    }

    private static synchronized void initializeInstanceField(
            final CryptoEngine cryptoEngine, final String fileStoragePrefix,
            final Module... modules) throws URISyntaxException {
        if (instance != null) {
            throw new IllegalStateException(
                    "initInstance can only be called once!");
        }
        logger.info("UserHelper.initInstance");
        instance = new UserHelper(cryptoEngine, fileStoragePrefix, modules);
    }

    public static synchronized boolean isInit() {
        return instance != null;
    }

    public static synchronized UserHelper getInstance()
            throws KeyManagerException {
        if (instance == null) {
            throw new IllegalStateException(
                    "initInstance not called before using UserHelper!");
        }
        return instance;
    }

    private UserAbcEngine engine;
    public AbcSmartcardManager smartcardManager;
    public CardStorage cardStorage;
    public CredentialManager credentialManager;

    private UserHelper(final CryptoEngine cryptoEngine,
            final String fileStoragePrefix, final Module... modules)
            throws URISyntaxException {
        logger.info("UserHelper : : create instance " + cryptoEngine + " : "
                + fileStoragePrefix);
        this.cryptoEngine = cryptoEngine;
        try {
            Module m = ProductionModuleFactory.newModule(cryptoEngine);
            Module combinedModule = Modules.override(m).with(modules);
            Injector injector = Guice.createInjector(combinedModule);

            this.keyManager = injector.getInstance(KeyManager.class);
            this.keyStorage = injector.getInstance(KeyStorage.class);
            this.credentialManager = injector
                    .getInstance(CredentialManager.class);
            this.smartcardManager = injector
                    .getInstance(AbcSmartcardManager.class);
            this.cardStorage = injector.getInstance(CardStorage.class);
            this.reloadTokens = injector
                    .getInstance(ReloadTokensCommunicationStrategy.class);
            //
            UserAbcEngine e = injector.getInstance(UserAbcEngine.class);
            this.engine = new SynchronizedUserAbcEngineImpl(e);

            if ((cryptoEngine == CryptoEngine.UPROVE)
                    || (cryptoEngine == CryptoEngine.BRIDGED)) {
                throw new RuntimeException("We only support Idemix. Sorry :(");
            }

        } catch (Exception e) {
            logger.catching(e);
            System.err.println("Init Failed");
            e.printStackTrace();
            throw new IllegalStateException("Could not setup user !", e);
        }
    }

    public UserAbcEngine getEngine() {
        return this.engine;
    }

}