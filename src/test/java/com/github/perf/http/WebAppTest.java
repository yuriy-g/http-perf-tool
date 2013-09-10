package com.github.perf.http;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.client.HttpResponseException;
import org.junit.Test;

import com.github.perf.http.HttpClient;

public class WebAppTest extends BaseHttpTest {

	@Test
	public void testSuccessfulLogin() throws Throwable {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("username", "test");
		params.put("password", "pass");
		assertEquals("Session created", client.request(BASE_URL + "/test/login", HttpClient.HttpMethod.POST, params));
		assertTrue(client.request(BASE_URL + "/test/state?state=1").indexOf("User state = 1") != -1);
		assertTrue(client.request(BASE_URL + "/test/state?state=2").indexOf("User state = 2") != -1);
		assertEquals("Session invalidated", client.request(BASE_URL + "/test/logout"));
	}
		
	@Test
	public void testFailedLogin() throws Throwable {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("username", "test3");
		params.put("password", "fpass3");
		assertEquals("Wrong user name or password", client.request(BASE_URL + "/test/login", HttpClient.HttpMethod.POST, params));
		try {
			client.request(BASE_URL + "/test/state");
		} catch (Exception e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof HttpResponseException);
			assertTrue(e.getCause().getMessage().startsWith("HTTP protocol violation: Authentication"));
		}
	}

}

