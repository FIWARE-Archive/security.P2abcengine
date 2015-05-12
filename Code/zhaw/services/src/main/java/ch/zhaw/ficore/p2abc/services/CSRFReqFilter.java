package ch.zhaw.ficore.p2abc.services;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.ext.Provider;

import com.sun.jersey.api.representation.Form;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

@Provider
public class CSRFReqFilter implements ContainerRequestFilter {

    public static boolean verboseLogging = false;

    @Override
    public ContainerRequest filter(final ContainerRequest arg0) {
        Form f = arg0.getFormParameters();
        String csrf = f.getFirst("csrf");
        System.out.println(csrf);
        if (csrf == null) {
            csrf = arg0.getQueryParameters().getFirst("csrf");
        }

        if (needsCSRF(arg0.getPath(false))) {

            Cookie csrf_cookie = arg0.getCookies().get("csrf");

            if (csrf_cookie == null) {
                throw new RuntimeException("CSRF Protection Triggered: "
                        + arg0.getPath());
            }

            String csrf_cookie_val = csrf_cookie.getValue();

            if (!csrf.equals(csrf_cookie_val)) {
                throw new RuntimeException("CSRF Protection Triggered: "
                        + arg0.getPath());
            }

        }

        return arg0;

    }

    public boolean needsCSRF(final String path) {

        if (path.equals("user-gui/profile")) {
            return false;
        }
        if (path.equals("issuance-gui/protected/profile")) {
            return false;
        }
        if (path.equals("verification-gui/protected/profile")) {
            return false;
        }

        if (path.startsWith("user-gui/")) {
            return true;
        }
        if (path.startsWith("issuance-gui/")) {
            return true;
        }
        if (path.startsWith("verification-gui/")) {
            return true;
        }

        return false;
    }

}