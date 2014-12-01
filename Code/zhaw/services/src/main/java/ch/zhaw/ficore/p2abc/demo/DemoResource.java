package ch.zhaw.ficore.p2abc.demo;

import java.net.URLEncoder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;

@Path("/demo-resource")
public class DemoResource {
    
    @GET()
    @Path("/page/")
    public Response resource(@QueryParam("accesstoken") String accessToken) throws Exception {
        try {
            String result = (String) RESTHelper.getRequest("http://localhost:9998/verification/verifyAccessToken?accesstoken=" +
                    URLEncoder.encode(accessToken, "UTF-8"));
            
            
            if(result.equals("resource")) {
                return Response.ok("You are allowed to access this page!").build();
            }
            
            return Response.status(Response.Status.FORBIDDEN).entity("You are not allowed to access this page!").build();
        }
        catch(Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.FORBIDDEN).entity("You are not allowed to access this page!").build();
        }
    }
}
