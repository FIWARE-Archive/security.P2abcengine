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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.ExceptionDumper;
import ch.zhaw.ficore.p2abc.services.ServiceType;
import ch.zhaw.ficore.p2abc.services.StorageModuleFactory;
import ch.zhaw.ficore.p2abc.services.helpers.user.UserGUI;
import ch.zhaw.ficore.p2abc.services.helpers.user.UserHelper;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthInfoSimple;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthenticationRequest;
import ch.zhaw.ficore.p2abc.services.issuance.xml.IssuanceRequest;

import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Body;
import com.hp.gagawa.java.elements.Br;
import com.hp.gagawa.java.elements.Div;
import com.hp.gagawa.java.elements.Form;
import com.hp.gagawa.java.elements.H1;
import com.hp.gagawa.java.elements.H2;
import com.hp.gagawa.java.elements.H3;
import com.hp.gagawa.java.elements.H4;
import com.hp.gagawa.java.elements.Head;
import com.hp.gagawa.java.elements.Html;
import com.hp.gagawa.java.elements.Input;
import com.hp.gagawa.java.elements.Label;
import com.hp.gagawa.java.elements.Li;
import com.hp.gagawa.java.elements.Option;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Select;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Title;
import com.hp.gagawa.java.elements.Tr;
import com.hp.gagawa.java.elements.Ul;

import eu.abc4trust.abce.internal.user.credentialManager.CredentialManagerException;
import eu.abc4trust.abce.internal.user.policyCredentialMatcher.PolicyCredentialMatcherImpl;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.exceptions.CannotSatisfyPolicyException;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyManagerException;
import eu.abc4trust.returnTypes.IssuanceReturn;
import eu.abc4trust.returnTypes.ObjectFactoryReturnTypes;
import eu.abc4trust.returnTypes.UiIssuanceArguments;
import eu.abc4trust.returnTypes.UiIssuanceReturn;
import eu.abc4trust.returnTypes.UiPresentationArguments;
import eu.abc4trust.returnTypes.UiPresentationReturn;
import eu.abc4trust.returnTypes.ui.TokenCandidatePerPolicy;
import eu.abc4trust.util.DummyForNewABCEInterfaces;
import eu.abc4trust.xml.ABCEBoolean;
import eu.abc4trust.xml.Attribute;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
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

    private static java.util.Map<String, String> uiContextToIssuerURL = new HashMap<String, String>();

    public static synchronized String getIssuerURL(String uiContext) {
        return uiContextToIssuerURL.get(uiContext);
    }

    public static synchronized void putIssuerURL(String uiContext,
            String issuerURL) {
        uiContextToIssuerURL.put(uiContext, issuerURL);
    }

    @GET()
    @Path("/status/")
    public Response status() {
        return Response.ok().build();
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
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response canBeSatisfied(PresentationPolicyAlternatives p) {
        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        try {
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
    public Response createPresentationToken(PresentationPolicyAlternatives p) {
        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        DummyForNewABCEInterfaces d = null;
        try {
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

    @POST()
    @Path("/createPresentationTokenUi/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response createPresentationToken(UiPresentationReturn upr) {

        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        try {
            PresentationToken presentationToken = instance.getEngine()
                    .createPresentationToken(upr);
            return log.exit(Response.ok(
                    of.createPresentationToken(presentationToken),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    @GET()
    @Path("/profile/")
    public Response profile() {
        log.entry();

        try {
            Html html = UserGUI.getHtmlPramble("Profile");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text("Profile")));

            String text = "Welcome to your profile! Here you can edit and manage your personal data and settings.";
            P p = new P().setCSSClass("info");
            mainDiv.appendChild(p);
            p.appendChild(new Text(text));
            p.appendChild(new Br());
            text = "Credentials contain attributes issued to you by issuers. Credential specifications specify what attributes a credential can or has to contain.";
            p.appendChild(new Text(text));

            Ul ul = new Ul();
            ul.appendChild(new Li().appendChild(new A()
                    .setHref("./credentials").appendChild(
                            new Text("Manage credentials"))));
            ul.appendChild(new Li().appendChild(new A().setHref(
                    "./credentialSpecifications").appendChild(
                    new Text("Manage credential specifications"))));

            mainDiv.appendChild(ul);

            return log.exit(Response.ok(html.write()).build());

        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log)).write())
                    .build());
        }
    }

    @GET()
    @Path("/credentialSpecifications/")
    public Response credentialSpecifications() {
        log.entry();

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();

            List<CredentialSpecification> credSpecs = new ArrayList<CredentialSpecification>();

            for (URI uri : instance.keyStorage.listUris()) {
                Object obj = SerializationUtils.deserialize(instance.keyStorage
                        .getValue(uri));
                if (obj instanceof CredentialSpecification) {
                    credSpecs.add((CredentialSpecification) obj);
                }
            }

            Html html = UserGUI.getHtmlPramble("Profile");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));

            mainDiv.appendChild(new H2().appendChild(new Text("Profile")));
            mainDiv.appendChild(new H3().appendChild(new Text(
                    "Credential Specifications")));

            for (CredentialSpecification credSpec : credSpecs) {

                Div credDiv = new Div().setCSSClass("credDiv");
                mainDiv.appendChild(credDiv);

                AttributeDescriptions attribDescs = credSpec
                        .getAttributeDescriptions();
                List<AttributeDescription> attrDescs = attribDescs
                        .getAttributeDescription();

                Table tbl = new Table();
                credDiv.appendChild(new H4().appendChild(new Text(credSpec
                        .getSpecificationUID().toString())));
                credDiv.appendChild(tbl);
                Tr tr = null;
                tr = new Tr()
                        .setCSSClass("heading")
                        .appendChild(new Td().appendChild(new Text("Name")))
                        .appendChild(new Td().appendChild(new Text("Type")))
                        .appendChild(new Td().appendChild(new Text("Encoding")));
                tbl.appendChild(tr);

                for (AttributeDescription attrDesc : attrDescs) {
                    String name = attrDesc.getFriendlyAttributeName().get(0)
                            .getValue();
                    String encoding = attrDesc.getEncoding().toString();
                    String type = attrDesc.getDataType().toString();
                    tr = new Tr()
                            .appendChild(new Td().appendChild(new Text(name)))
                            .appendChild(new Td().appendChild(new Text(type)))
                            .appendChild(
                                    new Td().appendChild(new Text(encoding)));
                    tbl.appendChild(tr);
                }
            }

            return log.exit(Response.ok(html.write()).build());

        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log)).write())
                    .build());
        }
    }

    @GET()
    @Path("/credentials/")
    public Response credentials() {

        log.entry();

        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();

            List<URI> credentialUids;

            credentialUids = instance.credentialManager.listCredentials();

            Html html = UserGUI.getHtmlPramble("Profile");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));

            mainDiv.appendChild(new H2().appendChild(new Text("Profile")));
            mainDiv.appendChild(new H3().appendChild(new Text("Credentials")));

            for (URI uri : credentialUids) {
                Div credDiv = new Div().setCSSClass("credDiv");
                mainDiv.appendChild(credDiv);
                eu.abc4trust.xml.Credential cred = instance.credentialManager
                        .getCredential(uri);
                CredentialDescription credDesc = cred
                        .getCredentialDescription();
                String credSpec = credDesc.getCredentialSpecificationUID()
                        .toString();
                credDiv.appendChild(new H4().appendChild(new Text(credSpec
                        + " (" + uri.toString() + ")")));
                List<Attribute> attribs = credDesc.getAttribute();
                Table tbl = new Table();
                credDiv.appendChild(tbl);
                Tr tr = null;
                tr = new Tr().setCSSClass("heading")
                        .appendChild(new Td().appendChild(new Text("Name")))
                        .appendChild(new Td().appendChild(new Text("Value")));
                tbl.appendChild(tr);
                for (Attribute attrib : attribs) {
                    AttributeDescription attribDesc = attrib
                            .getAttributeDescription();
                    String name = attribDesc.getFriendlyAttributeName().get(0)
                            .getValue();
                    tr = new Tr().appendChild(
                            new Td().appendChild(new Text(name))).appendChild(
                            new Td().appendChild(new Text(attrib
                                    .getAttributeValue().toString())));
                    tbl.appendChild(tr);
                }
                Form f = new Form("./deleteCredential");
                f.setMethod("post");
                credDiv.appendChild(f);
                f.appendChild(new Input().setType("submit").setValue(
                        "Delete credential"));
                f.appendChild(new Input().setType("hidden").setName("credUid")
                        .setValue(uri.toString()));
            }

            return log.exit(Response.ok(html.write()).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log)).write())
                    .build());
        }
    }

    @POST()
    @Path("/deleteCredential/")
    public Response deleteCredential(@FormParam("credUid") String credUid) {
        try {
            this.initializeHelper();

            UserHelper instance = UserHelper.getInstance();

            boolean success = instance.credentialManager
                    .deleteCredential(new URI(credUid));

            String text = "";
            String cls = "";

            if (success) {
                text = "You've successfully deleted the credential!";
                cls = "success";
            } else {
                text = "Could not delete credential. Sorry about that.";
                cls = "error";
            }

            Html html = UserGUI.getHtmlPramble("Delete Credential");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text(
                    "Delete Credential")));
            mainDiv.appendChild(new P().setCSSClass(cls).appendChild(
                    new Text(text)));
            return log.exit(Response.ok(html.write()).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log)).write())
                    .build());
        }
    }

    @POST()
    @Path("/issuanceArguments/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response issuanceArguments(JAXBElement<IssuanceReturn> args_) {
        UiIssuanceArguments args = args_.getValue().uia;
        if (args.tokenCandidates.size() == 1
                && args.tokenCandidates.get(0).credentials.size() == 0) {
            Html html = UserGUI.getHtmlPramble("Identity Selection");
            Head head = new Head().appendChild(new Title()
                    .appendChild(new Text("Obtain Credential [2]")));
            html.appendChild(head);
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text(
                    "Obtain Credential")));
            Div div = new Div();
            div.appendChild(new P()
                    .setCSSClass("info")
                    .appendChild(
                            new Text(
                                    "The issuer isn't asking you to reveal anything.")));
            Form f = new Form("./obtainCredential3");
            f.setMethod("post");
            f.appendChild(new Input().setType("hidden").setName("uic")
                    .setValue(args.uiContext.toString()));
            f.appendChild(new Input().setType("hidden").setName("policyId") // chosenPolicy
                    .setValue(Integer.toString(0)));
            f.appendChild(new Input().setType("hidden").setName("candidateId") // chosenPresentationToken
                                                                               // or
                                                                               // chosenIssuanceToken
                                                                               // (weird
                                                                               // stuff)
                    .setValue(Integer.toString(0)));
            f.appendChild(new Input().setType("hidden").setName("pseudonymId") // chosenPseudonymList
                    .setValue(Integer.toString(0)));
            f.appendChild(new Input().setType("submit").setValue("Continue"));
            div.appendChild(f);

            mainDiv.appendChild(div);
            return Response.ok(html.write()).build();
        } else {
            Html html = new Html();
            Head head = new Head().appendChild(new Title()
                    .appendChild(new Text("Identity Selection")));
            html.appendChild(head);
            Div mainDiv = new Div();
            html.appendChild(new Body().appendChild(mainDiv));
            mainDiv.appendChild(new H1().appendChild(new Text(
                    "Obtain Credential")));
            Div div = UserGUI.getDivForTokenCandidates(args.tokenCandidates, 0,
                    args.uiContext.toString());
            mainDiv.appendChild(div);
            return Response.ok(html.write()).build();
        }
    }

    /**
     * This is the second step for the User to obtain a credential from an
     * issuer. This method will display the Identity Selection and direct the
     * User to obtainCredential3
     * 
     * @param username
     *            Username (authInfo)
     * @param password
     *            Password (authInfo)
     * @param issuerUrl
     *            URL of the issuance service
     * @param credSpecUid
     *            UID of the CredentialSpecification of the Credential to obtain
     * @return
     */
    @POST
    @Path("/obtainCredential2")
    public Response obtainCredential2(@FormParam("un") String username,
            @FormParam("pw") String password,
            @FormParam("is") String issuerUrl,
            @FormParam("cs") String credSpecUid) {
        try {
            /* Make an IssuanceRequest */
            IssuanceRequest ir = new IssuanceRequest();

            AuthInfoSimple authSimple = new AuthInfoSimple();
            authSimple.password = password;
            authSimple.username = username;

            ir.credentialSpecificationUid = credSpecUid;
            ir.authRequest = new AuthenticationRequest(authSimple);

            log.warn("issuerUrl: " + issuerUrl);

            IssuanceMessageAndBoolean issuanceMessageAndBoolean = (IssuanceMessageAndBoolean) UserGUI
                    .postRequest(issuerUrl + "/issuanceRequest",
                            UserGUI.toXML(IssuanceRequest.class, ir),
                            IssuanceMessageAndBoolean.class);

            IssuanceMessage firstIssuanceMessage = issuanceMessageAndBoolean
                    .getIssuanceMessage();

            Response r = issuanceProtocolStep(of
                    .createIssuanceMessage(firstIssuanceMessage));
            if (r.getStatus() != 200)
                throw new RuntimeException("Internal step failed!");

            @SuppressWarnings("unchecked")
            IssuanceReturn issuanceReturn = ((JAXBElement<IssuanceReturn>) r
                    .getEntity()).getValue();

            putIssuerURL(issuanceReturn.uia.uiContext.toString(), issuerUrl);

            return issuanceArguments(ObjectFactoryReturnTypes
                    .wrap(issuanceReturn));
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log)).write())
                    .build());
        }
    }

    /**
     * This is the third step for a User to obtain a credential from an issuer.
     * 
     * @param policyId
     *            Chosen Policy
     * @param candidateId
     *            Chosen Candidate
     * @param pseudonymId
     *            Chosen Pseudonymlist
     * @param uiContext
     *            Context identifier
     * @return
     */
    @SuppressWarnings("unchecked")
    @POST
    @Path("/obtainCredential3")
    public Response obtainCredential3(@FormParam("policyId") String policyId,
            @FormParam("candidateId") String candidateId,
            @FormParam("pseudonymId") String pseudonymId,
            @FormParam("uic") String uiContext) {
        try {
            UiIssuanceReturn uir = new UiIssuanceReturn();
            uir.uiContext = new URI(uiContext);
            uir.chosenIssuanceToken = Integer.parseInt(candidateId);
            uir.chosenPseudonymList = Integer.parseInt(pseudonymId);

            String issuerUrl = getIssuerURL(uiContext);

            Response r = issuanceProtocolStep(uir);
            if (r.getStatus() != 200)
                throw new RuntimeException("Internal step failed!");

            IssuanceMessage secondIssuanceMessage = ((JAXBElement<IssuanceMessage>) r
                    .getEntity()).getValue();
            log.warn(UserGUI.toXML(IssuanceMessage.class,
                    of.createIssuanceMessage(secondIssuanceMessage)));
            IssuanceMessageAndBoolean thirdIssuanceMessageAndBoolean = (IssuanceMessageAndBoolean) UserGUI
                    .postRequest(
                            issuerUrl + "/issuanceProtocolStep",
                            UserGUI.toXML(
                                    IssuanceMessage.class,
                                    of.createIssuanceMessage(secondIssuanceMessage)),
                            IssuanceMessageAndBoolean.class);
            IssuanceMessage thirdIssuanceMessage = thirdIssuanceMessageAndBoolean
                    .getIssuanceMessage();

            r = issuanceProtocolStep(of
                    .createIssuanceMessage(thirdIssuanceMessage));
            if (r.getStatus() != 200)
                throw new RuntimeException("Internal step failed!");

            Html html = UserGUI.getHtmlPramble("Obtain Credential [3]");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            mainDiv.appendChild(new H2().appendChild(new Text(
                    "Obtain Credential")));
            html.appendChild(UserGUI.getBody(mainDiv));
            mainDiv.appendChild(new P()
                    .setCSSClass("success")
                    .appendChild(
                            new Text(
                                    "You've successfully obtained the requested credential from the issuer.")));

            return Response.ok(html.write()).build();
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }

    }

    /**
     * This is the entry point for the User to obtain a credential from an
     * issuer. This method will display a webpage asking for the required data
     * and will direct the User to obtainCredential2
     * 
     * @return
     */
    @GET
    @Path("/obtainCredential/")
    public Response obtainCredential() {
        try {
            Html html = UserGUI.getHtmlPramble("Obtain Credential [1]");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text(
                    "Obtain Credential")));
            mainDiv.appendChild(new P()
                    .setCSSClass("info")
                    .appendChild(
                            new Text(
                                    "Please enter the information required to obtain the credential.")));
            Form f = new Form("./obtainCredential2");
            f.setMethod("post");

            Table tbl = new Table();
            Tr row = null;
            f.appendChild(tbl);

            row = new Tr();
            row.appendChild(new Td().appendChild(new Label()
                    .appendChild(new Text("Username:"))));
            row.appendChild(new Td().appendChild(new Input().setType("text")
                    .setName("un")));
            tbl.appendChild(row);

            row = new Tr();
            row.appendChild(new Td().appendChild(new Label()
                    .appendChild(new Text("Password:"))));
            row.appendChild(new Td().appendChild(new Input()
                    .setType("password").setName("pw")));
            tbl.appendChild(row);

            row = new Tr();
            row.appendChild(new Td().appendChild(new Label()
                    .appendChild(new Text("Issuer:"))));
            row.appendChild(new Td().appendChild(new Input().setType("text")
                    .setName("is")));
            tbl.appendChild(row);

            row = new Tr();
            row.appendChild(new Td().appendChild(new Label()
                    .appendChild(new Text("Credential specification:"))));
            Select sel = new Select().setName("cs");
            row.appendChild(new Td().appendChild(sel));
            tbl.appendChild(row);

            f.appendChild(new Input().setType("submit").setValue("Obtain"));

            mainDiv.appendChild(f);

            this.initializeHelper();
            UserHelper instance = UserHelper.getInstance();

            for (URI uri : instance.keyStorage.listUris()) {
                Object obj = SerializationUtils.deserialize(instance.keyStorage
                        .getValue(uri));
                if (obj instanceof CredentialSpecification) {
                    sel.appendChild(new Option().appendChild(new Text(uri
                            .toString())));
                }
            }

            return Response.ok(html.write()).build();
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
        }
    }

    @POST
    @Path("/presentationArguments/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response presentationArguments(
            JAXBElement<UiPresentationArguments> args_) {
        UiPresentationArguments args = args_.getValue();
        Html html = new Html();
        Head head = new Head();
        head.appendChild(new Title()
                .appendChild(new Text("Candidate Selection")));

        html.appendChild(head);

        Div mainDiv = new Div();

        html.appendChild(new Body().appendChild(mainDiv));

        for (TokenCandidatePerPolicy tcpp : args.tokenCandidatesPerPolicy) {

            Div div = UserGUI.getDivForTokenCandidates(tcpp.tokenCandidates,
                    tcpp.policyId, args.uiContext.toString());

            mainDiv.appendChild(div);
        }

        return Response.ok(html.write()).build();
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
    public Response issuanceProtocolStep(JAXBElement<IssuanceMessage> jm) {

        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        IssuanceMessage m = jm.getValue();

        DummyForNewABCEInterfaces d = null;
        try {
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

    @POST()
    @Path("/issuanceProtocolStepUi/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response issuanceProtocolStep(UiIssuanceReturn uir) {

        log.entry();

        this.initializeHelper();

        UserHelper instance = UserHelper.getInstance();

        try {
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
        } catch (Exception e) {
            log.catching(e);
            return log.exit(ExceptionDumper.dumpException(e, log));
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

            return log.exit(Response.ok(
                    of.createCredentialDescription(credDesc),
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

    @PUT()
    @Path("/storeSystemParameters/")
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
                System.out.println("Smartcards: " + uri);
            }
        } catch (Exception ex) {
            System.out.println("Create UserHelper FAILED " + ex);
            ex.printStackTrace();
        }
    }

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
