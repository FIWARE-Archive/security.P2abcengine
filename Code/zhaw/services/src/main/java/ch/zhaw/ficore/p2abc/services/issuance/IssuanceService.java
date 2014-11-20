package ch.zhaw.ficore.p2abc.services.issuance;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.ExceptionDumper;
import ch.zhaw.ficore.p2abc.services.ServiceType;
import ch.zhaw.ficore.p2abc.services.StorageModuleFactory;
import ch.zhaw.ficore.p2abc.services.helpers.issuer.IssuanceHelper;
import ch.zhaw.ficore.p2abc.services.helpers.issuer.IssuerGUI;
import ch.zhaw.ficore.p2abc.services.helpers.user.UserGUI;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AttributeInfoCollection;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthenticationRequest;
import ch.zhaw.ficore.p2abc.services.issuance.xml.IssuanceRequest;
import ch.zhaw.ficore.p2abc.services.issuance.xml.QueryRule;
import ch.zhaw.ficore.p2abc.storage.GenericKeyStorage;
import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;
import ch.zhaw.ficore.p2abc.storage.UnsafeTableNameException;

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

import eu.abc4trust.cryptoEngine.util.SystemParametersUtil;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.util.CryptoUriUtil;
import eu.abc4trust.xml.ABCEBoolean;
import eu.abc4trust.xml.Attribute;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.AttributeDescriptions;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuancePolicy;
import eu.abc4trust.xml.IssuancePolicyAndAttributes;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.IssuerParametersInput;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.SystemParameters;

//from Code/core-abce/abce-services (COPY)

@Path("/issuance")
public class IssuanceService {
    @Context
    ServletContext context;
    @Context
    HttpServletRequest request;

    private static final URI CRYPTOMECHANISM_URI_IDEMIX = URI
            .create("urn:abc4trust:1.0:algorithm:idemix");

    private static final String errNoCredSpec = "CredentialSpecification is missing!";
    private static final String errNoIssuancePolicy = "IssuancePolicy is missing!";
    private static final String errNoQueryRule = "QueryRule is missing!";
    private static final String errNotImplemented = "Sorry, the requested operation is not implemented and/or not supported.";

    private ObjectFactory of = new ObjectFactory();

    private Logger logger;

    public IssuanceService() throws ClassNotFoundException, SQLException,
            UnsafeTableNameException {
        logger = LogManager.getLogger();
    }

    @GET()
    @Path("/protected/status")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response issuerStatus() {
        return Response.ok(request.getRemoteUser()).build();
    }

    @POST()
    @Path("/protected/gui/deleteFriendlyDescription/")
    public Response deleteFriendlyDescription(@FormParam("i") int index,
            @FormParam("cs") String credSpecUid,
            @FormParam("language") String language) {

        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            CredentialSpecification credSpec = null;

            for (URI uri : instance.keyStorage.listUris()) {
                Object obj = SerializationUtils.deserialize(instance.keyStorage
                        .getValue(uri));
                if (obj instanceof CredentialSpecification) {
                    if (((CredentialSpecification) obj).getSpecificationUID()
                            .toString().equals(credSpecUid)) {
                        credSpec = (CredentialSpecification) obj;
                    }
                }
            }

            if (credSpec == null) {
                return logger.exit(Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(IssuerGUI.errorPage(
                                "Credential specification could not be found!")
                                .write()).build());
            }

            AttributeDescription attrDesc = credSpec.getAttributeDescriptions()
                    .getAttributeDescription().get(index);

            FriendlyDescription fd = null;

            for (FriendlyDescription fc : attrDesc.getFriendlyAttributeName())
                if (fc.getLang().equals(language)) {
                    fd = fc;
                    break;
                }

            if (fd != null)
                attrDesc.getFriendlyAttributeName().remove(fd);

            instance.keyManager.storeCredentialSpecification(new URI(
                    credSpecUid), credSpec);

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

    @POST()
    @Path("/protected/gui/deleteAttribute/")
    public Response deleteAttribute(@FormParam("i") int index,
            @FormParam("cs") String credSpecUid) {

        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            CredentialSpecification credSpec = null;

            for (URI uri : instance.keyStorage.listUris()) {
                Object obj = SerializationUtils.deserialize(instance.keyStorage
                        .getValue(uri));
                if (obj instanceof CredentialSpecification) {
                    if (((CredentialSpecification) obj).getSpecificationUID()
                            .toString().equals(credSpecUid)) {
                        credSpec = (CredentialSpecification) obj;
                    }
                }
            }

            if (credSpec == null) {
                return logger.exit(Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(IssuerGUI.errorPage(
                                "Credential specification could not be found!")
                                .write()).build());
            }

            credSpec.getAttributeDescriptions().getAttributeDescription()
                    .remove(index);

            instance.keyManager.storeCredentialSpecification(new URI(
                    credSpecUid), credSpec);

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

    @POST()
    @Path("/protected/gui/addFriendlyDescription/")
    public Response addFriendlyDescription(@FormParam("i") int index,
            @FormParam("cs") String credSpecUid,
            @FormParam("language") String language,
            @FormParam("value") String value) {

        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            CredentialSpecification credSpec = null;

            for (URI uri : instance.keyStorage.listUris()) {
                Object obj = SerializationUtils.deserialize(instance.keyStorage
                        .getValue(uri));
                if (obj instanceof CredentialSpecification) {
                    if (((CredentialSpecification) obj).getSpecificationUID()
                            .toString().equals(credSpecUid)) {
                        credSpec = (CredentialSpecification) obj;
                    }
                }
            }

            if (credSpec == null) {
                return logger.exit(Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(IssuerGUI.errorPage(
                                "Credential specification could not be found!")
                                .write()).build());
            }

            AttributeDescription attrDesc = credSpec.getAttributeDescriptions()
                    .getAttributeDescription().get(index);

            FriendlyDescription fd = new FriendlyDescription();
            fd.setLang(language);
            fd.setValue(value);

            attrDesc.getFriendlyAttributeName().add(fd);

            instance.keyManager.storeCredentialSpecification(new URI(
                    credSpecUid), credSpec);

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

    @POST()
    @Path("/protected/gui/obtainCredentialSpecification2")
    public Response obtainCredentialSpecification2(@FormParam("n") String name) {
        logger.entry();

        try {
            Response r = this.attributeInfoCollection(name);
            if (r.getStatus() != 200) {
                logger.exit(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(IssuerGUI.errorPage(
                                "Could not obtain attribute info collection!")
                                .write()).build());
            }

            AttributeInfoCollection aic = (AttributeInfoCollection) r
                    .getEntity();
            r = this.genCredSpec(aic);

            if (r.getStatus() != 200) {
                logger.exit(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(IssuerGUI.errorPage(
                                "Could not generate credential specification!")
                                .write()).build());
            }

            @SuppressWarnings("unchecked")
            CredentialSpecification credSpec = ((JAXBElement<CredentialSpecification>) r
                    .getEntity()).getValue();

            r = this.storeCredentialSpecification(
                    credSpec.getSpecificationUID(), credSpec);

            if (r.getStatus() != 200) {
                logger.exit(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(IssuerGUI.errorPage(
                                "Could not store credential specification!")
                                .write()).build());
            }

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
    @Path("/protected/gui/issuerParameters/")
    public Response issuerParameters() {
        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            Html html = IssuerGUI.getHtmlPramble("Issuer Parameters");
            Div mainDiv = new Div().setCSSClass("mainDiv");
            html.appendChild(IssuerGUI.getBody(mainDiv));
            mainDiv.appendChild(new H2().appendChild(new Text("Issuer Parameters")));

            List<IssuerParameters> issuerParams = new ArrayList<IssuerParameters>();

            for (URI uri : instance.keyStorage.listUris()) {
                Object obj = SerializationUtils.deserialize(instance.keyStorage
                        .getValue(uri));
                if (obj instanceof IssuerParameters) {
                    issuerParams.add((IssuerParameters) obj);
                }
            }
            
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
    @Path("/protected/gui/obtainCredentialSpecification")
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
    
    @POST()
    @Path("/protected/gui/deleteCredentialSpecification")
    public Response deleteCredentialSpecification(@FormParam("cs") String credSpecUid) {
        logger.entry();
        
        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();
            
            if(instance.keyManager.getCredentialSpecification(new URI(credSpecUid)) == null)
                return logger.exit(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(IssuerGUI.errorPage(
                                errNoCredSpec)
                                .write()).build());
            
            //@#@#^%$ KeyStorage has no delete()
            if(instance.keyStorage instanceof GenericKeyStorage) {
                GenericKeyStorage keyStorage = (GenericKeyStorage)instance.keyStorage;
                keyStorage.delete(new URI(credSpecUid));
            }
            else {
                return logger.exit(Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(IssuerGUI.errorPage(
                                errNotImplemented)
                                .write()).build());
            }
            
            return logger.exit(credentialSpecifications());
        }
        catch (Exception e) {
            return logger.exit(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(IssuerGUI.errorPage(
                            ExceptionDumper.dumpExceptionStr(e, logger))
                            .write()).build());
        }
    }

    @GET()
    @Path("/protected/gui/credentialSpecifications/")
    public Response credentialSpecifications() {
        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            List<CredentialSpecification> credSpecs = new ArrayList<CredentialSpecification>();

            for (URI uri : instance.keyStorage.listUris()) {
                Object obj = SerializationUtils.deserialize(instance.keyStorage
                        .getValue(uri));
                if (obj instanceof CredentialSpecification) {
                    credSpecs.add((CredentialSpecification) obj);
                }
            }

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
    @Path("/protected/gui/profile/")
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

    /**
     * This method is called by a user to initiate an issuance protocol. The
     * user must provide an issuance request containing his authentication
     * information and the uid of the corresponding credential specification.
     * The issuer will then try to authenticate the user by using an
     * authentication source (e.g. LDAP) and fetch the attributes required by
     * the credential specification from an attribute source (e.g. LDAP) and
     * initiates the round based issuance protocol.
     * 
     * If authentication of the user fails this method will return the status
     * code FORBIDDEN. If the issuer is missing the credential specification,
     * the issuance policy or the query rule this method will return status code
     * NOT_FOUND.
     * 
     * @param request
     *            a valid IssuanceRequset
     * @return Response (IssuanceMessageAndBoolean)
     */
    @POST()
    @Path("/issuanceRequest/")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response issuanceRequest(IssuanceRequest request) {

        AttributeValueProvider attrValProvider = null;
        AuthenticationProvider authProvider = null;

        try {
            IssuanceConfiguration configuration = ServicesConfiguration
                    .getIssuanceConfiguration();
            attrValProvider = AttributeValueProvider
                    .getAttributeValueProvider(configuration);
            authProvider = AuthenticationProvider
                    .getAuthenticationProvider(configuration);

            if (!authProvider.authenticate(request.authRequest.authInfo))
                return Response.status(Response.Status.FORBIDDEN).build();

            this.initializeHelper(CryptoEngine.IDEMIX);
            IssuanceHelper instance = IssuanceHelper.getInstance();

            CredentialSpecification credSpec = instance.keyManager
                    .getCredentialSpecification(new URI(
                            request.credentialSpecificationUid));
            IssuancePolicy ip = instance.issuanceStorage
                    .getIssuancePolicy(new URI(
                            request.credentialSpecificationUid));
            QueryRule qr = instance.issuanceStorage.getQueryRule(new URI(
                    request.credentialSpecificationUid));

            if (credSpec == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(errNoCredSpec).build();
            if (ip == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(errNoIssuancePolicy).build();
            if (qr == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(errNoQueryRule).build();

            IssuancePolicyAndAttributes ipa = of
                    .createIssuancePolicyAndAttributes();

            ipa.setIssuancePolicy(ip);
            ipa.getAttribute().addAll(
                    attrValProvider.getAttributes(qr.queryString,
                            authProvider.getUserID(), credSpec));

            return initIssuanceProtocol(ipa);
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        } finally {
            if (attrValProvider != null)
                attrValProvider.shutdown();
            if (authProvider != null)
                authProvider.shutdown();
        }
    }

    /**
     * Store QueryRule.
     * 
     * This method is protected by the magic cookie.
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct.
     * 
     * @param magicCookie
     *            the magic cookie
     * @param credentialSpecificationUid
     *            UID of the credSpec
     * @return Response
     */
    @PUT()
    @Path("/protected/storeQueryRule/{credentialSpecificationUid}")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response storeQueryRule(
            @PathParam("credentialSpecificationUid") String credentialSpecificationUid,
            QueryRule rule) {

        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);
            IssuanceHelper instance = IssuanceHelper.getInstance();

            instance.issuanceStorage.addQueryRule(new URI(
                    credentialSpecificationUid), rule);
            return logger.exit(Response.ok("OK").build());
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /**
     * Retrieve a QueryRule.
     * 
     * This method is protected by the magic cookie.
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct. This method will return status code NOT_FOUND if no query rule
     * with the given uid is found.
     * 
     * @param magicCookie
     *            the magic cookie
     * @param credentialSpecificationUid
     * @return QueryRule
     */
    @GET()
    @Path("/protected/getQueryRule/{credentialSpecificationUid}")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response getQueryRule(
            @PathParam("credentialSpecificationUid") String credentialSpecificationUid) {

        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);
            IssuanceHelper instance = IssuanceHelper.getInstance();

            QueryRule rule = instance.issuanceStorage.getQueryRule(new URI(
                    credentialSpecificationUid));
            if (rule == null)
                return logger.exit(Response.status(Response.Status.NOT_FOUND)
                        .build());
            else
                return logger.exit(Response.ok(rule, MediaType.APPLICATION_XML)
                        .build());
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /**
     * Store IssuancePolicy.
     * 
     * This method is protected by the magic cookie.
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct.
     * 
     * @param magicCookie
     *            the magic cookie
     * @param credentialSpecificationUid
     *            UID of the credSpec
     * @return Response
     */
    @PUT()
    @Path("/protected/storeIssuancePolicy/{credentialSpecificationUid}")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response storeIssuancePolicy(
            @PathParam("credentialSpecificationUid") String credentialSpecificationUid,
            IssuancePolicy policy) {

        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);
            IssuanceHelper instance = IssuanceHelper.getInstance();

            instance.issuanceStorage.addIssuancePolicy(new URI(
                    credentialSpecificationUid), policy);
            return logger.exit(Response.ok("OK").build());
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /**
     * Retrieve an IssuancePolicy
     * 
     * This method is protected by the magic cookie.
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct. This method will return status code NOT_FOUND if no issuance
     * policy with the given uid is found.
     * 
     * @param magicCookie
     *            the magic cookie
     * @param credentialSpecificationUid
     * @return IssuancePolicy
     */
    @GET()
    @Path("/protected/getIssuancePolicy/{credentialSpecificationUid}")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response getIssuancePolicy(
            @PathParam("credentialSpecificationUid") String credentialSpecificationUid) {

        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);
            IssuanceHelper instance = IssuanceHelper.getInstance();

            IssuancePolicy policy = instance.issuanceStorage
                    .getIssuancePolicy(new URI(credentialSpecificationUid));
            if (policy == null)
                return logger.exit(Response.status(Response.Status.NOT_FOUND)
                        .build());
            else
                return logger.exit(Response.ok(of.createIssuancePolicy(policy),
                        MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /**
     * This method can be used to test the authentication. It returns a response
     * with status code OK if the authentication was successful, otherwise it
     * returns a response with status code FORBIDDEN.
     * 
     * This method will return status code FORBIDDEN if authentication failed.
     * 
     * @param authReq
     *            an AuthenticationRequest
     * @return response
     */
    @POST()
    @Path("/testAuthentication")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response testAuthentication(AuthenticationRequest authReq) {

        AuthenticationProvider authProvider = null;

        try {
            IssuanceConfiguration configuration = ServicesConfiguration
                    .getIssuanceConfiguration();
            authProvider = AuthenticationProvider
                    .getAuthenticationProvider(configuration);

            if (authProvider.authenticate(authReq.authInfo)) {
                authProvider.shutdown();
                return Response.ok("OK").build();
            } else {
                return Response.status(Response.Status.FORBIDDEN).entity("ERR")
                        .build();
            }
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        } finally {
            if (authProvider != null)
                authProvider.shutdown();
        }
    }

    /**
     * This method can be used to obtain the AttributeInfoCollection that may
     * later be converted into a CredentialSpecification. This method contacts
     * the identity source to obtain the necessary attributes for <em>name</em>.
     * <em>name</em> refers to a <em>kind</em> of credential a user can get
     * issued. For example <em>name</em> may refer to an objectClass in LDAP.
     * However, the exact behaviour of <em>name</em> depends on the
     * configuration of this service.
     * 
     * This method is protected by the magic cookie.
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct.
     * 
     * @param magicCookie
     *            the magic cookie
     * @param name
     *            name (see description of this method above)
     * @return an AttributeInfoCollection as application/xml.
     */
    @GET()
    @Path("/protected/attributeInfoCollection/{name}")
    public Response attributeInfoCollection(
            @PathParam("name") String name) {
        AttributeInfoProvider attribInfoProvider = null;

        try {
            IssuanceConfiguration configuration = ServicesConfiguration
                    .getIssuanceConfiguration();
            attribInfoProvider = AttributeInfoProvider
                    .getAttributeInfoProvider(configuration);


            return Response.ok(attribInfoProvider.getAttributes(name),
                    MediaType.APPLICATION_XML).build();
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        } finally {
            if (attribInfoProvider != null)
                attribInfoProvider.shutdown();
        }
    }

    /**
     * Generates (or creates) the corresponding CredentialSpecification for a
     * given AttributeInfoCollection. This method assumes that the given
     * AttributeInfoCollection is sane.
     * 
     * This method is protected by the magic cookie.
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct.
     * 
     * @param magicCookie
     *            the magic cookie
     * @param attrInfoColl
     *            the AttributeInfoCollection
     * @return a CredentialSpecification
     */
    @POST()
    @Path("/protected/genCredSpec/")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response genCredSpec(
            AttributeInfoCollection attrInfoCol) {
        

        try {

            return Response
                    .ok(of.createCredentialSpecification(new CredentialSpecGenerator()
                            .generateCredentialSpecification(attrInfoCol)),
                            MediaType.APPLICATION_XML).build();
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /*
     * The following section contains code copied from the original issuance
     * service from the tree Code/core-abce/abce-service
     */

    /* SECTION */

    private void initializeHelper(CryptoEngine cryptoEngine) {
        logger.info("IssuanceService loading...");

        try {
            if (IssuanceHelper.isInit()) {
                logger.info("IssuanceHelper is initialized");
            } else {

                logger.info("Initializing IssuanceHelper...");

                IssuanceHelper
                        .initInstanceForService(
                                cryptoEngine,
                                "",
                                "",
                                StorageModuleFactory
                                        .getModulesForServiceConfiguration(ServiceType.ISSUANCE));

                logger.info("IssuanceHelper is initialized");
            }
        } catch (Exception e) {
            System.out.println("Create Domain FAILED " + e);
            e.printStackTrace();
        }
    }

    /**
     * Store a CredentialSpecification at the issuer.
     * 
     * This method is protected by the magic cookie.
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct.
     * 
     * @param magicCookie
     *            the magic cookie
     * @param credentialSpecificationUid
     *            UID of the CredentialSpecification
     * @param credSpec
     *            the CredentialSpecification to store
     * @return Response
     */
    @PUT()
    @Path("/protected/storeCredentialSpecification/{credentialSpecifationUid}")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response storeCredentialSpecification(
            @PathParam("credentialSpecifationUid") URI credentialSpecifationUid,
            CredentialSpecification credSpec) {

        logger.entry();

        logger.info("IssuanceService - storeCredentialSpecification: \""
                + credentialSpecifationUid + "\"");

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            KeyManager keyManager = instance.keyManager;

            boolean r1 = keyManager.storeCredentialSpecification(
                    credentialSpecifationUid, credSpec);

            ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
            createABCEBoolean.setValue(r1);

            return logger.exit(Response.ok(
                    of.createABCEBoolean(createABCEBoolean),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception ex) {
            logger.catching(ex);
            return logger.exit(ExceptionDumper.dumpException(ex, logger));
        }
    }

    /**
     * Retreive a CredentialSpecification from the issuer.
     * 
     * This method is protected by the magic cookie.
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct. This method will return status code NOT_FOUND if no credential
     * specification with the given uid is found.
     * 
     * @param magicCookie
     *            the magic cookie
     * @param credentialSpecificationUid
     *            UID of the CredentialSpecification to retreive
     * @return Response (CredentialSpecification)
     */
    @GET()
    @Path("/protected/getCredentialSpecification/{credentialSpecificationUid}")
    public Response getCredentialSpecification(
            @PathParam("magicCookie") String magicCookie,
            @PathParam("credentialSpecificationUid") String credentialSpecificationUid) {
        logger.entry();

        logger.info("IssuanceService - getCredentialSpecification: "
                + credentialSpecificationUid);

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            CredentialSpecification credSpec = instance.keyManager
                    .getCredentialSpecification(new URI(
                            credentialSpecificationUid));

            if (credSpec == null) {
                return logger.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(errNoCredSpec).build());
            } else
                return logger.exit(Response.ok(
                        of.createCredentialSpecification(credSpec),
                        MediaType.APPLICATION_XML).build());
        } catch (Exception ex) {
            logger.catching(ex);
            return logger.exit(ExceptionDumper.dumpException(ex, logger));
        }
    }

    /**
     * This method generates a fresh set of system parameters for the given
     * security level, expressed as the bitlength of a symmetric key with
     * comparable security, and cryptographic mechanism. Issuers can generate
     * their own system parameters, but can also reuse system parameters
     * generated by a different entity. More typically, a central party (e.g., a
     * standardization body) will generate and publish system parameters for a
     * number of different key lengths that will be used by many Issuers.
     * Security levels 80 and 128 MUST be supported; other values MAY also be
     * supported.
     * 
     * Currently, the supported mechanism URIs are
     * urn:abc4trust:1.0:algorithm:idemix for Identity Mixer and
     * urn:abc4trust:1.0:algorithm:uprove for U-Prove.
     * 
     * This method will overwrite any existing system parameters.
     * 
     * Protected by magic cookie
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct.
     * 
     * @param magicCookie
     * @param securityLevel
     * @param cryptoMechanism
     * @return
     * @throws Exception
     */
    @POST()
    @Path("/protected/setupSystemParameters/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response setupSystemParameters(
            @QueryParam("securityLevel") int securityLevel,
            @QueryParam("cryptoMechanism") URI cryptoMechanism)
            throws Exception {

        logger.entry();


        try {
            logger.info("IssuanceService - setupSystemParameters "
                    + securityLevel + ", " + cryptoMechanism);

            CryptoEngine cryptoEngine = this
                    .parseCryptoMechanism(cryptoMechanism);

            this.initializeHelper(cryptoEngine);

            IssuanceHelper issuanceHelper = IssuanceHelper.getInstance();

            int idemixKeylength = this.parseIdemixSecurityLevel(securityLevel);

            int uproveKeylength = this.parseUProveSecurityLevel(securityLevel);

            SystemParameters systemParameters = issuanceHelper
                    .createNewSystemParametersWithIdemixSpecificKeylength(
                            idemixKeylength, uproveKeylength);

            SystemParameters serializeSp = SystemParametersUtil
                    .serialize(systemParameters);

            return logger.exit(Response.ok(
                    this.of.createSystemParameters(serializeSp),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    private int parseIdemixSecurityLevel(int securityLevel) {
        if (securityLevel == 80) {
            return 1024;
        }
        return com.ibm.zurich.idmx.utils.SystemParameters
                .equivalentRsaLength(securityLevel);
    }

    private int parseUProveSecurityLevel(int securityLevel) {
        switch (securityLevel) {
        case 80:
            return 2048;
        case 128:
            return 3072;
        }
        throw new RuntimeException("Unsupported securitylevel: \""
                + securityLevel + "\"");
    }

    private CryptoEngine parseCryptoMechanism(URI cryptoMechanism) {
        if (cryptoMechanism == null) {
            throw new RuntimeException("No cryptographic mechanism specified");
        }
        if (cryptoMechanism.equals(CRYPTOMECHANISM_URI_IDEMIX)) {
            return CryptoEngine.IDEMIX;
        }
        throw new IllegalArgumentException("Unkown crypto mechanism: \""
                + cryptoMechanism + "\"");
    }

    /**
     * This method generates a fresh issuance key and the corresponding Issuer
     * parameters. The issuance key is stored in the Issuer’s key store, the
     * Issuer parameters are returned as output of the method. The input to this
     * method specify the credential specification credspec of the credentials
     * that will be issued with these parameters, the system parameters syspars,
     * the unique identifier uid of the generated parameters, the hash algorithm
     * identifier hash, and, optionally, the parameters identifier for any
     * Issuer-driven Revocation Authority.
     * 
     * Currently, the only supported hash algorithm is SHA-256 with identifier
     * urn:abc4trust:1.0:hashalgorithm:sha-256.
     * 
     * Protected by magic cookie.
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct.
     * 
     * @return
     * @throws Exception
     */
    @POST()
    @Path("/protected/setupIssuerParameters/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response setupIssuerParameters(
            IssuerParametersInput issuerParametersInput) throws Exception {

        logger.entry();


        try {
            CryptoEngine cryptoEngine = this
                    .parseCryptoMechanism(issuerParametersInput
                            .getAlgorithmID());

            this.initializeHelper(cryptoEngine);

            this.validateInput(issuerParametersInput);
            URI hashAlgorithm = issuerParametersInput.getHashAlgorithm();

            String systemAndIssuerParamsPrefix = "";

            IssuanceHelper instance = IssuanceHelper.getInstance();

            KeyManager keyManager = instance.keyManager;
            SystemParameters systemParameters = keyManager
                    .getSystemParameters();

            URI credentialSpecUid = issuerParametersInput
                    .getCredentialSpecUID();
            CredentialSpecification credspec = keyManager
                    .getCredentialSpecification(credentialSpecUid);

            if (credspec == null) {
                return logger.exit(Response.status(Response.Status.NOT_FOUND)
                        .entity(errNoCredSpec + "(" + credentialSpecUid.toString() + ")").build());
            }

            URI issuerParametersUid = issuerParametersInput.getParametersUID();
            URI hash = hashAlgorithm;
            URI revocationParametersUid = issuerParametersInput
                    .getRevocationParametersUID();
            List<FriendlyDescription> friendlyDescriptions = issuerParametersInput
                    .getFriendlyIssuerDescription();
            System.out.println("FriendlyIssuerDescription: "
                    + friendlyDescriptions.size());
            IssuerParameters issuerParameters = instance.setupIssuerParameters(
                    cryptoEngine, credspec, systemParameters,
                    issuerParametersUid, hash, revocationParametersUid,
                    systemAndIssuerParamsPrefix, friendlyDescriptions);

            logger.info("IssuanceService - issuerParameters generated");

            List<Object> objs = systemParameters.getAny();
            for (Object obj : objs)
                System.out.println(obj + "-" + obj.getClass());

            SystemParameters serializeSp = SystemParametersUtil
                    .serialize(systemParameters);

            issuerParameters.setSystemParameters(serializeSp);
            return logger.exit(Response.ok(
                    this.of.createIssuerParameters(issuerParameters),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    private void validateInput(IssuerParametersInput issuerParametersTemplate) {
        if (issuerParametersTemplate == null) {
            throw new IllegalArgumentException(
                    "issuer paramters input is required");
        }

        if (issuerParametersTemplate.getCredentialSpecUID() == null) {
            throw new IllegalArgumentException(
                    "Credential specifation UID is required");
        }

        if (issuerParametersTemplate.getParametersUID() == null) {
            throw new IllegalArgumentException(
                    "Issuer parameters UID is required");
        }

        if (issuerParametersTemplate.getAlgorithmID() == null) {
            throw new IllegalArgumentException(
                    "Crypto Algorithm ID is required");
        }

        if (issuerParametersTemplate.getHashAlgorithm() == null) {
            throw new IllegalArgumentException("Hash algorithm is required");
        }

        if (!issuerParametersTemplate.getHashAlgorithm().equals(
                CryptoUriUtil.getHashSha256())) {
            throw new IllegalArgumentException("Unknown hashing algorithm");
        }

    }

    /**
     * This method is invoked by the Issuer to initiate an issuance protocol
     * based on the given issuance policy ip and the list of attribute
     * type-value pairs atts to be embedded in the new credential. It returns an
     * IssuanceMessage that is to be sent to the User and fed to the
     * issuanceProtocolStep method on the User’s side. The IssuanceMessage
     * contains a Context attribute that will be the same for all message
     * exchanges in this issuance protocol, to facilitate linking the different
     * flows of the protocol.
     * 
     * In case of an issuance “from scratch”, i.e., for which the User does not
     * have to prove ownership of existing credentials or established
     * pseudonyms, the given issuance policy ip merely specifies the credential
     * specification and the issuer parameters for the credential to be issued.
     * In this case, the returned issuance message is the first message in the
     * actual cryptographic issuance protocol.
     * 
     * In case of an “advanced” issuance, i.e., where the User has to prove
     * ownership of existing credentials or pseudonyms to carry over attributes,
     * a user secret, or a device secret, the returned IssuanceMessage is simply
     * a wrapper around the issuance policy ip with a fresh Context attribute.
     * The returned boolean indicates whether this is the last flow of the
     * issuance protocol. If the IssuanceMessage is not the final one, the
     * Issuer will subsequently invoke its issuanceProtocolStep method on the
     * next incoming IssuanceMessage from the User. The issuer also returns the
     * uid of the stored issuance log entry that contains an issuance token
     * together with the attribute values provided by the issuer to keep track
     * of the issued credentials.
     * 
     * Protected by magic cookie.
     * 
     * This method will return status code FORBIDDEN if the magic cookie is not
     * correct.
     */
    @POST()
    @Path("/protected/initIssuanceProtocol/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response initIssuanceProtocol(
            IssuancePolicyAndAttributes issuancePolicyAndAttributes)
            throws Exception {

        logger.entry();


        try {
            IssuancePolicy ip = issuancePolicyAndAttributes.getIssuancePolicy();
            List<Attribute> attributes = issuancePolicyAndAttributes
                    .getAttribute();

            URI issuerParametersUid = ip.getCredentialTemplate()
                    .getIssuerParametersUID();

            CryptoEngine cryptoEngine = this
                    .getCryptoEngine(issuerParametersUid);

            this.initializeHelper(cryptoEngine);

            this.initIssuanceProtocolValidateInput(issuancePolicyAndAttributes);

            IssuanceHelper issuanceHelper = IssuanceHelper.getInstance();

            IssuanceMessageAndBoolean issuanceMessageAndBoolean = issuanceHelper
                    .initIssuanceProtocol(ip, attributes);

            return logger
                    .exit(Response
                            .ok(this.of
                                    .createIssuanceMessageAndBoolean(issuanceMessageAndBoolean),
                                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }

    }

    private CryptoEngine getCryptoEngine(URI issuerParametersUid) {
        if (issuerParametersUid.toString().endsWith("idemix")) {
            return CryptoEngine.IDEMIX;
        }

        throw new IllegalArgumentException(
                "Unkown crypto engine from issuer parameters uid: \""
                        + issuerParametersUid + "\"");
    }

    private void initIssuanceProtocolValidateInput(
            IssuancePolicyAndAttributes issuancePolicyAndAttributes) {
        if (issuancePolicyAndAttributes == null) {
            throw new IllegalArgumentException(
                    "\"issuancePolicyAndAttributes\" is required.");
        }

        if (issuancePolicyAndAttributes.getIssuancePolicy() == null) {
            throw new IllegalArgumentException(
                    "\"Issuance policy\" is required.");
        }

        if (issuancePolicyAndAttributes.getAttribute() == null) {
            throw new IllegalArgumentException("\"Attributes\" are required.");
        }
    }

    /**
     * This method performs one step in an interactive issuance protocol. On
     * input an incoming issuance message m received from the User, it returns
     * the outgoing issuance message that is to be sent back to the User, a
     * boolean indicating whether this is the last message in the protocol, and
     * the uid of the stored issuance log entry that contains an issuance token
     * together with the attribute values provided by the issuer to keep track
     * of the issued credentials. The Context attribute of the outgoing message
     * has the same value as that of the incoming message, allowing the Issuer
     * to link the different messages of this issuance protocol.
     */
    @POST()
    @Path("/issuanceProtocolStep")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response issuanceProtocolStep(final IssuanceMessage issuanceMessage) {

        logger.entry();

        logger.info("IssuanceService - step - context : "
                + issuanceMessage.getContext());

        try {
            CryptoEngine engine = this.getCryptoEngine(issuanceMessage);

            this.initializeHelper(engine);

            IssuanceMessageAndBoolean response;
            try {
                response = IssuanceHelper.getInstance().issueStep(engine,
                        issuanceMessage);
            } catch (Exception e) {
                logger.info("- got Exception from IssuaceHelper/ABCE Engine - processing IssuanceMessage from user...");
                e.printStackTrace();
                throw new IllegalStateException(
                        "Failed to proces IssuanceMessage from user");
            }

            IssuanceMessage issuanceMessageFromResponce = response
                    .getIssuanceMessage();
            if (response.isLastMessage()) {
                logger.info(" - last message for context : "
                        + issuanceMessageFromResponce.getContext());
            } else {
                logger.info(" - more steps context : "
                        + issuanceMessageFromResponce.getContext());
            }

            return logger.exit(Response.ok(
                    this.of.createIssuanceMessageAndBoolean(response),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            logger.catching(e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    private CryptoEngine getCryptoEngine(final IssuanceMessage issuanceMessage) {
        CryptoEngine engine = CryptoEngine.IDEMIX;

        if (issuanceMessage.getAny().get(0) instanceof JAXBElement) {
            engine = CryptoEngine.IDEMIX;
            return engine;
        }
        throw new RuntimeException("We only support idemix. Sorry :(");
    }

    /* END SECTION */
}
