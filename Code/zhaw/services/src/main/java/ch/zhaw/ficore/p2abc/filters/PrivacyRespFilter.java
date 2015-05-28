package ch.zhaw.ficore.p2abc.filters;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

public class PrivacyRespFilter implements ContainerResponseFilter {

    /** The logger. */
    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(PrivacyRespFilter.class));

    public ContainerResponse filter(final ContainerRequest req,
            final ContainerResponse resp) {

        String accesstoken = req.getHeaderValue("X-P2ABC-ACCESSTOKEN");
        LOGGER.info("X-P2ABC-ACCESSTOKEN = " + accesstoken);

        NewCookie nc = new NewCookie("x-p2abc-accesstoken", accesstoken);

        Response response = resp.getResponse();

        Response cookieResponse = Response.fromResponse(response).cookie(nc)
                .build();

        resp.setResponse(cookieResponse);

        return resp;
    }
}
