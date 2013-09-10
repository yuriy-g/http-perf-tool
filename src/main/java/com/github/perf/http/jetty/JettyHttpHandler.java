package com.github.perf.http.jetty;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class JettyHttpHandler extends AbstractHandler {
	
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		if (!dispatch(request.getRequestURI(), request, response)) {
			if (request.getMethod().equals("POST"))
				response.getWriter().print(request.getParameter("param"));
			else
				response.getWriter().print("Test");
		}
	}

	private boolean dispatch(String requestURI, 
			HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		if ("/login".equals(requestURI)) {
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			if ("test".equals(username) && "pass".equals(password)) {
				response.getWriter().print("Session created " + request.getSession(true).getId());
			} else {
				response.getWriter().print("Wrong user name or password");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			}
			return true;
		}
		return false;
	}
	
}
