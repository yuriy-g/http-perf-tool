package com.github.perf.util;

import org.junit.Test;

import com.github.perf.util.Stat;

public class StatTest {

	@Test
	public void testLocal() throws Exception {
		Stat cm = Stat.getLocalStat();
		Thread.sleep(1000);
		System.out.println(cm.getValuesString());
	}
	
	@Test
	public void testRemote() throws Exception {
		Stat cm = Stat.getRemoteStat("remote", "localhost", 9999);
		for (int i = 0; i < 1; i++) {
			Thread.sleep(1000);
			System.out.println(cm.getValuesString());
		}
	}
	
}
