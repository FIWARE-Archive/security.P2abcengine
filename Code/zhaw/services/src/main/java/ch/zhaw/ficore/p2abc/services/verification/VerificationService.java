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

package ch.zhaw.ficore.p2abc.services.verification;

import java.net.URI;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.ExceptionDumper;
import ch.zhaw.ficore.p2abc.services.ServiceType;
import ch.zhaw.ficore.p2abc.services.StorageModuleFactory;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.helpers.user.UserHelper;
import ch.zhaw.ficore.p2abc.services.helpers.verification.VerificationHelper;
import ch.zhaw.ficore.p2abc.storage.GenericKeyStorage;
import ch.zhaw.ficore.p2abc.xml.Settings;
import ch.zhaw.ficore.p2abc.xml.PresentationPolicyAlternativesCollection;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.exceptions.TokenVerificationException;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyStorage;
import eu.abc4trust.xml.ABCEBoolean;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributePredicate;
import eu.abc4trust.xml.CredentialInPolicy;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicy;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationPolicyAlternativesAndPresentationToken;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.PresentationTokenDescription;
import eu.abc4trust.xml.SystemParameters;
import eu.abc4trust.xml.AttributePredicate.Attribute;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

//import java.util.logging.Logger;
//import eu.abc4trust.ri.servicehelper.verifier.VerificationHelper;

@SuppressWarnings("unused")
@Path("/verification")
public class VerificationService {

    private final Logger log = LogManager.getLogger();
    private final static String errMagicCookie = "Magic cookie is not correct!";
    private static Map<String, String> accessTokens = new HashMap<String, String>();
    private final static String errNotImplemented = "The requested operation is not supported and/or not implemented.";
    private final static String errNoAttrib = "The attribute was not found in any credential specification alternative.";
    private final static String errUid = "The given UID in the path does not match the actual UID.";

    ObjectFactory of = new ObjectFactory();

    public VerificationService() throws Exception {

        if (VerificationHelper.isInit()) {
        } else {
            if (System.getProperty("PathToUProveExe", null) == null) {
                String uprovePath = "./../uprove/UProveWSDLService/ABC4Trust-UProve/bin/Release";
                System.setProperty("PathToUProveExe", uprovePath);
            }

            VerificationHelper
                    .initInstance(
                            CryptoEngine.IDEMIX,
                            StorageModuleFactory
                                    .getModulesForServiceConfiguration(ServiceType.VERIFICATION));
        }
    }

    /**
     * <b>Path</b>: /protected/status/ (GET)<br>
     * <br>
     * <b>Description</b>: This method is available when the service is running.<br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * </ul>
     * @return
     */
    @GET()
    @Path("/protected/status/")
    public Response status() {
        return Response.ok().build();
    }

    /**
     * <b>Path</b>: /verifyTokenAgainstPolicy (POST) <br>
     * <br>
     * <b>Description</b>: This method verifies a given presentation token against a given PresentationPolicyAlternatives. <br>
     * This method will return a PresentationTokenDescription.
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>400 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type:</b> <tt>PresentationPolicyAlternativesAndPresentationToken</tt> <br>
     * <b>Return type:</b> <tt>PresentationTokenDescription</tt> <br>
     * @param ppaAndpt PresentationPolicyAlternativesAndPresentationToken
     * @return Response
     * @throws TokenVerificationException when something went wrong.
     * @throws CryptoEngineException when something went wrong.
     */
    @Path("/verifyTokenAgainstPolicy")
    @POST()
    public Response verifyTokenAgainstPolicy(
            JAXBElement<PresentationPolicyAlternativesAndPresentationToken> ppaAndpt)
            throws TokenVerificationException, CryptoEngineException {
        log.entry();

        try {

            boolean store = false;
            
            VerificationHelper verficationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternativesAndPresentationToken value = ppaAndpt
                    .getValue();
            PresentationPolicyAlternatives presentationPolicyAlternatives = value
                    .getPresentationPolicyAlternatives();
            PresentationToken presentationToken = value.getPresentationToken();
            PresentationTokenDescription ptd = verficationHelper.engine
                    .verifyTokenAgainstPolicy(presentationPolicyAlternatives,
                            presentationToken, store);
            return log.exit(Response.ok(
                    of.createPresentationTokenDescription(ptd),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }
    
    @POST()
    @Path("/protected/presentationPolicy/addPredicate/{resource}")
    public Response addPredicate(@PathParam("resource") String resource, @FormParam("cv") String constantValue,
            @FormParam("at") String attribute, @FormParam("p") String predicate, @FormParam("al") String alias) {
        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();
            
            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage.getPresentationPolicy(new URI(resource));
            
            boolean found = false;
            
            for(PresentationPolicy pp : ppa.getPresentationPolicy()) {
                AttributePredicate ap = new AttributePredicate();
                ap.setFunction(new URI(predicate));
                Attribute atr = new Attribute();

                
                for(CredentialInPolicy cip : pp.getCredential()) {
                    for(URI credSpecUid : cip.getCredentialSpecAlternatives().getCredentialSpecUID()) {
                        CredentialSpecification credSpec = verificationHelper.
                                keyManager.getCredentialSpecification(credSpecUid);
                        for(AttributeDescription attrDesc :
                            credSpec.getAttributeDescriptions().getAttributeDescription()) {
                            if(attrDesc.getType().toString().equals(attribute)) {
                                atr.setAttributeType(attrDesc.getType());
                                atr.setCredentialAlias(new URI(alias));
                                found = true;
                            }
                        }
                    }
                }
                
                if(found) {
                    ap.getAttributeOrConstantValue().add(atr);
                    Element e = createW3DomElement("ConstantValue", constantValue);
                    ap.getAttributeOrConstantValue().add(e);
                    pp.getAttributePredicate().add(ap);
                    break;
                }
            }
            
            if(!found)
                return log.exit(Response.status(Response.Status.NOT_FOUND).entity(errNoAttrib).build());
            
            verificationHelper.verificationStorage.addPresentationPolicy(new URI(resource), ppa);
        }
        catch(Exception e) {
            
        }
        
        return Response.ok().build();
    }
    
    private static Element createW3DomElement(String elementName, String value) {
        Element element;
        try {
            element = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement(elementName);
        } catch (DOMException e) {
            throw new IllegalStateException("This should always work!",e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("This should always work!",e);
        }
        element.setTextContent(value);
        return element;
    }

    
    /**
     * <b>Path</b>: /protected/systemParameters/store (PUT)<br>
     * <br>
     * <b>Description</b>: Stores system parameters at this service.<br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     *  <li>200 - OK (application xml)</li>
     *  <li>500 - ERROR</li>
     * </ul>
     * @param systemParameters SystemParameters
     * @return Response
     */
    @PUT()
    @Path("/protected/systemParameters/store")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeSystemParameters(SystemParameters systemParameters) {

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();
            KeyManager keyManager = verificationHelper.keyManager;

            boolean r = keyManager.storeSystemParameters(systemParameters);

            if(!r)
                throw new RuntimeException("Could not store system parameters.");
            
            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }
    
    /**
     * <b>Path</b>: /protected/issuerParameters/delete/{issuerParametersUid} (DELETE)<br>
     * <br>
     * <b>Description</b>: Deletes issuer parameters. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>issuerParametersUid - UID of the issuer parameters to delet.
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * @param issuerParametersUid UID of the IssuerParameters
     * @return Response
     */
    @DELETE()
    @Path("/protected/issuerParameters/delete/{issuerParametersUid}")
    public Response deleteIssuerParameters(
            @PathParam("issuerParametersUid") String issuerParametersUid) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            KeyStorage keyStorage = verificationHelper.keyStorage;

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

    /**
     * <b>Path</b>: /protected/issuerParameters/store/{issuerParametersUid} (PUT)<br>
     * <br>
     * <b>Description</b>: Stores issuer parameters at this service. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>issuerParametersUid - UID of the issuer parameters to store</li>
     * </ul>
     * <br>
     * <b>Response status:</b>
     * <ul>
     * <li>200 - OK</li>
     * <li>409 - The issuerParemetersUid does not match the actual issuer parameters' UID.</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type:</b> <tt>IssuerParameters</tt> <br>
     * @param issuerParametersUid UID of the IssuerParameters
     * @param issuerParameters IssuerParameters
     * @return Response
     */
    @PUT()
    @Path("/protected/issuerParameters/store/{issuerParametersUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeIssuerParameters(
            @PathParam("issuerParametersUid") URI issuerParametersUid,
            IssuerParameters issuerParameters) {

        log.entry();

        this.log.info("VerificationService - storeIssuerParameters - issuerParametersUid: "
                + issuerParametersUid
                + ", "
                + issuerParameters.getParametersUID());
        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();
            KeyManager keyManager = verificationHelper.keyManager;
            
            if(!issuerParametersUid.toString().equals(issuerParameters.getParametersUID().toString()))
                return log.exit(Response.status(Response.Status.CONFLICT).build());

            boolean r = keyManager.storeIssuerParameters(issuerParametersUid,
                    issuerParameters);
            
            if(!r)
                throw new RuntimeException("Could not store issuer parameters!");

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * <b>Path</b>: /createPresentationPolicy/ (POST) <br>
     * <br>
     * <b>Description</b>: Given a presentation policy template creates a presentation policy (while also embedding nonce bytes). <br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR </li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>PresentationPolicyAlternatives</tt><br>
     * <b>Return type</b>: <tt>PresentationPolicyAlternatives</tt><br>
     * @param applicationData
     * @param presentationPolicy
     * @return
     */
    @POST()
    @Path("/createPresentationPolicy")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response createPresentationPolicy(
            @PathParam("applicationData") String applicationData,
            PresentationPolicyAlternatives presentationPolicy) {

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            Map<URI, URI> revocationInformationUids = new HashMap<URI, URI>();

            PresentationPolicyAlternatives modifiedPresentationPolicyAlternatives = verificationHelper
                    .createPresentationPolicy(presentationPolicy,
                            applicationData, revocationInformationUids);
            this.log.info("VerificationService - createPresentationPolicy - done ");

            return log
                    .exit(Response
                            .ok(of.createPresentationPolicyAlternatives(modifiedPresentationPolicyAlternatives),
                                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * <b>Path</b>: /protected/credentialSpecification/store/{credentialSpecificationUid} (PUT)<br>
     * <br>
     * <b>Description</b>: Stores a credential specification at this service. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification to store.</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>409 - UID given on the path does not match the actual UID.</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>CredentialSpecification</tt> <br>
     * @param credentialSpecifationUid
     * @param credSpec
     * @return
     */
    @PUT()
    @Path("/protected/credentialSpecification/store/{credentialSpecifationUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeCredentialSpecification(
            @PathParam("credentialSpecifationUid") URI credentialSpecificationUid,
            CredentialSpecification credSpec) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            KeyManager keyManager = verificationHelper.keyManager;
            
            if(!credentialSpecificationUid.toString().equals(credSpec.getSpecificationUID().toString()))
                return log.exit(Response.status(Response.Status.CONFLICT).entity(errUid).build());

            boolean r = keyManager.storeCredentialSpecification(
                    credentialSpecificationUid, credSpec);
           

            if(!r)
                throw new RuntimeException("Could not store the credential specification");

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }
    
    /**
     * <b>Path</b>: /protected/credentialSpecification/get/{credentialSpecificationUid} (GET)<br>
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
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>CredentialSpecification</tt> <br>
     * @param credSpecUid
     * @return
     */
    @GET()
    @Path("/protected/credentialSpecification/get/{credentialSpecificationUid}")
    public Response getCredentialSpecification(@PathParam("credentialSpecificationUid") String credSpecUid) {
        log.entry();
        
        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            KeyManager keyManager = verificationHelper.keyManager;
            
            CredentialSpecification credSpec = keyManager.getCredentialSpecification(new URI(credSpecUid));
            
            return log.exit(Response.ok(of.createCredentialSpecification(credSpec), MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }
    
    /**
     * <b>Path</b>: /protected/credentialSpecification/{credentialSpecificationUid} (DELETE) <br>
     * <br>
     * <b>Description</b>: Deletes a credential specification. <br>
     * <br>
     * <b>Path parameters</b>: 
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification to delete.</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * @param credSpecUid UID of the credential specification to delete
     * @return Response
     */
    @DELETE()
    @Path("/protected/credentialSpecification/delete/{credentialSpecificationUid}")
    public Response deleteCredentialSpecification(
            @PathParam("credentialSpecificationUid") String credSpecUid) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            KeyStorage keyStorage = verificationHelper.keyStorage;

            // @#@#^%$ KeyStorage has no delete()
            if (keyStorage instanceof GenericKeyStorage) {
                GenericKeyStorage gkeyStorage = (GenericKeyStorage) keyStorage;
                gkeyStorage.delete(new URI(credSpecUid));
            } else {
                return log.exit(
                        Response.status(Response.Status.BAD_REQUEST).entity(
                                errNotImplemented)).build();
            }

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    @PUT()
    @Path("/protected/presentationPolicy/store/{resource}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storePresentationPolicy(
            @PathParam("resource") String resource,
            PresentationPolicyAlternatives ppa) {

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            verificationHelper.verificationStorage.addPresentationPolicy(
                    new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }
    
    @GET()
    @Path("/protected/presentationPolicy/get/{resource}")
    public Response getPresentationPolicy(@PathParam("resource") String resource) {
        log.entry();
        
        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();
            
            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage.getPresentationPolicy(new URI(resource));
            return log.exit(Response.ok(of.createPresentationPolicyAlternatives(ppa), MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }
    
    /**
     * <b>Path</b>: /protected/presentationPolicy/list (GET)<br>
     * <br>
     * <b>Description</b>: Lists all presentation policies stored at this service. <br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>PresentationPolicyAlternativesCollection</tt> <br>
     * @return Response
     */
    @GET()
    @Path("/protected/presentationPolicy/list")
    public Response presentationPolicies() {
        log.entry();
        
        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();
            
            PresentationPolicyAlternativesCollection ppac = new PresentationPolicyAlternativesCollection();
            ppac.presentationPolicyAlternatives = verificationHelper.verificationStorage.listPresentationPolicies();
            List<String> uris = new ArrayList<String>();
            for(URI uri : verificationHelper.verificationStorage.listPresentationPoliciesURIS())
                uris.add(uri.toString());
            ppac.uris = uris;
            return log.exit(Response.ok(ppac, MediaType.APPLICATION_XML).build());
        }
        catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    @PUT()
    @Path("/protected/redirectURI/store/{resource}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeRedirectURI(@PathParam("resource") String resourceUri,
            String redirectUri) {

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            verificationHelper.verificationStorage.addRedirectURI(new URI(
                    resourceUri), new URI(redirectUri));

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    @GET()
    @Path("/requestResource/{resource}")
    public Response requestResource(@PathParam("resource") String resource) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicy(new URI(resource));
            return log.exit(Response.ok(
                    of.createPresentationPolicyAlternatives(ppa)).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    @POST()
    @Path("/requestResource2/{resource}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response requestResource2(@PathParam("resource") String resource,
            PresentationToken pt) {

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicy(new URI(resource));

            PresentationPolicyAlternativesAndPresentationToken ppat = of
                    .createPresentationPolicyAlternativesAndPresentationToken();
            ppat.setPresentationPolicyAlternatives(ppa);
            ppat.setPresentationToken(pt);

            Response r = this
                    .verifyTokenAgainstPolicy(
                            of.createPresentationPolicyAlternativesAndPresentationToken(ppat));
            if (r.getStatus() != 200)
                return log.exit(Response.status(Response.Status.FORBIDDEN)
                        .entity("NOT OK").build());

            URI redirect = verificationHelper.verificationStorage
                    .getRedirectURI(new URI(resource));
            String token = generateAccessToken();

            log.info("VPut: " + token + "," + resource);
            accessTokens.put(token, resource);

            return log.exit(Response.ok(
                    redirect.toString() + "?accesstoken="
                            + URLEncoder.encode(token, "UTF-8")).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    @GET()
    @Path("/verifyAccessToken/")
    public Response verifyAccessToken(
            @QueryParam("accesstoken") String accessToken) {
        log.info("VGet: " + accessToken);
        if (!accessTokens.containsKey(accessToken))
            return Response.status(Response.Status.FORBIDDEN).build();
        else {
            String resourceString = accessTokens.get(accessToken);
            accessTokens.remove(accessToken);
            return Response.ok(accessTokens.get(accessToken)).build();
        }
    }
    
    /**
     * <b>Path</b>: /protected/loadSettings/ (GET)<br>
     * <br>
     * <b>Description</b>: Download and load settings from an issuer or any
     * settings provider. This method will cause the user service to make a
     * <tt>GET</tt> request to the specified <tt>url</tt> and download the
     * contents which must be valid <tt>Settings</tt>. DO NOT use this method
     * with untrusted URLs or issuers (or any other settings providers) with
     * DIFFERENT system parameters as this method will overwrite existing system
     * parameters. (see {@link #getSettings()}) <br>
     * <br>
     * <b>Query parameters</b>:
     * <ul>
     * <li>url - a valid URL (String)</li>
     * </ul>
     * <br>
     * <b>Response Status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>400 - ERROR</li>
     * </ul>
     * 
     * @param url
     *            URL to download settings from.
     * @return Response
     */
    @GET()
    @Path("/protected/loadSettings/")
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
     * <li>400 - ERROR</li>
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
            VerificationHelper instance = VerificationHelper.getInstance();

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
                    Response.status(Response.Status.BAD_REQUEST).entity(
                            ExceptionDumper.dumpExceptionStr(e, log))).build();
        }
    }

    private String generateAccessToken() {
        Random rand = new SecureRandom();
        String prefix = "" + rand.nextInt() + "-";
        byte[] bytes = new byte[16];
        rand.nextBytes(bytes);
        return prefix + DigestUtils.sha1Hex(bytes);
    }
}
