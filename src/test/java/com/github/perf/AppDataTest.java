package com.github.perf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.perf.App;
import com.github.perf.User;

public class AppDataTest {

	private static App app;
	
	@BeforeClass
	public static void setup() throws Exception {
		app = new App(AppConfTest.getConfFiles());
	}
	
	@Test
	public void testUsers() {
		List<User> users = app.getUsers();
		assertEquals(5, users.size());
		for (User user : users)
			System.out.println(user);
		// don't forget binary search works only for sorted collections
		assertTrue(Collections.binarySearch(users, new User("tes")) > -1);
		assertTrue(Collections.binarySearch(users, new User("test")) > -1);		
		assertTrue(Collections.binarySearch(users, new User("test_01")) > -1);
		assertTrue(Collections.binarySearch(users, new User("test_02")) > -1);
		assertTrue(Collections.binarySearch(users, new User("test_03")) > -1);
	}
	
}
