package com.github.perf.http.jetty;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@SuppressWarnings("serial")
public class JettyHttpServlet extends HttpServlet {

	public JettyHttpServlet() {
	}

	private void defaultHandler(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		if (request.getMethod().equals("POST"))
			response.getWriter().print(request.getParameter("param"));
		else
			response.getWriter().print("Test");
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		if (!dispatch(request.getRequestURI(), request, response))
			defaultHandler(request, response);			
	}


	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		if (!dispatch(request.getRequestURI(), request, response))
			defaultHandler(request, response);
	}

	private boolean dispatch(String requestURI, 
			HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		if (requestURI.indexOf('.') != -1) {
			response.getWriter().print("content");
			return true;
		}
		if (requestURI.indexOf("/echo") != -1) {
			defaultHandler(request, response);
			return true;
		} else if (requestURI.indexOf("/login") != -1) {
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			if (authenticate(username, password)) {
				request.getSession(true);
				response.getWriter().print("Session created");
			} else {
				response.getWriter().print("Wrong user name or password");
			}
			return true;
		} else {
			HttpSession session = request.getSession(false);
			if (session == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return true;
			} else {
				if (requestURI.indexOf("/state") != -1) {
					String state = request.getParameter("state");
					response.getWriter().print(
							"<html><head>" +
							"<link rel='shortcut icon' href='favicon.ico' />" +
							"<link rel='stylesheet' type='text/css' href='c.css'>" +
							"<script type='text/javascript' src='a.js'></script>" +
							"<script type='text/javascript' src='b.js'></script>" +
							"</head><body>" +
							"<div id='divId' name='divName'><span id='spanId' name='spanName'>content</span></div>" +
							"<form name='form1'><input name='inp1' value='val1'></form>" +
							"<script type='text/javascript' src='b.js'></script>" +
							"<div id='state'>User state = " + state + "</div><img src='a.gif' ><img src='b.gif' >" +
							"</body>"
					);
					return true;
				} else if (requestURI.indexOf("/logout") != -1) {
					session.invalidate();
					response.getWriter().print("Session invalidated");
					return true;
				}
			}
		}
		return false;
	}

	private boolean authenticate(String username, String password) {
		return username != null && username.startsWith("test") &&
				password != null && password.startsWith("pass");
	}
	
}
