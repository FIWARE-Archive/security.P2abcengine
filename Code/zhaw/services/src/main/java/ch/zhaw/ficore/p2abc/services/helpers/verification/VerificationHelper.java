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

package ch.zhaw.ficore.p2abc.services.helpers.verification;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.bind.JAXBElement;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import eu.abc4trust.abce.external.verifier.SynchronizedVerifierAbcEngineImpl;
import eu.abc4trust.abce.external.verifier.VerifierAbcEngine;
import eu.abc4trust.exceptions.TokenVerificationException;
import eu.abc4trust.guice.ProductionModuleFactory;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.ri.servicehelper.AbstractHelper;
import eu.abc4trust.xml.ApplicationData;
import eu.abc4trust.xml.CredentialInPolicy;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives.IssuerParametersUID;
import eu.abc4trust.xml.CredentialInToken;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.Message;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicy;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.RevocationInformation;
import eu.abc4trust.xml.util.XmlUtils;

/**
 * @author hgk
 * 
 */
public class VerificationHelper extends AbstractHelper {

    private static VerificationHelper instance;

    public static synchronized VerificationHelper initInstance(
            CryptoEngine cryptoEngine, Module[] modules,
            String... presentationPolicyResourceList) throws URISyntaxException {
        if (instance != null) {
            throw new IllegalStateException(
                    "initInstance can only be called once!");
        }
        System.out.println("VerificationHelper.initInstance ([])");
        instance = new VerificationHelper(cryptoEngine, modules);

        System.out.println("VerificationHelper.initInstance : DONE");

        return instance;
    }

    /**
     * @return true if VerificationHelper has been initialized
     */
    public static synchronized boolean isInit() {
        return instance != null;
    }

    /**
     * @return initialized instance of VerificationHelper
     */
    public static synchronized VerificationHelper getInstance() {
        System.out.println("VerificationHelper.getInstance : " + instance);
        if (instance == null) {
            throw new IllegalStateException(
                    "initInstance not called before using VerificationHelper!");
        }
        return instance;
    }

    public VerifierAbcEngine engine;
    private Random random;


    /**
     * holds map resources by filename (without path) and the bytes of resource
     */
    private final Map<String, byte[]> policyResouceMap = new HashMap<String, byte[]>();
    private final ObjectFactory of = new ObjectFactory();

    /**
     * Used when creating a presentationPolicy to be later fetched when
     * verifying a presentation token.
     */
    Map<URI, RevocationInformation> revocationInformationStore = new HashMap<URI, RevocationInformation>();
    private byte[] nonce;

    /**
     * Private constructor - initializes ABCE
     * 
     * @param fileStoragePrefix
     * @throws URISyntaxException
     */
    private VerificationHelper(CryptoEngine cryptoEngine, Module... modules)
            throws URISyntaxException {
        System.out.println("VerificationHelper : create instance "
                + cryptoEngine);
        this.cryptoEngine = cryptoEngine;
        try {

            Module newModule = ProductionModuleFactory.newModule(cryptoEngine);
            Module combinedModule = Modules.override(newModule).with(modules);
            Injector injector = Guice.createInjector(combinedModule);

            VerifierAbcEngine e = injector.getInstance(VerifierAbcEngine.class);

            this.engine = new SynchronizedVerifierAbcEngineImpl(e);
            this.keyManager = injector.getInstance(KeyManager.class);

            this.random = injector.getInstance(Random.class);


            if ((cryptoEngine == CryptoEngine.UPROVE)
                    || (cryptoEngine == CryptoEngine.BRIDGED)) {
                throw new RuntimeException("We only support Idemix. Sorry :(");
            }

        } catch (Exception e) {
            System.err.println("Init Failed");
            e.printStackTrace();
            throw new IllegalStateException("Could not setup Verifier !", e);
        }
    }

    /**
     * Adds extra policy resorces to VerificationHelper
     * 
     * @param applicationData
     * @param revInfoUIDs
     * 
     * @param presentationPolicyResourceList
     * @return
     * @throws Exception
     */
    public PresentationPolicyAlternatives createPresentationPolicy(
            PresentationPolicyAlternatives presentationPolicyAlternatives,
            String applicationData, Map<URI, URI> revInfoUIDs) throws Exception {

        this.nonce = this.generateNonce();

        PresentationPolicyAlternatives modifiedPresentationPolicy = this
                .modifyPPA(presentationPolicyAlternatives, applicationData,
                        this.nonce, revInfoUIDs);
        return modifiedPresentationPolicy;
    }

    /**
     * @param policyName
     *            name of policy resource (without path)
     * @param applicationData
     *            if present - will be inserted on all presentation policies
     * @return PresentationPolicyAlternatives - patched with applicationData -
     *         marshaled to String
     * @throws Exception
     */
    public String createPresentationPolicy_String(String policyName,
            byte[] nonce, String applicationData) throws Exception {
        System.out.println("VerificationHelper - create policy String : "
                + policyName + " - data : " + applicationData);

        PresentationPolicyAlternatives pp_alternatives = this
                .createPresentationPolicy(policyName, nonce, applicationData,
                        null);
        JAXBElement<PresentationPolicyAlternatives> result = this.of
                .createPresentationPolicyAlternatives(pp_alternatives);

        String xml = XmlUtils.toXml(result, false);

        System.out.println(" - createPolicy - return  XML");

        return xml;

    }

    public PresentationPolicyAlternatives modifyPresentationPolicy(
            PresentationPolicyAlternatives ppa, byte[] nonce,
            String applicationData, Map<URI, URI> revInfoUIDs) throws Exception {
        this.modifyPPA(ppa, applicationData, nonce, revInfoUIDs);
        return ppa;
    }

    /**
     * @param policyName
     *            name of policy resource (without path)
     * @param applicationData
     *            if present - will be inserted on all presentation policies
     * @param revInfoUIDs
     *            if present - will try to fetch revocation information based on
     *            the uids.
     * @return PresentationPolicyAlternatives - patched with applicationData
     * @throws Exception
     */
    public PresentationPolicyAlternatives createPresentationPolicy(
            String policyName, byte[] nonce, String applicationData,
            Map<URI, URI> revInfoUIDs) throws Exception {
        System.out.println("VerificationHelper - create policy : " + policyName
                + " - data : " + applicationData);

        PresentationPolicyAlternatives pp_alternatives;
        byte[] policyBytes = this.policyResouceMap.get(policyName);
        if (policyBytes == null) {
            System.err
                    .println(" - policy not found : " + this.policyResouceMap);
            System.err.println(" - policy not found : "
                    + this.policyResouceMap.keySet());
            throw new IllegalStateException("PresentationPolicy not found : "
                    + policyName);
        }
        try {
            pp_alternatives = (PresentationPolicyAlternatives) XmlUtils
                    .getObjectFromXML(new ByteArrayInputStream(policyBytes),
                            false);
        } catch (Exception e) {
            System.err.println(" - could init sample XML - create default");
            e.printStackTrace();
            throw new IllegalStateException(
                    "Could not init PresentationPolicy - event though it should have been verifed...");
        }

        this.modifyPPA(pp_alternatives, applicationData, nonce, revInfoUIDs);

        return pp_alternatives;
    }

    public static boolean cacheRevocationInformation = false;
    public static long REVOCATION_INFORMATION_MAX_TIME_TO_EXPIRE_IN_MINUTTES = 60;
    private static Map<URI, RevocationInformation> revocationInformationCache = new HashMap<URI, RevocationInformation>();

    private PresentationPolicyAlternatives modifyPPA(
            PresentationPolicyAlternatives pp_alternatives,
            String applicationData, byte[] nonce, Map<URI, URI> revInfoUIDs)
            throws Exception {

        System.out.println("Modify PPA... ");

        // try to make sure that RevocationInformation is only fetch once per
        // RevAuth
        Map<URI, RevocationInformation> revocationInformationMap = new HashMap<URI, RevocationInformation>();

        for (PresentationPolicy pp : pp_alternatives.getPresentationPolicy()) {

            Message message = pp.getMessage();
            // set nonce
            message.setNonce(nonce);

            // set application data
            if (applicationData != null) {

                System.out.println("- SET APPLICATION DATA : "
                        + applicationData);
                // message.setApplicationData(applicationData.getBytes());
                ApplicationData a = message.getApplicationData();
                a.getContent().clear();
                a.getContent().add(applicationData);
            }

            // REVOCATION!
            for (CredentialInPolicy cred : pp.getCredential()) {
                System.out.println("- check Credential In Policy - alias : "
                        + cred.getAlias());
                List<URI> credSpecURIList = cred
                        .getCredentialSpecAlternatives().getCredentialSpecUID();
                boolean containsRevoceableCredential = false;
                CredentialSpecification credSpec = null;
                for (URI uri : credSpecURIList) {
                    try {
                        credSpec = this.keyManager
                                .getCredentialSpecification(uri);
                        if (credSpec.isRevocable()) {
                            System.out
                                    .println(" - credentialSpecification is Revocable : "
                                            + uri);
                            containsRevoceableCredential = true;
                            break;
                        } else {
                            System.out
                                    .println(" - credentialSpecification is NOT Revocable : "
                                            + uri);
                        }
                    } catch (KeyManagerException ignore) {
                        System.out.println(" - ERROR credspec not registed : "
                                + uri);
                    }
                }
                if (containsRevoceableCredential) {
                    IssuerAlternatives ia = cred.getIssuerAlternatives();
                    for (IssuerParametersUID ipUid : ia
                            .getIssuerParametersUID()) {
                        System.out.println(" - check issuer parameter : "
                                + ipUid.getValue() + " : "
                                + ipUid.getRevocationInformationUID());
                        IssuerParameters ip = this.keyManager
                                .getIssuerParameters(ipUid.getValue());
                        if ((ip != null)
                                && (ip.getRevocationParametersUID() != null)) {
                            // issuer params / credspec has revocation...
                            RevocationInformation ri;
                            if (revInfoUIDs != null) {
                                System.out
                                        .println("Trying to get revInfo under "
                                                + credSpec
                                                        .getSpecificationUID());
                                URI revInformationUid = revInfoUIDs
                                        .get(credSpec.getSpecificationUID());
                                ri = this.revocationInformationStore
                                        .get(revInformationUid);
                                if (ri != null) {
                                    System.out.println("Got revInfo: "
                                            + ri.getInformationUID()
                                            + ", which should be the same as: "
                                            + revInformationUid);
                                } else {
                                    System.out
                                            .println("Revocation information is not there");
                                }
                            } else {
                                ri = revocationInformationMap.get(ip
                                        .getRevocationParametersUID());
                                if (ri != null) {
                                    System.out
                                            .println(" - revocationInformation found in (reuse) map");
                                }
                            }
                            if ((ri == null) && cacheRevocationInformation) {
                                ri = revocationInformationCache.get(ip
                                        .getRevocationParametersUID());
                                if (ri != null) {
                                    Calendar now = Calendar.getInstance();
                                    if (now.getTimeInMillis() > ri.getExpires()
                                            .getTimeInMillis()) {
                                        System.out
                                                .println(" - revocationInformation has expired! - now : "
                                                        + now.getTime()
                                                        + " - created : "
                                                        + ri.getCreated()
                                                                .getTime()
                                                        + " : - expires : "
                                                        + ri.getExpires()
                                                                .getTime());
                                    } else if (now.getTimeInMillis() > (ri
                                            .getExpires().getTimeInMillis() - (REVOCATION_INFORMATION_MAX_TIME_TO_EXPIRE_IN_MINUTTES * 60 * 1000))) {
                                        long millis_to_expiration = (ri
                                                .getExpires().getTimeInMillis() - (REVOCATION_INFORMATION_MAX_TIME_TO_EXPIRE_IN_MINUTTES * 60 * 1000))
                                                - now.getTimeInMillis();
                                        System.out
                                                .println(" - revocationInformation was invalidated ! - now : "
                                                        + now.getTime()
                                                        + " - created : "
                                                        + ri.getCreated()
                                                                .getTime()
                                                        + " : - expires : "
                                                        + ri.getExpires()
                                                                .getTime()
                                                        + " MILLIS TO EXPIRE "
                                                        + millis_to_expiration);
                                        ri = null;
                                    } else {
                                        long millis_to_expiration = (ri
                                                .getExpires().getTimeInMillis() - (REVOCATION_INFORMATION_MAX_TIME_TO_EXPIRE_IN_MINUTTES * 60 * 1000))
                                                - now.getTimeInMillis();
                                        System.out
                                                .println(" - valid revocationInformation found in Cache - now : "
                                                        + now.getTime()
                                                        + " - created : "
                                                        + ri.getCreated()
                                                                .getTime()
                                                        + " : - expires : "
                                                        + ri.getExpires()
                                                                .getTime()
                                                        + " MILLES TO EXPIRE : "
                                                        + millis_to_expiration);
                                        revocationInformationMap
                                                .put(ip.getRevocationParametersUID(),
                                                        ri);
                                    }
                                }
                            }
                            if (ri == null) {
                                System.out
                                        .println(" - get latest information from RevocationAuthority - uid : "
                                                + ip.getRevocationParametersUID());
                                ri = this.keyManager
                                        .getLatestRevocationInformation(ip
                                                .getRevocationParametersUID());
                                revocationInformationMap.put(
                                        ip.getRevocationParametersUID(), ri);
                                this.revocationInformationStore.put(
                                        ri.getInformationUID(), ri);
                                if (cacheRevocationInformation) {
                                    System.out
                                            .println(" - storege RevocationInformation in cache : "
                                                    + ip.getRevocationParametersUID());
                                    revocationInformationCache
                                            .put(ip.getRevocationParametersUID(),
                                                    ri);
                                }
                            }
                            URI revInfoUid = ri.getInformationUID();
                            ipUid.setRevocationInformationUID(revInfoUid);
                        }
                    }
                }
            }

        }
        System.out.println(" - presentationPolicy created");

        return pp_alternatives;
    }

    /**
     * TODO : Remove when ABCE-Components can properly compare XML Private
     * Helper
     * 
     * @param orig
     *            XML as string
     * @return patched XML as JaxB
     * @throws Exception
     */
    private PresentationToken getPatchedPresetationToken(String orig)
            throws Exception {
        /**
         * FIXME: What the hell is this doing and what the hell is it doing it
         * for? -- munt
         */
        String patched = orig
                .replace(
                        "ConstantValue xmlns=\"http://abc4trust.eu/wp2/abcschemav1.0\"",
                        "ConstantValue");

        patched = patched.replace(" xmlns=\"\"", "");
        patched = patched.replace(
                "xmlns:ns2=\"http://abc4trust.eu/wp2/abcschemav1.0\"", "");

        return (PresentationToken) XmlUtils.getObjectFromXML(
                new ByteArrayInputStream(patched.getBytes("UTF-8")), true);

    }

    /**
     * @param policyName
     *            name of policy resource (without path)
     * @param applicationData
     *            if present - will be inserted on all presentation policies -
     *            must match application data supplied when creating Policy
     * @param presentationToken
     * @return
     * @throws Exception
     */
    public boolean verifyToken(String policyName, byte[] nonce,
            String applicationData, PresentationToken presentationToken)
            throws Exception {
        System.out.println("VerificationHelper - verify token : " + policyName
                + " - applicationData : " + applicationData);

        String orig = XmlUtils.toXml(this.of
                .createPresentationToken(presentationToken));
        presentationToken = this.getPatchedPresetationToken(orig);

        Map<URI, URI> revInfoUIDs = this.extractRevInfoUIDs(presentationToken);

        PresentationPolicyAlternatives pp = this.createPresentationPolicy(
                policyName, nonce, applicationData, revInfoUIDs);

        return this.verifyToken(pp, presentationToken);
    }

    private Map<URI, URI> extractRevInfoUIDs(PresentationToken pt) {
        Map<URI, URI> revInfoUIDs = null;
        for (CredentialInToken cred : pt.getPresentationTokenDescription()
                .getCredential()) {
            boolean containsRevoceableCredential = false;
            try {
                CredentialSpecification credSpec = this.keyManager
                        .getCredentialSpecification(cred.getCredentialSpecUID());
                if (credSpec.isRevocable()) {
                    containsRevoceableCredential = true;
                }
            } catch (KeyManagerException ignore) {
            }
            if (containsRevoceableCredential) {
                if (revInfoUIDs == null) {
                    revInfoUIDs = new HashMap<URI, URI>();
                }
                revInfoUIDs.put(cred.getCredentialSpecUID(),
                        cred.getRevocationInformationUID());
            }
        }
        return revInfoUIDs;
    }

    /**
     * @param ppa
     *            PresentationPolicyAlternatives
     * @param presentationToken
     * @return
     * @throws Exception
     */
    public boolean verifyToken(PresentationPolicyAlternatives ppa,
            PresentationToken presentationToken) throws Exception {
        try {
            // verify in ABCE
            this.engine.verifyTokenAgainstPolicy(ppa, presentationToken, true);
            System.out.println(" - OK!");
            return true;
        } catch (TokenVerificationException e) {
            System.err.println(" - TokenVerificationException : " + e);
            throw e;
        } catch (Exception e) {
            System.err.println(" - Failed verifying token : " + e);
            return false;
        }

    }

    public byte[] generateNonce() {
        // TODO : is 10 bytes correct ?
        byte[] nonceBytes = new byte[10];
        this.random.nextBytes(nonceBytes);
        return nonceBytes;
    }

}
