package ch.zhaw.ficore.p2abc.services;

import javax.ws.rs.ext.Provider;

import com.sun.jersey.api.representation.Form;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

@Provider
public class LoggingFilter implements
        ContainerRequestFilter {

    public static boolean verboseLogging = false;

    @Override
    public ContainerRequest filter(ContainerRequest arg0) {
        Form f = arg0.getFormParameters();
        String csrf = f.getFirst("csrf");
        System.out.println(csrf);
        f.remove("csrf");
        return arg0;
    }

   
}