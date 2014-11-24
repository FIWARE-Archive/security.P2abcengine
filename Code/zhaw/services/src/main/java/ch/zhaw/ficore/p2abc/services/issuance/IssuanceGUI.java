package ch.zhaw.ficore.p2abc.services.issuance;

import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.ExceptionDumper;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.helpers.issuer.IssuerGUI;
import ch.zhaw.ficore.p2abc.services.helpers.user.UserGUI;
import ch.zhaw.ficore.p2abc.xml.AttributeInfoCollection;
import ch.zhaw.ficore.p2abc.xml.QueryRule;
import ch.zhaw.ficore.p2abc.xml.QueryRuleCollection;
import ch.zhaw.ficore.p2abc.xml.Settings;

import com.hp.gagawa.java.elements.A;
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
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Table;
import com.hp.gagawa.java.elements.Td;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Tr;
import com.hp.gagawa.java.elements.Ul;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.ObjectFactory;

//from Code/core-abce/abce-services (COPY)

@Path("/issuance-gui")
public class IssuanceGUI {
    @Context
    ServletContext context;
    @Context
    HttpServletRequest request;

    

    private static String issuanceServiceURL = "http://localhost:8888/zhaw-p2abc-webservices/issuance/";
    
    private ObjectFactory of = new ObjectFactory();

    private Logger logger;

    public IssuanceGUI()  {
        logger = LogManager.getLogger();
    }
    
    @POST()
    @Path("/protected/deleteAttribute")
    public Response deleteAttribute(@FormParam("cs") String credSpecUid,
            @FormParam("i") int index) {
        logger.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("i", Integer.toString(index));
            
            RESTHelper.postRequest(issuanceServiceURL + "protected/credentialSpecification/deleteAttribute/"
                    + URLEncoder.encode(credSpecUid,"UTF-8"), params);
            
            return credentialSpecifications();
        }
        catch(Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(IssuerGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }
    
    @POST()
    @Path("/protected/deleteCredentialSpecification/")
    public Response deleteCredentialSpecification(@FormParam("cs") String credSpecUid) {
        logger.entry();
        
        try {
            RESTHelper.postRequest(issuanceServiceURL + "protected/credentialSpecification/delete/"
                    + URLEncoder.encode(credSpecUid,"UTF-8"));
            
            return credentialSpecifications();
        }
        catch(Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(IssuerGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }
    
    @POST()
    @Path("/protected/addFriendlyDescription/")
    public Response addFriendlyDescription(@FormParam("i") int index,
            @FormParam("cs") String credSpecUid,
            @FormParam("language") String language,
            @FormParam("value") String value) {
        logger.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("i", Integer.toString(index));
            params.add("language", language);
            params.add("value", value);
            
            RESTHelper.postRequest(issuanceServiceURL + "protected/credentialSpecification/addFriendlyDescription/"
                    + URLEncoder.encode(credSpecUid,"UTF-8"), params);
            
            return credentialSpecifications();
        }
        catch(Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(IssuerGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }
    
    @POST()
    @Path("/protected/deleteFriendlyDescription/")
    public Response deleteFriendlyDescription(@FormParam("i") int index,
            @FormParam("cs") String credSpecUid,
            @FormParam("language") String language) {
        logger.entry();
        
        try {
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("i", Integer.toString(index));
            params.add("language", language);
            
            RESTHelper.postRequest(issuanceServiceURL + "protected/credentialSpecification/deleteFriendlyDescription/"
                    + URLEncoder.encode(credSpecUid,"UTF-8"), params);
            
            return credentialSpecifications();
        }
        catch(Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(IssuerGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }
    
    @POST()
    @Path("/protected/generateIssuerParameters/")
    public Response generateIssuerParameters(
            @FormParam("cs") String credSpecUid) {
        logger.entry();
        
        try {
            RESTHelper.postRequest(issuanceServiceURL + "protected/generateIssuerParameters/"
                    + URLEncoder.encode(credSpecUid,"UTF-8"));
            
            return issuerParameters();
        }
        catch(Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(IssuerGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/protected/queryRules/")
    public Response queryRules() {
        logger.entry();

        try {
            QueryRuleCollection qrc = (QueryRuleCollection) RESTHelper.getRequest(issuanceServiceURL + "protected/queryRules", 
                    QueryRuleCollection.class);

            Html html = IssuerGUI.getHtmlPramble("Query Rules");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(IssuerGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text("Query Rules")));
            
            
            Table tbl = new Table();
            Tr tr = null;
            
            tr = new Tr().appendChild(
                    new Td().appendChild(new Text("Credential specification")))
                    .appendChild(
                            new Td().appendChild(new Text("Query string")))
                     .appendChild(
                             new Td().appendChild(new Text("Action")))
                    .setCSSClass("heading");
            tbl.appendChild(tr);

            for(int i = 0; i < qrc.queryRules.size(); i++) {
                URI uri = new URI(qrc.uris.get(i));
                QueryRule qr = qrc.queryRules.get(i);
            
                String qs = (qr.queryString.length() > 0) ? qr.queryString : "(empty)";
                String cs = uri.toString();
                
                Form f = new Form("./deleteIssuerParameters").setMethod("post").setCSSClass("nopad");
                f.appendChild(new Input().setType("hidden").setName("cs").setValue(cs));
                f.appendChild(new Input().setType("submit").setValue("Delete"));
                
                tr = new Tr().appendChild(
                        new Td().appendChild(new Text(cs)))
                        .appendChild(
                                new Td().appendChild(new Text(qs)))
                        .appendChild(
                                new Td().appendChild(f));
                tbl.appendChild(tr);
            }
            mainDiv.appendChild(tbl);

            return Response.ok(html.write()).build();
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(IssuerGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }
    
    @POST()
    @Path("/protected/obtainCredentialSpecification2")
    public Response obtainCredentialSpecification2(@FormParam("n") String name) {
        logger.entry();

        try {
            AttributeInfoCollection aic = (AttributeInfoCollection) RESTHelper.getRequest(issuanceServiceURL + "protected/attributeInfoCollection/"
                    + URLEncoder.encode(name, "UTF-8"), AttributeInfoCollection.class);
            
            CredentialSpecification credSpec = (CredentialSpecification) RESTHelper.postRequest(
                    issuanceServiceURL + "protected/credentialSpecification/generate", 
                    RESTHelper.toXML(AttributeInfoCollection.class, aic), 
                    CredentialSpecification.class);
            

           RESTHelper.putRequest(
                   issuanceServiceURL + "protected/credentialSpecification/store/"
                   + URLEncoder.encode(credSpec.getSpecificationUID().toString(),"UTF-8"),
                   RESTHelper.toXML(CredentialSpecification.class, of.createCredentialSpecification(credSpec)), String.class);

           

            return credentialSpecifications();
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(IssuerGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/protected/obtainCredentialSpecification")
    public Response obtainCredentialSpecification() {
        logger.entry();

        try {
            Html html = IssuerGUI
                    .getHtmlPramble("Obtain credential specification [1]");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(IssuerGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text(
                    "Obtain credential specification")));
            mainDiv.appendChild(new P()
                    .setCSSClass("info")
                    .appendChild(
                            new Text(
                                    "Please enter the name of the structure or data container in the underlying identity source you whish to "
                                            + "generate a credential specification from. For an LDAP identity source this might be the name of an object class or "
                                            + "for SQL name might be the name of a table. However, the exact behaviour of name is provider specific. Please refer to your service's"
                                            + " configuration. ")));

            Form f = new Form("./obtainCredentialSpecification2")
                    .setMethod("post");
            Table tbl = new Table();
            Tr tr = new Tr();
            tr.appendChild(new Td().appendChild(new Label()
                    .appendChild(new Text("Name:"))));
            tr.appendChild(new Td().appendChild(new Input().setType("text")
                    .setName("n")));
            tbl.appendChild(tr);
            f.appendChild(tbl);
            f.appendChild(new Input().setType("submit").setValue("Obtain"));

            mainDiv.appendChild(f);

            return Response.ok(html.write()).build();
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(IssuerGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/protected/issuerParameters/")
    public Response issuerParameters() {
        logger.entry();

        try {
            Settings settings = 
                    (Settings) RESTHelper.getRequest(issuanceServiceURL + "getSettings/", 
                    Settings.class);

            Html html = IssuerGUI.getHtmlPramble("Issuer Parameters");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(IssuerGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text("Issuer Parameters")));

            List<IssuerParameters> issuerParams = settings.issuerParametersList;

            
            
            Table tbl = new Table();
            Tr tr = null;
            
            tr = new Tr().appendChild(
                    new Td().appendChild(new Text("Issuer Parameters Uid")))
                    .appendChild(
                            new Td().appendChild(new Text("Credential Specification Uid")))
                     .appendChild(
                             new Td().appendChild(new Text("Action")))
                    .setCSSClass("heading");
            tbl.appendChild(tr);

            for (IssuerParameters ip : issuerParams) {
                String cs = ip.getCredentialSpecUID().toString();
                String is = ip.getParametersUID().toString();
                
                Form f = new Form("./deleteIssuerParameters").setMethod("post").setCSSClass("nopad");
                f.appendChild(new Input().setType("hidden").setName("is").setValue(is));
                f.appendChild(new Input().setType("submit").setValue("Delete"));
                
                tr = new Tr().appendChild(
                        new Td().appendChild(new Text(is)))
                        .appendChild(
                                new Td().appendChild(new Text(cs)))
                        .appendChild(
                                new Td().appendChild(f));
                tbl.appendChild(tr);
            }
            mainDiv.appendChild(tbl);

            return Response.ok(html.write()).build();
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(IssuerGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }

    @GET()
    @Path("/protected/credentialSpecifications/")
    public Response credentialSpecifications() {
        logger.entry();

        try {
            Settings settings = 
                    (Settings) RESTHelper.getRequest(issuanceServiceURL + "getSettings/", 
                    Settings.class);

            List<CredentialSpecification> credSpecs = settings.credentialSpecifications;

            Html html = IssuerGUI.getHtmlPramble("Profile");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(IssuerGUI.getBody(mainDiv));

            mainDiv.appendChild(new H2().appendChild(new Text("Profile")));
            mainDiv.appendChild(new H3().appendChild(new Text(
                    "Credential Specifications")));

            for (CredentialSpecification credSpec : credSpecs) {
                int index = 0;

                Div credDiv = new Div().setCSSClass("credDiv");
                mainDiv.appendChild(credDiv);

                AttributeDescriptions attribDescs = credSpec
                        .getAttributeDescriptions();
                List<AttributeDescription> attrDescs = attribDescs
                        .getAttributeDescription();
                credDiv.appendChild(new H4().appendChild(new Text(credSpec
                        .getSpecificationUID().toString())));

                for (AttributeDescription attrDesc : attrDescs) {
                    String name = attrDesc.getType().toString();
                    String encoding = attrDesc.getEncoding().toString();
                    String type = attrDesc.getDataType().toString();

                    credDiv.appendChild(new H5().appendChild(new Text(name)));
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
                                    new Td().appendChild(new Text("Value")))
                            .appendChild(
                                    new Td().appendChild(new Text("Action")));
                    fdTbl.appendChild(tr);

                    Form f = null;

                    for (FriendlyDescription fd : attrDesc
                            .getFriendlyAttributeName()) {
                        f = new Form("./deleteFriendlyDescription").setMethod(
                                "post").setCSSClass("nopad");
                        f.appendChild(new Input().setType("hidden")
                                .setName("language").setValue(fd.getLang()));
                        f.appendChild(new Input()
                                .setType("hidden")
                                .setValue(
                                        credSpec.getSpecificationUID()
                                                .toString()).setName("cs"));
                        f.appendChild(new Input().setType("hidden")
                                .setValue(Integer.toString(index)).setName("i"));
                        f.appendChild(new Input().setType("submit").setValue(
                                "delete"));
                        tr = new Tr()
                                .appendChild(
                                        new Td().appendChild(new Text(fd
                                                .getLang())))
                                .appendChild(
                                        new Td().appendChild(new Text(fd
                                                .getValue())))
                                .appendChild(new Td().appendChild(f));
                        fdTbl.appendChild(tr);
                    }

                    tr = new Tr().appendChild(
                            new Td().appendChild(new Text(type))).appendChild(
                            new Td().appendChild(new Text(encoding)));
                    tbl.appendChild(tr);
                    group.appendChild(fdTbl);

                    f = new Form("./addFriendlyDescription").setMethod("post");
                    tbl = new Table().setCSSClass("pad");
                    tr = new Tr().appendChild(
                            new Td().appendChild(new Label()
                                    .appendChild(new Text("Language:"))))
                            .appendChild(
                                    new Td().appendChild(new Input().setType(
                                            "text").setName("language")));
                    tbl.appendChild(tr);
                    tr = new Tr().appendChild(
                            new Td().appendChild(new Label()
                                    .appendChild(new Text("Value:"))))
                            .appendChild(
                                    new Td().appendChild(new Input().setType(
                                            "text").setName("value")));
                    tbl.appendChild(tr);
                    f.appendChild(tbl);
                    f.appendChild(new Input().setType("submit").setValue(
                            "Add new friendly description"));
                    f.appendChild(new Input()
                            .setType("hidden")
                            .setValue(credSpec.getSpecificationUID().toString())
                            .setName("cs"));
                    f.appendChild(new Input().setType("hidden")
                            .setValue(Integer.toString(index)).setName("i"));
                    group.appendChild(f);

                    topGroup.appendChild(group);
                    f = new Form("./deleteAttribute").setMethod("post");
                    f.appendChild(new Input().setType("submit").setValue(
                            "Delete attribute"));
                    f.appendChild(new Input()
                            .setType("hidden")
                            .setValue(credSpec.getSpecificationUID().toString())
                            .setName("cs"));
                    f.appendChild(new Input().setType("hidden")
                            .setValue(Integer.toString(index)).setName("i"));
                    topGroup.appendChild(f);

                    index++;
                }
                
                Form f = new Form("./deleteCredentialSpecification").setMethod("post");
                f.appendChild(new Input().setType("submit").setValue(
                        "Delete credential specification"));
                f.appendChild(new Input()
                        .setType("hidden")
                        .setValue(credSpec.getSpecificationUID().toString())
                        .setName("cs"));
                credDiv.appendChild(f);
                f = new Form("./generateIssuerParameters").setMethod("post");
                f.appendChild(new Input().setType("submit").setValue(
                        "Generate issuer parameters"));
                f.appendChild(new Input()
                        .setType("hidden")
                        .setValue(credSpec.getSpecificationUID().toString())
                        .setName("cs"));
                credDiv.appendChild(f);
            }

            return logger.exit(Response.ok(html.write()).build());

        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }
    
    @GET()
    @Path("/protected/profile/")
    public Response profile() {
        logger.entry();

        try {
            Html html = UserGUI.getHtmlPramble("Profile");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(IssuerGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text("Profile")));

            String text = "Welcome to your profile! Here you can edit and manage your settings.";
            P p = new P().setCSSClass("info");
            mainDiv.appendChild(p);
            p.appendChild(new Text(text));
            p.appendChild(new Br());
            text = "Credential specifications specify what attributes a credential can or has to contain.";
            p.appendChild(new Text(text));

            Ul ul = new Ul();
            ul.appendChild(new Li().appendChild(new A()
                    .setHref("./issuerParameters").appendChild(
                            new Text("Manage issuer parameters"))));
            ul.appendChild(new Li().appendChild(new A().setHref(
                    "./credentialSpecifications").appendChild(
                    new Text("Manage credential specifications"))));
            ul.appendChild(new Li().appendChild(new A().setHref(
                    "./queryRules").appendChild(
                    new Text("Manage query rules"))));

            mainDiv.appendChild(ul);

            return logger.exit(Response.ok(html.write()).build());

        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(UserGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger)).write())
                    .build());
        }
    }

    
}
