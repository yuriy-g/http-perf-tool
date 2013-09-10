package com.github.perf.http;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.github.perf.http.HttpClient;
import com.github.perf.http.HttpServer;

public class BaseHttpTest {

	public static final int PORT = 8888;
	protected static final String BASE_URL = "http://localhost:" + PORT;
	private static HttpServer server;
	protected HttpClient client;
	
	@BeforeClass
	public static void init() throws Exception {
		server = HttpServer.getServer(PORT);
		server.start();
	}

	@AfterClass
	public static void uninit() throws Exception {
		server.stop();
	}

	@Before
	public void start() throws Exception {
		client = HttpClient.getClient();
		client.start();
	}
	
	@After
	public void stop() throws Exception {
		client.stop();
	}
	
}

