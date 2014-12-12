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

package ch.zhaw.ficore.p2abc.services.verification;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.ExceptionDumper;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.helpers.verification.VerificationGUI;
import ch.zhaw.ficore.p2abc.xml.PresentationPolicyAlternativesCollection;
import ch.zhaw.ficore.p2abc.xml.Settings;

import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.B;
import com.hp.gagawa.java.elements.Br;
import com.hp.gagawa.java.elements.Div;
import com.hp.gagawa.java.elements.Form;
import com.hp.gagawa.java.elements.H2;
import com.hp.gagawa.java.elements.H3;
import com.hp.gagawa.java.elements.H4;
import com.hp.gagawa.java.elements.H5;
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
import com.hp.gagawa.java.elements.Tr;
import com.hp.gagawa.java.elements.Ul;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.AttributePredicate;
import eu.abc4trust.xml.AttributePredicate.Attribute;
import eu.abc4trust.xml.CredentialInPolicy;
import eu.abc4trust.xml.CredentialInPolicy.IssuerAlternatives.IssuerParametersUID;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.PresentationPolicy;
import eu.abc4trust.xml.PresentationPolicyAlternatives;

@Path("/verification-gui")
public class VerificationServiceGUI {
    
    @Context
    HttpServletRequest request;
    
    private Logger log = LogManager.getLogger();
    private static String verificationServiceURL = ServicesConfiguration.getVerificationServiceURL();
    private static List<String> predicateFunctions;
    
    static {
        predicateFunctions = new ArrayList<String>();
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:string-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:boolean-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:integer-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:date-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:time-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:dateTime-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:anyURI-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:integer-greater-than");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:integer-greater-than-or-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:integer-less-than");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:integer-less-than-or-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:date-greater-than");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:date-greater-than-or-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:date-less-than");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:date-less-than-or-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:dateTime-greater-than");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:dateTime-greater-than-or-equal");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:dateTime-less-than");
        predicateFunctions.add("urn:oasis:names:tc:xacml:1.0:function:dateTime-less-than-or-equal");
        predicateFunctions.add("urn:abc4trust:1.0:function:string-not-equal");
        predicateFunctions.add("urn:abc4trust:1.0:function:boolean-not-equal");
        predicateFunctions.add("urn:abc4trust:1.0:function:integer-not-equal");
        predicateFunctions.add("urn:abc4trust:1.0:function:date-not-equal");
        predicateFunctions.add("urn:abc4trust:1.0:function:time-not-equal");
        predicateFunctions.add("urn:abc4trust:1.0:function:dateTime-not-equal");
        predicateFunctions.add("urn:abc4trust:1.0:function:anyURI-not-equal");
        predicateFunctions.add("urn:abc4trust:1.0:function:string-equal-oneof");
        predicateFunctions.add("urn:abc4trust:1.0:function:boolean-equal-oneof");
        predicateFunctions.add("urn:abc4trust:1.0:function:integer-equal-oneof");
        predicateFunctions.add("urn:abc4trust:1.0:function:date-equal-oneof");
        predicateFunctions.add("urn:abc4trust:1.0:function:time-equal-oneof");
        predicateFunctions.add("urn:abc4trust:1.0:function:dateTime-equal-oneof");
        predicateFunctions.add("urn:abc4trust:1.0:function:anyURI-equal-oneof");
    }
    
    @GET()
    @Path("/protected/profile/")
    public Response profile() {
        log.entry();

        try {
            Html html = VerificationGUI.getHtmlPramble("Profile", request);
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(VerificationGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text("Profile")));

            String text = "Welcome to your profile! Here you can edit and manage your personal data and settings.";
            P p = new P().setCSSClass("info");
            mainDiv.appendChild(p);
            p.appendChild(new Text(text));
            p.appendChild(new Br());
            text = "Credential specifications specify what attributes a credential can or has to contain.";
            p.appendChild(new Text(text));

            Ul ul = new Ul();
            ul.appendChild(new Li().appendChild(new A().setHref(
                    "./credentialSpecifications").appendChild(
                    new Text("Manage credential specifications"))));
            ul.appendChild(new Li().appendChild(new A().setHref(
                    "./issuerParameters").appendChild(
                    new Text("Manage issuer parameters"))));
            ul.appendChild(new Li().appendChild(new A().setHref(
                    "./presentationPolicies").appendChild(
                    new Text("Manage presentation policies and/or resources"))));

            mainDiv.appendChild(ul);

            return log.exit(Response.ok(html.write()).build());

        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @POST()
    @Path("/protected/deleteIssuerParameters") 
    public Response deleteIssuerParameters(
            @FormParam("is") String issuerParamsUid) {
        log.entry();
        
        try {
            RESTHelper.deleteRequest(verificationServiceURL + "protected/issuerParameters/delete/"
                    + URLEncoder.encode(issuerParamsUid, "UTF-8"));
            return issuerParameters();
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @POST()
    @Path("/protected/deleteCredentialSpecification") 
    public Response deleteCredentialSpecification(
            @FormParam("cs") String credSpecUid) {
        log.entry();
        
        try {
            RESTHelper.deleteRequest(verificationServiceURL + "protected/credentialSpecification/delete/"
                    + URLEncoder.encode(credSpecUid, "UTF-8"));
            return credentialSpecifications();
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/protected/issuerParameters/")
    public Response issuerParameters() {
        log.entry();

        try {
            Settings settings = (Settings) RESTHelper.getRequest(
                    verificationServiceURL + "getSettings/", Settings.class);

            Html html = VerificationGUI.getHtmlPramble("Issuer Parameters", request);
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(VerificationGUI.getBody(mainDiv));
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
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @POST()
    @Path("/protected/addCredSpecAlt/")
    public Response addCredSpecAlt(@FormParam("resource") String resource, @FormParam("al") String alias,
            @FormParam("cs") String credSpecUid, @FormParam("puid") String puid) {
        log.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("cs", credSpecUid);
            params.add("al", alias);
            
            RESTHelper.postRequest(verificationServiceURL + "protected/presentationPolicy/addCredentialSpecificationAlternative/"
                    + URLEncoder.encode(resource, "UTF-8") + "/" + URLEncoder.encode(puid, "UTF-8"), params);
            
            return log.exit(presentationPolicy(resource));
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @POST()
    @Path("/protected/deleteCredSpecAlt/")
    public Response deleteCredSpecAlt(@FormParam("resource") String resource, @FormParam("al") String alias,
            @FormParam("cs") String credSpecUid, @FormParam("puid") String puid) {
        log.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("cs", credSpecUid);
            params.add("al", alias);
            
            RESTHelper.postRequest(verificationServiceURL + "protected/presentationPolicy/deleteCredentialSpecificationAlternative/"
                    + URLEncoder.encode(resource, "UTF-8") + "/" + URLEncoder.encode(puid, "UTF-8"), params);
            
            return log.exit(presentationPolicy(resource));
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @POST()
    @Path("/protected/addIssuerAlt/")
    public Response addIssuerAlt(@FormParam("resource") String resource, @FormParam("al") String alias,
            @FormParam("ip") String issuerParamsUid, @FormParam("puid") String puid) {
        log.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("ip", issuerParamsUid);
            params.add("al", alias);
            
            RESTHelper.postRequest(verificationServiceURL + "protected/presentationPolicy/addIssuerAlternative/"
                    + URLEncoder.encode(resource, "UTF-8") + "/" + URLEncoder.encode(puid, "UTF-8"), params);
            
            return log.exit(presentationPolicy(resource));
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @POST()
    @Path("/protected/deleteIssuerAlt/")
    public Response deleteIssuerAlt(@FormParam("resource") String resource, @FormParam("al") String alias,
            @FormParam("ip") String issuerParamsUid, @FormParam("puid") String puid) {
        log.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("ip", issuerParamsUid);
            params.add("al", alias);
            
            RESTHelper.postRequest(verificationServiceURL + "protected/presentationPolicy/deleteIssuerAlternative/"
                    + URLEncoder.encode(resource, "UTF-8") + "/" + URLEncoder.encode(puid, "UTF-8"), params);
            
            return log.exit(presentationPolicy(resource));
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @POST()
    @Path("/protected/deleteAlias/")
    public Response deleteAlias(@FormParam("resource") String resource, @FormParam("al") String alias,
            @FormParam("puid") String puid) {
        log.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("al", alias);
            
            RESTHelper.postRequest(verificationServiceURL + "protected/presentationPolicy/deleteAlias/"
                    + URLEncoder.encode(resource, "UTF-8") + "/" + URLEncoder.encode(puid, "UTF-8"), params);
            
            return log.exit(presentationPolicy(resource));
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @POST()
    @Path("/protected/addAlias/")
    public Response addAlias(@FormParam("resource") String resource, @FormParam("al") String alias,
            @FormParam("puid") String puid) {
        log.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("al", alias);
            
            RESTHelper.postRequest(verificationServiceURL + "protected/presentationPolicy/addAlias/"
                    + URLEncoder.encode(resource, "UTF-8") + "/" + URLEncoder.encode(puid, "UTF-8"), params);
            
            return log.exit(presentationPolicy(resource));
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @POST()
    @Path("/protected/addPolicyAlt")
    public Response addPolicyAlt(@FormParam("resource") String resource, @FormParam("puid") String puid) {
        log.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("puid", puid);
            
            RESTHelper.postRequest(verificationServiceURL + "protected/presentationPolicy/addPolicyAlternative/"
                    + URLEncoder.encode(resource, "UTF-8"), params);
            
            return log.exit(presentationPolicy(resource));
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @POST()
    @Path("/protected/createResource")
    public Response createResource(@FormParam("rs") String resourceString, @FormParam("ru") String redirectURL) {
        log.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("redirectURL", redirectURL);
            
            RESTHelper.putRequest(verificationServiceURL + "protected/resource/create/"
                    + URLEncoder.encode(resourceString, "UTF-8"), params);
            
            return log.exit(presentationPolicy(resourceString));
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @POST()
    @Path("/protected/addPredicate/") 
    public Response addPredicate(@FormParam("resource") String resource, @FormParam("cv") String constantValue,
            @FormParam("at") String attribute, @FormParam("p") String predicate, @FormParam("al") String alias,
            @FormParam("puid") String puid) {
        log.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("cv", constantValue);
            params.add("at", attribute);
            params.add("p", predicate);
            params.add("al", alias);
            
            RESTHelper.postRequest(verificationServiceURL + "protected/presentationPolicy/addPredicate/"
                    + URLEncoder.encode(resource, "UTF-8") + "/" + URLEncoder.encode(puid, "UTF-8"), params);
            
            return log.exit(presentationPolicy(resource));
        }
        catch(Exception e) {
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @GET()
    @Path("/protected/presentationPolicy")
    public Response presentationPolicy(@QueryParam("resource") String resource) {
        log.entry();
        
        try {
            Html html = VerificationGUI.getHtmlPramble("Presentation Policy", request);
            Div mainDiv = new Div();
            html.appendChild(VerificationGUI.getBody(mainDiv));
            
            PresentationPolicyAlternatives ppa = (PresentationPolicyAlternatives) RESTHelper.getRequest(
                    verificationServiceURL + "protected/presentationPolicy/get/"
                    + URLEncoder.encode(resource, "UTF-8"), PresentationPolicyAlternatives.class);
            
            mainDiv.appendChild(new H2().appendChild(new Text("Alternatives")));
            
            Form f;
            f = new Form("./addPolicyAlt").setMethod("post");
            f.appendChild(new Input().setType("hidden").setName("resource").setValue(resource));
            f.appendChild(new Label().appendChild(new Text("Policy UID: ")));
            f.appendChild(new Input().setType("text").setName("puid"));
            f.appendChild(new Input().setType("submit").setValue("Add new policy alternative"));
            mainDiv.appendChild(f);
            
            
            for(PresentationPolicy pp : ppa.getPresentationPolicy()) {
                Div ppDiv = new Div();
                mainDiv.appendChild(ppDiv);
                ppDiv.appendChild(new H3().appendChild(new Text(pp.getPolicyUID().toString())));
                
                Ul ul = new Ul();
                ppDiv.appendChild(ul);
                
                for(AttributePredicate ap : pp.getAttributePredicate()) {
                    List<Object> objs = ap.getAttributeOrConstantValue();
                    String s = "";
                    for(Object obj : objs) {
                        if(obj instanceof Attribute) {
                            Attribute attrib = (Attribute)obj;
                            s += (attrib).getAttributeType().toString() + " (" +  attrib.getCredentialAlias().toString() +")" + ";";
                        } else {
                            log.info("InstanceOf Element? " + (obj instanceof Element));
                            Element i = (Element)obj;
                            s += i.getTextContent() + ";";
                            log.info("Node name is: " + (i.getNodeName()));
                        }
                    }
                    ul.appendChild(new Li().appendChild(
                            new B().appendChild(new Text(ap.getFunction().toString())))
                            .appendChild(new Text(" - " + s)));
                }
                
                Settings settings = (Settings) RESTHelper.getRequest(verificationServiceURL
                        + "getSettings/", Settings.class);

                List<CredentialSpecification> credSpecsSettings = settings.credentialSpecifications;
                List<IssuerParameters> issuerParamsSettings = settings.issuerParametersList;
                
                Select s;
                
                
                List<CredentialInPolicy> cips = pp.getCredential();
                for(CredentialInPolicy cip : cips) {
                    
                    Div groupDiv = new Div().setCSSClass("group");
                    mainDiv.appendChild(groupDiv);
                    
                    groupDiv.appendChild(new H4().appendChild(new Text(cip.getAlias().toString())));
                    
                    Div subGroupDiv = new Div().setCSSClass("group");
                    groupDiv.appendChild(subGroupDiv);
                    
                    subGroupDiv.appendChild(new H5().appendChild(new Text("Credential specification alternatives")));
                    List<URI> credSpecs = cip.getCredentialSpecAlternatives().getCredentialSpecUID();
                    ul = new Ul();
                    List<String> credAttributes = new ArrayList<String>();
                    for(URI uri : credSpecs) {
                        f = new Form("./deleteCredSpecAlt").setMethod("post").setCSSClass("inl");
                        f.appendChild(new Input().setType("hidden").setName("al").setValue(cip.getAlias().toString()));
                        f.appendChild(new Input().setType("hidden").setName("cs").setValue(uri.toString()));
                        f.appendChild(new Input().setType("hidden").setName("resource").setValue(resource));
                        f.appendChild(new Input().setType("hidden").setName("puid").setValue(pp.getPolicyUID().toString()));
                        f.appendChild(new Input().setType("submit").setValue("Delete"));
                        ul.appendChild(new Li().appendChild(new Text(uri.toString())).appendChild(f));
                        CredentialSpecification credSpec = (CredentialSpecification) RESTHelper.getRequest(
                                verificationServiceURL + "protected/credentialSpecification/get/"
                                + URLEncoder.encode(uri.toString(), "UTF-8"), CredentialSpecification.class);
                        for(AttributeDescription attrDescs : credSpec.getAttributeDescriptions().getAttributeDescription()) {
                            credAttributes.add(attrDescs.getType().toString());
                        }
                        
                    }
                    subGroupDiv.appendChild(ul);
                    
                    
                    s = new Select().setName("cs");
                    for(CredentialSpecification cs : credSpecsSettings) {
                        Option o = new Option();
                        o.setValue(cs.getSpecificationUID().toString());
                        o.appendChild(new Text(cs.getSpecificationUID().toString()));
                        s.appendChild(o);
                    }
                    
                    f = new Form("./addCredSpecAlt").setMethod("post");
                    f.appendChild(s);
                    f.appendChild(new Br());
                    f.appendChild(new Input().setType("hidden").setName("al").setValue(cip.getAlias().toString()));
                    f.appendChild(new Input().setType("hidden").setName("resource").setValue(resource));
                    f.appendChild(new Input().setType("hidden").setName("puid").setValue(pp.getPolicyUID().toString()));
                    f.appendChild(new Input().setType("submit").setValue("Add credential specification alternative"));
                    subGroupDiv.appendChild(f);
                    
                    subGroupDiv.appendChild(new H5().appendChild(new Text("Issuer alternatives")));
                    ul = new Ul();
                    for(IssuerParametersUID uid : cip.getIssuerAlternatives().getIssuerParametersUID()) {
                        f = new Form("./deleteIssuerAlt").setMethod("post").setCSSClass("inl");
                        f.appendChild(new Input().setType("hidden").setName("al").setValue(cip.getAlias().toString()));
                        f.appendChild(new Input().setType("hidden").setName("ip").setValue(uid.getValue().toString()));
                        f.appendChild(new Input().setType("hidden").setName("resource").setValue(resource));
                        f.appendChild(new Input().setType("hidden").setName("puid").setValue(pp.getPolicyUID().toString()));
                        f.appendChild(new Input().setType("submit").setValue("Delete"));
                        if(uid.getRevocationInformationUID() != null)
                            ul.appendChild(new Li().appendChild(new Text(uid.getValue().toString() + " (" + uid.getRevocationInformationUID().toString()  +")")).appendChild(f));
                        else
                            ul.appendChild(new Li().appendChild(new Text(uid.getValue().toString())).appendChild(f));
                    }
                    subGroupDiv.appendChild(ul);
                    
                    s = new Select().setName("ip");
                    for(IssuerParameters ip : issuerParamsSettings) {
                        Option o = new Option();
                        o.setValue(ip.getParametersUID().toString());
                        o.appendChild(new Text(ip.getParametersUID().toString()));
                        s.appendChild(o);
                    }
                    
                    f = new Form("./addIssuerAlt").setMethod("post");
                    f.appendChild(s);
                    f.appendChild(new Br());
                    f.appendChild(new Input().setType("hidden").setName("al").setValue(cip.getAlias().toString()));
                    f.appendChild(new Input().setType("hidden").setName("resource").setValue(resource));
                    f.appendChild(new Input().setType("hidden").setName("puid").setValue(pp.getPolicyUID().toString()));
                    f.appendChild(new Input().setType("submit").setValue("Add issuer alternative"));
                    subGroupDiv.appendChild(f);
                    
                    f = new Form("./addPredicate").setMethod("post");
                    subGroupDiv = new Div().setCSSClass("group");
                    groupDiv.appendChild(subGroupDiv);
                    subGroupDiv.appendChild(f);
                    
                    s = new Select().setName("p");
                    for(String fp : predicateFunctions) {
                        Option o = new Option();
                        o.setValue(fp);
                        o.appendChild(new Text(fp));
                        s.appendChild(o);
                    }
                    
                    f.appendChild(new Label().appendChild(new Text("Predicate function:"))).appendChild(new Br());
                    f.appendChild(s);
                    f.appendChild(new Br());
                    
                    s = new Select().setName("at");
                    for(String at : credAttributes) {
                        Option o = new Option();
                        o.setValue(at);
                        o.appendChild(new Text(at));
                        s.appendChild(o);
                    }
                    
                    f.appendChild(new Label().appendChild(new Text("Attribute:"))).appendChild(new Br());
                    f.appendChild(s);
                    f.appendChild(new Br());
                    
                    f.appendChild(new Label().appendChild(new Text("Constant value:"))).appendChild(new Br());
                    f.appendChild(new Input().setType("text").setName("cv"));
                    f.appendChild(new Br());
                    
                    f.appendChild(new Input().setType("hidden").setName("al").setValue(cip.getAlias().toString()));
                    f.appendChild(new Input().setType("hidden").setName("resource").setValue(resource));
                    f.appendChild(new Input().setType("hidden").setName("puid").setValue(pp.getPolicyUID().toString()));
                    f.appendChild(new Input().setType("submit").setValue(("Add predicate")));
                    
                    f = new Form("./deleteAlias").setMethod(("post"));
                    f.appendChild(new Input().setType("hidden").setName("al").setValue(cip.getAlias().toString()));
                    f.appendChild(new Input().setType("hidden").setName("resource").setValue(resource));
                    f.appendChild(new Input().setType("hidden").setName("puid").setValue(pp.getPolicyUID().toString()));
                    f.appendChild(new Input().setType("submit").setValue(("Delete this alias")));
                    groupDiv.appendChild(f.setCSSClass("raw"));
                }
                
                f = new Form("./addAlias").setMethod("post");
                f.appendChild(new Input().setType("hidden").setName("resource").setValue(resource));
                f.appendChild(new Label().appendChild(new Text("Alias name (URI):"))).appendChild(new Br());
                f.appendChild(new Input().setType("hidden").setName("puid").setValue(pp.getPolicyUID().toString()));
                f.appendChild(new Input().setType("text").setName("al"));
                f.appendChild(new Input().setType("submit").setValue(("Add new alias")));
                mainDiv.appendChild(f);
                

            }
            
            
            
            return log.exit(Response.ok(html.write()).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log),
                            request).write()).build());
        }
    }
    
    @GET()
    @Path("/protected/credentialSpecification/")
    public Response credentialSpecification(@QueryParam("cs") String credSpecUid) {
        log.entry();
        
        try {
            Html html = VerificationGUI.getHtmlPramble("Credential Specification", request);
            Div mainDiv = new Div();
            html.appendChild(VerificationGUI.getBody(mainDiv));
            Div credDiv = new Div().setCSSClass("credDiv");
            mainDiv.appendChild(credDiv);
            
            CredentialSpecification credSpec = (CredentialSpecification) RESTHelper.getRequest(
                    verificationServiceURL + "protected/credentialSpecification/get/"
                    + URLEncoder.encode(credSpecUid, "UTF-8"), CredentialSpecification.class);
    
            AttributeDescriptions attribDescs = credSpec
                    .getAttributeDescriptions();
            List<AttributeDescription> attrDescs = attribDescs
                    .getAttributeDescription();
            credDiv.appendChild(new H2().appendChild(new Text(credSpec
                    .getSpecificationUID().toString())));
    
            for (AttributeDescription attrDesc : attrDescs) {
                String name = attrDesc.getType().toString();
                String encoding = attrDesc.getEncoding().toString();
                String type = attrDesc.getDataType().toString();
    
                credDiv.appendChild(new H3().appendChild(new Text(name)));
                Div topGroup = new Div().setCSSClass("group");
                Div group = new Div().setCSSClass("group");
                Table tbl = new Table();
                group.appendChild(tbl);
                Tr tr = null;
                tr = new Tr()
                        .setCSSClass("heading")
                        .appendChild(
                                new Td().appendChild(new Text("DataType")))
                        .appendChild(
                                new Td().appendChild(new Text("Encoding")));
                tbl.appendChild(tr);
    
                credDiv.appendChild(topGroup);
                topGroup.appendChild(group);
                group = new Div().setCSSClass("group");
    
                Table fdTbl = new Table();
                tr = new Tr()
                        .setCSSClass("heading")
                        .appendChild(
                                new Td().appendChild(new Text("Language")))
                        .appendChild(
                                new Td().appendChild(new Text("Value")));
                fdTbl.appendChild(tr);
    
                for (FriendlyDescription fd : attrDesc
                        .getFriendlyAttributeName()) {
                    tr = new Tr()
                            .appendChild(
                                    new Td().appendChild(new Text(fd
                                            .getLang())))
                            .appendChild(
                                    new Td().appendChild(new Text(fd
                                            .getValue())));
                    fdTbl.appendChild(tr);
                }
    
                tr = new Tr().appendChild(
                        new Td().appendChild(new Text(type))).appendChild(
                        new Td().appendChild(new Text(encoding)));
                tbl.appendChild(tr);
                group.appendChild(fdTbl);
                
                topGroup.appendChild(group);
            }  
            
            Form f = new Form("./deleteCredentialSpecification")
                .setMethod("post").setCSSClass("raw");
                f.appendChild(new Input().setType("submit").setValue(
                        "Delete credential specification"));
            f.appendChild(new Input().setType("hidden")
                    .setValue(credSpec.getSpecificationUID().toString())
                    .setName("cs"));
            mainDiv.appendChild(f);
            
            return log.exit(Response.ok(html.write()).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/protected/presentationPolicies")
    public Response presentationPolicies() {
        log.entry();
        
        try {
            PresentationPolicyAlternativesCollection ppac = 
                    (PresentationPolicyAlternativesCollection) RESTHelper.getRequest(verificationServiceURL + "protected/presentationPolicy/list",
                    PresentationPolicyAlternativesCollection.class);
            
            Html html = VerificationGUI.getHtmlPramble("Presentation Policies", request);
            Div mainDiv = new Div();
            html.appendChild(VerificationGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text("Presentation policies")));
            
            Ul ul = new Ul();
            
            for(String ppaUri : ppac.uris) {
                A a = new A();
                a.setHref("./presentationPolicy?resource="+URLEncoder.encode(ppaUri.toString(),"UTF-8"));
                a.appendChild(new Text(ppaUri.toString()));
                ul.appendChild(new Li().appendChild(a));
            }
            
            mainDiv.appendChild(ul);
            
            Form f = new Form("./createResource").setMethod("post");
            f.appendChild(new Label().appendChild(new Text("Resource string: ")));
            f.appendChild(new Input().setType("text").setName("rs"));
            f.appendChild(new Br());
            f.appendChild(new Label().appendChild(new Text("Redirect URL: ")));
            f.appendChild(new Input().setType("text").setName("ru"));
            f.appendChild(new Br());
            f.appendChild(new Input().setType("submit").setValue("Create resource"));
            mainDiv.appendChild(f);
            
            return log.exit(Response.ok(html.write()).build());
        }
        catch(Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/protected/loadSettings")
    public Response loadSettings() {
        log.entry();
        
        try {
            Html html = VerificationGUI.getHtmlPramble("Load Settings", request);
            Div mainDiv = new Div();
            html.appendChild(VerificationGUI.getBody(mainDiv));
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
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @POST()
    @Path("/protected/loadSettings2")
    public Response loadSettings2(@FormParam("url") String url) {
        log.entry();
        
        try {
            RESTHelper.getRequest(verificationServiceURL + "protected/loadSettings?url=" + URLEncoder.encode(url, "UTF-8"));
            
            Html html = VerificationGUI.getHtmlPramble("Load Settings", request);
            Div mainDiv = new Div();
            html.appendChild(VerificationGUI.getBody(mainDiv));
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
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/protected/credentialSpecifications/")
    public Response credentialSpecifications() {
        log.entry();

        try {
            Settings settings = (Settings) RESTHelper.getRequest(verificationServiceURL
                    + "getSettings/", Settings.class);

            List<CredentialSpecification> credSpecs = settings.credentialSpecifications;

            Html html = VerificationGUI.getHtmlPramble("Profile", request);
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(VerificationGUI.getBody(mainDiv));

            mainDiv.appendChild(new H2().appendChild(new Text("Profile")));
            mainDiv.appendChild(new H3().appendChild(new Text(
                    "Credential Specifications")));
            
            Ul ul = new Ul();

            for (CredentialSpecification credSpec : credSpecs) {
                
                List<FriendlyDescription> friendlies = credSpec.getFriendlyCredentialName();

                String friendlyDesc = "n/a";
                if(friendlies.size() > 0)
                    friendlyDesc = friendlies.get(0).getValue();
                
                String href = "./credentialSpecification?cs=" + URLEncoder.encode(
                        credSpec.getSpecificationUID().toString(),
                        "UTF-8");
                Li li = new Li().appendChild(
                        new A().setHref(href).appendChild(new B().appendChild(new Text(credSpec.getSpecificationUID().toString()))));
                li.appendChild(new Text(" - " + friendlyDesc));
                
                ul.appendChild(li);
              
            }
            
            mainDiv.appendChild(ul);

            return log.exit(Response.ok(html.write()).build());

        } catch (Exception e) {
            log.catching(e);
            return log.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(VerificationGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, log), request)
                            .write()).build());
        }
    }
}