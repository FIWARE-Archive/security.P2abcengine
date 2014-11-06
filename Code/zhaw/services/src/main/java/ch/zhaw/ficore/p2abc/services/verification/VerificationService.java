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
import java.util.HashMap;
import java.util.Map;
//import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.ServiceType;
import ch.zhaw.ficore.p2abc.services.StorageModuleFactory;
import ch.zhaw.ficore.p2abc.services.helpers.verification.VerificationHelper;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.exceptions.TokenVerificationException;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
//import eu.abc4trust.ri.servicehelper.verifier.VerificationHelper;
import eu.abc4trust.xml.ABCEBoolean;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationPolicyAlternativesAndPresentationToken;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.PresentationTokenDescription;
import eu.abc4trust.xml.SystemParameters;

@Path("/verification")
public class VerificationService {

    private final Logger log = LogManager.getLogger();
    private final static String errMagicCookie = "Magic cookie is not correct!";

    ObjectFactory of = new ObjectFactory();

    public VerificationService() throws Exception {
        System.out.println("VerificationService");

        if (VerificationHelper.isInit()) {
            System.out.println(" - Helper already init'ed");
        } else {
            String fileStoragePrefix = "verifier_storage/";
            if(System.getProperty("PathToUProveExe", null) == null) {
                String uprovePath = "./../uprove/UProveWSDLService/ABC4Trust-UProve/bin/Release";
                System.setProperty("PathToUProveExe", uprovePath);
            }

           


            VerificationHelper.initInstance(CryptoEngine.IDEMIX,
                    StorageModuleFactory.getModulesForServiceConfiguration(
                            ServiceType.VERIFICATION));
        }
    }

    @Path("/verifyTokenAgainstPolicy")
    @POST()
    public Response verifyTokenAgainstPolicy(JAXBElement<PresentationPolicyAlternativesAndPresentationToken> ppaAndpt, @QueryParam("store") String storeString) throws TokenVerificationException, CryptoEngineException{
        log.entry();
        
        try {
        
            boolean store = false;
            if ((storeString != null) && storeString.toUpperCase().equals("TRUE")) {
                store = true;
            }
            VerificationHelper verficationHelper = VerificationHelper.getInstance();
            
            PresentationPolicyAlternativesAndPresentationToken value = ppaAndpt.getValue();
            PresentationPolicyAlternatives presentationPolicyAlternatives = value.getPresentationPolicyAlternatives();
            PresentationToken presentationToken = value.getPresentationToken();
            PresentationTokenDescription ptd = verficationHelper.engine.verifyTokenAgainstPolicy(presentationPolicyAlternatives, presentationToken, store);
            return log.exit(Response.ok(of.createPresentationTokenDescription(ptd),
                    MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    @Path("/getToken")
    @GET()
    public Response getToken(@QueryParam("tokenUID") URI tokenUid){
        log.entry();
        
        try {
        VerificationHelper verificationHelper = VerificationHelper.getInstance();
        PresentationToken pt = verificationHelper.engine.getToken(tokenUid);
        return log.exit(Response.ok(of.createPresentationToken(pt),
                MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    @Path("/deleteToken")
    @POST()
    public Response deleteToken(@QueryParam("tokenUID") URI tokenUid){
        log.entry();
        
        try {
            boolean result = VerificationHelper.getInstance().engine.deleteToken(tokenUid);
            JAXBElement<Boolean> jaxResult = new JAXBElement<Boolean>(new QName("deleteToken"), Boolean.TYPE, result);
            return log.exit(Response.ok(jaxResult, MediaType.APPLICATION_XML).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    @POST()
    @Path("/storeSystemParameters/{magicCookie}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeSystemParameters(@PathParam("magicCookie") String magicCookie,
            SystemParameters systemParameters) {
        
        log.entry();
        
        if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
            return log.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();
            KeyManager keyManager = verificationHelper.keyManager;

            boolean r = keyManager.storeSystemParameters(systemParameters);

            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
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
    @Path("/storeIssuerParameters/{magicCookie}/{issuerParametersUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeIssuerParameters(
            @PathParam("magicCookie") String magicCookie, 
            @PathParam("issuerParametersUid") URI issuerParametersUid,
            IssuerParameters issuerParameters) {
        
        log.entry();
        
        if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
            return log.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

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

            return log.exit(Response.ok(of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } 
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    @GET()
    @Path("/createPresentationPolicy")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    @Produces(MediaType.TEXT_XML)
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

            return log.exit(Response.ok(of
                    .createPresentationPolicyAlternatives(modifiedPresentationPolicyAlternatives),
                    MediaType.APPLICATION_XML).build());
        } 
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

    @PUT()
    @Path("/storeCredentialSpecification/{magicCookie}/{credentialSpecifationUid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response storeCredentialSpecification(
            @PathParam("magicCookie") String magicCookie, 
            @PathParam("credentialSpecifationUid") URI credentialSpecifationUid,
            CredentialSpecification credSpec) {
        log.entry();
        
        if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
            return log.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

        try {
            VerificationHelper verificationHelper = VerificationHelper
                    .getInstance();

            KeyManager keyManager = verificationHelper.keyManager;

            boolean r = keyManager.storeCredentialSpecification(
                    credentialSpecifationUid, credSpec);

            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
            createABCEBoolean.setValue(r);

            return log.exit(Response.ok(of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } 
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response.serverError().build());
        }
    }

}
