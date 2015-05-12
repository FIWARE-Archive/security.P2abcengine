package ch.zhaw.ficore.p2abc.filters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;

import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public class PrivacyReqFilter implements ContainerRequestFilter {

	private String callbackRegex = null;
	private String resourceName = null;
	private String verifierURL = null;
	private String pathRegex = null;
	private List<String> tokens = new ArrayList<String>();
	private static final int MAX_TOKENS = 4096;

	@Override
	public ContainerRequest filter(ContainerRequest req) {
		String path = req.getPath(false);

		if (path.matches(callbackRegex)) {
			/* this is the callback */
			List<String> temp = req.getQueryParameters().get("accesstoken");

			if (temp == null || temp.size() < 1) {
				return denyRequest(req);
			}

			String accesstoken = (String) req.getQueryParameters()
			        .get("accesstoken").get(0);
			if (accesstoken == null) {
				return denyRequest(req);
			}

			/* Ok. Let's ask the verifier about this */
			try {
				String url = verifierURL + "/verifyAccessToken?accesstoken="
				        + URLEncoder.encode(accesstoken, "UTF-8");
				String result = (String) RESTHelper.getRequestUnauth(url);

				if (result.equals(resourceName)) {

					synchronized (tokens) {
						if (tokens.size() > MAX_TOKENS) {
							tokens.remove(0);
						}
						tokens.add(accesstoken);

						MultivaluedMap<String, String> headers = req
						        .getRequestHeaders();
						headers.add("X-P2ABC-ACCESSTOKEN", accesstoken);
						req.setHeaders((InBoundHeaders) headers);
					}

					return req;
				} else {
					return denyRequest(req);
				}

			} catch (Exception e) {
				e.printStackTrace();
				return denyRequest(req);
			}
		} else if (path.matches(pathRegex)) {
			Cookie cookie = req.getCookies().get("x-p2abc-accesstoken");
			if (cookie == null)
				return denyRequest(req);
			String token = cookie.getValue();
			if (!tokens.contains(token))
				return denyRequest(req);
		}

		return req;
	}

	public ContainerRequest denyRequest(ContainerRequest req) {
		throw new WebApplicationException(Response
		        .status(Response.Status.FORBIDDEN).entity("Request denied!")
		        .build());
	}

	public PrivacyReqFilter() throws NamingException {
		Context initCtx = new InitialContext();
		Context envCtx = (Context) initCtx.lookup("java:/comp/env");

		callbackRegex = (String) envCtx
		        .lookup("cfg/p2abc-filter/callbackRegex");
		pathRegex = (String) envCtx.lookup("cfg/p2abc-filter/pathRegex");
		resourceName = (String) envCtx.lookup("cfg/p2abc-filter/resourceName");
		verifierURL = (String) envCtx.lookup("cfg/p2abc-filter/verifierURL");
	}

}
