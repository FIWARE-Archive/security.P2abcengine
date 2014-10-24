package ch.zhaw.ficore.p2abc.services.issuance;

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
import javax.ws.rs.WebApplicationException;

<<<<<<< HEAD
import ch.zhaw.ficore.p2abc.services.ServicesConfiguration;
import ch.zhaw.ficore.p2abc.services.ServicesConfiguration.ServiceType;
=======
import ch.zhaw.ficore.p2abc.services.ConfigurationData;
import ch.zhaw.ficore.p2abc.services.ServiceConfiguration;
import ch.zhaw.ficore.p2abc.services.UserStorageManager; //from Code/core-abce/abce-services (COPY)
>>>>>>> branch 'feature/ldap-issuer' of https://github.engineering.zhaw.ch/neut/p2abcengine.git
import ch.zhaw.ficore.p2abc.services.issuance.xml.AttributeInfoCollection;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthInfoSimple;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthenticationRequest;

import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.ABCEBoolean;
import eu.abc4trust.xml.CredentialSpecification;
import javax.xml.bind.JAXBElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.ri.servicehelper.issuer.IssuanceHelper;



import java.net.URI;

@Path("/ldap-issuance-service")
public class LdapIssuanceService {
	@Context
	ServletContext context;

	private static final String ldapConfigPathProperty = "abc4trust-ldapSrvConfPath";
	private static final String ldapConfigPathDefault = "/etc/abc4trust/ldapServiceConfig.xml";
	private static final String errMagicCookie = "Magic-Cookie is not correct!";
	private static AuthenticationProvider authProvider;
	private static AttributeInfoProvider attribInfoProvider;
	private ObjectFactory of = new ObjectFactory(); 
	
	private final String fileStoragePrefix = "issuer_storage/"; //TODO: Files
	
	private Logger logger;

	static {
<<<<<<< HEAD
		//ServiceConfiguration.getInstance().setLdapParameters(false, "localhost", 10389, "", "");
		ServicesConfiguration.setFakeIssuanceParameters();
=======
		ConfigurationData cfgData = new ConfigurationData();
		cfgData.ldapUseTls = false;
		cfgData.ldapServerName = "localhost";
		cfgData.ldapServerPort = 10389;
		cfgData.ldapUser = "uid=admin, ou=system";
		cfgData.ldapPassword = "secret";
		cfgData.identitySource = ConfigurationData.IdentitySource.LDAP;
		ServiceConfiguration.setServiceConfiguration(cfgData);
>>>>>>> branch 'feature/ldap-issuer' of https://github.engineering.zhaw.ch/neut/p2abcengine.git
		initializeWithConfiguration();
	}
	
	public static void initializeWithConfiguration() {
<<<<<<< HEAD
	  IssuanceConfigurationData configuration = ServicesConfiguration.getIssuanceConfiguration();
=======
		ConfigurationData configuration = ServiceConfiguration.getServiceConfiguration();
>>>>>>> branch 'feature/ldap-issuer' of https://github.engineering.zhaw.ch/neut/p2abcengine.git
		authProvider = AuthenticationProvider.getAuthenticationProvider(configuration);
		attribInfoProvider = AttributeInfoProvider.getAttributeInfoProvider(configuration);
	}


	public LdapIssuanceService() {
		logger = LogManager.getLogger(LdapIssuanceService.class.getName());
	}

	@GET()
    @Path("/status")
    @Produces({MediaType.TEXT_PLAIN})
    public Response issuerStatus() {
        //this.log.info("IssuanceService - status : running");
        return Response.ok().build();
    }

	/**
	 * /showConfig/{magicCookie} will send the client
	 * the configuration of this service if and only if 
	 * the supplied magicCookie is correct (which means
	 * the supplied magicCookie matches the magicCookie
	 * in the configuration of this service.)
	 * 
	 * Status: - FORBIDDEN if magicCookie is not correct.
	 *         - OK otherwise.
	 *
	 * @param magicCookie - the magicCookie
	 *//*
	@GET()
	@Path("/showConfig/{magicCookie}")
	public Response verifyMagicCookie(@PathParam("magicCookie") String magicCookie) {
		synchronized(configLock) {
			if(!ldapSrvConf.isMagicCookieCorrect(magicCookie)) {
				return Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build();
			}
		}

		return Response.ok(ldapSrvConf, MediaType.APPLICATION_XML).build();
	}*/

	/**
	 *//*
	@GET()
	@Path("/reloadConfig/{magicCookie}")
	public Response reloadConfig(@PathParam("magicCookie") String magicCookie) {
		synchronized(configLock) {
			if(!ldapSrvConf.isMagicCookieCorrect(magicCookie)) {
				return Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build();
			}
		}

		try {
			loadConfig();
			return Response.ok().entity("OK").build();
		}
		catch(Exception e) {
			e.printStackTrace();
			return Response.serverError().entity(e.toString()).build();
		}
	}*/
	
	@GET()
	@Path("/test")
	public Response test() {
		return Response.ok(new AuthenticationRequest(new AuthInfoSimple("hi","there"))).build();
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
		
        logger.info("IssuanceService - storeCredentialSpecification: \""
                + credentialSpecifationUid + "\"");

        try {
            CryptoEngine engine = CryptoEngine.IDEMIX;
            KeyManager keyManager = UserStorageManager
                    .getKeyManager(IssuanceHelper.getFileStoragePrefix(
                            this.fileStoragePrefix, engine));

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
	
	@GET()
	@Path("/getCredentialSpecification/{magicCookie}/{credentialSpecificationUid}")
	public Response getCredentialSpecification(
			@PathParam("magicCookie") String magicCookie,
			@PathParam("credentialSpecificationUid") String credentialSpecificationUid) {
		logger.entry();
		
		logger.info("IssuanceService - getCredentialSpecification: " + credentialSpecificationUid);
		
		try {
			KeyManager keyManager = UserStorageManager.getKeyManager(
					IssuanceHelper.getFileStoragePrefix(this.fileStoragePrefix,
					CryptoEngine.IDEMIX));
			
			System.out.println("WOOT: " + IssuanceHelper.getFileStoragePrefix(this.fileStoragePrefix,
					CryptoEngine.IDEMIX));
			
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
}
