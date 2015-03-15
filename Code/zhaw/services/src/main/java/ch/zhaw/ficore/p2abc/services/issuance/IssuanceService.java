package ch.zhaw.ficore.p2abc.services.issuance;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ch.zhaw.ficore.p2abc.configuration.IssuanceConfiguration;
import ch.zhaw.ficore.p2abc.configuration.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.ExceptionDumper;
import ch.zhaw.ficore.p2abc.services.ServiceType;
import ch.zhaw.ficore.p2abc.services.StorageModuleFactory;
import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.helpers.issuer.IssuanceHelper;
import ch.zhaw.ficore.p2abc.storage.GenericKeyStorage;
import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;
import ch.zhaw.ficore.p2abc.storage.UnsafeTableNameException;
import ch.zhaw.ficore.p2abc.xml.AttributeInfoCollection;
import ch.zhaw.ficore.p2abc.xml.AuthenticationRequest;
import ch.zhaw.ficore.p2abc.xml.IssuanceRequest;
import ch.zhaw.ficore.p2abc.xml.QueryRule;
import ch.zhaw.ficore.p2abc.xml.QueryRuleCollection;
import ch.zhaw.ficore.p2abc.xml.Settings;
import eu.abc4trust.cryptoEngine.util.SystemParametersUtil;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyStorage;
import eu.abc4trust.util.CryptoUriUtil;
import eu.abc4trust.xml.Attribute;
import eu.abc4trust.xml.AttributeDescription;
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
    private static final String errCredSpecUid = "The credential specification uid does not match or is invalid!";
    private static final String defaultIPUid = "abc4trust:default-issuance-policy";
    private static final int sysParamsSecurityLevel = 80;
    private static final String sysParamsCryptoMechanism = "urn:abc4trust:1.0:algorithm:idemix";

    private ObjectFactory of = new ObjectFactory();

    private static final XLogger logger = new XLogger(LoggerFactory.getLogger(IssuanceService.class));

    public IssuanceService() throws ClassNotFoundException, SQLException,
            UnsafeTableNameException {
        setup();
    }

    private void setup() {
        try {
            // This will load the defaultIssuancePolicy and will also
            // setup system parameters 80,idemix if non exist.

            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            String ipDefault = IssuanceHelper
                    .readTextFile("defaultIssuancePolicy.xml");
            IssuancePolicy ip = (IssuancePolicy) RESTHelper.fromXML(
                    IssuancePolicy.class, ipDefault);

            instance.issuanceStorage.addIssuancePolicy(new URI(defaultIPUid),
                    ip);

            if (!instance.keyManager.hasSystemParameters())
                this.setupSystemParameters(sysParamsSecurityLevel, new URI(
                        sysParamsCryptoMechanism));
        } catch (Exception e) {
            ExceptionDumper.dumpExceptionStr(e, logger);
        }
    }

    /* GENERAL METHODS */

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
    @Path("/protected/reset")
    public Response reset() throws Exception {
        logger.entry();
        try {
            this.initializeHelper(CryptoEngine.IDEMIX);
    
            IssuanceHelper.getInstance();
    
            URIBytesStorage.clearEverything();
            return logger.exit(Response.ok().build());
        }
        catch(Exception e) {
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /**
     * <b>Path</b>: /protected/status (GET)<br>
     * <br>
     * <b>Description</b>: This method is available when the service is running. <br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * </ul>
     * @return Response
     */
    @GET()
    @Path("/protected/status")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response issuerStatus() {
        return Response.ok("OK").build();
    }

    /**
     * <b>Path</b>: /testAuthentication (GET) <br>
     * <b>Description</b>: This method can be used to test authentication by
     * sending an authentication request.<br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (Authentication successful)</li>
     * <li>401 - Authentication was not successful.</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>AuthenticationRequest</tt><br>
     * 
     * @param authReq
     *            the authentication request
     * @return Response
     */
    @POST()
    @Path("/testAuthentication")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response testAuthentication(AuthenticationRequest authReq) { /* [TEST EXISTS] */
        
        logger.entry();

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
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        } finally {
            if (authProvider != null)
                authProvider.shutdown();
        }
    }

    /**
     * <b>Path</b>: /getSettings/ (GET)<br>
     * <br>
     * <b>Description</b>: Returns the settings of this issuance service.
     * Settings includes issuer parameters, credential specifications and the
     * system parameters. This method is usually called by a user service or a verification
     * service to download the settings. <br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>Settings</tt><br>
     * @return Response
     */
    @GET()
    @Path("/getSettings/")
    public Response getSettings() {
        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            Settings settings = new Settings();

            List<IssuerParameters> issuerParams = new ArrayList<IssuerParameters>();

            for (URI uri : instance.keyStorage.listUris()) {
                Object obj = SerializationUtils.deserialize(instance.keyStorage
                        .getValue(uri));
                if (obj instanceof IssuerParameters) {
                    IssuerParameters ip = (IssuerParameters) obj;

                    SystemParameters serializeSp = SystemParametersUtil
                            .serialize(ip.getSystemParameters());

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
            settings.systemParameters = SystemParametersUtil
                    .serialize(instance.keyManager.getSystemParameters());

            return logger.exit(Response.ok(settings, MediaType.APPLICATION_XML)
                    .build());
        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                            ExceptionDumper.dumpExceptionStr(e, logger)))
                            .build();
        }
    }

    /**
     * <b>Path</b>: /issuanceRequest/ (POST)<br>
     * <br>
     * <b>Description</b>: This method is called by a user to initiate an
     * issuance protocol. The user must provide an issuance request containing
     * his authentication information and the UID of the corresponding
     * credential specification. The issuer will then try to authenticate the
     * user by using an authentication source (e.g. LDAP) and fetch the
     * attributes required by the credential specification from an attribute
     * source (e.g. LDAP) and initiates the round based issuance protocol.<br><br>
     * 
     * If authentication of the user fails this method will return the status
     * code FORBIDDEN. If the issuer is missing the credential specification,
     * the issuance policy or the query rule this method will return status code
     * NOT_FOUND.<br><br>
     * 
     * 
     * This method will search for an issuance policy and a query rule using the
     * UID of the credential specification as the key. If the issuance policy could
     * not be found a default issuance policy will be used which asks the user
     * to reveal nothing in particular. <br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>401 - Authentication failed</li>
     * <li>404 - A resource needed to process the request was not found</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>IssuanceRequest</tt><br>
     * <b>Return type</b>: <tt>IssuanceMessageAndBoolean</tt><br>
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

            if (ip == null) {
                // No specific issuance policy was registered so we'll use a
                // default one.
                ip = instance.issuanceStorage.getIssuancePolicy(new URI(
                        defaultIPUid));
                if (ip != null) {
                    ip.getCredentialTemplate().setCredentialSpecUID(
                            new URI(request.credentialSpecificationUid));
                    ip.getCredentialTemplate().setIssuerParametersUID(
                            new URI(request.credentialSpecificationUid
                                    + ":issuer-params"));
                }
            }

            if (credSpec == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(errNoCredSpec + ": "
                                + request.credentialSpecificationUid).build();
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

            return logger.exit(initIssuanceProtocol(ipa));
        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        } finally {
            if (attrValProvider != null)
                attrValProvider.shutdown();
            if (authProvider != null)
                authProvider.shutdown();
        }
    }

    /**
     * <b>Path</b>: /issuanceProtocolStep (POST)<br>
     * <br>
     * <b>Description</b>:
     * This method performs one step in an interactive issuance protocol. On
     * input an incoming issuance message <tt>m</tt> received from the User, it returns
     * the outgoing issuance message that is to be sent back to the User, a
     * boolean indicating whether this is the last message in the protocol, and
     * the UID of the stored issuance log entry that contains an issuance token
     * together with the attribute values provided by the issuer to keep track
     * of the issued credentials. The Context attribute of the outgoing message
     * has the same value as that of the incoming message, allowing the Issuer
     * to link the different messages of this issuance protocol.<br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>IssuanceMessage</tt><br>
     * <b>Return type:</b>: <tt>IssuanceMessageAndBoolean</tt><br>
     * 
     * @param issuanceMessage
     *            an IssuanceMessage.
     * @return Response
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
                logger.info("- got Exception from IssuaceHelper/ABCE Engine - processing IssuanceMessage from user");
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
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /* CREDENTIAL SPECIFICATION */

    /**
     * <b>Path</b>: /protected/credentialSpecification/delete/{credentialSpecificationUid} (DELETE)<br>
     * <br>
     * <b>Description</b>: Deletes a credential specification that was stored
     * under the UID provided as part of the path. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification to
     * delete</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>404 - Credential specification was not found.</li>
     * <li>500 - ERROR</li>
     * </ul>
     * 
     * @param credSpecUid
     *            UID of the credential specification
     * @return Response
     */
    @DELETE()
    @Path("/protected/credentialSpecification/delete/{credentialSpecificationUid}")
    public Response deleteCredentialSpecification( /* [TEST EXISTS] */
            @PathParam("credentialSpecificationUid") String credSpecUid) {
        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            if (instance.keyManager.getCredentialSpecification(new URI(
                    credSpecUid)) == null)
                return logger.exit(
                        Response.status(Response.Status.NOT_FOUND).entity(
                                errNoCredSpec)).build();

            // @#@#^%$ KeyStorage has no delete()
            if (instance.keyStorage instanceof GenericKeyStorage) {
                GenericKeyStorage keyStorage = (GenericKeyStorage) instance.keyStorage;
                keyStorage.delete(new URI(credSpecUid));
            } else {
                return logger.exit(
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                                errNotImplemented)).build();
            }

            return logger.exit(Response.ok("OK").build());
        } catch (Exception e) {
            return logger.exit(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                            ExceptionDumper.dumpExceptionStr(e, logger)))
                            .build();
        }
    }

    /**
     * <b>Path</b>: /protected/credentialSpecification/deleteAttribute/{credentialSpecificationUid} (DELETE)<br>
     * <br>
     * <b>Description</b>: Deletes an attribute from a credential specification. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification to
     * delete the attribute from.</li>
     * </ul>
     * <b>Delete parameters</b>:
     * <ul>
     * <li>i - Index of the attribute (in the credential specification) to
     * delete.</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * <li>404 - Credential specification was not found.</li>
     * </ul>
     * 
     * @param index
     *            Index of the attribute
     * @param credSpecUid
     *            UID of the credential specification
     * @return Response
     */
    @DELETE()
    @Path("/protected/credentialSpecification/deleteAttribute/{credentialSpecificationUid}")
    public Response deleteAttribute(@FormParam("i") int index,
            @PathParam("credentialSpecificationUid") String credSpecUid) {

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
                return logger
                        .exit(Response
                                .status(Response.Status.NOT_FOUND)
                                .entity("Credential specification could not be found!"))
                                .build();
            }

            credSpec.getAttributeDescriptions().getAttributeDescription()
            .remove(index);

            instance.keyManager.storeCredentialSpecification(new URI(
                    credSpecUid), credSpec);

            return logger.exit(Response.ok("OK").build());
        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                            ExceptionDumper.dumpExceptionStr(e, logger)))
                            .build();
        }
    }

    /**
     * <b>Path</b>: /protected/credentialSpecification/deleteFriendlyDescriptionAttribute/{credentialSpecificationUid} (DELETE)<br>
     * <br>
     * <b>Description</b>: Deletes a friendly description from an attribute of credential
     * specification. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification.</li>
     * </ul>
     * <br>
     * <b>Delete parameters</b>:
     * <ul>
     * <li>i - Index of the attribute the friendly description belongs to.</li>
     * <li>language - Language identifier of the friendly description to delete.
     * </li>
     * </ul>
     * <br>
     * <b>Response status:</b>
     * <ul>
     * <li>200 - OK</li>
     * <li>404 - Credential specification, attribute description or friendly description could not be found.</li>
     * <li>500 - ERROR</li>
     * </ul>
     * 
     * @param index
     *            Index of the attribute.
     * @param credSpecUid
     *            UID of the credential specification.
     * @param language
     *            language identifier of the friendly description.
     * @return Response
     */
    @DELETE()
    @Path("/protected/credentialSpecification/deleteFriendlyDescriptionAttribute/{credentialSpecificationUid}")
    public Response deleteFriendlyDescription(@FormParam("i") int index, /* [TEST EXISTS] */
            @PathParam("credentialSpecificationUid") String credSpecUid,
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

            if (credSpec == null || index >= credSpec.getAttributeDescriptions().
                    getAttributeDescription().size()) {
                return logger
                        .exit(Response
                                .status(Response.Status.NOT_FOUND)
                                .entity("Credential specification or attribute description could not be found!"))
                                .build();
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
            else
                return logger
                        .exit(Response
                                .status(Response.Status.NOT_FOUND)
                                .entity("Friendly description could not be found!"))
                                .build();

            instance.keyManager.storeCredentialSpecification(new URI(
                    credSpecUid), credSpec);

            return logger.exit(Response.ok("OK").build());
        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                            ExceptionDumper.dumpExceptionStr(e, logger)))
                            .build();
        }
    }

    /**
     * <b>Path</b>: /protected/credentialSpecification/addFriendlyDescriptionAttribute/{credentialSpecificationUid} (PUT)<br>
     * <br>
     * <b>Description</b>: Adds a friendly description to an attribute of a
     * credential specification.<br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification.<li>
     * </ul>
     * <br>
     * <b>Put parameters</b>:
     * <ul>
     * <li>i - Index of the attribute to add the friendly description to.</li>
     * <li>language - Language identifier.</li>
     * <li>value - Value of the friendly description.</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>404 - Credential specification or attribute description could not be found.</li>
     * <li>500 - ERROR</li>
     * </ul>
     * 
     * @param index
     *            Index of the attribute.
     * @param credSpecUid
     *            UID of the credential specification.
     * @param language
     *            language identifier of the friendly description.
     * @param value
     *            value of the friendly description.
     * @return Response
     */
    @PUT()
    @Path("/protected/credentialSpecification/addFriendlyDescriptionAttribute/{credentialSpecificationUid}")
    public Response addFriendlyDescriptionAttribute(@FormParam("i") int index, /* [TEST EXISTS] */
            @PathParam("credentialSpecificationUid") String credSpecUid,
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

            if (credSpec == null || credSpec.getAttributeDescriptions().getAttributeDescription().size()
                    <= index) {
                return logger
                        .exit(Response
                                .status(Response.Status.NOT_FOUND)
                                .entity("Credential specification or attribute description could not be found!"))
                                .build();
            }

            AttributeDescription attrDesc = credSpec.getAttributeDescriptions()
                    .getAttributeDescription().get(index);

            FriendlyDescription fd = new FriendlyDescription();
            fd.setLang(language);
            fd.setValue(value);

            attrDesc.getFriendlyAttributeName().add(fd);

            instance.keyManager.storeCredentialSpecification(new URI(
                    credSpecUid), credSpec);

            return logger.exit(Response.ok("OK").build());
        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                            ExceptionDumper.dumpExceptionStr(e, logger)))
                            .build();
        }
    }

    /**
     * <b>Path</b>: /protected/credentialSpecification/store/{credentialSpecificationUid} (PUT)<br>
     * <br>
     * <b>Description</b>: Store a credential specification at this service. The UID given as part of the path
     * must match the UID of the passed credential specification. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification.</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>409 - The credentialSpecificationUid given on the path does not match
     * the actual credential specification's UID</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>CredentialSpecification</tt><br>
     * 
     * @param credentialSpecifationUid
     *            UID of the credential specification
     * @param credSpec
     *            the credential specification
     * @return Response
     */
    @PUT()
    @Path("/protected/credentialSpecification/store/{credentialSpecifationUid}")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response storeCredentialSpecification( /* [TEST EXISTS] */
            @PathParam("credentialSpecifationUid") URI credentialSpecifationUid,
            CredentialSpecification credSpec) {

        logger.entry();

        logger.info("IssuanceService - storeCredentialSpecification: \""
                + credentialSpecifationUid + "\"");

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            if (!credSpec.getSpecificationUID().toString()
                    .equals(credentialSpecifationUid.toString())) {
                return logger.exit(Response.status(Response.Status.CONFLICT)
                        .entity(errCredSpecUid).build());
            }

            KeyManager keyManager = instance.keyManager;

            boolean r1 = keyManager.storeCredentialSpecification(
                    credentialSpecifationUid, credSpec);

            if(!r1)
                throw new RuntimeException("Could not store the credential specification.");

            return logger.exit(Response.ok("OK").build());
        } catch (Exception ex) {
            logger.catching( ex);
            return logger.exit(ExceptionDumper.dumpException(ex, logger));
        }
    }

    /**
     * <b>Path</b>: /protected/credentialSpecification/get/{credentialSpecificationUid} (GET)<br>
     * <br>
     * <b>Description</b>: Retrieve a credential specification.<br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>404 - Credential specification was not found.</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>CredentialSpecification</tt><br>
     * 
     * @param credentialSpecificationUid
     *            UID of the credential specification
     * @return Response
     */
    @GET()
    @Path("/protected/credentialSpecification/get/{credentialSpecificationUid}")
    public Response getCredentialSpecification( /* [TEST EXISTS] */
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
            logger.catching( ex);
            return logger.exit(ExceptionDumper.dumpException(ex, logger));
        }
    }

    /* ISSUER PARAMETERS */

    /**
     * <b>Path</b>: /protected/issuerParameters/generate/{credentialSpecificationUid} (POST)<br>
     * <br>
     * <b>Description</b>: Generates issuer parameters for a specified
     * credential specification. The generated issuer parameters will
     * automatically be stored at this issuance service.<br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification to
     * generate the issuer parameters for.</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * 
     * @param credSpecUid
     *            UID of the credential specification
     * @return Response
     */
    @POST()
    @Path("/protected/issuerParameters/generate/{credentialSpecificationUid}")
    public Response generateIssuerParameters(
            @PathParam("credentialSpecificationUid") String credSpecUid) {
        logger.entry();

        try {
            URI algorithmID = new URI("urn:abc4trust:1.0:algorithm:idemix");
            URI hashAlgorithm = new URI(
                    "urn:abc4trust:1.0:hashalgorithm:sha-256");
            IssuerParametersInput ip = new IssuerParametersInput();

            ip.setAlgorithmID(algorithmID);
            ip.setHashAlgorithm(hashAlgorithm);

            ip.setCredentialSpecUID(new URI(credSpecUid));
            ip.setParametersUID(new URI(credSpecUid + ":issuer-params"));
            ip.setRevocationParametersUID(new URI(credSpecUid
                    + ":revocation-params"));

            Response r = setupIssuerParameters(ip);

            if (r.getStatus() != 200) {
                throw new RuntimeException("Internal step failed! ("
                        + r.getStatus() + ")");
            }

            return Response.ok("OK").build();

        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                            ExceptionDumper.dumpExceptionStr(e, logger)))
                            .build();
        }
    }

    /**
     * <b>Path</b>: /protected/issuerParameters/delete/{issuerParametersUid} (DELETE)<br>
     * <br>
     * <b>Description</b>: Deletes issuer parameters.<br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>issuerParametersUid - UID of the issuer parameters to delete.
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
    @Path("/protected/issuerParameters/delete/{issuerParametersUid}")
    public Response deleteIssuerParameters(
            @PathParam("issuerParametersUid") String issuerParametersUid) {
        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);

            IssuanceHelper instance = IssuanceHelper.getInstance();

            KeyStorage keyStorage = instance.keyStorage;

            // @#@#^%$ KeyStorage has no delete()
            if (keyStorage instanceof GenericKeyStorage) {
                GenericKeyStorage gkeyStorage = (GenericKeyStorage) keyStorage;
                gkeyStorage.delete(new URI(issuerParametersUid));
            } else {
                return logger.exit(
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                                errNotImplemented)).build();
            }

            return logger.exit(Response.ok("OK").build());
        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /* QUERY RULE */

    /**
     * <b>Path</b>: /protected/queryRule/store/{credentialSpecificationUid} (PUT)<br>
     * <br>
     * <b>Description</b>: Stores a query rule and associates it with the
     * specified credential specification. A query rule is stored at the
     * issuance service with the given credential specification UID which the
     * issuance service will use to look up the corresponding query rule. <br>
     * <br>
     * <b>Response status:</b>
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>QueryRule</tt><br>
     * 
     * @param credentialSpecificationUid
     *            UID of the credential specification
     * @param rule
     *            QueryRule
     * @return Response
     */
    @PUT()
    @Path("/protected/queryRule/store/{credentialSpecificationUid}")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response storeQueryRule( /* [TEST EXISTS] */
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
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /**
     * <b>Path</b>: /protected/queryRule/delete/{credentialSpecificationUid} (DELETE)<br>
     * <br>
     * <b>Description</b>: Deletes a query rule.<br>
     * <br>
     * <b>Path parameters:</b>
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification the query rule is associated with.</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * @param credSpecUid UID of the credential specification.
     * @return Response
     */
    @DELETE()
    @Path("/protected/queryRule/delete/{credentialSpecificationUid}")
    public Response deleteQueryRule( /* [TEST EXISTS] */
            @PathParam("credentialSpecificationUid") String credSpecUid) {
        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);
            IssuanceHelper instance = IssuanceHelper.getInstance();

            instance.issuanceStorage.deleteQueryRule(new URI(credSpecUid));

            return logger.exit(Response.ok("OK").build());
        }
        catch(Exception e) {
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger)); 
        }
    }

    /**
     * <b>Path</b>: /protected/queryRule/get/{credentialSpecificationUid} (GET)<br>
     * <br>
     * <b>Description</b>: Retrieves a previously stored query rule. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification the
     * query rule is associated with.</li>
     * </ul>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>404 - Query rule could not be found.</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>QueryRule</tt><br>
     * 
     * @param credentialSpecificationUid
     *            UID of the credential specification
     * @return Response
     */
    @GET()
    @Path("/protected/queryRule/get/{credentialSpecificationUid}")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response getQueryRule( /* [TEST EXISTS] */
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
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /**
     * <b>Path</b>: /protected/queryRule/list (GET)<br>
     * <br>
     * <b>Description</b>: Lists all query rules stored at this issuance
     * service.<br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>QueryRuleCollection</tt><br>
     * 
     * @return Response
     */
    @GET()
    @Path("/protected/queryRule/list")
    public Response queryRules() { /* [TEST EXISTS] */
        logger.entry();

        try {
            this.initializeHelper(CryptoEngine.IDEMIX);
            IssuanceHelper instance = IssuanceHelper.getInstance();

            IssuanceStorage storage = instance.issuanceStorage;
            List<URI> uris = storage.listQueryRules();
            List<String> uriStrings = new ArrayList<String>();
            List<QueryRule> queryRules = new ArrayList<QueryRule>();
            for (URI uri : uris) {
                uriStrings.add(uri.toString());
                queryRules.add(storage.getQueryRule(uri));
            }
            QueryRuleCollection qrc = new QueryRuleCollection();
            qrc.queryRules = queryRules;
            qrc.uris = uriStrings;
            return logger.exit(Response.ok(qrc, MediaType.APPLICATION_XML)
                    .build());
        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /* ISSUANCE POLICY */

    /**
     * <b>Path</b>: /protected/issuancePolicy/store/{credentialSpecificationUid} (PUT)<br>
     * <br>
     * <b>Description</b>: Stores an issuance policy and associates it with a
     * credential specification.<br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification to
     * associate the issuance policy with.</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>IssuancePolicy</tt><br>
     * 
     * @param credentialSpecificationUid
     *            UID of the credential specification.
     * @param policy
     *            IssuancePolicy to store.
     * @return Response
     */
    @PUT()
    @Path("/protected/issuancePolicy/store/{credentialSpecificationUid}")
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
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /**
     * <b>Path</b>: /protected/issuancePolicy/get/{credentialSpecificationUid} (GET)<br>
     * <br>
     * <b>Description</b>: Retrieve an issuance policy that was previously
     * stored. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>credentialSpecificationUid - UID of the credential specification the
     * issuance policy is associated with.</li>
     * </ul>
     * <br>
     * <b>Response status:</b>
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>404 - Issuance policy could not be found.</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <b>Return type</b>: <tt>IssuancePolicy</tt><br>
     * 
     * @param credentialSpecificationUid
     *            UID of the credential specification
     * @return Response
     */
    @GET()
    @Path("/protected/issuancePolicy/get/{credentialSpecificationUid}")
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
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /* CREDENTIAL SPECIFICATION GENERATION */

    /**
     * <b>Path</b>: /protected/attributeInfoCollection/{name} (GET)<br>
     * <br>
     * <b>Description</b>: This method can be used to obtain information about
     * attributes from the attribute source (i.e. LDAP, JDBC or something else).
     * This method will return an <tt>AttributeInfoCollection</tt> that can be
     * passed to
     * {@link #generateCredentialSpecification(AttributeInfoCollection)}. <br>
     * <br>
     * <b>Path parameters</b>:
     * <ul>
     * <li>name - Name identifies the entity from which to extract/gather
     * attribute information. For LDAP <em>name
     *  </em> is an object class and for JDBC <em>name</em> is the name of a
     * table in a database. Please be aware that <em>name</em> is ALWAYS
     * provider specific.</li>
     * </ul>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>AtributeInfoCollection</tt><br>
     * 
     * @param name
     *            Name
     * @return Response
     */
    @GET()
    @Path("/protected/attributeInfoCollection/{name}")
    public Response attributeInfoCollection(@PathParam("name") String name) { /* [TEST EXISTS] */
        logger.entry();
        
        AttributeInfoProvider attribInfoProvider = null;

        try {
            IssuanceConfiguration configuration = ServicesConfiguration
                    .getIssuanceConfiguration();
            attribInfoProvider = AttributeInfoProvider
                    .getAttributeInfoProvider(configuration);

            return Response.ok(attribInfoProvider.getAttributes(name),
                    MediaType.APPLICATION_XML).build();
        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        } finally {
            if (attribInfoProvider != null)
                attribInfoProvider.shutdown();
        }
    }

    /**
     * <b>Path</b>: /protected/credentialSpecification/generate (POST)<br>
     * <br>
     * <b>Description</b>: Generate a credential specification based on the
     * supplied <tt>AttributeInfoCollection</tt>.<br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>AttributeInfoCollection</tt><br>
     * <b>Return type</b>: <tt>CredentialSpecification</tt><br>
     * 
     * @param attrInfoCol
     *            the attribute info collection
     * @return Response
     */
    @POST()
    @Path("/protected/credentialSpecification/generate")
    @Consumes({ MediaType.APPLICATION_XML })
    public Response generateCredentialSpecification( /* [TEST EXISTS] */
            AttributeInfoCollection attrInfoCol) {

        try {

            return Response
                    .ok(of.createCredentialSpecification(new CredentialSpecGenerator()
                    .generateCredentialSpecification(attrInfoCol)),
                    MediaType.APPLICATION_XML).build();
        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /* SYSTEM PARAMETERS */

    /**
     * <b>Path</b>: /protected/setupSystemParameters/ (POST)<br>
     * <b>Description</b>:
     * 
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
     * urn:abc4trust:1.0:algorithm:idemix for Identity Mixer
     * 
     * This method will overwrite any existing system parameters.<br>
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK</li>
     * <li>500 - ERROR</li>
     * </ul>
     * <br>
     * <b>Return type</b>: <tt>SystemParameters</tt><br>
     * 
     * @param securityLevel
     *            Security level
     * @param cryptoMechanism
     *            Crypto mechanism (idemix)
     * @return Response
     */
    @POST()
    @Path("/protected/setupSystemParameters/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response setupSystemParameters(
            @QueryParam("securityLevel") int securityLevel,
            @QueryParam("cryptoMechanism") URI cryptoMechanism) {

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
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /* ISSUER PARAMETERS */

    /**
     * <b>Path</b>: /protected/setupIssuerParameters/ (POST) <br>
     * <br>
     * <b>Description:</b>
     * 
     * This method generates a fresh issuance key and the corresponding Issuer
     * parameters. The issuance key is stored in the Issuer's key store, the
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
     * <br>
     * <b>Response status</b>:
     * <ul>
     * <li>200 - OK (application/xml)</li>
     * <li>500 - ERROR</li>
     * <li>404 - Credential specification could not be found.</li>
     * </ul>
     * <br>
     * <b>Input type</b>: <tt>IssuerParametersInput</tt><br>
     * <b>Return type</b>: <tt>IssuerParameters</tt><br>
     * 
     * @param issuerParametersInput
     *            Input for issuer parameters setup.
     * @return Response
     */
    @POST()
    @Path("/protected/setupIssuerParameters/")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response setupIssuerParameters(
            IssuerParametersInput issuerParametersInput) {

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
                return logger.exit(Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(errNoCredSpec + "("
                                + credentialSpecUid.toString() + ")").build());
            }

            URI issuerParametersUid = issuerParametersInput.getParametersUID();
            URI hash = hashAlgorithm;
            URI revocationParametersUid = issuerParametersInput
                    .getRevocationParametersUID();
            List<FriendlyDescription> friendlyDescriptions = issuerParametersInput
                    .getFriendlyIssuerDescription();
            IssuerParameters issuerParameters = instance.setupIssuerParameters(
                    cryptoEngine, credspec, systemParameters,
                    issuerParametersUid, hash, revocationParametersUid,
                    systemAndIssuerParamsPrefix, friendlyDescriptions);

            logger.info("IssuanceService - issuerParameters generated");

            List<Object> objs = systemParameters.getAny();
            for (Object obj : objs)
                logger.info(obj + "-" + obj.getClass());

            SystemParameters serializeSp = SystemParametersUtil
                    .serialize(systemParameters);

            issuerParameters.setSystemParameters(serializeSp);
            return logger.exit(Response.ok(
                    this.of.createIssuerParameters(issuerParameters),
                    MediaType.APPLICATION_XML).build());
        } catch (Exception e) {
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }
    }

    /* NON-REST METHODS */

    private void initializeHelper(CryptoEngine cryptoEngine) {
        logger.info("IssuanceService loading");

        try {
            if (IssuanceHelper.isInit()) {
                logger.info("IssuanceHelper is initialized");
            } else {

                logger.info("Initializing IssuanceHelper");

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
            logger.warn("Create Domain FAILED " + e);
            e.printStackTrace();
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
     */
    // @POST()
    // @Path("/protected/initIssuanceProtocol/")
    // @Consumes({ MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    private Response initIssuanceProtocol(
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
            logger.catching( e);
            return logger.exit(ExceptionDumper.dumpException(e, logger));
        }

    }

    private CryptoEngine getCryptoEngine(URI issuerParametersUid) {
        /*
         * there was some endsWith check on the Uid actually. We only support
         * Idemix for now. -- munt
         */
        return CryptoEngine.IDEMIX;
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

    private CryptoEngine getCryptoEngine(final IssuanceMessage issuanceMessage) {
        CryptoEngine engine = CryptoEngine.IDEMIX;

        if (issuanceMessage.getAny().get(0) instanceof JAXBElement) {
            engine = CryptoEngine.IDEMIX;
            return engine;
        }
        throw new RuntimeException("We only support idemix. Sorry :(");
    }

}
