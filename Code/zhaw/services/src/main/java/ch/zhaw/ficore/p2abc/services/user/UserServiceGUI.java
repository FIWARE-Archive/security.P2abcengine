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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.ExceptionDumper;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.helpers.user.UserGUI;
import ch.zhaw.ficore.p2abc.storage.JdbcURIBytesStorage;
import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;
import ch.zhaw.ficore.p2abc.xml.AuthInfoSimple;
import ch.zhaw.ficore.p2abc.xml.AuthenticationRequest;
import ch.zhaw.ficore.p2abc.xml.CredentialCollection;
import ch.zhaw.ficore.p2abc.xml.IssuanceRequest;
import ch.zhaw.ficore.p2abc.xml.Settings;

import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.B;
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
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

import eu.abc4trust.returnTypes.IssuanceReturn;
import eu.abc4trust.returnTypes.ObjectFactoryReturnTypes;
import eu.abc4trust.returnTypes.UiIssuanceArguments;
import eu.abc4trust.returnTypes.UiIssuanceReturn;
import eu.abc4trust.returnTypes.UiPresentationArguments;
import eu.abc4trust.returnTypes.UiPresentationReturn;
import eu.abc4trust.returnTypes.ui.TokenCandidatePerPolicy;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.Credential;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationToken;

@Path("/user-gui")
public class UserServiceGUI {

    private final ObjectFactory of = new ObjectFactory();
    private Logger log = LogManager.getLogger();

    private static java.util.Map<String, String> uiContextToURL = new HashMap<String, String>();
    private static java.util.Map<String, String> uiContextToResource = new HashMap<String, String>();
    private static URIBytesStorage urlStorage;
    
    private final static String errMissingUIArgs = "Did not receive any UI presentation arguments. This is most likely due to you not " +
                                                "having the required credentials to request the resource. ";
    
    private final static String errMissingPPA = "Did not receive any presentation policy alternatives. This is most likely due to the fact " +
                                                "that the resource does not exist at the verifier. ";
    
    
    
    static {
        try {
            urlStorage = new JdbcURIBytesStorage("URIBytesStorage", "usergui_urls");
        }
        catch(Exception e) {
            e.printStackTrace(); //ignore it, will die later on in the process I guess.
        }
    }

    public static synchronized String getURL(String uiContext) {
        return uiContextToURL.get(uiContext);
    }

    public static synchronized void putURL(String uiContext, String url) {
        uiContextToURL.put(uiContext, url);
    }

    public static synchronized String getResource(String uiContext) {
        return uiContextToResource.get(uiContext);
    }

    public static synchronized void putResource(String uiContext, String url) {
        uiContextToResource.put(uiContext, url);
    }

    @Context
    HttpServletRequest request;

    @GET()
    @Path("/profile/")
    public Response profile() {
        log.entry();

        try {
            Html html = UserGUI.getHtmlPramble("Profile", request);
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text("Profile")));

            String text = "Welcome to your profile! Here you can edit and manage your personal data and settings.";
            P p = new P().setCSSClass("info");
            mainDiv.appendChild(p);
            p.appendChild(new Text(text));
            p.appendChild(new Br());
            text = "Credentials contain attributes issued to you by issuers. Credential specifications specify what attributes a credential can or has to contain." +
            "You can also define aliases for URLs which are required for obtaining credentials or requesting resources. An alias for an URL " +
                    "is just a shorthand name you can choose so you don't have to remember long URLs.";
            p.appendChild(new Text(text));

            Ul ul = new Ul();
            ul.appendChild(new Li().appendChild(new A()
                    .setHref("./credentials").appendChild(
                            new Text("Manage credentials"))));
            ul.appendChild(new Li().appendChild(new A().setHref(
                    "./credentialSpecifications").appendChild(
                    new Text("Manage credential specifications"))));
            ul.appendChild(new Li().appendChild(new A().setHref(
                    "./urls").appendChild(new Text("Manage URL aliases"))));

            mainDiv.appendChild(ul);

            return log.exit(Response.ok(html.write()).build());

        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/issuerParameters/")
    public Response issuerParameters() {
        log.entry();

        try {
            Settings settings = (Settings) RESTHelper.getRequest(
                    ServicesConfiguration.getUserServiceURL() + "getSettings/", Settings.class);

            Html html = UserGUI.getHtmlPramble("Issuer Parameters", request);
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text(
                    "Issuer Parameters")));

            List<IssuerParameters> issuerParams = settings.issuerParametersList;

            Table tbl = new Table();
            Tr tr = null;

            tr = new Tr()
                    .appendChild(
                            new Td().appendChild(new Text(
                                    "Issuer Parameters Uid")))
                    .appendChild(
                            new Td().appendChild(new Text(
                                    "Credential Specification Uid")))
                    .appendChild(new Td().appendChild(new Text("Action")))
                    .setCSSClass("heading");
            tbl.appendChild(tr);

            for (IssuerParameters ip : issuerParams) {
                String cs = ip.getCredentialSpecUID().toString();
                String is = ip.getParametersUID().toString();

                Form f = new Form("./deleteIssuerParameters").setMethod("post")
                        .setCSSClass("nopad");
                f.appendChild(new Input().setType("hidden").setName("is")
                        .setValue(is));
                f.appendChild(new Input().setType("submit").setValue("Delete"));

                tr = new Tr().appendChild(new Td().appendChild(new Text(is)))
                        .appendChild(new Td().appendChild(new Text(cs)))
                        .appendChild(new Td().appendChild(f));
                tbl.appendChild(tr);
            }
            mainDiv.appendChild(tbl);

            return Response.ok(html.write()).build();
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @POST()
    @Path("/deleteIssuerParameters") 
    public Response deleteIssuerParameters(
            @FormParam("is") String issuerParamsUid) {
        log.entry();
        
        try {
            RESTHelper.deleteRequest(ServicesConfiguration.getUserServiceURL() + "issuerParameters/delete/"
                    + URLEncoder.encode(issuerParamsUid, "UTF-8"));
            return issuerParameters();
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }

    @POST()
    @Path("/requestResource3/")
    public Response requestResource3(@FormParam("policyId") String policyId,
            @FormParam("candidateId") String candidateId,
            @FormParam("pseudonymId") String pseudonymId,
            @FormParam("uic") String uiContext,
            @FormParam("apdata") String applicationData) {
        log.entry();

        try {
            UiPresentationReturn uir = new UiPresentationReturn();
            uir.uiContext = new URI(uiContext);
            uir.chosenPresentationToken = Integer.parseInt(candidateId);
            uir.chosenPseudonymList = Integer.parseInt(pseudonymId);
            uir.metadataToChange = new HashMap<>();

            PresentationToken pt = (PresentationToken) RESTHelper.postRequest(
                    ServicesConfiguration.getUserServiceURL() + "createPresentationTokenUi",
                    RESTHelper.toXML(UiPresentationReturn.class, uir),
                    PresentationToken.class);
            
            log.info("Get " + uiContext + "," + getResource(uiContext));

            String redirectURI = (String) RESTHelper.postRequest(
                    getURL(uiContext)
                            + "/requestResource2/"
                            + URLEncoder
                                    .encode(getResource(uiContext), "UTF-8"),
                    RESTHelper.toXML(PresentationToken.class,
                            of.createPresentationToken(pt)));

            Html html = UserGUI.getHtmlPramble("Request Resource [3]", request);
            Div mainDiv = new Div();
            html.appendChild(UserGUI.getBody(mainDiv));

            P p = new P()
                    .setCSSClass("info")
                    .appendChild(
                            new Text(
                                    "You have successfully verified your credential "
                                            + "at the verifier. Please follow the link below to access the resource you requested."));

            mainDiv.appendChild(p);

            A a = new A().setHref(redirectURI).appendChild(
                    new Text(redirectURI));
            mainDiv.appendChild(a);

            return log.exit(Response.ok(html.write()).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }

    @POST()
    @Path("/requestResource2")
    public Response requestResource(@FormParam("vu") String verificationURL,
            @FormParam("r") String resource) {
        log.entry();

        try {

            PresentationPolicyAlternatives ppa = (PresentationPolicyAlternatives) RESTHelper
                    .getRequest(verificationURL + "/requestResource/"
                            + URLEncoder.encode(resource, "UTF-8"),
                            PresentationPolicyAlternatives.class);
            
            if(ppa == null) {
                return log.exit(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(UserGUI.errorPage(errMissingPPA, request).write()).build());
            }

            UiPresentationArguments args = (UiPresentationArguments) RESTHelper
                    .postRequest(
                            ServicesConfiguration.getUserServiceURL() + "createPresentationToken",
                            RESTHelper.toXML(
                                    PresentationPolicyAlternatives.class,
                                    of.createPresentationPolicyAlternatives(ppa)),
                            UiPresentationArguments.class);
            
            if(args == null) {
                return log.exit(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(UserGUI.errorPage(errMissingUIArgs, request).write()).build());
            }

            putURL(args.uiContext.toString(), verificationURL);
            putResource(args.uiContext.toString(), resource);
            
            log.info("Put " + args.uiContext.toString() + "," + resource);

            return this.presentationArguments(ObjectFactoryReturnTypes
                    .wrap(args));
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }

    @GET()
    @Path("/requestResource/")
    public Response requestResource() {
        log.entry();

        try {
            Html html = UserGUI.getHtmlPramble("Request resource [1]", request);
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));

            mainDiv.appendChild(new H2().appendChild(new Text(
                    "Request resource")));
            
            mainDiv.appendChild(new P()
            .setCSSClass("info")
            .appendChild(
                    new Text(
                            "Please enter the information required for the resource. " +
                    "If the the Verifier field is blank please add an alias for an URL first via the Profile page.")));

            Form f = new Form("./requestResource2").setMethod("post");
            Table tbl = new Table();
            Tr tr = null;
            
            Select s = new Select().setName("vu");
            
            
            List<String> keys = urlStorage.keysAsStrings();
            List<byte[]> values = urlStorage.values();
            List<String> urls = new ArrayList<String>();
            
            for(byte[] b : values) {
                urls.add((String) SerializationUtils.deserialize(b));
            }
            
            for(int i = 0; i < keys.size(); i++) {
                Option o = new Option().appendChild(new Text(keys.get(i))).setValue(urls.get(i));
                s.appendChild(o);
            }

            tr = new Tr().appendChild(
                    new Td().appendChild(new Label().appendChild(new Text(
                            "Verifier:")))).appendChild(
                    new Td().appendChild(s));
            tbl.appendChild(tr);
            tr = new Tr().appendChild(
                    new Td().appendChild(new Label().appendChild(new Text(
                            "Resource:")))).appendChild(
                    new Td().appendChild(new Input().setType("text").setName(
                            "r")));
            tbl.appendChild(tr);
            f.appendChild(tbl);
            f.appendChild(new Input().setType("submit").setValue("Request"));
            mainDiv.appendChild(f);
            return log.exit(Response.ok(html.write()).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @POST()
    @Path("/deleteURL")
    public Response deleteURL(@FormParam("name") String name) {
        log.entry();
        
        try {
            urlStorage.delete(name);
            return log.exit(urls());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @POST()
    @Path("/addURL")
    public Response addURL(@FormParam("name") String name, @FormParam("url") String url) {
        log.entry();
        
        try {
            urlStorage.put(name, SerializationUtils.serialize(url));
            return log.exit(urls());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/urls")
    public Response urls() {
        log.entry();
        
        try {
            List<String> keys = urlStorage.keysAsStrings();
            List<byte[]> values = urlStorage.values();
            List<String> urls = new ArrayList<String>();
            
            for(byte[] b : values) {
                urls.add((String) SerializationUtils.deserialize(b));
            }
            
            Html html = UserGUI.getHtmlPramble("URLs", request);
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));
            
            String text = "Here you can define aliases for URLs which are later required when you choose the issuer or verifier while obtaining credentials or requesting a resource.";
            P p = new P().setCSSClass("info");
            mainDiv.appendChild(p);
            p.appendChild(new Text(text));

            mainDiv.appendChild(new H2().appendChild(new Text("URLs")));
            
            Table tbl = new Table();
            Tr heading = new Tr().setCSSClass("heading");
            heading.appendChild(new Td().appendChild(new Text("Name")));
            heading.appendChild(new Td().appendChild(new Text("URL")));
            heading.appendChild(new Td().appendChild(new Text("Action")));
            tbl.appendChild(heading);
            int i = 0;
            for(String key : keys) {
                Tr tr = new Tr();
                tr.appendChild(new Td().appendChild(new Text(key)));
                tr.appendChild(new Td().appendChild(new Text(urls.get(i))));
                Form f = new Form("./deleteURL").setMethod("post").setCSSClass("nopad");
                f.appendChild(new Input().setType("submit").setValue("delete"));
                f.appendChild(new Input().setType("hidden").setName("name").setValue(key));
                tr.appendChild(new Td().appendChild(f));
                i++;
                tbl.appendChild(tr);
            }
            mainDiv.appendChild(tbl);
            
            tbl = new Table();
            Tr tr = new Tr();
            tr.appendChild(new Td().appendChild(new Text("Name: ")));
            tr.appendChild(new Td().appendChild(new Input().setType("text").setName("name")));
            tbl.appendChild(tr);
            tr = new Tr();
            tr.appendChild(new Td().appendChild(new Text("URL: ")));
            tr.appendChild(new Td().appendChild(new Input().setType("text").setName("url")));
            tbl.appendChild(tr);
            
            Form f = new Form("./addURL").setMethod("post");
            f.appendChild(tbl);
            f.appendChild(new Input().setType("Submit").setValue("Add"));
            mainDiv.appendChild(f);
            
            
            return log.exit(Response.ok(html.write()).build());
            
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }

    @GET()
    @Path("/credentialSpecifications/")
    public Response credentialSpecifications() {
        log.entry();

        try {
            Settings settings = (Settings) RESTHelper.getRequest(ServicesConfiguration.getUserServiceURL()
                    + "getSettings/", Settings.class);

            List<CredentialSpecification> credSpecs = settings.credentialSpecifications;

            Html html = UserGUI.getHtmlPramble("Profile", request);
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
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }

    @GET()
    @Path("/credentials/")
    public Response credentials() {

        log.entry();

        try {
            CredentialCollection credCol = (CredentialCollection) RESTHelper
                    .getRequest(ServicesConfiguration.getUserServiceURL() + "credential/list",
                            CredentialCollection.class);

            List<Credential> credentials = credCol.credentials;

            Html html = UserGUI.getHtmlPramble("Profile", request);
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));

            mainDiv.appendChild(new H2().appendChild(new Text("Profile")));
            mainDiv.appendChild(new H3().appendChild(new Text("Credentials")));

            for (Credential cred : credentials) {
                URI uri = cred.getCredentialDescription().getCredentialUID();
                Div credDiv = UserGUI.getDivForCredential(cred);
                Form f = new Form("./deleteCredential");
                f.setMethod("post");
                credDiv.appendChild(f);
                f.appendChild(new Input().setType("submit").setValue(
                        "Delete credential"));
                f.appendChild(new Input().setType("hidden").setName("credUid")
                        .setValue(uri.toString()));
                mainDiv.appendChild(credDiv);
            }

            return log.exit(Response.ok(html.write()).build());
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }

    @POST()
    @Path("/deleteCredential/")
    public Response deleteCredential(@FormParam("credUid") String credUid) {
        try {

            RESTHelper.deleteRequest(ServicesConfiguration.getUserServiceURL() + "credential/delete/"
                    + URLEncoder.encode(credUid, "UTF-8"));

            String text = "";
            String cls = "";

            text = "You've successfully deleted the credential!";
            cls = "success";

            Html html = UserGUI.getHtmlPramble("Delete Credential", request);
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
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }

    @POST()
    @Path("/issuanceArguments/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response issuanceArguments(JAXBElement<IssuanceReturn> args_) throws ClientHandlerException, UniformInterfaceException, UnsupportedEncodingException, JAXBException, NamingException {
        UiIssuanceArguments args = args_.getValue().uia;
        if (args.tokenCandidates.size() == 1
                && args.tokenCandidates.get(0).credentials.size() == 0) {
            Html html = UserGUI.getHtmlPramble("Identity Selection", request);
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
                    args.uiContext.toString(), "", "", ServicesConfiguration.getUserServiceURL());
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
     * @return Response
     */
    @POST
    @Path("/obtainCredential2")
    public Response obtainCredential2(@FormParam("un") String username,
            @FormParam("pw") String password,
            @FormParam("is") String issuerUrl,
            @FormParam("cs") String credSpecUid) {
        try {
            // Make an IssuanceRequest
            IssuanceRequest ir = new IssuanceRequest();

            AuthInfoSimple authSimple = new AuthInfoSimple();
            authSimple.password = password;
            authSimple.username = username;

            ir.credentialSpecificationUid = credSpecUid;
            ir.authRequest = new AuthenticationRequest(authSimple);

            log.warn("issuerUrl: " + issuerUrl);

            IssuanceMessageAndBoolean issuanceMessageAndBoolean = (IssuanceMessageAndBoolean) RESTHelper
                    .postRequest(issuerUrl + "/issuanceRequest",
                            RESTHelper.toXML(IssuanceRequest.class, ir),
                            IssuanceMessageAndBoolean.class);

            IssuanceMessage firstIssuanceMessage = issuanceMessageAndBoolean
                    .getIssuanceMessage();

            IssuanceReturn issuanceReturn = (IssuanceReturn) RESTHelper
                    .postRequest(
                            ServicesConfiguration.getUserServiceURL() + "issuanceProtocolStep",
                            RESTHelper.toXML(
                                    IssuanceMessage.class,
                                    of.createIssuanceMessage(firstIssuanceMessage)),
                            IssuanceReturn.class);

            putURL(issuanceReturn.uia.uiContext.toString(), issuerUrl);

            return issuanceArguments(ObjectFactoryReturnTypes
                    .wrap(issuanceReturn));
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
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
     * @return Response
     */
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

            String issuerUrl = getURL(uiContext);

            IssuanceMessage secondIssuanceMessage = (IssuanceMessage) RESTHelper
                    .postRequest(ServicesConfiguration.getUserServiceURL() + "issuanceProtocolStepUi",
                            RESTHelper.toXML(UiIssuanceReturn.class, uir),
                            IssuanceMessage.class);

            IssuanceMessageAndBoolean thirdIssuanceMessageAndBoolean = (IssuanceMessageAndBoolean) RESTHelper
                    .postRequest(
                            issuerUrl + "/issuanceProtocolStep",
                            RESTHelper.toXML(
                                    IssuanceMessage.class,
                                    of.createIssuanceMessage(secondIssuanceMessage)),
                            IssuanceMessageAndBoolean.class);
            IssuanceMessage thirdIssuanceMessage = thirdIssuanceMessageAndBoolean
                    .getIssuanceMessage();

            RESTHelper.postRequest(
                    ServicesConfiguration.getUserServiceURL() + "issuanceProtocolStep",
                    RESTHelper.toXML(IssuanceMessage.class,
                            of.createIssuanceMessage(thirdIssuanceMessage)),
                    IssuanceReturn.class);

            Html html = UserGUI
                    .getHtmlPramble("Obtain Credential [3]", request);
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
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }

    }

    /**
     * This is the entry point for the User to obtain a credential from an
     * issuer. This method will display a webpage asking for the required data
     * and will direct the User to obtainCredential2
     * 
     * @return Response
     */
    @GET
    @Path("/obtainCredential/")
    public Response obtainCredential() {
        try {
            Html html = UserGUI
                    .getHtmlPramble("Obtain Credential [1]", request);
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(UserGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text(
                    "Obtain Credential")));
            mainDiv.appendChild(new P()
                    .setCSSClass("info")
                    .appendChild(
                            new Text(
                                    "Please enter the information required to obtain the credential. " +
                            "If the the Issuer field is blank please add an alias for an URL first via the Profile page.")));
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
            //row.appendChild(new Td().appendChild(new Input().setType("text")
            //        .setName("is")));
            Select s = new Select().setName("is");
            
            row.appendChild(new Td().appendChild(s));
            //        .setName("is")));
            
            List<String> keys = urlStorage.keysAsStrings();
            List<byte[]> values = urlStorage.values();
            List<String> urls = new ArrayList<String>();
            
            for(byte[] b : values) {
                urls.add((String) SerializationUtils.deserialize(b));
            }
            
            for(int i = 0; i < keys.size(); i++) {
                Option o = new Option().appendChild(new Text(keys.get(i))).setValue(urls.get(i));
                s.appendChild(o);
            }
            
            tbl.appendChild(row);

            row = new Tr();
            row.appendChild(new Td().appendChild(new Label()
                    .appendChild(new Text("Credential specification:"))));
            Select sel = new Select().setName("cs");
            row.appendChild(new Td().appendChild(sel));
            tbl.appendChild(row);

            f.appendChild(new Input().setType("submit").setValue("Obtain"));

            mainDiv.appendChild(f);

            Settings settings = (Settings) RESTHelper.getRequest(ServicesConfiguration.getUserServiceURL()
                    + "getSettings/", Settings.class);

            List<CredentialSpecification> credSpecs = settings.credentialSpecifications;

            for (CredentialSpecification credSpec : credSpecs) {
                URI uri = credSpec.getSpecificationUID();
                sel.appendChild(new Option().appendChild(new Text(uri
                        .toString())));
            }

            return Response.ok(html.write()).build();
        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }

    @POST
    @Path("/presentationArguments/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response presentationArguments(
            JAXBElement<UiPresentationArguments> args_) throws ClientHandlerException, UniformInterfaceException, UnsupportedEncodingException, JAXBException, NamingException {
        UiPresentationArguments args = args_.getValue();
        Html html = UserGUI.getHtmlPramble("Candidate selection", request);
        Div mainDiv = new Div();
        html.appendChild(UserGUI.getBody(mainDiv));

        for (TokenCandidatePerPolicy tcpp : args.tokenCandidatesPerPolicy) {
            List<Object> content = tcpp.policy.getMessage().getApplicationData().getContent();
            if(content == null || content.size() < 1)
                throw new RuntimeException("Expecting application data!");
            content = tcpp.policy.getMessage().getVerifierIdentity().getContent();
            if(content == null || content.size() < 1)
                throw new RuntimeException("Expecting verifier identity!");
            String vi = (String)content.get(0);
            
            mainDiv.appendChild(new H2().appendChild(new Text(tcpp.policy.getPolicyUID().toString())));
            mainDiv.appendChild(new B().appendChild(new Text("Verifier Identity: ")));
            mainDiv.appendChild(new Text(vi));
            
            
            Div div = UserGUI.getDivForTokenCandidates(tcpp.tokenCandidates,
                    tcpp.policyId, args.uiContext.toString(), (String)content.get(0),
                    "./requestResource3", ServicesConfiguration.getUserServiceURL());

            mainDiv.appendChild(div);
        }

        return Response.ok(html.write()).build();
    }
    
    @POST()
    @Path("/loadSettings2")
    public Response loadSettings2(@FormParam("url") String url) {
        log.entry();
        
        try {
            RESTHelper.postRequest(ServicesConfiguration.getUserServiceURL() + "loadSettings?url=" + URLEncoder.encode(url, "UTF-8"));
            
            Html html = UserGUI.getHtmlPramble("Load Settings", request);
            Div mainDiv = new Div();
            html.appendChild(UserGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text("Load Settings")));
            P p = new P().setCSSClass("success");
            p.appendChild(new Text("You've successfully downloaded the settings."));
            mainDiv.appendChild(p);
            return log.exit(Response.ok(html.write()).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/loadSettings")
    public Response loadSettings() {
        log.entry();
        
        try {
            Html html = UserGUI.getHtmlPramble("Load Settings", request);
            Div mainDiv = new Div();
            html.appendChild(UserGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text("Load Settings")));
            P p = new P().setCSSClass("info");
            p.appendChild(new Text("Download settings from a settings provider or issuer. Please be careful to only" +
            		" download settings from trusted sources as this will overwrite certain critical settings."));
            mainDiv.appendChild(p);
            Form f = new Form("./loadSettings2").setMethod("post");
            Table tbl = new Table();
            tbl.appendChild(new Tr().appendChild(new Td().appendChild(new Label().appendChild(new Text("Settings provider URL:"))))
                    .appendChild(new Td().appendChild(new Input().setType("text").setName("url"))));
            f.appendChild(tbl);
            f.appendChild(new Input().setType("Submit").setValue("Download settings"));
            mainDiv.appendChild(f);
            return log.exit(Response.ok(html.write()).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }

}
