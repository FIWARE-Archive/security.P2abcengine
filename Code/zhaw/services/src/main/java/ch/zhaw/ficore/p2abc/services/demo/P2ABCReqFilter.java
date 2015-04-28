package ch.zhaw.ficore.p2abc.services.demo;

import java.io.IOException;
import java.net.URLEncoder;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import ch.zhaw.ficore.p2abc.services.helpers.RESTHelper;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public class P2ABCReqFilter implements
	Filter {
	
	private static String callbackRegex = null;
	private static String redirectUrl = null;
	private static String resourceName = null;
	
	public P2ABCReqFilter() throws NamingException {
		Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:/comp/env");
        
        callbackRegex = (String) envCtx.lookup("cfg/filter/callbackRegex");
        redirectUrl = (String) envCtx.lookup("cfg/filter/redirectUrl");
        resourceName = (String) envCtx.lookup("cfg/filter/resourceName");
	}

	

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest req_, ServletResponse resp_,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) req_;
		HttpServletResponse resp = (HttpServletResponse) resp_;
		
		String path = req.getPathInfo();
		System.out.println("RealPath: " + path);
		
		if(path.matches(callbackRegex)) {
			String accessToken = req.getParameter("accesstoken");
			
			if(accessToken == null) {
				resp.sendError(403);
				return;
			}
			
			try {
				String result = (String) RESTHelper
				        .getRequest("http://localhost:8080/zhaw-p2abc-webservices/verification/verifyAccessToken?accesstoken="
				                + URLEncoder.encode(accessToken, "UTF-8"));
				
				System.out.println("Resource: " + result);
				
				if(result.equals(resourceName)) {
					resp.addHeader("X-P2ABC-VERIFIED", "TRUE");
				}
				else {
					resp.sendError(403);
					return;
				}
			} catch (ClientHandlerException | UniformInterfaceException
					| JAXBException | NamingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		chain.doFilter(req_, resp_);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
	
	
}
