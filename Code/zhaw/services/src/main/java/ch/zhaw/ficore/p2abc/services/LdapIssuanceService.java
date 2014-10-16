package ch.zhaw.ficore.p2abc.services;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.servlet.ServletContext;

@Path("/ldap-issuance-service")
public class LdapIssuanceService {
	@Context
	ServletContext context;

	private LdapServiceConfig ldapSrvConf;
	private static final String ldapConfigPathProperty = "abc4trust-ldapSrvConfPath";
	private static final String ldapConfigPathDefault = "/etc/abc4trust/ldapServiceConfig.xml";

	public LdapIssuanceService() {
		String ldapSrvConfPath = System.getProperties().getProperty(ldapConfigPathProperty);

		if(ldapSrvConfPath == null)
			ldapSrvConfPath = ldapConfigPathDefault;

		ldapSrvConf = LdapServiceConfig.fromFile(ldapSrvConfPath);
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
	 */
	@GET()
	@Path("/showConfig/{magicCookie}")
	public Response verifyMagicCookie(@PathParam("magicCookie") String magicCookie) {
		if(!ldapSrvConf.isMagicCookieCorrect(magicCookie)) {
		    return Response.status(Response.Status.FORBIDDEN).entity("Magic-Cookie is not correct").build();
		}
		return Response.ok(ldapSrvConf, MediaType.APPLICATION_XML).build();
	}
}
