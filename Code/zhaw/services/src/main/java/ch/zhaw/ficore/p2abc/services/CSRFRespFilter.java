package ch.zhaw.ficore.p2abc.services;

import java.security.SecureRandom;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

public class CSRFRespFilter implements ContainerResponseFilter {

    private final static int NUM_HEX_DIGITS = 16;

    @Override
    public ContainerResponse filter(final ContainerRequest arg0,
            final ContainerResponse resp) {

        NewCookie nc = new NewCookie("csrf", randomString());

        Response response = resp.getResponse();
        Response cookieResponse = Response.fromResponse(response).cookie(nc)
                .build();

        resp.setResponse(cookieResponse);

        return resp;
    }

    private String randomString() {
        StringBuilder sb = new StringBuilder();

        SecureRandom sr = new SecureRandom();

        char[] chars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        for (int i = 0; i < NUM_HEX_DIGITS; i++) {
            int n = sr.nextInt(0x10);
            sb.append(chars[n]);
        }

        return sb.toString();
    }
}
