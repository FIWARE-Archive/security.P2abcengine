package ch.zhaw.ficore.p2abc.services;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.servlet.ServletContext;

@Path("/ldap-issuance-service")
public class LdapIssuanceService {
	@Context
	ServletContext context;

	@GET()
    @Path("/status")
    @Produces({MediaType.TEXT_PLAIN})
    public Response issuerStatus() {
        //this.log.info("IssuanceService - status : running");
        return Response.ok().build();
    }
}
