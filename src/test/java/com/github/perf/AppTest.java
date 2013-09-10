package com.github.perf;

import java.io.StringReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.github.perf.App;
import com.github.perf.http.BaseHttpTest;


public class AppTest extends BaseHttpTest {

	public static final int PORT = 8888;
	
	static {
		Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.INFO);
	}

	private static App app;
	
	@BeforeClass
	public static void init() throws Exception {
		BaseHttpTest.init();		
		app = new App(AppConfTest.getConfFiles());
	}

	static StringReader initScript() {
		return new StringReader("setState({ state: 'state1' });");
	}

	
	@AfterClass
	public static void uninit() throws Exception {
		BaseHttpTest.uninit();
	}
	
	@Test
	public void testExecute() {
		app.execute();
	}
	
}
