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

	private static LdapServiceConfig ldapSrvConf;

	static {
		loadConfig();
	}

	public synchronized static void loadConfig() {
		synchronized(configLock) {
			String ldapSrvConfPath = System.getProperties().getProperty(ldapConfigPathProperty);

			if(ldapSrvConfPath == null)
				ldapSrvConfPath = ldapConfigPathDefault;

			ldapSrvConf = LdapServiceConfig.fromFile(ldapSrvConfPath);
		}
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
	 */
	@GET()
	@Path("/showConfig/{magicCookie}")
	public Response verifyMagicCookie(@PathParam("magicCookie") String magicCookie) {
		synchronized(configLock) {
			if(!ldapSrvConf.isMagicCookieCorrect(magicCookie)) {
				return Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build();
			}
		}

		return Response.ok(ldapSrvConf, MediaType.APPLICATION_XML).build();
	}

	/**
	 */
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
	}
	
	@GET()
	@Path("/test")
	public Response test() {
		return Response.ok(new AuthentificationRequest(new AuthInfoSimple("hi","there"))).build();
	}
}
