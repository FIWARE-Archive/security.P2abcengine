package ch.zhaw.ficore.p2abc.services;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

public class CSRFRespFilter implements ContainerResponseFilter {

    @Override
    public ContainerResponse filter(ContainerRequest arg0,
            ContainerResponse arg1) {
        List<Object> ls = new ArrayList<Object>();
        ls.add("csrf=test");
        arg1.getHttpHeaders().put(HttpHeaders.SET_COOKIE, ls);
        return arg1;
    }

}
