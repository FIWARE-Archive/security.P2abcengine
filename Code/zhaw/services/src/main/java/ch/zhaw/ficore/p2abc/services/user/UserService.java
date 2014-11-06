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

//This is a copy of the original UserService from the Code/core-abce/abce-services tree.

package ch.zhaw.ficore.p2abc.services.user;

import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.ExceptionDumper;
import ch.zhaw.ficore.p2abc.services.ServiceType;
import ch.zhaw.ficore.p2abc.services.StorageModuleFactory;
import ch.zhaw.ficore.p2abc.services.helpers.user.UserHelper;
import eu.abc4trust.abce.internal.user.credentialManager.CredentialManagerException;
import eu.abc4trust.abce.internal.user.policyCredentialMatcher.PolicyCredentialMatcherImpl;
//import eu.abc4trust.abce.utils.SecretWrapper;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.exceptions.CannotSatisfyPolicyException;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.returnTypes.IssuanceReturn;
import eu.abc4trust.returnTypes.ObjectFactoryReturnTypes;
import eu.abc4trust.returnTypes.UiIssuanceReturn;
import eu.abc4trust.returnTypes.UiPresentationArguments;
import eu.abc4trust.returnTypes.UiPresentationReturn;
import eu.abc4trust.util.DummyForNewABCEInterfaces;
import eu.abc4trust.xml.ABCEBoolean;
import eu.abc4trust.xml.CredentialDescription;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.SystemParameters;
import eu.abc4trust.xml.URISet;
//import eu.abc4trust.ri.servicehelper.user.UserHelper;
//import eu.abc4trust.services.helpers.UserDebugger;

@Path("/user")
public class UserService {

    private static final CryptoEngine CRYPTO_ENGINE = CryptoEngine.IDEMIX; //use idemix always -- munt

    private final ObjectFactory of = new ObjectFactory();

    private Logger log = LogManager.getLogger();

    private final String fileStoragePrefix = ""; //no prefix -- munt
    
    @GET()
    @Path("/status/")
    public Response status() {
        return ExceptionDumper.dumpException(new RuntimeException("hi"), log);
        //return Response.ok().build();
    }

    /**
     * This method, on input a presentation policy p, decides whether the
     * credentials in the User’s credential store could be used to produce a
     * valid presentation token satisfying the policy p. If so, this method
     * returns true, otherwise, it returns false.
     * 
     * @param p
     * @return
     */
    @POST()
    @Path("/canBeSatisfied/")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    public Response canBeSatisfied(
            PresentationPolicyAlternatives p) {
        log.entry();
        
        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        try {
            boolean b = instance.getEngine().canBeSatisfied(p);
            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
            createABCEBoolean.setValue(b);
            return log.exit(Response.ok(this.of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception ex) {
            log.catching(ex);
            return log.exit(ExceptionDumper.dumpException(ex, log));
        }
    }

    /**
     * This method, on input a presentation policy alternatives p, returns an
     * argument to be passed to the UI for choosing how to satisfy the policy,
     * or returns an error if the policy cannot be satisfied (if the
     * canBeSatisfied method would have returned false). For returning such an
     * argument, this method will investigate whether the User has the necessary
     * credentials and/or established pseudonyms to create one or more (e.g., by
     * satisfying different alternatives in the policy, or by using different
     * sets of credentials to satisfy one alternative) presentation tokens that
     * satisfiy the policy.
     * 
     * The return value of this method should be passed to the User Interface
     * (or to some other component that is capable of rendering a
     * UiPresentationReturn object from a UiPresentationArguments object). The
     * return value of the UI must then be passed to the method
     * createPresentationToken(UiPresentationReturn) for creating a presentation
     * token.
     * 
     * @param p
     * @return
     * @throws CannotSatisfyPolicyException
     * @throws CredentialManagerException
     * @throws KeyManagerException
     */
    @POST()
    @Path("/createPresentationToken/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response createPresentationToken(
            PresentationPolicyAlternatives p) {
        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        DummyForNewABCEInterfaces d = null;
        try {
            UiPresentationArguments uiPresentationArguments = instance.getEngine().createPresentationToken(p, d);
            return log.exit(Response.ok(ObjectFactoryReturnTypes.wrap(uiPresentationArguments),
                    MediaType.APPLICATION_XML).build());
        } catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }

    }

    @POST()
    @Path("/createPresentationTokenUi/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response createPresentationToken(
            UiPresentationReturn upr) {
        
        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        

        try {
            PresentationToken presentationToken = instance.getEngine()
                    .createPresentationToken(upr);
            return log.exit(Response.ok(of.createPresentationToken(presentationToken),
                    MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    /**
     * This method performs one step in an interactive issuance protocol. On
     * input an incoming issuance message im obtained from the Issuer, it either
     * returns the outgoing issuance message that is to be sent back to the
     * Issuer, an object that must be sent to the User Interface (UI) to allow
     * the user to decide how to satisfy a policy (or confirm the only choice),
     * or returns a description of the newly issued credential at successful
     * completion of the protocol. In the first case, the Context attribute of
     * the outgoing message has the same value as that of the incoming message,
     * allowing the Issuer to link the different messages of this issuance
     * protocol.
     * 
     * If this is the first time this method is called for a given context, the
     * method expects the issuance message to contain an issuance policy, and
     * returns an object that is to be sent to the UI (allowing the user to
     * chose his preferred way of generating the presentation token, or to
     * confirm the only possible choice).
     * 
     * This method throws an exception if the policy cannot be satisfied with
     * the user's current credentials.
     * 
     * If this method returns an IssuanceMessage, that message should be
     * forwarded to the Issuer. If this method returns a CredentialDescription,
     * then the issuance protocol was successful. If this method returns a
     * UiIssuanceArguments, that object must be forwarded to the UI (or to some
     * other component that is capable of rendering a UiIssuanceReturn object
     * from a UiIssuanceArguments object); the method
     * issuanceProtocolStep(UiIssuanceReturn) should then be invoked with the
     * object returned by the UI.
     * 
     * @param im
     * @return
     * @throws CannotSatisfyPolicyException
     * @throws CryptoEngineException
     * @throws KeyManagerException
     * @throws CredentialManagerException
     */
    @POST()
    @Path("/issuanceProtocolStep/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response issuanceProtocolStep(
            JAXBElement<IssuanceMessage> jm) {
        
        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        IssuanceMessage m = jm.getValue();

        DummyForNewABCEInterfaces d = null;
        try {
            IssuanceReturn issuanceReturn = instance.getEngine()
                    .issuanceProtocolStep(m, d);
            return log.exit(Response.ok(ObjectFactoryReturnTypes.wrap(issuanceReturn),
                    MediaType.APPLICATION_XML).build());
        } 
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    @POST()
    @Path("/issuanceProtocolStepUi/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response issuanceProtocolStep(
            UiIssuanceReturn uir) {

        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        try {
            IssuanceMessage issuanceMessage = instance.getEngine()
                    .issuanceProtocolStep(uir);
            return log.exit(Response.ok(new ObjectFactory().createIssuanceMessage(issuanceMessage),
                    MediaType.APPLICATION_XML).build());
        } 
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    /**
     * This method returns an array of all unique credential identifiers (UIDs)
     * available in the Credential Manager.
     * 
     * @return
     */
    @GET()
    @Path("/listCredentials/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response listCredentials() {
        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        List<URI> credentialUids;
        try {
            credentialUids = instance.credentialManager.listCredentials();

            URISet uriList = this.of.createURISet();
            uriList.getURI().addAll(credentialUids);
            return log.exit(Response.ok(of.createURISet(uriList),
                    MediaType.APPLICATION_XML).build());
        } 
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    /**
     * This method returns the description of the credential with the given
     * unique identifier. The unique credential identifier credUid is the
     * identifier which was included in the credential description that was
     * returned at successful completion of the issuance protocol.
     * 
     * @param credUid
     * @return
     */
    @GET()
    @Path("/getCredentialDescription/{credentialUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response getCredentialDescription(
            @PathParam("credentialUid") URI credUid) {
        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        try {
            CredentialDescription credDesc = instance.credentialManager
                    .getCredentialDescription(credUid);

            return log.exit(Response.ok(of.createCredentialDescription(credDesc),
                    MediaType.APPLICATION_XML).build());

        } catch (CredentialManagerException ex) {
            throw new WebApplicationException(ex,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * This method deletes the credential with the given identifier from the
     * credential store. If deleting is not possible (e.g. if the referred
     * credential does not exist) the method returns false, and true otherwise.
     * 
     * @param credentialUid
     * @return
     */
    @DELETE()
    @Path("/deleteCredential/{credentialUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response deleteCredential(
            @PathParam("credentialUid") URI credentialUid) {
        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        try {
            boolean r = instance.credentialManager.deleteCredential(credentialUid);

            ABCEBoolean createABCEBoolean = this.of
                    .createABCEBoolean();
            createABCEBoolean.setValue(r);

            return log.exit(Response.ok(of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } 
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    @PUT()
    @Path("/storeCredentialSpecification/{credentialSpecifationUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeCredentialSpecification(
            @PathParam("credentialSpecifationUid") URI credentialSpecifationUid,
            CredentialSpecification credSpec) {
        log.entry();

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();
            
            KeyManager keyManager = instance.keyManager;

            boolean r = keyManager.storeCredentialSpecification(
                    credentialSpecifationUid, credSpec);

            ABCEBoolean createABCEBoolean = this.of
                    .createABCEBoolean();
            createABCEBoolean.setValue(r);

            return log.exit(Response.ok(of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } 
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    @POST()
    @Path("/storeSystemParameters/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeSystemParameters(
            SystemParameters systemParameters) {
        log.entry();

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();
            
            KeyManager keyManager = instance.keyManager;

            boolean r = keyManager
                    .storeSystemParameters(systemParameters);

            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
            createABCEBoolean.setValue(r);

            if (r) {
                instance.loadSystemParameters();
            }

            return log.exit(Response.ok(of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } 
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    @PUT()
    @Path("/storeIssuerParameters/{issuerParametersUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeIssuerParameters(
            @PathParam("issuerParametersUid") URI issuerParametersUid,
            IssuerParameters issuerParameters) {
        
        log.entry();

        this.log.info("UserService - storeIssuerParameters - issuerParametersUid: "
                + issuerParametersUid
                + ", "
                + issuerParameters.getParametersUID());
        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();
            
            KeyManager keyManager = instance.keyManager;

            boolean r = keyManager.storeIssuerParameters(issuerParametersUid,
                    issuerParameters);

            ABCEBoolean createABCEBoolean = this.of
                    .createABCEBoolean();
            createABCEBoolean.setValue(r);

            this.log.info("UserService - storeIssuerParameters - done ");

            return log.exit(Response.ok(of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }


    private void initializeHelper() {
        this.log.info("UserService loading...");

        try {
         
            PolicyCredentialMatcherImpl.GENERATE_SECRET_IF_NONE_EXIST = true; 

            if (UserHelper.isInit()) {
                this.log.info("UserHelper is initialized");
                //AbstractHelper.verifyFiles(false, this.fileStoragePrefix);
            } else {
                this.log.info("Initializing UserHelper...");


                UserHelper.initInstanceForService(CRYPTO_ENGINE,
                        this.fileStoragePrefix,
                        StorageModuleFactory.getModulesForServiceConfiguration(
                                ServiceType.USER));

                this.log.info("UserHelper is initialized");
            }
            UserHelper instance = UserHelper.getInstance();
            Set<URI> keySet = instance.cardStorage
                    .getSmartcards().keySet();
            for (URI uri : keySet) {
                System.out.println("Smartcards: " + uri);
            }
        } catch (Exception ex) {
            System.out.println("Create UserHelper FAILED " + ex);
            ex.printStackTrace();
        }
        UserHelper instance = UserHelper.getInstance();
    }

    @POST()
    @Path("/extractIssuanceMessage/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response extractIssuanceMessage(
            IssuanceMessageAndBoolean issuanceMessageAndBoolean)
                    {
        log.entry();
        
        try {
        
            IssuanceMessage issuanceMessage = issuanceMessageAndBoolean
                    .getIssuanceMessage();
    
            ObjectFactory of = new ObjectFactory();
    
            return log.exit(Response.ok(of.createIssuanceMessage(issuanceMessage),
                    MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }


}