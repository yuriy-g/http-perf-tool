package com.github.perf.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.github.perf.util.HtmlParser;

public class HtmlParserTest {

	private static String HTML1 = 
			"<html><head></head><body>" +
			"<div id='divA'>Div A</div>" +
			"</body>";

	private static String HTML2 = 
			"" +
			"<form name='form1'><input name='inp1' value='val1'></form>" +
			"";

	private HtmlParser parser = new HtmlParser();
	
	@Before
	public void start() {
		parser.parse(HTML1);
	}
	
	@Test
	public void testTwoParses() {
		HtmlParser.Element divA = parser.root().id("divA");
		assertTrue(!divA.isNull());
		assertEquals("Div A", divA.text().trim());
		
		parser.parse(HTML2);
		HtmlParser.Element form1 = parser.root().name("form1");
		assertTrue(!form1.isNull());
		HtmlParser.Element inp1 = form1.name("inp1");
		assertTrue(!inp1.isNull());
		assertEquals("val1" , inp1.val());
		
		inp1 = parser.root().name("inp1");
		assertTrue(!inp1.isNull());
		assertEquals("val1" , inp1.val());
	}
	
}
