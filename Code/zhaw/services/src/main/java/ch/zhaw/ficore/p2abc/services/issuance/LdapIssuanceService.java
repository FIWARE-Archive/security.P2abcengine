package ch.zhaw.ficore.p2abc.services.issuance;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.servlet.ServletContext;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import ch.zhaw.ficore.p2abc.ldap.helper.*;
import ch.zhaw.ficore.p2abc.services.issuance.xml.*;

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

	static {
		ServiceConfiguration.getInstance().setLdapParameters(false, "localhost", 10389, "", "");
		initializeWithConfiguration();
	}
	
	public static void initializeWithConfiguration() {
		authProvider = AuthenticationProvider.getAuthenticationProvider(ServiceConfiguration.getInstance());
		attribInfoProvider = AttributeInfoProvider.getAttributeInfoProvider(ServiceConfiguration.getInstance());
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
	 * @param magicCookie the magic cookie
	 * @param name name (see description of this method above)
	 * @return response
	 */
	@GET()
	@Path("/attributeInfoCollection/{magicCookie}/{name}")
	public Response attributeInfoCollection(@PathParam("magicCookie") String magicCookie, 
			@PathParam("name") String name) {
		if(!ServiceConfiguration.getInstance().isMagicCookieCorrect(magicCookie))
			return Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build();
		return Response.ok(attribInfoProvider.getAttributes(name)).build();
	}
}
