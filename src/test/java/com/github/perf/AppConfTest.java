package com.github.perf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Reader;
import java.util.Map;

import org.junit.Test;

import com.github.perf.Action;
import com.github.perf.App;
import com.github.perf.AppConf;
import com.github.perf.UserGroup;
import com.github.perf.http.HttpClient;

public class AppConfTest {

	static Reader[] getConfFiles() {
		return App.getConfFiles(new String[] { "conf/junit.conf.js", "conf/junit.run.js" });
	}
	
	@Test
	public void testConf() {
		AppConf conf = AppConf.initConf(getConfFiles());

		Map<String, String> events = conf.getEvents();
		Map<String, Action> mappings = conf.getMappings();

		assertTrue(!conf.isStatEnabled());
		assertTrue(!conf.isLocalStatEnabled());
		Map<String, Object> hostMap = conf.getRemoteStatHost();
		assertEquals("localhost", hostMap.get("host"));
		assertEquals(9999.0d, hostMap.get("port"));
		assertEquals("http://localhost:8888/test", conf.getUrl());
		assertTrue(!conf.isHttpStatEnabled());
		assertTrue(!conf.isActionStatEnabled());
		
		Map<String, String> params = conf.getMandatoryHttpParams();
		assertNotNull(params);
		assertTrue(params.containsKey("sessionid"));
		assertEquals("user.sessionid", params.get("sessionid"));
		for (UserGroup group : conf.getUserGroups())
			for (Map<String, Object> user : group.getUsers()) {
				if ("tes".equals(user.get("name"))) {
					assertEquals("fail", user.get("password"));
					assertEquals("ru", user.get("language"));
				}
				if ("test".equals(user.get("name"))) {
					assertEquals("password", user.get("password"));
					assertEquals("en", user.get("language"));
				}
				if ("test_00".equals(user.get("name"))) {
					assertEquals("pass_00", user.get("password"));
					assertEquals("en", user.get("language"));
					assertEquals(1.0, user.get("first"));
					assertEquals(3.0, user.get("last"));
					assertEquals(true, user.get("pattern"));
				}
			}

		assertEquals("login", events.get("onStart"));
		assertEquals("logout", events.get("onStop"));
		
		Action action;
		Action.Param param;

		action = mappings.get("login");
		assertEquals("login", action.getUri());
		assertEquals(HttpClient.HttpMethod.POST, action.getMethod());
		param = action.getParam("username");
		assertNotNull(param);
		assertEquals(Action.ParamType.USER, param.getType());
		assertEquals("name", param.getValue());
		param = action.getParam("password");
		assertNotNull(param);
		assertEquals(Action.ParamType.USER, param.getType());
		assertEquals("password", param.getValue());
		param = action.getParam("dummy");
		assertNotNull(param);
		assertEquals(Action.ParamType.STRING, param.getType());
		assertEquals("empty", param.getValue());
		assertNotNull(action.getHandlers().get(App.AFTER_HANDLER));
		
		action = mappings.get("logout");
		assertEquals("logout", action.getUri());
		assertEquals(HttpClient.HttpMethod.GET, action.getMethod());
		assertEquals(1, action.getParams().size());
		assertNotNull(action.getHandlers().get(App.AFTER_HANDLER));

		action = mappings.get("setState");
		assertEquals("state", action.getUri());
		assertEquals(HttpClient.HttpMethod.GET, action.getMethod());
		param = action.getParam("state");
		assertNotNull(param);
		assertEquals(Action.ParamType.ARG, param.getType());
		assertEquals("state", param.getValue());
		assertNotNull(action.getHandlers().get(App.AFTER_HANDLER));

	}
	
}
