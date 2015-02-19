package ch.zhaw.ficore.p2abc.services.demo;

import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.hp.gagawa.java.elements.Body;
import com.hp.gagawa.java.elements.Div;
import com.hp.gagawa.java.elements.H2;
import com.hp.gagawa.java.elements.Head;
import com.hp.gagawa.java.elements.Html;
import com.hp.gagawa.java.elements.Link;
import com.hp.gagawa.java.elements.P;
import com.hp.gagawa.java.elements.Text;
import com.hp.gagawa.java.elements.Title;

import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;
import ch.zhaw.ficore.p2abc.services.helpers.user.UserGUI;

@Path("/demo-resource")
public class DemoResource {
    
    @Context
    HttpServletRequest request;
    
    @GET()
    @Path("/page/")
    public Response resource(@QueryParam("accesstoken") String accessToken)
            throws Exception {
        try {
            String result = (String) RESTHelper
                    .getRequest("http://localhost:8080/zhaw-p2abc-webservices/verification/verifyAccessToken?accesstoken="
                            + URLEncoder.encode(accessToken, "UTF-8"));

            if (result.equals("resource")) {
                return Response.ok(resource()).build();
            }

            return Response.ok(error()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(error()).build();
        }
    }
    
    public String resource() {
        Html html = new Html();
        Head head = new Head().appendChild(new Title().appendChild(new Text(
                "Demo-Resource")));
        html.appendChild(head);
        head.appendChild(new Link().setHref(request.getContextPath() + "/css/style.css")
                .setRel("stylesheet").setType("text/css"));
        
        Div mainDiv = new Div().setCSSClass("mainDiv");
        mainDiv.appendChild(new H2().appendChild(new Text("Demo-Resource")));
        
        Div containerDiv = new Div().setCSSClass("containerDiv");
        html.appendChild(new Body().appendChild(containerDiv));
        containerDiv.appendChild(mainDiv);
        
        P p = new P().setCSSClass("success");
        p.appendChild(new Text("Welcome! You have been granted access to this resource. "));
        mainDiv.appendChild(p);
        
        return html.write();
    }
    
    public String error() {
        Html html = new Html();
        Head head = new Head().appendChild(new Title().appendChild(new Text(
                "Demo-Resource")));
        html.appendChild(head);
        head.appendChild(new Link().setHref(request.getContextPath() + "/css/style.css")
                .setRel("stylesheet").setType("text/css"));
        
        Div mainDiv = new Div().setCSSClass("mainDiv");
        mainDiv.appendChild(new H2().appendChild(new Text("Demo-Resource")));
        
        Div containerDiv = new Div().setCSSClass("containerDiv");
        html.appendChild(new Body().appendChild(containerDiv));
        containerDiv.appendChild(mainDiv);
        
        P p = new P().setCSSClass("error");
        p.appendChild(new Text("We are sorry but the access token you provided was invalid and we therefore can not grant you access to this resource!"));
        mainDiv.appendChild(p);
        
        return html.write();
    }
}
