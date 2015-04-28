package ch.zhaw.ficore.p2abc.services.demo;

import java.io.IOException;

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

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public class P2ABCReqFilter implements
	Filter {
	
	private static String pathRegex = null;
	private static String callbackRegex = null;
	private static String redirectUrl = null;
	
	public P2ABCReqFilter() throws NamingException {
		Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:/comp/env");
        
        pathRegex = (String) envCtx.lookup("cfg/filter/pathRegex");
        callbackRegex = (String) envCtx.lookup("cfg/filter/callbackRegex");
        redirectUrl = (String) envCtx.lookup("cfg/filter/redirectUrl");
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
			//Contact verifier (we don't do that yet)
			resp.addHeader("X-P2ABC-VERIFIED", "TRUE");
		}
		
		chain.doFilter(req_, resp_);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
	
	
}
