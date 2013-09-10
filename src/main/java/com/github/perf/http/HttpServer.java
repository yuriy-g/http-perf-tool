package com.github.perf.http;

import com.github.perf.http.jetty.JettyHttpHandler;
import com.github.perf.http.jetty.JettyHttpServer;

public abstract class HttpServer {

	protected HttpServer() {}
	
	public abstract void start() throws Exception;
	public abstract void stop() throws Exception;
	
	public static HttpServer getServer(int port) {
		return new JettyHttpServer(port, new JettyHttpHandler());
	}
	
}
