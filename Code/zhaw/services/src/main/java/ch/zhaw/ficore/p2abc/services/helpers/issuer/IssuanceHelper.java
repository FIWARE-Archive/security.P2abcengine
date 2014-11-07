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

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.issuance.IssuanceStorage;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import eu.abc4trust.abce.external.issuer.IssuerAbcEngine;
import eu.abc4trust.abce.external.issuer.SynchronizedIssuerAbcEngineImpl;
import eu.abc4trust.abce.internal.issuer.credentialManager.CredentialManager;
import eu.abc4trust.abce.internal.issuer.tokenManagerIssuer.TokenStorageIssuer;
import eu.abc4trust.cryptoEngine.idemix.user.IdemixCryptoEngineUserImpl;
import eu.abc4trust.cryptoEngine.uprove.util.UProveUtils;
import eu.abc4trust.cryptoEngine.util.SystemParametersUtil;
import eu.abc4trust.guice.ProductionModule;
import eu.abc4trust.guice.ProductionModuleFactory;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.ri.servicehelper.AbstractHelper;
//import eu.abc4trust.ri.servicehelper.FileSystem;
import eu.abc4trust.ri.servicehelper.SystemParametersHelper;
import eu.abc4trust.xml.Attribute;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuanceLogEntry;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuancePolicy;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.SecretKey;
import eu.abc4trust.xml.SystemParameters;

@SuppressWarnings("deprecation")
public class IssuanceHelper extends AbstractHelper {

    static Logger log = LogManager.getLogger();

    private static IssuanceHelper instance;
    
    public IssuanceStorage issuanceStorage;
   
    public static synchronized IssuanceHelper initInstanceForService(
            CryptoEngine cryptoEngine, String systemAndIssuerParamsPrefix,
            String fileStoragePrefix, Module... modules) throws Exception {
        if (instance != null) {
            throw new IllegalStateException(
                    "initInstance can only be called once!");
        }
        log.info("IssuanceHelper.initInstanceForService(Array)");

        instance = new IssuanceHelper(cryptoEngine,
                systemAndIssuerParamsPrefix, fileStoragePrefix, new String[0],
                modules);

        return instance;
    }

    

    /**
     * Private constructor
     * 
     * @param fileStoragePrefix
     *            this prefix will be prepended on storage files needed by the
     *            IssuerAbcEnginge
     * @throws Exception
     */
    private IssuanceHelper(CryptoEngine cryptoEngine, String systemAndIssuerParamsPrefix,
            String fileStoragePrefix,
            String[] revocationAuthorityParametersResourcesList,
            Module... modules)
                    throws Exception {
        IssuanceHelper.log
        .info("IssuanceHelper : create instance for issuer service "
                + cryptoEngine + " : " + fileStoragePrefix);
        this.cryptoEngine = cryptoEngine;
        this.systemAndIssuerParamsPrefix = systemAndIssuerParamsPrefix;
        this.fileStoragePrefix = fileStoragePrefix;
        this.systemParametersResource = this.fileStoragePrefix
                + SYSTEM_PARAMS_NAME_BRIDGED;

        UProveUtils uproveUtils = new UProveUtils();
        this.setupSingleEngineForService(
                cryptoEngine, uproveUtils,
                revocationAuthorityParametersResourcesList, modules);

        this.random = new SecureRandom();
    }


    /**
     * @return true if IssuanceHelper has been initialized
     */
    public static synchronized boolean isInit() {
        return instance != null;
    }


    /**
     * @return initialized instance of IssuanceHelper
     */
    public static synchronized IssuanceHelper getInstance() {
        log.info("IssuanceHelper.getInstance : " + instance
                + (instance == null ? "" : " : " + instance.cryptoEngine));
        if (instance == null) {
            throw new IllegalStateException("getInstance not called before using IssuanceHelper!");
        }
        return instance;
    }

    private IssuerAbcEngine singleEngine = null;
    private IssuerAbcEngine uproveEngine = null;
    private IssuerAbcEngine idemixEngine = null;


    private final Map<String, SpecAndPolicy> specAndPolicyMap = new HashMap<String, SpecAndPolicy>();

    private Random random;

    private final ObjectFactory of = new ObjectFactory();

    private final List<TokenStorageIssuer> issuerStorageManagerList = new ArrayList<TokenStorageIssuer>();
    private KeyManager uproveKeyManager;
    private KeyManager idemixKeyManager;

    private final String systemAndIssuerParamsPrefix;
    private final String fileStoragePrefix;
    private final String systemParametersResource;

    private CredentialManager credentialManager;


    private void setupSingleEngineForService(
            CryptoEngine cryptoEngine, UProveUtils uproveUtils,
            String[] revocationAuthorityParametersResourcesList,
            Module... modules)
                    throws Exception {


        Module newModule = ProductionModuleFactory
                .newModule(cryptoEngine); 
        Module combinedModule = Modules.override(newModule).with(modules);
        Injector injector = Guice.createInjector(combinedModule);

        IssuerAbcEngine engine = injector.getInstance(IssuerAbcEngine.class);

        this.singleEngine = new SynchronizedIssuerAbcEngineImpl(engine);

        if (cryptoEngine == CryptoEngine.UPROVE) {
            throw new RuntimeException("We only support Idemix. Sorry :(");
        }

        this.keyManager = injector.getInstance(KeyManager.class);
        this.issuanceStorage = injector.getInstance(IssuanceStorage.class);

        String systemParametersResource = this.fileStoragePrefix
                + SYSTEM_PARAMS_NAME_BRIDGED;

        SystemParameters systemParameters = SystemParametersHelper
                .checkAndLoadSystemParametersIfAbsent(this.keyManager,
                        systemParametersResource);

        if (systemParameters == null) {
            IssuanceHelper.log.info("No system parameters loaded");
        }

        this.issuerStorageManagerList.add(injector
                .getInstance(TokenStorageIssuer.class));
        this.addRevocationAuthorities(this.keyManager,
                revocationAuthorityParametersResourcesList);
    }

    private SystemParameters generatedSystemParameters = null;


    public SystemParameters createNewSystemParametersWithIdemixSpecificKeylength(
            int idemixKeylength, int uproveKeylength) throws IOException,
            KeyManagerException, Exception {

        return this.createNewSystemParametersWithIdemixSpecificKeylength(
                idemixKeylength, uproveKeylength, this.keyManager);

    }

    private SystemParameters createNewSystemParametersWithIdemixSpecificKeylength(
            int idemixKeylength, int uproveKeylength, KeyManager keyManager)
                    throws IOException,
                    KeyManagerException, Exception {
        IssuanceHelper.log.info("- create new system parameters with keysize: "
                + idemixKeylength);
        // ok - we have to generate them from scratch...
        this.generatedSystemParameters = SystemParametersUtil
                .generatePilotSystemParameters_WithIdemixSpecificKeySize(
                        idemixKeylength, uproveKeylength, UPROVE_ISSUER_NUMBER_OF_CREDENTIAL_TOKENS_TO_GENERATE);


        // store in keyManager
        keyManager.storeSystemParameters(this.generatedSystemParameters);

        IdemixCryptoEngineUserImpl
        .loadIdemixSystemParameters(this.generatedSystemParameters);

        IssuanceHelper.log.info("- new SystemParameters.");

        return this.generatedSystemParameters;
    }

    private IssuerParameters setupAndStoreIssuerParameters(
            CryptoEngine cryptoEngine,
            IssuerAbcEngine initEngine,
            KeyManager keyManager, CredentialManager credentialManager,
            String systemAndIssuerParamsPrefix,
            SystemParameters systemParameters,
            CredentialSpecification credSpec, URI hash, URI issuerParamsUid,
            URI revocationParamsUid, List<FriendlyDescription> friendlyDescriptions) throws Exception {
        IssuerParameters issuerParameters;
        IssuanceHelper.log.info(" - create Issuer Parameters!");

        issuerParameters = this.setupIssuerParameters(cryptoEngine, initEngine,
                systemParameters, credSpec, hash, issuerParamsUid,
                revocationParamsUid, friendlyDescriptions);

        IssuanceHelper.log.info(" - store Issuer Parameters! "
                + issuerParamsUid + " : " + issuerParameters
                + " - with version number : " + issuerParameters.getVersion());

        keyManager.storeIssuerParameters(issuerParamsUid, issuerParameters);
        boolean urnScheme = "urn".equals(issuerParamsUid.getScheme());
      


        if (credentialManager != null) {
            SecretKey issuerPrivateKeyForIssuerParameters =
                    credentialManager.getIssuerSecretKey(issuerParamsUid);

          
        }

        IssuanceHelper.log.info(" - created issuerParameters with UID : "
                + issuerParameters.getParametersUID());

        return issuerParameters;
    }

    private IssuerParameters setupIssuerParameters(CryptoEngine cryptoEngine,
            IssuerAbcEngine initEngine, SystemParameters systemParameters,
            CredentialSpecification credSpec, URI hash, URI issuerParamsUid,
            URI revocationParamsUid, List<FriendlyDescription> friendlyDescriptions) {
        IssuerParameters issuerParameters;
        issuerParameters =
                initEngine.setupIssuerParameters(credSpec, systemParameters, issuerParamsUid, hash,
                        /*URI.create("Idemix")*/URI.create(this.cryptoEngine.toString()), revocationParamsUid, friendlyDescriptions);
        return issuerParameters;
    }

    public IssuerParameters setupIssuerParameters(CryptoEngine cryptoEngine,
            CredentialSpecification credSpec,
            SystemParameters systemParameters, URI issuerParamsUid, URI hash,
            URI revocationParamsUid, String systemAndIssuerParamsPrefix,
            List<FriendlyDescription> friendlyDescriptions) throws Exception {
        IssuerAbcEngine engine;
        IssuerParameters issuerParameters = null;
        IssuanceHelper.log.info("cryptoEngine: " + cryptoEngine);
        switch (cryptoEngine) {
        case IDEMIX:
            engine = this.idemixEngine;
            KeyManager akeyManager = this.idemixKeyManager;
            if (this.idemixEngine == null) {
                engine = this.singleEngine;
            }
            if (this.idemixKeyManager == null) {
                akeyManager = this.keyManager;
            }
            issuerParameters = this.setupAndStoreIssuerParameters(cryptoEngine,
                    engine, akeyManager, this.credentialManager,
                    systemAndIssuerParamsPrefix,
                    systemParameters, credSpec, hash, issuerParamsUid,
                    revocationParamsUid, friendlyDescriptions);
            break;
        case UPROVE:
            engine = this.uproveEngine;
            if (this.uproveEngine == null) {
                engine = this.singleEngine;
            }
            akeyManager = this.uproveKeyManager;
            if (this.idemixEngine == null) {
                engine = this.singleEngine;
            }
            if (this.uproveKeyManager == null) {
                akeyManager = this.keyManager;
            }
            issuerParameters = this.setupAndStoreIssuerParameters(cryptoEngine,
                    engine, akeyManager, this.credentialManager,
                    systemAndIssuerParamsPrefix,
                    systemParameters, credSpec, hash, issuerParamsUid,
                    revocationParamsUid, friendlyDescriptions);

            break;
        default:
            throw new IllegalStateException("The crypto engine: "
                    + cryptoEngine
                    + " is not supported use IDEMIX or UPROVE instead");
        }

        return issuerParameters;
    }


    /**
     * Process next step of issuance on IssuanceMessager
     * 
     * @param issuanceMessage IssuanceMessager as String
     * @return
     * @throws Exception
     */
    public IssuanceMessageAndBoolean issueStep(IssuanceMessage issuanceMessage) throws Exception {
        IssuanceHelper.log.info("IssuanceHelper - step_jaxb - marchalled object: "
                + issuanceMessage);

        if(this.singleEngine==null) {
            throw new IllegalStateException("IssuanceHelper.issueStep called without specifying CryptoEngine!");
        }
        return this.issueStep(this.singleEngine, issuanceMessage);
    }

    public IssuanceMessageAndBoolean issueStep(ProductionModule.CryptoEngine cryptoEngine, IssuanceMessage issuanceMessage) throws Exception {
        return this.issueStep(oldCryptoEngineToNewCryptoEngine(cryptoEngine), issuanceMessage);
    }
    /**
     * Process next step of issuance on IssuanceMessager
     * 
     * @param issuanceMessage IssuanceMessager as String
     * @return
     * @throws Exception
     */
    public IssuanceMessageAndBoolean issueStep(CryptoEngine cryptoEngine, IssuanceMessage issuanceMessage) throws Exception {
        IssuanceHelper.log.info("IssuanceHelper - step_jaxb - marchalled object: "
                + issuanceMessage);

        IssuerAbcEngine useEngine;
        if(this.singleEngine!=null) {
            if(this.cryptoEngine != cryptoEngine) {
                throw new IllegalStateException("IssuanceHelper.issueStep called specifying CryptoEngine - but not initialized using BRIDGED! - running " + this.cryptoEngine + " - requesting " + cryptoEngine);
            } else {
                useEngine = this.singleEngine;
            }
        } else {
            if(cryptoEngine == CryptoEngine.IDEMIX) {
                useEngine = this.idemixEngine;
            } else if(cryptoEngine == CryptoEngine.UPROVE) {
                useEngine = this.uproveEngine;
            } else {
                throw new IllegalStateException("IssuanceHelper.issueStep : idemix/uprove engine not initialized...");
            }
        }
        return this.issueStep(useEngine, issuanceMessage);
    }


    private IssuanceMessageAndBoolean issueStep(IssuerAbcEngine useEngine, IssuanceMessage issuanceMessage) throws Exception {

        IssuanceMessageAndBoolean response;
        try {
            response = useEngine.issuanceProtocolStep(issuanceMessage);
        } catch (Exception e) {
            System.err.println("- IssuerABCE could not process Step IssuanceMessage from UserABCE : " + e);

            throw new Exception("Could not process next step on issuauce : ", e);
        }
        if (response.isLastMessage()) {
            IssuanceHelper.log.info(" - last step - on server");
        } else {
            IssuanceHelper.log.info(" - continue steps");
        }

        // String xml = XmlUtils.toXml(of.createIssuanceMessage(response.im), false);
        // this.log.info("IssuanceService - step - return  XML : " + xml);

        return response;
    }

    private IssuanceMessageAndBoolean initIssuanceProtocol(
            IssuerAbcEngine useEngine,
            List<Attribute> issuerAtts, IssuancePolicy clonedIssuancePolicy,
            URI policyIssuerParametersUID) throws Exception {
        IssuanceMessageAndBoolean response = null;
        try {


            IssuanceHelper.log.info(" - call ABCE - policy : " + clonedIssuancePolicy
                    + " : " + policyIssuerParametersUID
                    + " - attributes : " + issuerAtts);
            response = useEngine.initIssuanceProtocol(clonedIssuancePolicy, issuerAtts);

        } catch (Exception e) {
            System.err.println("- got Exception from ABCE Engine - try to create sample XML");
            e.printStackTrace();
            throw new Exception("Failed to initIsuanceProtocol", e);
        }

        if (response.isLastMessage()) {
            // cannot be last message
            throw new IllegalStateException(
                    "Internal error in IssuerABCEngine - lastmessage returned from initIssuanceProtocol");
        }

        return response;
    }


    private void populateIssuerAttributes(SpecAndPolicy specAndPolicy,
            List<Attribute> issuerAtts, Map<String, Object> attributeValueMap) {

        CredentialSpecification credSpec = specAndPolicy.getCredentialSpecification();

        // TODO - to make proper check - also check 'unknown' from IssuancePolicy
        // eg findUnknownAttributtes(ip);
        // and check that all attribues in credspecs are matched..
        if (credSpec.getAttributeDescriptions().getAttributeDescription().size() < attributeValueMap
                .size()) {
            throw new IllegalStateException("Wrong number of attributes ? - in credspec : "
                    + credSpec.getAttributeDescriptions().getAttributeDescription().size() + " - in map "
                    + attributeValueMap.size());
        }


        Map<String, AttributeDescription> adMap = new HashMap<String, AttributeDescription>();
        for (AttributeDescription ad : credSpec.getAttributeDescriptions().getAttributeDescription()) {
            adMap.put(ad.getType().toString(), ad);
        }

        Set<String> definedAttributes = attributeValueMap.keySet();
        for (String key : definedAttributes) {
            AttributeDescription ad = adMap.get(key);
            if (ad == null) {
                throw new IllegalStateException("No Attribute in Credspec with type : " + key);
            }

            Object value = attributeValueMap.get(key);

            Attribute attribute = this.of.createAttribute();
            // TODO : Howto create vaules ??
            attribute.setAttributeUID(URI.create("" + this.random.nextLong()));

            Object xmlValue = new AttributeValueConverter().convertValue(ad
                    .getDataType().toString(), value);
  
            attribute.setAttributeValue(xmlValue);
            attribute.setAttributeDescription(this.of.createAttributeDescription());
            attribute.getAttributeDescription().setDataType(URI.create(ad.getDataType().toString()));
            attribute.getAttributeDescription().setEncoding(URI.create(ad.getEncoding().toString()));
            attribute.getAttributeDescription().setType(URI.create(ad.getType().toString()));

            // TODO:SETTING Friendly's should be handled inside User Engine!!!
            attribute.getAttributeDescription().getFriendlyAttributeName()
            .addAll(ad.getFriendlyAttributeName());

            //
            issuerAtts.add(attribute);
        }
    }

    public IssuanceMessageAndBoolean initIssuanceProtocol(
            IssuancePolicy issuancePolicy, List<Attribute> attributes)
                    throws Exception {
        IssuerAbcEngine engine;
        IssuanceMessageAndBoolean issuanceMessageAndBoolean = null;
        this.validateIssuancePolicy(issuancePolicy);

        URI issuerPolicyParametersUid = issuancePolicy.getCredentialTemplate()
                .getIssuerParametersUID();

        boolean urnScheme = "urn".equals(issuerPolicyParametersUid.getScheme());
        issuerPolicyParametersUid = URI.create(issuerPolicyParametersUid
                + ((urnScheme ? ":" : "/") + this.cryptoEngine).toLowerCase());

        switch (this.cryptoEngine) {
        case IDEMIX:
            engine = this.idemixEngine;
            if (this.idemixEngine == null) {
                engine = this.singleEngine;
            }
            issuanceMessageAndBoolean = this.initIssuanceProtocol(engine,
                    attributes, issuancePolicy, issuerPolicyParametersUid);
            break;
        case UPROVE:
            engine = this.idemixEngine;
            if (this.idemixEngine == null) {
                engine = this.singleEngine;
            }
            issuanceMessageAndBoolean = this.initIssuanceProtocol(engine,
                    attributes, issuancePolicy, issuerPolicyParametersUid);

            break;
        default:
            throw new IllegalStateException("The crypto engine: "
                    + this.cryptoEngine
                    + " is not supported use IDEMIX or UPROVE instead");
        }
        return issuanceMessageAndBoolean;
    }

    private void validateIssuancePolicy(IssuancePolicy issuancePolicy) {
        if (issuancePolicy.getCredentialTemplate() == null) {
            throw new RuntimeException("Credential template should be present");
        }

        if (issuancePolicy.getPresentationPolicy() == null) {
            throw new RuntimeException("Presentation policy should be present");
        }

        if (issuancePolicy.getPresentationPolicy().getMessage() == null) {
            throw new RuntimeException(
                    "Presentation policy message should be present");
        }

    }

    public IssuanceLogEntry getIssuanceLogEntry(CryptoEngine engine,
            URI issuanceEntryUid) throws Exception {
        return this.singleEngine.getIssuanceLogEntry(issuanceEntryUid);
    }
}