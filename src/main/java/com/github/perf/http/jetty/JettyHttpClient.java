package com.github.perf.http.jetty;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.perf.http.HttpClient;

public class JettyHttpClient extends HttpClient {

	static final Logger LOG = LoggerFactory.getLogger(JettyHttpClient.class);
	
	private org.eclipse.jetty.client.HttpClient httpClient;
	
	private boolean isContentCached;
	private Cache cache;
	
	// no waiting in JettyClient until all requests completed
	// therefore implementing our sync
	private static final int MAX_PERMITS = 100;
	private Semaphore semaphore = new Semaphore(MAX_PERMITS);
	private ReentrantLock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	public JettyHttpClient(boolean isContentCached) {
		this.isContentCached = isContentCached;
		cache = new Cache(isContentCached);
		httpClient = new org.eclipse.jetty.client.HttpClient();
		httpClient.setFollowRedirects(false);
	}
	
	public JettyHttpClient start() throws Exception {
		httpClient.start();
		return this;
	}
	
	public void stop() throws Exception {
		if (semaphore.availablePermits() < MAX_PERMITS) {
			lock.lock();
			try {
				condition.await();
			} finally {
				lock.unlock();
			}
		}
		httpClient.stop();
	}
	
	// here asynchronous call forced to be synchronous
	// just for fun
	public String request(final String url, final HttpMethod method, final Map<String, Object> params) throws Exception {
		if (LOG.isDebugEnabled())
			LOG.debug("Requesting " + url + ", Method: " + method + ", Params: " + params);
		Request request = httpClient.
				newRequest(url).
		        method(convert(method));
		if (isContentCached)
			cache.handleRequest(url, request);
		boolean isGet = HttpMethod.GET.equals(method);
		if (params != null)
			if (isGet)
				for (Map.Entry<String, Object> entry : params.entrySet()) {
					Object value = entry.getValue();
					request.param(entry.getKey(), value != null ? value.toString() : "");
				}
			else {
				StringBuilder paramString = new StringBuilder();
				for (Map.Entry<String, Object> entry : params.entrySet()) {
					Object value = entry.getValue();
					paramString.append(entry.getKey()).append("=").append(value != null ? value.toString() : "").append("&");
				}
				request.content(new StringContentProvider(paramString.toString()), "application/x-www-form-urlencoded");
			}
		ContentResponse response = request.send();
		if (isContentCached)
			cache.handleResponse(url, response);
		return new String(response.getContent());
	}
	
	public void request(final String url, final HttpMethod method, final Map<String, Object> params, final HttpListener listener) {
		if (LOG.isDebugEnabled())
			LOG.debug("Requesting " + url + ", Method: " + method + ", Params: " + params);		
		Request request = httpClient.
				newRequest(url).
				method(convert(method));
		if (isContentCached)
			cache.handleRequest(url, request);
		if (params != null)
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				Object value = entry.getValue();
				request.param(entry.getKey(), value != null ? value.toString() : "");
			}
		final StringBuilder content = new StringBuilder();
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
		}
		request.
			send(new Response.Listener.Empty() {
				
				@Override
				public void onComplete(Result result) {
					if (isContentCached)
						cache.handleResponse(url, result.getResponse());
					if (listener != null)
						listener.onComplete(content.toString());
					lock.lock();
					try {
						semaphore.release();
						if (semaphore.availablePermits() == MAX_PERMITS)
							condition.signal();
					} finally {
						lock.unlock();
					}
				}
				
				@Override
	            public void onContent(Response response, ByteBuffer buffer) {
					StringBuilder sb = new StringBuilder();
					try {
		            	int bsize = 1024;
		            	byte[] dst = new byte[bsize];
		            	int size = bsize;
		            	while (size == bsize) {
		            		int remaining = buffer.remaining();
		            		if (remaining == 0)
		            			break;
		            		if (remaining < size)
		            			size = remaining;
		            		buffer.get(dst, 0, size);
		            		sb.append(new String(dst, 0, size));
		            	}
					} finally {
						if (listener != null)
							listener.onContent(sb.toString());
						content.append(sb);
					}
	            }

				@Override
				public void onFailure(Response response, Throwable failure) {
					if (listener != null)
						listener.onFailure(failure);
				}
				
				@Override
				public void onSuccess(Response response) {
					if (listener != null)
						listener.onSuccess();
				}
				
			});
	}

	private class Cache {

		private class Entry {
			
			private String eTag;
			private String lastModified;
			
			public Entry(String eTag, String lastModified) {
				super();
				this.eTag = eTag;
				this.lastModified = lastModified;
			}

			@Override
			public String toString() {
				return "Entry [ETag=" + eTag
						+ ", Last-Modified=" + lastModified + "]";
			}

		}
		
		private boolean isContentCached;
		private Map<String, Entry> entries; 
		
		private Cache(boolean isContentCached) {
			this.isContentCached = isContentCached;
			if (isContentCached)
				 entries = new HashMap<String, Entry>();
		}
		
		private void handleResponse(String url, Response response) {
			if (isContentCached) {
				HttpFields fields = response.getHeaders();
				String eTag = fields.get("ETag");
				String lastModified = fields.get("Last-Modified");
				if (eTag != null || lastModified != null)
					entries.put(url, new Entry(eTag, lastModified));
			}
		}

		private void handleRequest(String url, Request request) {
			if (isContentCached) {
				HttpFields fields = request.getHeaders();
				Entry entry = entries.get(url);
				if (entry != null) {
					if (entry.eTag != null)
						fields.add("If-None-Match", entry.eTag);
					if (entry.lastModified != null)
						fields.add("If-Modified-Since", entry.lastModified);
				}
			}
		}

	}
	
}
