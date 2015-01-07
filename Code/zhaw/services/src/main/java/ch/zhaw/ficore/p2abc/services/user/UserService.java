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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.ExceptionDumper;
import ch.zhaw.ficore.p2abc.services.ServiceType;
import ch.zhaw.ficore.p2abc.services.StorageModuleFactory;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.helpers.user.UserHelper;
import ch.zhaw.ficore.p2abc.storage.GenericKeyStorage;
import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;
import ch.zhaw.ficore.p2abc.xml.CredentialCollection;
import ch.zhaw.ficore.p2abc.xml.Settings;
import eu.abc4trust.abce.internal.user.policyCredentialMatcher.PolicyCredentialMatcherImpl;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyStorage;
import eu.abc4trust.returnTypes.IssuanceReturn;
import eu.abc4trust.returnTypes.ObjectFactoryReturnTypes;
import eu.abc4trust.returnTypes.UiIssuanceReturn;
import eu.abc4trust.returnTypes.UiPresentationArguments;
import eu.abc4trust.returnTypes.UiPresentationReturn;
import eu.abc4trust.util.DummyForNewABCEInterfaces;
import eu.abc4trust.xml.ABCEBoolean;
import eu.abc4trust.xml.Credential;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.SystemParameters;

@Path("/user")
public class UserService {

    private static final CryptoEngine CRYPTO_ENGINE = CryptoEngine.IDEMIX; // use
                                                                           // idemix
                                                                           // always
                                                                           // --
                                                                           // munt

    private final ObjectFactory of = new ObjectFactory();
    private Logger log = LogManager.getLogger();

    private final String fileStoragePrefix = ""; // no prefix -- munt

    private static String errCredSpecUid = "The credential specification uid does not match or is invalid!";
    private static String errIssParamsUid = "The issuer parameters uid does not match or is invalid!";
    private static String errNotImplemented = "The requested operation is not supported and/or not implemented.";
    private static String errNoCred = "The credential could not be found!";
    private final static String errNotFound = "The requested resource or parts of it could not be found.";

    /**
     * <b>Path</b>: /status/ (GET)<br>
     * <br>
     * <b>Description</b>: If the service is running this method is available.<br>
     * <br>
     * <b>Response status:</b>
     * <ul>
     * <li>200 - OK</li>
     * </ul>
     * 
     * @return Response
     */
    @GET()
    @Path("/status/")
    public Response status() {
        return Response.ok().build();
    }
    
    /**
     * <b>Path</b>: /protected/reset (POST)<br>
     * <br>
     * <b>Description</b>: This method reloads the configuration of the webservice(s) and will completely wipe
     * all storage of the webservice(s). Use with extreme caution! <br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * @return Response
     * @throws Exception when something went wrong
     */
    @POST()
    @Path("/reset")
    public Response reset() throws Exception {
        log.entry(); 
        
        this.initializeHelper();
        
        ServicesConfiguration.staticInit();
        URIBytesStorage.clearEverything();
        return log.exit(Response.ok().build());
    }

    /**
     * <b>Path:</b> /canBeSatisfied/ (POST)<br>
     * <br>
     * <b>Description</b>: This method, on input of a presentation policy
     * decides whether the credentials in the User’s credential store could be
     * used to produce a valid presentation token satisfying the policy. If so,
     * this method returns true, otherwise, it returns false. <br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type:</b> <tt>PresentationPolicyAlternatives</tt><br>
     * <b>Return type:</b> <tt>ABCEBoolean</tt><br>
     * 
     * @param p
     *            PresentationPolicyAlternatives
     * @return Response
     */
    @POST()
    @Path("/canBeSatisfied/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response canBeSatisfied(PresentationPolicyAlternatives p) {
        log.entry();

        

        

        try {
            this.initializeHelper();
            UserHelper instance = UserHelper.getInstance();
            
            boolean b = instance.getEngine().canBeSatisfied(p);
            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
            createABCEBoolean.setValue(b);
            return log.exit(Response.ok(
                    this.of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception ex) {
            log.catching(ex);
            return log.exit(ExceptionDumper.dumpException(ex, log));
        }
    }

    /**
     * <b>Path</b>: /createPresentationToken/ (POST)<br>
     * <br>
     * <b>Description</b>: This method, on input a presentation policy
     * alternatives, returns an argument to be passed to the UI for choosing how
     * to satisfy the policy, or returns an error if the policy cannot be
     * satisfied (if the canBeSatisfied method would have returned false). For
     * returning such an argument, this method will investigate whether the User
     * has the necessary credentials and/or established pseudonyms to create one
     * or more (e.g., by satisfying different alternatives in the policy, or by
     * using different sets of credentials to satisfy one alternative)
     * presentation tokens that satisfiy the policy.
     * 
     * The return value of this method should be passed to the User Interface
     * (or to some other component that is capable of rendering a
     * UiPresentationReturn object from a UiPresentationArguments object). The
     * return value of the UI must then be passed to the method
     * createPresentationToken(UiPresentationReturn) for creating a presentation
     * token.<br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: PresentationPolicyAlternatives<br>
     * <b>Return type</b>: UiPresentationArguments<br>
     * 
     * @param p
     *            PresentationPolicyAlternatives
     * @return Response
     */
    @POST()
    @Path("/createPresentationToken/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response createPresentationToken(PresentationPolicyAlternatives p) {
        log.entry();

        

        DummyForNewABCEInterfaces d = null;
        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();
            
            UiPresentationArguments uiPresentationArguments = instance
                    .getEngine().createPresentationToken(p, d);
            return log.exit(Response.ok(
                    ObjectFactoryReturnTypes.wrap(uiPresentationArguments),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }

    }

    /**
     * <b>Path</b>: /createPresentationTokenUi/ (POST)<br>
     * <br>
     * <b>Description</b>: Performs the next step to complete creation of
     * presentation tokens. This method should be called when the user interface
     * is done with its selection. <br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type:</b> <tt>UiPresentationReturn</tt><br>
     * <b>Return type:</b> <tt>PresentationToken</tt><br>
     * 
     * @param upr
     *            UiPresentationReturn
     * @return Response
     */
    @POST()
    @Path("/createPresentationTokenUi/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response createPresentationToken(UiPresentationReturn upr) {

        log.entry();

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();
            
            PresentationToken presentationToken = instance.getEngine()
                    .createPresentationToken(upr);
            /*VerifierIdentity vi = presentationToken.getPresentationTokenDescription().getMessage().getVerifierIdentity();
            if(vi == null) {
                presentationToken.getPresentationTokenDescription().getMessage().setVerifierIdentity(new VerifierIdentity());
                vi = presentationToken.getPresentationTokenDescription().getMessage().getVerifierIdentity();
                vi.getContent().clear();
            }*/
            return log.exit(Response.ok(
                    of.createPresentationToken(presentationToken),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * <b>Path</b>: /loadSettings/ (POST)<br>
     * <br>
     * <b>Description</b>: Download and load settings from an issuer or any
     * settings provider. This method will cause the user service to make a
     * <tt>GET</tt> request to the specified <tt>url</tt> and download the
     * contents which must be valid <tt>Settings</tt>. DO NOT use this method
     * with untrusted URLs or issuers (or any other settings providers) with
     * DIFFERENT system parameters as this method will overwrite existing system
     * parameters. See also {@link #getSettings()}. <br>
     * <br>
     * <b>Query parameters</b>:
     * <ul>
     * <li>url - a valid URL (String)</li>
     * </ul>
     * <br>
     * <b>Response Status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * 
     * @param url
     *            URL to download settings from.
     * @return Response
     */
    @POST()
    @Path("/loadSettings/")
    public Response loadSettings(@QueryParam("url") String url) {
        log.entry();

        try {
            Settings settings = (Settings) RESTHelper.getRequest(url,
                    Settings.class);

            for (IssuerParameters ip : settings.issuerParametersList) {
                Response r = this.storeIssuerParameters(ip.getParametersUID(),
                        ip);
                if (r.getStatus() != 200)
                    throw new RuntimeException(
                            "Could not load issuer parameters!");
            }

            for (CredentialSpecification cs : settings.credentialSpecifications) {
                Response r = this.storeCredentialSpecification(
                        cs.getSpecificationUID(), cs);
                if (r.getStatus() != 200)
                    throw new RuntimeException(
                            "Could not load credential specification!");
            }

            Response r = this.storeSystemParameters(settings.systemParameters);
            log.info(settings.systemParameters + "|"
                    + settings.systemParameters.toString());
            if (r.getStatus() != 200)
                throw new RuntimeException("Could not load system parameters!");

            return log.exit(Response.ok().build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * <b>Path</b>: /getSettings/ (GET)<br>
     * <br>
     * <b>Description</b>: Returns the settings of the service as obtained
     * from an issuance service. Settings includes issuer parameters, credential
     * specifications and the system parameters. This method may thus be used to
     * retrieve all credential specifications stored at the user service and
     * their corresponding issuer parameters. The return type of this method is
     * <tt>Settings</tt>.<br>
     * <br>
     * The user service is capable of downloading settings from an issuer (or
     * from anything that provides settings). To download settings use
     * <tt>/loadSetting?url=...</tt> ({@link #loadSettings(String)}). <br>
     * <br>
     * <b>Response Status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Return type:</b> <tt>Settings</tt> <br>
     * 
     * @return Response
     */
    @GET()
    @Path("/getSettings/")
    public Response getSettings() {
        log.entry();

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();

            Settings settings = new Settings();

            List<IssuerParameters> issuerParams = new ArrayList<IssuerParameters>();

            for (URI uri : instance.keyStorage.listUris()) {
                Object obj = SerializationUtils.deserialize(instance.keyStorage
                        .getValue(uri));
                if (obj instanceof IssuerParameters) {
                    IssuerParameters ip = (IssuerParameters) obj;

                    SystemParameters serializeSp = (ip.getSystemParameters());

                    ip.setSystemParameters(serializeSp);

                    issuerParams.add(ip);
                }
            }

            List<CredentialSpecification> credSpecs = new ArrayList<CredentialSpecification>();

            for (URI uri : instance.keyStorage.listUris()) {
                Object obj = SerializationUtils.deserialize(instance.keyStorage
                        .getValue(uri));
                if (obj instanceof CredentialSpecification) {
                    credSpecs.add((CredentialSpecification) obj);
                }
            }

            settings.credentialSpecifications = credSpecs;
            settings.issuerParametersList = issuerParams;
            settings.systemParameters = /* SystemParametersUtil.serialize */(instance.keyManager
                    .getSystemParameters());

            return log.exit(Response.ok(settings, MediaType.APPLICATION_XML)
                    .build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                            ExceptionDumper.dumpExceptionStr(e, log))).build();
        }
    }

    /**
     * <b>Path</b>: /credential/list (GET)<br>
     * <br>
     * <b>Description</b>: Returns all obtained credentials as a
     * <tt>CredentialCollection</tt>.<br>
     * <br>
     * <b>Response Status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>CredentialCollection</tt><br>
     * 
     * @return Response
     */
    @GET()
    @Path("/credential/list")
    public Response credentials() {

        log.entry();

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();

            List<URI> credentialUids;

            credentialUids = instance.credentialManager.listCredentials();

            List<eu.abc4trust.xml.Credential> credentials = new ArrayList<eu.abc4trust.xml.Credential>();

            for (URI uri : credentialUids) {
                eu.abc4trust.xml.Credential cred = instance.credentialManager
                        .getCredential(uri);
                credentials.add(cred);
            }

            CredentialCollection credCol = new CredentialCollection();
            credCol.credentials = credentials;

            return log.exit(Response.ok(credCol, MediaType.APPLICATION_XML)
                    .build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                            ExceptionDumper.dumpExceptionStr(e, log))).build();
        }
    }
    
    /**
     * <b>Path</b>: /credential/get/{credUid} (GET)<br>
     * <br>
     * <b>Description</b>: Retrieve a credential. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credUid - UID of the credential</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * <li>404 - The credential could not be found.</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>Credential</tt><br>
     * @param credUid UID of the credential
     * @return Response
     */
    @GET()
    @Path("/credential/get/{credUid}")
    public Response getCredential(@PathParam ("credUid") String credUid) {
        log.entry();
        
        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();
            
            Credential cred = instance.credentialManager.getCredential(new URI(credUid));
            
            if(cred == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND).entity(errNoCred).build());
            }
            
            return log.exit(Response.ok(of.createCredential(cred), MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                            ExceptionDumper.dumpExceptionStr(e, log))).build();
        }
    }

    /**
     * <b>Path</b>: /issuanceProtocolStep/ (POST)<br>
     * <br>
     * <b>Description</b>: This method performs one step in an interactive
     * issuance protocol. On input an incoming issuance message im obtained from
     * the Issuer, it either returns the outgoing issuance message that is to be
     * sent back to the Issuer, an object that must be sent to the User
     * Interface (UI) to allow the user to decide how to satisfy a policy (or
     * confirm the only choice), or returns a description of the newly issued
     * credential at successful completion of the protocol. In the first case,
     * the Context attribute of the outgoing message has the same value as that
     * of the incoming message, allowing the Issuer to link the different
     * messages of this issuance protocol.
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
     * object returned by the UI.<br>
     * <br>
     * <b>Response status:</b>
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>IssuanceMessage</tt><br>
     * <b>Return type</b>: <tt>IssuanceReturn</tt><br>
     * 
     * @param jm
     *            IssuanceMessage
     * @return Response
     */
    @POST()
    @Path("/issuanceProtocolStep/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response issuanceProtocolStep(JAXBElement<IssuanceMessage> jm) {

        log.entry();

        
        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();

            IssuanceMessage m = jm.getValue();

            DummyForNewABCEInterfaces d = null;
            
            IssuanceReturn issuanceReturn = instance.getEngine()
                    .issuanceProtocolStep(m, d);
            return log.exit(Response.ok(
                    ObjectFactoryReturnTypes.wrap(issuanceReturn),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * <b>Path</b>: /issuanceProtocolStepUi/ (POST)<br>
     * <br>
     * <b>Description</b>: This method performs the next step in the issuance
     * protocol after the UI is done with its selection. <br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>UiIssuanceReturn</tt><br>
     * <b>Return type</b>: <tt>IssuanceMessage</tt><br>
     * 
     * @param uir
     *            UiIssuanceReturn
     * @return Response
     */
    @POST()
    @Path("/issuanceProtocolStepUi/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response issuanceProtocolStep(UiIssuanceReturn uir) {

        log.entry();

        

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();
            
            IssuanceMessage issuanceMessage = instance.getEngine()
                    .issuanceProtocolStep(uir);
            return log.exit(Response.ok(
                    new ObjectFactory().createIssuanceMessage(issuanceMessage),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * <b>Path</b>: /credential/delete/{credentialUid} (DELETE)<br>
     * <br>
     * <b>Description</b>: This method deletes the credential with the given
     * identifier from the credential store. If deleting is not possible (e.g.
     * if the referred credential does not exist) the method returns false, and
     * true otherwise.<br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialUid - UID of the Credential</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>ABCEBoolean</tt><br>
     * 
     * @param credentialUid
     *            - UID of the credential
     * @return Response
     */
    @DELETE()
    @Path("/credential/delete/{credentialUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response deleteCredential(
            @PathParam("credentialUid") URI credentialUid) {
        log.entry();

        

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();
            
            boolean r = instance.credentialManager
                    .deleteCredential(credentialUid);

            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
            createABCEBoolean.setValue(r);

            return log.exit(Response.ok(
                    of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * <b>Path</b>: /credentialSpecification/store/{credentialSpecificationUid}
     * (PUT)<br>
     * <br>
     * <b>Description</b>: Stores a credential specification under the given
     * UID. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * <li>409 - <tt>credentialSpecificationUid</tt> does not match the actual
     * UID or is invalid.</li>
     * </ul>
     * 
     * @param credentialSpecificationUid
     *            UID of the credential specification.
     * @param credSpec
     *            The credential specification.
     * @return Response
     */
    @PUT()
    @Path("/credentialSpecification/store/{credentialSpecifationUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeCredentialSpecification(
            @PathParam("credentialSpecifationUid") URI credentialSpecificationUid,
            CredentialSpecification credSpec) {
        log.entry();

        try {

            if (!credentialSpecificationUid.toString().equals(
                    credSpec.getSpecificationUID().toString()))
                return log.exit(Response.status(Response.Status.CONFLICT)
                        .entity(errCredSpecUid).build());

            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();

            KeyManager keyManager = instance.keyManager;

            boolean r = keyManager.storeCredentialSpecification(
                    credentialSpecificationUid, credSpec);

            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
            createABCEBoolean.setValue(r);

            return log.exit(Response.ok(
                    of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }
    
    /**
     * <b>Path</b>: /credentialSpecification/get/{credentialSpecificationUid} (GET)<br>
     * <br>
     * <b>Description</b>: Retreive a credential specification stored at this service.<br>
     * <br>
     * <b>Path parameters:</b>
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification to retrieve.</li>
     * </ul>
     * <br>
     * <b>Response status:</b>
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * <li>404 - The credential specification could not be found.</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>CredentialSpecification</tt> <br>
     * @param credSpecUid UID of the credential specification
     * @return Response
     */
    @GET()
    @Path("/credentialSpecification/get/{credentialSpecificationUid}")
    public Response getCredentialSpecification(@PathParam("credentialSpecificationUid") String credSpecUid) {
        log.entry();
        
        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();

            KeyManager keyManager = instance.keyManager;
            
            CredentialSpecification credSpec = keyManager.getCredentialSpecification(new URI(credSpecUid));
            
            if(credSpec == null)
                return log.exit(Response.status(Response.Status.NOT_FOUND).entity(errNotFound).build());
            
            return log.exit(Response.ok(of.createCredentialSpecification(credSpec), MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * <b>Path</b>: /systemParameters/store (PUT)<br>
     * <br>
     * <b>Description</b>: Store (and overwrite existing) system parameters at
     * the service. This method returns true if the system parameters were
     * successfully stored. <br>
     * <b>Response status:</b>
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>SystemParameters</tt><br>
     * <b>Return type</b>: <tt>ABCEBoolean</tt><br>
     * 
     * @param systemParameters
     *            SystemParameters
     * @return Response
     */
    @PUT()
    @Path("/systemParameters/store")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeSystemParameters(SystemParameters systemParameters) {
        log.entry();

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();

            KeyManager keyManager = instance.keyManager;

            boolean r = keyManager.storeSystemParameters(systemParameters);

            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
            createABCEBoolean.setValue(r);

            if (r) {
                UserHelper.loadSystemParameters();
            }

            return log.exit(Response.ok(
                    of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * <b>Path</b>: /issuerParameters/store/{issuerParametersUid} (PUT)<br>
     * <br>
     * <b>Description</b>: Store (and overwrite existing) issuer parameters at
     * the service (using the given identifier). This method returns true if the
     * system parameters were successfully stored. <br>
     * <b>Response status:</b>
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * <li>409 - <em>issuerParametersUid</em> does not match or is invalid.
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>IssuerParameters</tt><br>
     * <b>Return type</b>: <tt>ABCEBoolean</tt><br>
     * 
     * @param issuerParametersUid
     *            UID of the IssuerParameters
     * @param issuerParameters
     *            IssuerParameters
     * @return Response
     */
    @PUT()
    @Path("/issuerParameters/store/{issuerParametersUid}")
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

            if (!(issuerParametersUid.toString().equals(issuerParameters
                    .getParametersUID().toString())))
                return log.exit(Response.status(Response.Status.CONFLICT)
                        .entity(errIssParamsUid).build());

            KeyManager keyManager = instance.keyManager;

            boolean r = keyManager.storeIssuerParameters(issuerParametersUid,
                    issuerParameters);

            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
            createABCEBoolean.setValue(r);

            this.log.info("UserService - storeIssuerParameters - done ");

            return log.exit(Response.ok(
                    of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }
    
    /**
     * <b>Path</b>: /issuerParameters/delete/{issuerParametersUid} (DELETE)<br>
     * <br>
     * <b>Description</b>: Deletes issuer parameters.<br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>issuerParamateresUid - UID of the issuer parameters to delete.</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * @param issuerParametersUid UID of the issuer parameters
     * @return Response
     */
    @DELETE()
    @Path("/issuerParameters/delete/{issuerParametersUid}")
    public Response deleteIssuerParameters(
            @PathParam("issuerParametersUid") String issuerParametersUid) {
        log.entry();

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();
            
            KeyStorage keyStorage = instance.keyStorage;

            // @#@#^%$ KeyStorage has no delete()
            if (keyStorage instanceof GenericKeyStorage) {
                GenericKeyStorage gkeyStorage = (GenericKeyStorage) keyStorage;
                gkeyStorage.delete(new URI(issuerParametersUid));
            } else {
                return log.exit(
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                                errNotImplemented)).build();
            }

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    private void initializeHelper() {
        this.log.info("UserService loading...");

        try {
            PolicyCredentialMatcherImpl.GENERATE_SECRET_IF_NONE_EXIST = true;

            if (UserHelper.isInit()) {
                this.log.info("UserHelper is initialized");
                
                // AbstractHelper.verifyFiles(false, this.fileStoragePrefix);
            } else {
                this.log.info("Initializing UserHelper...");

                UserHelper
                        .initInstanceForService(
                                CRYPTO_ENGINE,
                                this.fileStoragePrefix,
                                StorageModuleFactory
                                        .getModulesForServiceConfiguration(ServiceType.USER));

                this.log.info("UserHelper is initialized");
            }
            UserHelper instance = UserHelper.getInstance();
            
            Set<URI> keySet = instance.cardStorage.getSmartcards().keySet();
            for (URI uri : keySet) {
                log.info("Smartcards: " + uri);
            }
        } catch (Exception ex) {
            log.info("Create UserHelper FAILED " + ex);
            ex.printStackTrace();
        }
    }

    /**
     * <b>Path</b>: /extractIssuanceMessage/ (POST)<br>
     * <br>
     * <b>Description</b>: This method extracts the IssuanceMessage from an
     * IssuanceMessageAndBoolean and returns the IssuanceMessage.<br>
     * <br>
     * <b>Response status:</b>
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type:</b> <tt>IssuanceMessageAndBoolean</tt><br>
     * <b>Return type:</b> <tt>IssuanceMessage</tt><br>
     * 
     * @param issuanceMessageAndBoolean
     *            IssuanceMessageAndBoolean
     * @return Response
     */
    @POST()
    @Path("/extractIssuanceMessage/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response extractIssuanceMessage(
            IssuanceMessageAndBoolean issuanceMessageAndBoolean) {
        log.entry();

        try {

            IssuanceMessage issuanceMessage = issuanceMessageAndBoolean
                    .getIssuanceMessage();

            ObjectFactory of = new ObjectFactory();

            return log.exit(Response.ok(
                    of.createIssuanceMessage(issuanceMessage),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

}
