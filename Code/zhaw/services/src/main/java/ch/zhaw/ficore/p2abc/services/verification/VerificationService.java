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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.ExceptionDumper;
import ch.zhaw.ficore.p2abc.services.ServiceType;
import ch.zhaw.ficore.p2abc.services.StorageModuleFactory;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.helpers.verification.VerificationHelper;
import ch.zhaw.ficore.p2abc.storage.GenericKeyStorage;
import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;
import ch.zhaw.ficore.p2abc.xml.PresentationPolicyAlternativesCollection;
import ch.zhaw.ficore.p2abc.xml.Settings;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.exceptions.TokenVerificationException;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyStorage;
import eu.abc4trust.xml.ApplicationData;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributePredicate;
import eu.abc4trust.xml.AttributePredicate.Attribute;
import eu.abc4trust.xml.CredentialInPolicy;
import eu.abc4trust.xml.CredentialInPolicy.CredentialSpecAlternatives;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives.IssuerParametersUID;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.Message;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicy;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationPolicyAlternativesAndPresentationToken;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.PresentationTokenDescription;
import eu.abc4trust.xml.SystemParameters;
import eu.abc4trust.xml.VerifierIdentity;

//import java.util.logging.Logger;
//import eu.abc4trust.ri.servicehelper.verifier.VerificationHelper;

@SuppressWarnings("unused")
@Path("/verification")
public class VerificationService {

    private static final XLogger log = new XLogger(
            LoggerFactory.getLogger(VerificationService.class));
    private static final String errMagicCookie = "Magic cookie is not correct!";
    private static Map<String, String> accessTokens = new HashMap<String, String>();
    private static Map<String, byte[]> nonces = new HashMap<String, byte[]>();

    private static final String ERR_NOT_IMPLEMENTED = "The requested operation is not supported and/or not implemented.";
    private static final String ERR_NO_ATTRIB = "The attribute was not found in any credential specification alternative.";
    private static final String ERR_UID = "The given UID in the path does not match the actual UID.";
    private static final String ERR_NOT_FOUND = "The requested resource or parts of it could not be found.";

    ObjectFactory of = new ObjectFactory();

    public VerificationService() throws Exception {

        if (VerificationHelper.isInit()) {
            log.info("init already done");
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

        VerificationHelper instance = VerificationHelper.getInstance();
        try {
            SystemParameters sp = instance.keyManager.getSystemParameters();
            if (sp != null) {
                this.storeSystemParameters(sp);
            }
        } catch (Exception ex) {
            log.catching(ex);
        }
    }

    /**
     * @fiware-rest-path /protected/status/
     * @fiware-rest-method GET
     *
     * @fiware-rest-description This method is available when the service is
     *                          running.
     *
     * @fiware-rest-response 200 OK
     *
     * @return Response
     */
    @GET()
    @Path("/protected/status/")
    public Response status() {
        return Response.ok().build();
    }

    /**
     * @fiware-rest-path /protected/reset
     * @fiware-rest-method POST
     *
     * @fiware-rest-description: This method reloads the configuration of the
     *                           webservice(s) and will completely wipe all
     *                           storage of the webservice(s). Use with extreme
     *                           caution!
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @return Response
     * @throws Exception
     *             when something went wrong
     */
    @POST()
    @Path("/protected/reset")
    public Response reset() throws Exception { /* [FLOW TEST] */
        log.entry();
        VerificationHelper.getInstance();

        URIBytesStorage.clearEverything();
        return log.exit(Response.ok().build());
    }

    /**
     * @fiware-rest-path /verifyTokenAgainstPolicy
     * @fiware-rest-method POST
     *
     * @fiware-rest-description This method verifies a given presentation token
     *                          against a given PresentationPolicyAlternatives.
     *                          This method will return a
     *                          PresentationTokenDescription.
     *
     * @fiware-rest-request-param token a presentation token (of type
     *                            PresentationPolicyAlternativesAndPresentationToken
     *                            )
     *
     * @fiware-rest-response status 200 OK (PresentationTokenDescription as
     *                       application/xml)
     * @fiware-rest-response 500 ERROR
     *
     * @param ppaAndpt
     *            PresentationPolicyAlternativesAndPresentationToken
     * @return Response
     * @throws TokenVerificationException
     *             when something went wrong.
     * @throws CryptoEngineException
     *             when something went wrong.
     */
    @Path("/verifyTokenAgainstPolicy")
    @POST()
    public Response verifyTokenAgainstPolicy(
            /* [FLOW TEST] */
            final JAXBElement<PresentationPolicyAlternativesAndPresentationToken> ppaAndpt)
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

    /**
     * @fiware-rest-path /protected/presentationPolicyAlternatives/
     *                   addCredentialSpecificationAlternative
     *                   /{resource}/{policyUid}
     * @fiware-rest-method POST
     *
     * @fiware-rest-description This method adds a credential specification
     *                          alternative to a presentation policy inside
     *                          <tt>PresentationPolicyAlternatives</tt>.
     *
     * @fiware-rest-path-param resource Resource URI
     * @fiware-rest-path-param policyUID UID of the presentation policy
     *
     * @fiware-rest-request-param al Alias
     * @fiware-rest-request-param cs UID of the credential specification
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 Either the alias, the resource or the
     *                       presentation policy could not be found.
     *
     * @param resource
     *            Resource URI
     * @param alias
     *            Alias
     * @param credSpecUid
     *            UID of the credential specification
     * @param policyUid
     *            UID of the presantation policy
     * @return Response
     */
    @POST()
    @Path("/protected/presentationPolicyAlternatives/addCredentialSpecificationAlternative/{resource}/{policyUid}")
    public Response addCredentialSpecificationAlternative(
            @PathParam("resource") final String resource,
            @FormParam("al") final String alias,
            @FormParam("cs") final String credSpecUid,
            @PathParam("policyUid") final String policyUid) { /* [TEST EXISTS] */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            boolean found = false;

            for (PresentationPolicy pp : ppa.getPresentationPolicy()) {
                if (!pp.getPolicyUID().toString().equals(policyUid)) {
                    continue;
                }

                for (CredentialInPolicy cip : pp.getCredential()) {
                    if (cip.getAlias().toString().equals(alias)) {
                        found = true;
                        cip.getCredentialSpecAlternatives()
                                .getCredentialSpecUID()
                                .add(new URI(credSpecUid));
                        break;
                    }
                }
            }

            if (!found) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /protected/presentationPolicyAlternatives/
     *                   deleteCredentialSpecificationAlternative
     *                   /{resource}/{policyUid}
     * @fiware-rest-method POST
     *
     * @fiware-rest-description Deletes a credential specification alternative
     *                          from a presentation policy inside a
     *                          <tt>PresentationPolicyAlternatives</tt>.
     *
     * @fiware-rest-path-param resource Resource URI
     * @fiware-rest-path-param policyUid UID of the presentation policy
     *
     * @fiware-rest-request-param al Alias
     * @fiware-rest-request-param cs UID of the credential specification
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 Either the alias, the resource or the
     *                       presentation policy could not be found.
     *
     * @param resource
     *            Resource URI
     * @param alias
     *            Alias
     * @param credSpecUid
     *            UID of the credential specification
     * @param policyUid
     *            UID of the presentation policy
     * @return Response
     */
    @POST()
    @Path("/protected/presentationPolicyAlternatives/deleteCredentialSpecificationAlternative/{resource}/{policyUid}")
    public Response deleteCredentialSpecificationAlternative(
            @PathParam("resource") final String resource,
            @FormParam("al") final String alias,
            @FormParam("cs") final String credSpecUid,
            @PathParam("policyUid") final String policyUid) { /* [TEST EXISTS] */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            boolean found = false;
            URI founduid = null;

            for (PresentationPolicy pp : ppa.getPresentationPolicy()) {
                if (!pp.getPolicyUID().toString().equals(policyUid)) {
                    continue;
                }

                for (CredentialInPolicy cip : pp.getCredential()) {
                    if (cip.getAlias().toString().equals(alias)) {

                        for (URI uri : cip.getCredentialSpecAlternatives()
                                .getCredentialSpecUID()) {
                            if (uri.toString().equals(credSpecUid)) {
                                found = true;
                                founduid = uri;
                                break;
                            }
                        }

                        if (found) {
                            cip.getCredentialSpecAlternatives()
                                    .getCredentialSpecUID().remove(founduid);
                            break;
                        }
                    }
                }
            }

            if (!found) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/presentationPolicyAlternatives/addIssuerAlternative
     *                   /{resource}/{policyUid}
     * @fiware-rest-method POST
     *
     * @fiware-rest-description Adds an issuer alternative to a presentation
     *                          policy inside a
     *                          <tt>PresentationPolicyAlternatives</tt>.
     *
     * @fiware-rest-path-param resource Resource URI
     * @fiware-rest-path-param policyUid UID of the presentation policy
     *
     * @fiware-rest-request-param al Alias
     * @fiware-rest-request-param ip UID of the issuer parameters
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 Either the alias, the resource or the
     *                       presentation policy could not be found.
     *
     * @param resource
     *            Resource URI
     * @param alias
     *            Alias
     * @param issuerParamsUid
     *            UID of the issuer parameters
     * @param policyUid
     *            UID of the presentation policy
     * @return Response
     */
    @POST()
    @Path("/protected/presentationPolicyAlternatives/addIssuerAlternative/{resource}/{policyUid}")
    public Response addIssuerAlternative(
            @PathParam("resource") final String resource,
            @FormParam("al") final String alias,
            @FormParam("ip") final String issuerParamsUid,
            @PathParam("policyUid") final String policyUid) { /* [TEST EXISTS] */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            boolean found = false;

            for (PresentationPolicy pp : ppa.getPresentationPolicy()) {
                if (!pp.getPolicyUID().toString().equals(policyUid)) {
                    continue;
                }

                for (CredentialInPolicy cip : pp.getCredential()) {
                    if (cip.getAlias().toString().equals(alias)) {
                        found = true;
                        IssuerParametersUID ipuid = new IssuerParametersUID();
                        ipuid.setValue(new URI(issuerParamsUid));
                        cip.getIssuerAlternatives().getIssuerParametersUID()
                                .add(ipuid);
                        break;
                    }
                }
            }

            if (!found) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/presentationPolicyAlternatives/deleteIssuerAlternative
     *                   /{resource}/{policyUid}
     * @fiware-rest-method POST
     *
     * @fiware-rest-description Deletes an issuer alternative from a
     *                          presentation policy inside a
     *                          <tt>PresentationPolicyAlternatives</tt>.
     *
     * @fiware-rest-path-param resource Resource URI
     * @fiware-rest-path-param policyUid UID of the presentation policy
     *
     *
     * @fiware-rest-request-param al Alias
     * @fiware-rest-request-param ip UID of the issuer parameters
     *
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 Either the alias, the resource or the
     *                       presentation policy could not be found.
     *
     * @param resource
     *            Resource URI
     * @param alias
     *            Alias
     * @param issuerParamsUid
     *            UID of the issuer parameters
     * @param policyUid
     *            UID of the presentation policy
     * @return Response
     */
    @POST()
    @Path("/protected/presentationPolicyAlternatives/deleteIssuerAlternative/{resource}/{policyUid}")
    public Response deleteIssuerAlternative(
            @PathParam("resource") final String resource,
            @FormParam("al") final String alias,
            @FormParam("ip") final String issuerParamsUid,
            @PathParam("policyUid") final String policyUid) { /* [TEST EXISTS] */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            boolean found = false;
            IssuerParametersUID founduid = null;

            for (PresentationPolicy pp : ppa.getPresentationPolicy()) {
                if (!pp.getPolicyUID().toString().equals(policyUid)) {
                    continue;
                }

                for (CredentialInPolicy cip : pp.getCredential()) {
                    if (cip.getAlias().toString().equals(alias)) {
                        for (IssuerParametersUID ipuid : cip
                                .getIssuerAlternatives()
                                .getIssuerParametersUID()) {
                            if (ipuid.getValue().toString()
                                    .equals(issuerParamsUid)) {
                                founduid = ipuid;
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            cip.getIssuerAlternatives()
                                    .getIssuerParametersUID().remove(founduid);
                            break;
                        }
                    }
                }
            }

            if (!found) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/presentationPolicyAlternatives/addPolicyAlternative
     *                   /{resource}
     * @fiware-rest-method POST
     *
     * @fiware-rest-description Adds a presentation policy alternative to a
     *                          <tt>PresentationPolicyAlternatives</tt>.
     *
     * @fiware-rest-path-param resource Resource URI
     *
     * @fiware-rest-request-param puid UID of the presentation policy
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 Either the alias, the resource or the
     *                       presentation policy could not be found.
     *
     * @param resource
     *            Resource URI
     * @param policyUid
     *            UID of the presentation policy
     * @return Response
     */
    @POST()
    @Path("/protected/presentationPolicyAlternatives/addPolicyAlternative/{resource}")
    /* [TEST EXISTS] */
    public Response addPolicyAlternative(
            @PathParam("resource") final String resource,
            @FormParam("puid") final String policyUid) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            PresentationPolicy pp = new PresentationPolicy();
            pp.setPolicyUID(new URI(policyUid));
            ppa.getPresentationPolicy().add(pp);
            Message m = new Message();
            ApplicationData apd = new ApplicationData();
            apd.getContent().add("n/a");
            m.setApplicationData(apd);
            pp.setMessage(m);

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/presentationPolicyAlternatives/deletePolicyAlternative
     *                   /{resource}
     * @fiware-rest-method POST
     *
     * @fiware-rest-description Delete a presentation policy alternative from a
     *                          <tt>PresentationPolicyAlternatives</tt>.
     *
     * @fiware-rest-path-param resource Resource URI
     *
     * @fiware-rest-request-param puid UID of the presentation policy
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 Either the alias, the resource or the
     *                       presentation policy could not be found.
     *
     * @param resource
     *            Resource URI
     * @param policyUid
     *            UID of the presentation policy
     * @return Response
     */
    @POST()
    @Path("/protected/presentationPolicyAlternatives/deletePolicyAlternative/{resource}")
    /* [TEST EXISTS] */
    public Response deletePolicyAlternative(
            @PathParam("resource") final String resource,
            @FormParam("puid") final String policyUid) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            boolean found = false;
            PresentationPolicy toDelete = null;

            for (PresentationPolicy pp : ppa.getPresentationPolicy()) {
                if (!pp.getPolicyUID().toString().equals(policyUid)) {
                    continue;
                }

                toDelete = pp;
                found = true;
                break;
            }

            if (!found) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            ppa.getPresentationPolicy().remove(toDelete);

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /protected/resource/create/{resource}
     * @fiware-rest-method PUT
     *
     * @fiware-rest-description Creates a resource under the URI given as part
     *                          of the path. This will create an empty
     *                          <tt>PresentationPolicyAlternatives</tt> stored
     *                          under the resource URI as the key.
     *
     * @fiware-rest-path-param resource Resource URI
     *
     * @fiware-rest-request-param redirectURI Redirect URI (in almost all cases
     *                            this will most likely be an URL of a website)
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @param resource
     *            Resource URI
     * @param redirectURI
     *            Redirect URI
     * @return Response
     */
    @PUT()
    @Path("/protected/resource/create/{resource}")
    /* [TEST EXISTS] */
    public Response createResource(
            @PathParam("resource") final String resource,
            @FormParam("redirectURI") final String redirectURI) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = new PresentationPolicyAlternatives();
            ppa.setVersion("1.0");

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);
            verificationHelper.verificationStorage.addRedirectURI(new URI(
                    resource), new URI(redirectURI));

            return log.exit(Response.ok("OK").build());

        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * Adds an alias to a presentation policy.
     *
     * @fiware-rest-path
     *                   /protected/presentationPolicyAlternatives/addAlias/{resource
     *                   }/{policyUid}
     * @fiware-rest-method POST
     * @fiware-rest-description Adds an alias to a presentation policy in a
     *                          <code>PresentationPolicyAlternatives</code>.
     *
     * @fiware-rest-path-param resource the resource URL
     * @fiware-rest-path-param policyUid the UID of the presentation policy
     *
     * @fiware-rest-request-param al alias (must be a valid URI)
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 Error
     * @fiware-rest-response 404 Either the resource, the alias or the
     *                       presentation policy could not be found
     *
     * @param resource
     *            Resource URI
     * @param policyUid
     *            UID of the presentation policy
     * @param alias
     *            name of the alias
     * @return Response
     */
    @POST()
    @Path("/protected/presentationPolicyAlternatives/addAlias/{resource}/{policyUid}")
    public Response addAlias(@PathParam("resource") final String resource,
            @PathParam("policyUid") final String policyUid,
            @FormParam("al") final String alias) { /* [TEST EXISTS] */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            boolean found = false;
            CredentialInPolicy foundcip = null;

            for (PresentationPolicy pp : ppa.getPresentationPolicy()) {
                if (!pp.getPolicyUID().toString().equals(policyUid)) {
                    continue;
                }

                found = true;
                CredentialInPolicy cip = new CredentialInPolicy();
                cip.setCredentialSpecAlternatives(new CredentialSpecAlternatives());
                cip.setIssuerAlternatives(new IssuerAlternatives());
                cip.setAlias(new URI(alias));
                pp.getCredential().add(cip);
            }

            if (!found) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/presentationPolicyAlternatives/deleteAlias/{resource
     *                   }/{policyUid}
     * @fiware-rest-method POST
     *
     * @fiware-rest-description Deletes an alias from a presentation policy
     *                          inside a <tt>PresentationPolicyAlternatives</tt>
     *                          .
     *
     * @fiware-rest-path-param resource Resource URI
     * @fiware-rest-path-param policyUid UID of the presentation policy
     *
     * @fiware-rest-request-param al Alias
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 Either the resource, the alias or the
     *                       presentation policy could not be found.
     *
     * @param resource
     *            Resource URI
     * @param alias
     *            Alias
     * @param policyUid
     *            UID of the presentation policy
     * @return Response
     */
    @POST()
    @Path("/protected/presentationPolicyAlternatives/deleteAlias/{resource}/{policyUid}")
    public Response deleteAlias(@PathParam("resource") final String resource,
            @FormParam("al") final String alias,
            @PathParam("policyUid") final String policyUid) { /* [TEST EXISTS] */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            boolean found = false;
            CredentialInPolicy foundcip = null;

            for (PresentationPolicy pp : ppa.getPresentationPolicy()) {
                if (!pp.getPolicyUID().toString().equals(policyUid)) {
                    continue;
                }

                for (CredentialInPolicy cip : pp.getCredential()) {
                    if (cip.getAlias().toString().equals(alias)) {
                        found = true;
                        foundcip = cip;
                        break;
                    }
                }
                if (found) {
                    pp.getCredential().remove(foundcip);
                    break;
                }
            }

            if (!found) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/presentationPolicyAlternatives/deletePredicate
     *                   /{resource}/{policyUid}
     * @fiware-rest-method POST
     *
     * @fiware-rest-description Deletes a predicate from a
     *                          <tt>PresentationPolicyAlternatives</tt>.
     *
     * @fiware-rest-path-param resource Resource URI
     * @fiware-rest-path-param policyUid UID of the presentation policy
     *
     * @fiware-rest-request-param index Index of the attribute as in the list of
     *                            predicates inside the presentation policy
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 The predicate or presentation policy does not
     *                       exist.
     *
     * @param resource
     *            Resource URI
     * @param policyUid
     *            UID of the presentation policy
     * @param index
     *            Index of the predicate
     * @return Response
     */
    @POST()
    @Path("/protected/presentationPolicyAlternatives/deletePredicate/{resource}/{policyUid}")
    public Response deletePredicate(
            @PathParam("resource") final String resource,
            @PathParam("policyUid") final String policyUid,
            @FormParam("index") final int index) { /* [GUI TEST] */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            boolean found = false;

            for (PresentationPolicy pp : ppa.getPresentationPolicy()) {
                if (!pp.getPolicyUID().toString().equals(policyUid)) {
                    continue;
                }

                pp.getAttributePredicate().remove(index);
                found = true;
            }

            if (!found) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/presentationPolicyAlternatives/addPredicate/{
     *                   resource}/{policyUid}
     * @fiware-rest-method POST
     *
     * @fiware-rest-description Add a predicate to a presentation policy in a
     *                          <tt>PresentationPolicyAlternatives</tt>. The
     *                          predicate <tt>p</tt> is a function (e.g.
     *                          integer-less) with two argument. An attribute
     *                          <tt>at</tt> as lvalue and a constant value (e.g.
     *                          123) as rvalue. This method does not allow
     *                          comparing attributes with other attributes as of
     *                          now.
     *
     * @fiware-rest-path-param resource Resource URI
     * @fiware-rest-path-param policyUid UID of the presentation policy
     *
     * @fiware-rest-request-param cv Constant Value
     * @fiware-rest-request-param at Attribute
     * @fiware-rest-request-param p Predicate
     * @fiware-rest-request-param al Alias
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 Either the resource, the attribute, the alias
     *                       or the presentation policy could not be found.
     *
     * @param resource
     *            Resource URI
     * @param constantValue
     *            Constant Value
     * @param attribute
     *            Attribute
     * @param predicate
     *            Predicate
     * @param alias
     *            Alias
     * @param policyUid
     *            UID of the presentation policy
     * @return Response
     */
    @POST()
    @Path("/protected/presentationPolicyAlternatives/addPredicate/{resource}/{policyUid}")
    public Response addPredicate(@PathParam("resource") final String resource,
            @FormParam("cv") final String constantValue,
            @FormParam("at") final String attribute,
            @FormParam("p") final String predicate,
            @FormParam("al") final String alias,
            @PathParam("policyUid") final String policyUid) { /* [GUI TEST] */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            boolean found = false;

            for (PresentationPolicy pp : ppa.getPresentationPolicy()) {
                if (!pp.getPolicyUID().toString().equals(policyUid)) {
                    continue;
                }

                AttributePredicate ap = new AttributePredicate();
                ap.setFunction(new URI(predicate));
                Attribute atr = new Attribute();

                for (CredentialInPolicy cip : pp.getCredential()) {
                    for (URI credSpecUid : cip.getCredentialSpecAlternatives()
                            .getCredentialSpecUID()) {
                        CredentialSpecification credSpec = verificationHelper.keyManager
                                .getCredentialSpecification(credSpecUid);
                        for (AttributeDescription attrDesc : credSpec
                                .getAttributeDescriptions()
                                .getAttributeDescription()) {
                            if (attrDesc.getType().toString().equals(attribute)) {
                                atr.setAttributeType(attrDesc.getType());
                                atr.setCredentialAlias(new URI(alias));
                                found = true;
                            }
                        }
                    }
                }

                if (found) {
                    ap.getAttributeOrConstantValue().add(atr);
                    Element e = createW3DomElement("ConstantValue",
                            constantValue);
                    ap.getAttributeOrConstantValue().add(e);
                    pp.getAttributePredicate().add(ap);
                    break;
                }
            }

            if (!found) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NO_ATTRIB).build());
            }

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);
        } catch (RuntimeException e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }

        return Response.ok().build();
    }

    private static Element createW3DomElement(final String elementName,
            final String value) {
        Element element;
        try {
            element = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .newDocument().createElement(elementName);
        } catch (DOMException e) {
            throw new IllegalStateException("This should always work!", e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("This should always work!", e);
        }
        element.setTextContent(value);
        return element;
    }

    /**
     * @fiware-rest-path /protected/systemParameters/store
     * @fiware-rest-method PUT
     *
     * @fiware-rest-description Stores system parameters at this service.
     *
     * @fiware-rest-response 200 OK (application xml)
     * @fiware-rest-response 500 ERROR
     *
     * @fiware-rest-input-type SystemParameters
     * @param systemParameters
     *            SystemParameters
     * @return Response
     */
    @PUT()
    @Path("/protected/systemParameters/store")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeSystemParameters(
            final SystemParameters systemParameters) { /*
                                                        * [ FLOW TEST ]
                                                        */

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();
            KeyManager keyManager = verificationHelper.keyManager;

            boolean r = keyManager.storeSystemParameters(systemParameters);

            if (!r) {
                throw new RuntimeException("Could not store system parameters.");
            }

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/issuerParameters/delete/{issuerParametersUid}
     * @fiware-rest-method DELETE
     *
     * @fiware-rest-description Deletes issuer parameters.
     *
     * @fiware-rest-path-param issuerParametersUid UID of the issuer parameters
     *                         to delete
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @param issuerParametersUid
     *            UID of the IssuerParameters
     * @return Response
     */
    @DELETE()
    @Path("/protected/issuerParameters/delete/{issuerParametersUid}")
    public Response deleteIssuerParameters( /* [TEST EXISTS] */
    @PathParam("issuerParametersUid") final String issuerParametersUid) {
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
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(ERR_NOT_IMPLEMENTED)).build();
            }

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /protected/issuerParameters/store/{issuerParametersUid}
     * @fiware-rest-method PUT
     *
     * @fiware-rest-description Stores issuer parameters at this service. The
     *                          UID given as part of the path must match the UID
     *                          of the passed issuer parameters.
     *
     * @fiware-rest-path-param issuerParametersUid UID of the issuer parameters
     *                         to store
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 409 The issuerParemetersUid does not match the
     *                       actual issuer parameters' UID.
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-input-type IssuerParameters
     * @param issuerParametersUid
     *            UID of the IssuerParameters
     * @param issuerParameters
     *            IssuerParameters
     * @return Response
     */
    @PUT()
    @Path("/protected/issuerParameters/store/{issuerParametersUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    /* [TEST EXISTS] */
    public Response storeIssuerParameters(
            @PathParam("issuerParametersUid") final URI issuerParametersUid,
            final IssuerParameters issuerParameters) {

        log.entry();

        log.info("VerificationService - storeIssuerParameters - issuerParametersUid: "
                + issuerParametersUid
                + ", "
                + issuerParameters.getParametersUID());
        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();
            KeyManager keyManager = verificationHelper.keyManager;

            if (!issuerParametersUid.toString().equals(
                    issuerParameters.getParametersUID().toString())) {
                return log.exit(Response.status(Response.Status.CONFLICT)
                        .build());
            }

            boolean r = keyManager.storeIssuerParameters(issuerParametersUid,
                    issuerParameters);

            if (!r) {
                throw new RuntimeException("Could not store issuer parameters!");
            }

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /createPresentationPolicy/
     * @fiware-rest-method POST
     *
     * @fiware-rest-description Given a presentation policy template creates a
     *                          presentation policy (while also embedding nonce
     *                          bytes).
     *
     * @fiware-rest-response 200 OK (application/xml)
     * @fiware-rest-response 500 ERROR
     *
     * @fiware-rest-input-type PresentationPolicyAlternatives
     * @fiware-rest-return-type PresentationPolicyAlternatives
     * @param applicationData
     *            Application Data
     * @param presentationPolicy
     *            PresentationPolicy
     * @return Response
     */
    @POST()
    @Path("/createPresentationPolicy")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    /* [FLOW TEST] */
    public Response createPresentationPolicy(
            @PathParam("applicationData") final String applicationData,
            final PresentationPolicyAlternatives presentationPolicy) {

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            Map<URI, URI> revocationInformationUids = new HashMap<URI, URI>();

            PresentationPolicyAlternatives modifiedPresentationPolicyAlternatives = verificationHelper
                    .createPresentationPolicy(presentationPolicy,
                            applicationData, revocationInformationUids);
            log.info("VerificationService - createPresentationPolicy - done ");

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
     * @fiware-rest-path
     *                   /protected/credentialSpecification/store/{credentialSpecificationUid
     *                   }
     * @fiware-rest-method PUT
     *
     * @fiware-rest-description Stores a credential specification at this
     *                          service. The UID given as part of the path must
     *                          match the UID of the passed credential
     *                          specification.
     *
     * @fiware-rest-path-param credentialSpecificationUid UID of the credential
     *                         specification to store.
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 409 UID given on the path does not match the actual
     *                       UID.
     * @fiware-rest-response 500 ERROR
     *
     * @fiware-rest-input-type CredentialSpecification
     * @param credentialSpecificationUid
     *            UID of the credential specification
     * @param credSpec
     *            Credential specification
     * @return Response
     */
    @PUT()
    @Path("/protected/credentialSpecification/store/{credentialSpecifationUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    /* [TEST EXISTS] */
    public Response storeCredentialSpecification(
            @PathParam("credentialSpecifationUid") final URI credentialSpecificationUid,
            final CredentialSpecification credSpec) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            KeyManager keyManager = verificationHelper.keyManager;

            if (!credentialSpecificationUid.toString().equals(
                    credSpec.getSpecificationUID().toString())) {
                return log.exit(Response.status(Response.Status.CONFLICT)
                        .entity(ERR_UID).build());
            }

            boolean r = keyManager.storeCredentialSpecification(
                    credentialSpecificationUid, credSpec);

            if (!r) {
                throw new RuntimeException(
                        "Could not store the credential specification");
            }

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/credentialSpecification/get/{credentialSpecificationUid
     *                   }
     * @fiware-rest-method GET
     *
     * @fiware-rest-description Retreive a credential specification stored at
     *                          this service.
     *
     * @fiware-rest-path-param credentialSpecificationUid UID of the credential
     *                         specification to retrieve.
     *
     * @fiware-rest-response 200 OK (application/xml)
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 The credential specification could not be
     *                       found.
     *
     * @fiware-rest-return-type CredentialSpecification
     * @param credSpecUid
     *            UID of the credential specification
     * @return Response
     */
    @GET()
    @Path("/protected/credentialSpecification/get/{credentialSpecificationUid}")
    /* [TEST EXISTS] */
    public Response getCredentialSpecification(
            @PathParam("credentialSpecificationUid") final String credSpecUid) {
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            KeyManager keyManager = verificationHelper.keyManager;

            CredentialSpecification credSpec = keyManager
                    .getCredentialSpecification(new URI(credSpecUid));

            if (credSpec == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            return log.exit(Response.ok(
                    of.createCredentialSpecification(credSpec),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/credentialSpecification/delete/{credentialSpecificationUid
     *                   }
     * @fiware-rest-method DELETE
     *
     * @fiware-rest-description Deletes a credential specification.
     *
     * @fiware-rest-path-param credentialSpecificationUid UID of the credential
     *                         specification to delete.
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @param credSpecUid
     *            UID of the credential specification to delete
     * @return Response
     */
    @DELETE()
    @Path("/protected/credentialSpecification/delete/{credentialSpecificationUid}")
    public Response deleteCredentialSpecification( /* [TEST EXISTS] */
    @PathParam("credentialSpecificationUid") final String credSpecUid) {
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
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(ERR_NOT_IMPLEMENTED)).build();
            }

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /protected/resource/delete/{resource}
     * @fiware-rest-method DELETE
     *
     * @fiware-rest-description Deletes a resource. This means, it deletes the
     *                          associated redirect URI and
     *                          <tt>PresentationPolicyAlternatives.</tt>
     *
     * @fiware-rest-path-param resource Resource URI
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @param resource
     *            Resource URI
     * @return Response
     */
    @DELETE()
    @Path("/protected/resource/delete/{resource}")
    public Response deleteResource(@PathParam("resource") final String resource) { /*
                                                                                    * [
                                                                                    * TEST
                                                                                    * EXISTS
                                                                                    * ]
                                                                                    */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            try {
                verificationHelper.verificationStorage
                        .deleteRedirectURI(new URI(resource));
            } catch (Exception e) {
                log.catching(e);
            }
            try {
                verificationHelper.verificationStorage
                        .deletePresentationPolicyAlternatives(new URI(resource));
            } catch (Exception e) {
                log.catching(e);
            }

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/presentationPolicyAlternatives/store/{resource
     *                   }
     * @fiware-rest-method PUT
     *
     * @fiware-rest-description Stores <tt>PresentationPolicyAlternatives</tt>
     *                          using the resource URI as part of the path as
     *                          the key (i.e. associates the
     *                          <tt>PresentationPolicyAlternatives</tt> with the
     *                          resource URI)
     *
     * @fiware-rest-path-param resource Resource URI
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @fiware-rest-input-type PresentationPolicyAlternatives
     * @param resource
     *            Resource URI
     * @param ppa
     *            PresentationPolicyAlternatives
     * @return Response
     */
    @PUT()
    @Path("/protected/presentationPolicyAlternatives/store/{resource}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storePresentationPolicy( /* [FLOW TEST] */
    @PathParam("resource") final String resource,
            final PresentationPolicyAlternatives ppa) {

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            verificationHelper.verificationStorage
                    .addPresentationPolicyAlternatives(new URI(resource), ppa);

            return log.exit(Response.ok("OK").build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path
     *                   /protected/presentationPolicyAlternatives/get/{resource}
     * @fiware-rest-method GET
     *
     * @fiware-rest-description Retrieves
     *                          <tt>PresentationPolicyAlternatives</tt>.
     *
     * @fiware-rest-path-param resource Resource URI the
     *                         <tt>PresentationPolicyAlternatives</tt> are
     *                         associated with.
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-response 404 <tt>PresentationPolicyAlternatives</tt> could
     *                       not be found.
     *
     * @fiware-rest-return-type PresentationPolicyAlternatives
     * @param resource
     *            Resource URI
     * @return Response
     */
    @GET()
    @Path("/protected/presentationPolicyAlternatives/get/{resource}")
    public Response getPresentationPolicy(
            @PathParam("resource") final String resource) { /*
                                                             * [ TEST EXISTS ]
                                                             */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));
            if (ppa == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            return log.exit(Response.ok(
                    of.createPresentationPolicyAlternatives(ppa),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /protected/presentationPolicyAlternatives/list
     * @fiware-rest-method GET
     *
     * @fiware-rest-description Lists all presentation policies stored at this
     *                          service.
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @fiware-rest-return-type PresentationPolicyAlternativesCollection
     * @return Response
     */
    @GET()
    @Path("/protected/presentationPolicyAlternatives/list")
    public Response presentationPolicies() { /* [TEST EXISTS] */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternativesCollection ppac = new PresentationPolicyAlternativesCollection();
            ppac.presentationPolicyAlternatives = verificationHelper.verificationStorage
                    .listPresentationPolicyAlternatives();
            List<String> uris = new ArrayList<String>();
            for (URI uri : verificationHelper.verificationStorage
                    .listResourceURIs()) {
                uris.add(uri.toString());
            }
            List<String> redirectURIs = new ArrayList<String>();
            for (URI uri : verificationHelper.verificationStorage
                    .listResourceURIs()) {
                redirectURIs.add(verificationHelper.verificationStorage
                        .getRedirectURI(uri).toString());
            }
            ppac.redirectURIs = redirectURIs;
            ppac.uris = uris;
            return log.exit(Response.ok(ppac, MediaType.APPLICATION_XML)
                    .build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /protected/redirectURI/store/{resource}
     * @fiware-rest-method PUT
     *
     * @fiware-rest-description Stores a redirect URI (URL) and associates it
     *                          with a resource.
     *
     * @fiware-rest-path-param resource Name/URI of the resource.
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @fiware-rest-input-type String
     * @param resourceUri
     *            URI of the resource
     * @param redirectUri
     *            Redirect URL
     * @return Response
     */
    @PUT()
    @Path("/protected/redirectURI/store/{resource}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeRedirectURI(
            @PathParam("resource") final String resourceUri,
            final String redirectUri) { /* [FLOW TEST] */

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

    /**
     * @fiware-rest-path /protected/redirectURI/get/{resource}
     * @fiware-rest-method GET
     *
     * @fiware-rest-description Retrieves a redirect URI.
     *
     * @fiware-rest-path-param resource Resource URI
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @fiware-rest-return-type String
     * @param resource
     *            Resource URI
     * @return Response
     */
    @GET()
    @Path("/protected/redirectURI/get/{resource}")
    public Response getRedirectURI(@PathParam("resource") final String resource) { /*
                                                                                    * [
                                                                                    * FLOW
                                                                                    * TEST
                                                                                    * ]
                                                                                    */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            URI uri = verificationHelper.verificationStorage
                    .getRedirectURI(new URI(resource));

            if (uri == null) {
                return log.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(ERR_NOT_FOUND).build());
            }

            return log.exit(Response.ok(uri.toString()).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /requestResource/{resource}
     *
     * @fiware-rest-description First step for a user to request a resource.
     *                          This method will look-up the corresponding
     *                          presentation policy alternatives and return them
     *                          for the user to create presentation tokens for.
     *
     * @fiware-rest-path-param resource Name/URI of the resource to request
     *                         access to/for.
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @param resource
     *            URI of the resource
     * @return Response
     */
    @GET()
    @Path("/requestResource/{resource}")
    public Response requestResource(@PathParam("resource") final String resource) { /*
                                                                                     * [
                                                                                     * FLOW
                                                                                     * TEST
                                                                                     * ]
                                                                                     */
        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            String key = System.currentTimeMillis() + ";"
                    + new SecureRandom().nextInt();
            byte[] nonce = verificationHelper.generateNonce();

            ppa = verificationHelper.modifyPPA(ppa, key, nonce,
                    ServicesConfiguration.getVerifierIdentity());

            synchronized (nonces) {
                nonces.put(key, nonce);
            }

            return log.exit(Response.ok(
                    of.createPresentationPolicyAlternatives(ppa)).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /requestResource2/{resource}
     * @fiware-rest-method POST
     *
     * @fiware-rest-description The second step for a user to request access to
     *                          a resource. This method will verify the
     *                          presentation token for the user and if
     *                          successful return the redirect URI and an access
     *                          token.
     *
     * @fiware-rest-path-param resource Name/URI of the resource.
     *
     * @fiware-rest-input-type PresentationToken
     * @fiware-rest-return-type String
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 403 FORBIDDEN (Access to resource denied)
     * @fiware-rest-response 500 ERROR
     *
     * @param resource
     *            Resource URI
     * @param pt
     *            PresentationToken
     * @return Response
     */
    @POST()
    @Path("/requestResource2/{resource}/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response requestResource2(
            @PathParam("resource") final String resource,
            final PresentationToken pt) { /* [FLOW TEST] */

        log.entry();

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            PresentationPolicyAlternatives ppa = verificationHelper.verificationStorage
                    .getPresentationPolicyAlternatives(new URI(resource));

            log.info("APD 0 is : "
                    + pt.getPresentationTokenDescription().getMessage()
                            .getApplicationData().getContent().get(0));
            String uid = (String) pt.getPresentationTokenDescription()
                    .getMessage().getApplicationData().getContent().get(0);

            for (PresentationPolicy pp : ppa.getPresentationPolicy()) {
                byte[] nonce;

                synchronized (nonces) {
                    nonce = nonces.get(uid);
                    nonces.remove(uid);
                }

                pp.getMessage().setNonce(nonce);
                pp.getMessage().getApplicationData().getContent().clear();
                pp.getMessage().getApplicationData().getContent().add(uid);
                VerifierIdentity vi = new VerifierIdentity();
                vi.getContent().clear();
                vi.getContent()
                        .add(ServicesConfiguration.getVerifierIdentity());
                pp.getMessage().setVerifierIdentity(vi);
            }

            log.info("VI 0 is "
                    + pt.getPresentationTokenDescription().getMessage()
                            .getVerifierIdentity().getContent().get(0));

            PresentationPolicyAlternativesAndPresentationToken ppat = of
                    .createPresentationPolicyAlternativesAndPresentationToken();
            ppat.setPresentationPolicyAlternatives(ppa);
            ppat.setPresentationToken(pt);

            Response r = this
                    .verifyTokenAgainstPolicy(of
                            .createPresentationPolicyAlternativesAndPresentationToken(ppat));
            if (r.getStatus() != 200) {
                return log.exit(Response.status(Response.Status.FORBIDDEN)
                        .entity("NOT OK").build());
            }

            URI redirect = verificationHelper.verificationStorage
                    .getRedirectURI(new URI(resource));
            String token = generateAccessToken();

            log.info("VPut: " + token + "," + resource);
            synchronized (accessTokens) {
                accessTokens.put(token, resource);
            }

            return log.exit(Response.ok(
                    redirect.toString() + "?accesstoken="
                            + URLEncoder.encode(token, "UTF-8")).build());
        } catch (RuntimeException e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /verifyAccessToken
     * @fiware-rest-method GET
     *
     * @fiware-rest-description Verifies that an access token is valid. This
     *                          means, that a user successfully verified his
     *                          credentials at this service for a resource. This
     *                          method will return the name/URI of the resource
     *                          the user requested. Once verified the access
     *                          token is deleted.
     *
     * @fiware-rest-request-param accesstoken The access token to verify.
     *
     * @fiware-rest-response 403 Token not valid.
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @fiware-rest-return-type String
     *
     * @param accessToken
     *            Access Token
     * @return Response
     */
    @GET()
    @Path("/verifyAccessToken/")
    public Response verifyAccessToken(
            @QueryParam("accesstoken") final String accessToken) { /*
                                                                    * [FLOW
                                                                    * TEST]
                                                                    */
        log.info("VGet: " + accessToken);
        synchronized (accessTokens) {

            if (ServicesConfiguration.getAllowFakeAccesstoken()) {
                if (accessToken.equals("FAKE")) {
                    return Response.ok("FAKE").build();
                }
            }

            if (!accessTokens.containsKey(accessToken)) {
                for (String key : accessTokens.keySet()) {
                    log.warn(" atkn: " + key);
                }
                log.info("Accesstoken not found!" + accessToken);
                return Response.status(Response.Status.FORBIDDEN).build();
            } else {
                log.info("Accesstoken found! " + accessToken);
                String resourceString = accessTokens.get(accessToken);
                log.info("Resource string is: " + resourceString);
                accessTokens.remove(accessToken);
                return Response.ok(resourceString).build();
            }
        }
    }

    /**
     * @fiware-rest-path /protected/loadSettings/
     * @fiware-rest-method POST
     *
     * @fiware-rest-description Download and load settings from an issuer or any
     *                          settings provider. This method will cause the
     *                          user service to make a <tt>GET</tt> request to
     *                          the specified <tt>url</tt> and download the
     *                          contents which must be valid <tt>Settings</tt>.
     *                          DO NOT use this method with untrusted URLs or
     *                          issuers (or any other settings providers) with
     *                          DIFFERENT system parameters as this method will
     *                          overwrite existing system parameters. (see
     *                          {@link #getSettings()})
     *
     * @fiware-rest-request-param url a valid URL (String)
     *
     * @fiware-rest-response 200 OK
     * @fiware-rest-response 500 ERROR
     *
     * @param url
     *            URL to download settings from.
     * @return Response
     */
    @POST()
    @Path("/protected/loadSettings/")
    public Response loadSettings(@QueryParam("url") final String url) { /*
                                                                         * [FLOW
                                                                         * TEST]
                                                                         */
        log.entry();

        try {
            Settings settings = (Settings) RESTHelper.getRequest(url,
                    Settings.class);

            for (IssuerParameters ip : settings.issuerParametersList) {
                Response r = this.storeIssuerParameters(ip.getParametersUID(),
                        ip);
                if (r.getStatus() != 200) {
                    throw new RuntimeException(
                            "Could not load issuer parameters!");
                }
            }

            for (CredentialSpecification cs : settings.credentialSpecifications) {
                Response r = this.storeCredentialSpecification(
                        cs.getSpecificationUID(), cs);
                if (r.getStatus() != 200) {
                    throw new RuntimeException(
                            "Could not load credential specification!");
                }
            }

            Response r = this.storeSystemParameters(settings.systemParameters);
            log.info(settings.systemParameters + "|"
                    + settings.systemParameters.toString());
            if (r.getStatus() != 200) {
                throw new RuntimeException("Could not load system parameters!");
            }

            return log.exit(Response.ok().build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    /**
     * @fiware-rest-path /getSettings/
     * @fiware-rest-method GET
     *
     * @fiware-rest-description Returns the settings of the service as obtained
     *                          from an issuance service. Settings includes
     *                          issuer parameters, credential specifications and
     *                          the system parameters. This method may thus be
     *                          used to retrieve all credential specifications
     *                          stored at the user service and their
     *                          corresponding issuer parameters. The return type
     *                          of this method is <tt>Settings</tt>.
     *
     *                          The user service is capable of downloading
     *                          settings from an issuer (or from anything that
     *                          provides settings). To download settings use
     *                          <tt>/loadSettings?url=...</tt> (
     *                          {@link #loadSettings(String)}).
     *
     * @fiware-rest-response 200 OK (application/xml)
     * @fiware-rest-response 500 ERROR
     * @fiware-rest-return-type Settings
     * @return Response
     */
    @GET()
    @Path("/getSettings/")
    public Response getSettings() { /* [TEST EXISTS] */
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
            try {
                settings.systemParameters = /* SystemParametersUtil.serialize */(instance.keyManager
                        .getSystemParameters());
            } catch (Exception e) {
                log.catching(e);
            }

            return log.exit(Response.ok(settings, MediaType.APPLICATION_XML)
                    .build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(ExceptionDumper.dumpExceptionStr(e, log)))
                    .build();
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
