package com.github.perf;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.perf.ScriptEnv;

public class ScriptEnvTest {

	private static ScriptEnv env;
	
	@BeforeClass
	public static void setup() {
		env = ScriptEnv.getScriptEnv();
	}
	
	@Test
	public void testScriptEnvs() throws Exception {
		env.set("a", 3);
		env.eval("a = a + 4");
		assertEquals(7.0, env.get("a"));
		ScriptEnv env2 = ScriptEnv.getScriptEnv();
		env2.set("a", 4);
		env2.eval("a = a + 5");
		assertEquals(9.0, env2.get("a"));
		assertEquals(7.0, env.get("a"));
	}
	
	@Test
	public void testScriptMap() throws Exception {
		env.eval("map = { a: 3 }");
		assertEquals(3.0, env.eval("map['a']"));
	}

	public class JQueryLike {
		public String id(String id) { return id; }
	}
	
	@Test
	public void testJQueryLike() throws Exception {
		env.set("jq", new JQueryLike());
		env.eval("function $(id) { return jq.id(id) }");
		assertEquals("a",  env.eval("$('a')"));
	}
	
}
