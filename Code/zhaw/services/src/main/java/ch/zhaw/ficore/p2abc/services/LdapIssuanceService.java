package ch.zhaw.ficore.p2abc.services;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnection;
import ch.zhaw.ficore.p2abc.ldap.helper.LdapConnectionConfig;

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

	/**
	 * Creates an XML representation of an LDAP objectClass
	 * that may be processed by an UI and sent back to this 
	 * service to generate a corresponding crendentialSpecification.
	 *
	 * STATUS: - ERROR if something went wrong.
	 *         - OK otherwise
	 * 
	 * @param magicCookie the magicCookie
	 * @param objectClass name of the objectClass (LDAP)
	 */
	@GET()
	@Path("/schemaDump/{magicCookie}/{oc}")
	public Response schemaDump(@PathParam("magicCookie") String magicCookie, @PathParam("oc") String objectClass) {
		synchronized(configLock) {
			if(!ldapSrvConf.isMagicCookieCorrect(magicCookie)) {
				return Response.status(Response.Status.FORBIDDEN).entity(errMagicCookie).build();
			}
		}

		try {
			LdapConnectionConfig cfg = null;
			synchronized(configLock) {
				cfg = new LdapConnectionConfig(ldapSrvConf.getPort(), ldapSrvConf.getHost());
				cfg.setAuth(ldapSrvConf.getAuthId(), ldapSrvConf.getAuthPw());
			}
			LdapConnection con = cfg.newConnection();
		
			DirContext ctx = con.getInitialDirContext();
			DirContext schema = ctx.getSchema("ou=schema");
		
			Attributes answer = schema.getAttributes("ClassDefinition/" + objectClass);
			javax.naming.directory.Attribute must = answer.get("must");
			javax.naming.directory.Attribute may = answer.get("may");

			ObjectClass oc = new ObjectClass(objectClass);
		
			for(int i = 0; i < must.size(); i++) {
				Attributes attrAnswer = schema.getAttributes("AttributeDefinition/" + must.get(i));
				oc.addAttribute(must.get(i).toString(), attrAnswer.get("syntax").get(0).toString());
			}
	
			for(int i = 0; i < may.size(); i++) {
				Attributes attrAnswer = schema.getAttributes("AttributeDefinition/" + may.get(i));
				oc.addAttribute(may.get(i).toString(), attrAnswer.get("syntax").get(0).toString());
			}

			return Response.ok(oc, MediaType.APPLICATION_XML).build();
		}
		catch(Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}
}
