package ch.zhaw.ficore.p2abc.services.issuance;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.zhaw.ficore.p2abc.services.ConfigurationException;
import ch.zhaw.ficore.p2abc.services.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.UserStorageManager; //from Code/core-abce/abce-services (COPY)
import ch.zhaw.ficore.p2abc.services.issuance.xml.AttributeInfoCollection;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthenticationRequest;
import ch.zhaw.ficore.p2abc.services.issuance.xml.QueryRule;
import ch.zhaw.ficore.p2abc.storage.SqliteURIBytesStorage;
import ch.zhaw.ficore.p2abc.storage.URIBytesStorage;
import ch.zhaw.ficore.p2abc.storage.UnsafeTableNameException;
import ch.zhaw.ficore.p2abc.services.helpers.issuer.*;
import ch.zhaw.ficore.p2abc.services.guice.*;

import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.keyManager.KeyStorage;
import eu.abc4trust.xml.ABCEBoolean;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.ObjectFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Path("/ldap-issuance-service")
public class LdapIssuanceService {
	@Context
	ServletContext context;

	//private static final String ldapConfigPathProperty = "abc4trust-ldapSrvConfPath";
	//private static final String ldapConfigPathDefault = "/etc/abc4trust/ldapServiceConfig.xml";
	private static final String errMagicCookie = "Magic-Cookie is not correct!";
	private static AuthenticationProvider authProvider;
	private static AttributeInfoProvider attribInfoProvider;
	private ObjectFactory of = new ObjectFactory(); 
	private URIBytesStorage queryRules;

	private final String fileStoragePrefix = "issuer_storage/"; //TODO: Files

	private Logger logger;

	static {
		IssuanceConfigurationData cfgData;
		try {
			cfgData = new IssuanceConfigurationData(false, "localhost", 10389,
					"uid=admin, ou=system", "secret");
		} catch (ConfigurationException e) {
			cfgData = null;
		}
		ServicesConfiguration.setIssuanceConfiguration(cfgData);
		initializeWithConfiguration();
	}

	public static void initializeWithConfiguration() {
		IssuanceConfigurationData configuration = ServicesConfiguration.getIssuanceConfiguration();
		authProvider = AuthenticationProvider.getAuthenticationProvider(configuration);
		attribInfoProvider = AttributeInfoProvider.getAttributeInfoProvider(configuration);
	}


	public LdapIssuanceService() throws ClassNotFoundException, SQLException, UnsafeTableNameException {
		logger = LogManager.getLogger();
		queryRules = new SqliteURIBytesStorage("/tmp/rules.db", "query_rules");
	}

	@GET()
	@Path("/status")
	@Produces({MediaType.TEXT_PLAIN})
	public Response issuerStatus() {
		//this.log.info("IssuanceService - status : running");
		return Response.ok().build();
	}

	@GET()
	@Path("/test")
	public Response test() throws URISyntaxException, SQLException, ClassNotFoundException, UnsafeTableNameException {
		Injector injector = Guice.createInjector(new SomeModule());
		KeyStorage keyStorage = injector.getInstance(KeyStorage.class);
		return Response.ok().build();
	}

	@GET()
	@Path("/test2/{param}")
	public Response test2(@PathParam("param") String param) throws URISyntaxException, SQLException, ClassNotFoundException, UnsafeTableNameException {
		boolean b = new SqliteURIBytesStorage("/tmp/foo.db", "foobar").containsKey(new URI(param));
		return Response.ok(b ? "true" : "false").build();
	}

	@GET()
	@Path("/test3")
	public Response test3() throws SQLException, ClassNotFoundException, UnsafeTableNameException {
		String s = "";
		for(URI uri : new SqliteURIBytesStorage("/tmp/foo.db", "foobar").keys()) {
			s += uri.toString() + ";";
		}
		return Response.ok(s).build();
	}

	/**
	 * Store QueryRule.
	 * 
	 * This function is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param credentialSpecificationUid UID of the credSpec
	 * @return Response
	 */
	@PUT()
	@Path("/storeQueryRule/{magicCookie}/{credentialSpecificationUid}")
	@Consumes({MediaType.APPLICATION_XML})
	public Response storeQueryRule(@PathParam("magicCookie") String magicCookie,
			@PathParam("credentialSpecificationUid") String credentialSpecificationUid,
			QueryRule rule) {

		logger.entry();

		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		try {
			queryRules.put(new URI(credentialSpecificationUid), SerializationUtils.serialize(rule));
			return logger.exit(Response.ok("OK").build());
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(Response.serverError().build());
		}
	}

	/**
	 * Retreive a QueryRule.
	 * 
	 * This function is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param credentialSpecificationUid
	 * @return QueryRule
	 */
	@GET()
	@Path("/getQueryRule/{magicCookie}/{credentialSpecificationUid}")
	@Consumes({MediaType.APPLICATION_XML})
	public Response storeQueryRule(@PathParam("magicCookie") String magicCookie,
			@PathParam("credentialSpecificationUid") String credentialSpecificationUid) {

		logger.entry();

		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		try {
			if(!queryRules.containsKey(new URI(credentialSpecificationUid)))
				return logger.exit(Response.status(Response.Status.NOT_FOUND).build());
			QueryRule rule = (QueryRule) SerializationUtils.deserialize(queryRules.get(new URI(credentialSpecificationUid)));
			return logger.exit(Response.ok(rule, MediaType.APPLICATION_XML).build());
		}
		catch(Exception e) {
			logger.catching(e);
			return logger.exit(Response.serverError().build());
		}
	}


	/**
	 * This function can be used to test the authentication.
	 * It returns a response with status code OK if the authentication
	 * was successful, otherwise it returns a response with status code FORBIDDEN.
	 * 
	 * @param authReq an AuthenticationRequest
	 * @return response
	 */
	@POST()
	@Path("/testAuthentication")
	@Consumes({MediaType.APPLICATION_XML})
	public Response testAuthentication(AuthenticationRequest authReq) {

		if(authProvider.authenticate(authReq.authInfo))
			return Response.ok("OK").build();
		else
			return Response.status(Response.Status.FORBIDDEN).entity("ERR").build();
	}

	/**
	 * This function can be used to obtain the AttributeInfoCollection
	 * that may later be converted into a CredentialSpecification. 
	 * This function contacts the identity source to obtain the necessary
	 * attributes for <em>name</em>. <em>name</em> refers to a <em>kind</em> of credential
	 * a user can get issued. For example <em>name</em> may refer to an objectClass
	 * in LDAP. However, the exact behaviour of <em>name</em> depends on the configuration
	 * of this service. 
	 * 
	 * This function is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param name name (see description of this method above)
	 * @return an AttributeInfoCollection as application/xml.
	 */
	@GET()
	@Path("/attributeInfoCollection/{magicCookie}/{name}")
	public Response attributeInfoCollection(@PathParam("magicCookie") String magicCookie, 
			@PathParam("name") String name) {
		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build();

		return Response.ok(attribInfoProvider.getAttributes(name), MediaType.APPLICATION_XML).build();
	}

	/**
	 * Generates (or creates) the corresponding CredentialSpecification
	 * for a given AttributeInfoCollection. This function assumes that the
	 * given AttributeInfoCollection is sane. 
	 * 
	 * This function is protected by the magic cookie.
	 * 
	 * @param magicCookie the magic cookie
	 * @param attrInfoColl the AttributeInfoCollection
	 * @return a CredentialSpecification
	 */
	@POST()
	@Path("/genCredSpec/{magicCookie}")
	@Consumes({MediaType.APPLICATION_XML})
	public Response genCredSpec(@PathParam("magicCookie") String magicCookie, AttributeInfoCollection attrInfoCol) {
		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build();

		return Response.ok(of.createCredentialSpecification(new CredentialSpecGenerator().
				generateCredentialSpecification(attrInfoCol)),
				MediaType.APPLICATION_XML).build();
	}

	//This function was copied from the original IssuanceService in Code/core-abce/abce-services
	@PUT()
	@Path("/storeCredentialSpecification/{magicCookie}/{credentialSpecifationUid}")
	@Consumes({ MediaType.APPLICATION_XML })
	public Response storeCredentialSpecification(
			@PathParam("magicCookie") String magicCookie, 
			@PathParam("credentialSpecifationUid") URI credentialSpecifationUid,
			CredentialSpecification credSpec) {

		logger.entry();

		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		logger.info("IssuanceService - storeCredentialSpecification: \""
				+ credentialSpecifationUid + "\"");

		try {
			CryptoEngine engine = CryptoEngine.IDEMIX;
			KeyManager keyManager = UserStorageManager
					.getKeyManager("idemix");

			boolean r1 = keyManager.storeCredentialSpecification(
					credentialSpecifationUid, credSpec);

			ABCEBoolean createABCEBoolean = this.of.createABCEBoolean();
			createABCEBoolean.setValue(r1);

			return logger.exit(
					Response.ok(of.createABCEBoolean(createABCEBoolean), MediaType.APPLICATION_XML).build());
		} catch (Exception ex) {
			logger.catching(ex);
			return logger.exit(Response.serverError().build());
		}
	}

	//This function was copied from the original IssuanceService in Code/core-abce/abce-services
	@GET()
	@Path("/getCredentialSpecification/{magicCookie}/{credentialSpecificationUid}")
	public Response getCredentialSpecification(
			@PathParam("magicCookie") String magicCookie,
			@PathParam("credentialSpecificationUid") String credentialSpecificationUid) {
		logger.entry();

		if(!ServicesConfiguration.isMagicCookieCorrect(magicCookie))
			return logger.exit(Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build());

		logger.info("IssuanceService - getCredentialSpecification: " + credentialSpecificationUid);

		try {
			KeyManager keyManager = UserStorageManager.getKeyManager("idemix");

			CredentialSpecification credSpec = keyManager.getCredentialSpecification(new URI(credentialSpecificationUid));

			if(credSpec == null) {
				return logger.exit(Response.status(Response.Status.NOT_FOUND).build());
			}
			else
				return logger.exit(Response.ok(of.createCredentialSpecification(credSpec), MediaType.APPLICATION_XML).build());
		} 
		catch(Exception ex) {
			logger.catching(ex);
			return logger.exit(Response.serverError().build());
		}
	}

	/*
	 * The following section contains code copied from the original issuance service from the tree
	 * Code/core-abce/abce-service
	 */

	/* SECTION */

	/*
	private void initializeHelper(CryptoEngine cryptoEngine) {
        logger.info("IssuanceService loading...");

        try {
            if (IssuanceHelper.isInit()) {
                logger.info("IssuanceHelper is initialized");
                IssuanceHelper.verifyFiles(false, this.fileStoragePrefix,
                        cryptoEngine);
            } else {

                logger.info("Initializing IssuanceHelper...");

                IssuanceHelper.initInstanceForService(cryptoEngine,
                        "", "");

                logger.info("IssuanceHelper is initialized");
            }
        } catch (Exception e) {
            System.out.println("Create Domain FAILED " + e);
            e.printStackTrace();
        }
    }*/

	/* END SECTION */
}
