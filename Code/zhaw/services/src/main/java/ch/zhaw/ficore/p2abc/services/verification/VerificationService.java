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
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.exceptions.TokenVerificationException;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyStorage;
import eu.abc4trust.xml.ABCEBoolean;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationPolicyAlternativesAndPresentationToken;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.PresentationTokenDescription;
import eu.abc4trust.xml.SystemParameters;
//import java.util.logging.Logger;
//import eu.abc4trust.ri.servicehelper.verifier.VerificationHelper;

@SuppressWarnings("unused")
@Path("/verification")
public class VerificationService {

    private final Logger log = LogManager.getLogger();
    private final static String errMagicCookie = "Magic cookie is not correct!";
    private static Map<String, String> accessTokens = new HashMap<String, String>();
    private final static String errNotImplemented = "The requested operation is not supported and/or not implemented.";

    ObjectFactory of = new ObjectFactory();

    public VerificationService() throws Exception {
        System.out.println("VerificationService");

        if (VerificationHelper.isInit()) {
            System.out.println(" - Helper already init'ed");
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

    @GET()
    @Path("/protected/status/")
    public Response status() {
        return Response.ok().build();
    }

    @Path("/verifyTokenAgainstPolicy")
    @POST()
    public Response verifyTokenAgainstPolicy(
            JAXBElement<PresentationPolicyAlternativesAndPresentationToken> ppaAndpt,
            @QueryParam("store") String storeString)
            throws TokenVerificationException, CryptoEngineException {
        log.entry();

        try {

            boolean store = false;
            if ((storeString != null)
                    && storeString.toUpperCase().equals("TRUE")) {
                store = true;
            }
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

    @Path("/protected/getToken")
    @GET()
    public Response getToken(@QueryParam("tokenUID") URI tokenUid) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();
            PresentationToken pt = verificationHelper.engine.getToken(tokenUid);
            return log.exit(Response.ok(of.createPresentationToken(pt),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    @Path("/protected/deleteToken")
    @POST()
    public Response deleteToken(@QueryParam("tokenUID") URI tokenUid) {
        log.entry();

        try {
            boolean result = VerificationHelper.getInstance().engine
                    .deleteToken(tokenUid);
            JAXBElement<Boolean> jaxResult = new JAXBElement<Boolean>(
                    new QName("deleteToken"), Boolean.TYPE, result);
            return log.exit(Response.ok(jaxResult, MediaType.APPLICATION_XML)
                    .build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    @PUT()
    @Path("/protected/storeSystemParameters/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeSystemParameters(SystemParameters systemParameters) {

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();
            KeyManager keyManager = verificationHelper.keyManager;

            boolean r = keyManager.storeSystemParameters(systemParameters);

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
    @Path("/protected/storeIssuerParameters/{issuerParametersUid}")
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

            boolean r = keyManager.storeIssuerParameters(issuerParametersUid,
                    issuerParameters);

            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
            createABCEBoolean.setValue(r);

            this.log.info("VerificationService - storeIssuerParameters - done ");

            return log.exit(Response.ok(
                    of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

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

    @PUT()
    @Path("/protected/storeCredentialSpecification/{credentialSpecifationUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeCredentialSpecification(
            @PathParam("credentialSpecifationUid") URI credentialSpecifationUid,
            CredentialSpecification credSpec) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            KeyManager keyManager = verificationHelper.keyManager;

            boolean r = keyManager.storeCredentialSpecification(
                    credentialSpecifationUid, credSpec);

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
    @Path("/protected/presentationPolicy/store/{policyUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storePresentationPolicy(
            @PathParam("policyUid") String policyUid,
            PresentationPolicyAlternatives ppa) {

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            verificationHelper.verificationStorage.addPresentationPolicy(
                    new URI(policyUid), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
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
                            of.createPresentationPolicyAlternativesAndPresentationToken(ppat),
                            "false");
            if (r.getStatus() != 200)
                return log.exit(Response.status(Response.Status.FORBIDDEN)
                        .entity("NOT OK").build());

            URI redirect = verificationHelper.verificationStorage
                    .getRedirectURI(new URI(resource));
            String token = generateAccessToken();

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
        if (!accessTokens.containsKey(accessToken))
            return Response.status(Response.Status.FORBIDDEN).build();
        else {
            accessTokens.remove(accessToken);
            return Response.ok(accessTokens.get(accessToken)).build();
        }
    }
    
    /**
     * <b>Path</b>: /loadSettings/ (GET)<br>
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
