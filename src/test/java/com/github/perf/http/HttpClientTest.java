package com.github.perf.http;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.github.perf.http.HttpClient;

public class HttpClientTest extends BaseHttpTest {


	@Test
	public void testGet() throws Throwable {
		assertEquals("Test", client.request(BASE_URL + "/test/echo"));
	}
	
	@Test
	public void testPost() throws Throwable {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("param", "value");
		assertEquals("value", client.request(BASE_URL + "/test/echo", HttpClient.HttpMethod.POST, params));
	}
	
	@Test
	public void testAsync() throws Throwable {
		final CountDownLatch latch = new CountDownLatch(1);
		final String[] s = new String[1]; // simple way to override final restriction of inner class is to pass an array
		client.request(BASE_URL + "/test/echo", new HttpClient.HttpListener() {

			public void onComplete(String content) {
				System.out.println("onComplete");
				latch.countDown();
			}

			public void onContent(String content) {
				System.out.println("onContent " + content);
				s[0] = content;
			}

			public void onFailure(Throwable failure) {
				System.out.println("onFailure " + failure);
			}

			public void onSuccess() {
				System.out.println("onSuccess");
			}
			
		});
		latch.await();
		assertEquals("Test", s[0]);
	}

}

