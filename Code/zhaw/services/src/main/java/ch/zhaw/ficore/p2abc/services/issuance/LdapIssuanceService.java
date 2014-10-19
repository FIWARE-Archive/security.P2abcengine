package ch.zhaw.ficore.p2abc.services.issuance;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ch.zhaw.ficore.p2abc.services.ConfigurationData;
import ch.zhaw.ficore.p2abc.services.ServiceConfiguration;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AttributeInfoCollection;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthInfoSimple;
import ch.zhaw.ficore.p2abc.services.issuance.xml.AuthenticationRequest;
import eu.abc4trust.xml.ObjectFactory;

@Path("/ldap-issuance-service")
public class LdapIssuanceService {
	@Context
	ServletContext context;

	private static final String ldapConfigPathProperty = "abc4trust-ldapSrvConfPath";
	private static final String ldapConfigPathDefault = "/etc/abc4trust/ldapServiceConfig.xml";
	private static final String errMagicCookie = "Magic-Cookie is not correct!";
	private static Object configLock = new Object();
	private static AuthenticationProvider authProvider;
	private static AttributeInfoProvider attribInfoProvider;
	private ObjectFactory of = new ObjectFactory(); 

	static {
		//ServiceConfiguration.getInstance().setLdapParameters(false, "localhost", 10389, "", "");
		ServiceConfiguration.setFakeParameters();
		initializeWithConfiguration();
	}
	
	public static void initializeWithConfiguration() {
	  ConfigurationData configuration = ServiceConfiguration.getServiceConfiguration();
		authProvider = AuthenticationProvider.getAuthenticationProvider(configuration);
		attribInfoProvider = AttributeInfoProvider.getAttributeInfoProvider(configuration);
	}


	public LdapIssuanceService() {
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
		if(!ServiceConfiguration.isMagicCookieCorrect(magicCookie))
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
		if(!ServiceConfiguration.isMagicCookieCorrect(magicCookie))
			return Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build();
		
		return Response.ok(of.createCredentialSpecification(new CredentialSpecGenerator().
					generateCredentialSpecification(attrInfoCol)),
				MediaType.APPLICATION_XML).build();
	}
}
