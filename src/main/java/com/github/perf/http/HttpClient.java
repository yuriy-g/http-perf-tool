package com.github.perf.http;

import java.util.Map;

import com.github.perf.http.jetty.JettyHttpClient;

public abstract class HttpClient {

	protected HttpClient() {}
	
	public static enum HttpMethod {
		GET, POST, HEAD, PUT, OPTIONS, DELETE, TRACE, CONNECT, MOVE
	}
	
	public static HttpMethod parse(final String methodString) {
		if ("get".equalsIgnoreCase(methodString)) return HttpMethod.GET;
		if ("post".equalsIgnoreCase(methodString)) return HttpMethod.POST;
		if ("head".equalsIgnoreCase(methodString)) return HttpMethod.HEAD;
		if ("put".equalsIgnoreCase(methodString)) return HttpMethod.PUT;
		if ("options".equalsIgnoreCase(methodString)) return HttpMethod.OPTIONS;
		if ("delete".equalsIgnoreCase(methodString)) return HttpMethod.DELETE;
		if ("trace".equalsIgnoreCase(methodString)) return HttpMethod.TRACE;
		if ("connect".equalsIgnoreCase(methodString)) return HttpMethod.CONNECT;
		if ("move".equalsIgnoreCase(methodString)) return HttpMethod.MOVE;
		return HttpMethod.GET;
	}
	protected static org.eclipse.jetty.http.HttpMethod convert(final HttpMethod method) {
		switch (method) {
		case GET: return org.eclipse.jetty.http.HttpMethod.GET;
		case POST: return org.eclipse.jetty.http.HttpMethod.POST;
		case HEAD: return org.eclipse.jetty.http.HttpMethod.HEAD;
		case PUT: return org.eclipse.jetty.http.HttpMethod.PUT;
		case OPTIONS: return org.eclipse.jetty.http.HttpMethod.OPTIONS;
		case DELETE: return org.eclipse.jetty.http.HttpMethod.DELETE;
		case TRACE: return org.eclipse.jetty.http.HttpMethod.TRACE;
		case CONNECT: return org.eclipse.jetty.http.HttpMethod.CONNECT;
		case MOVE: return org.eclipse.jetty.http.HttpMethod.MOVE;
		default: return org.eclipse.jetty.http.HttpMethod.GET;
		}
	}

	public static interface HttpListener {
		void onComplete(String content);
		void onContent(String content);
		void onFailure(Throwable failure);
		void onSuccess();
	}
	
	public abstract HttpClient start() throws Exception;
	public abstract void stop() throws Exception;
	public abstract String request(final String url, final HttpMethod method, final Map<String, Object> params) throws Exception;
	public abstract void request(final String url, final HttpMethod method, final Map<String, Object> params, HttpListener listener);

	public String request(String url) throws Exception {
		return request(url, HttpMethod.GET, null);
	}
	
	public void request(String url, HttpListener listener) {
		request(url, HttpMethod.GET, null, listener);
	}

	public static HttpClient getClient() {
		return new JettyHttpClient(false);
	}

	public static HttpClient getClient(boolean isContentCached) {
		return new JettyHttpClient(isContentCached);
	}

}
