package com.github.perf.http.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.perf.http.HttpServer;

public class JettyHttpServer extends HttpServer {

	static final Logger LOG = LoggerFactory.getLogger(JettyHttpServer.class);
	
	private Server server;
	
	public JettyHttpServer(int port, Handler handler) {
		server = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/test");
        server.setHandler(context);
 
        context.addServlet(new ServletHolder(new JettyHttpServlet()), "/*");
	}
	
	public void start() throws Exception {
		if (LOG.isInfoEnabled())
			LOG.info("Starting JettyHttpServer");
		server.start();
	}
	
	public void stop() throws Exception {
		if (LOG.isInfoEnabled())
			LOG.info("Stopping JettyHttpServer");
		server.stop();
	}
	
	public static void main(String[] args) throws Exception {
		JettyHttpServer server = new JettyHttpServer(8888, null);
		server.start();
	}
	
}
