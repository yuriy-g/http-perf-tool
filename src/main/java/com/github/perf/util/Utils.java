package com.github.perf.util;

import java.io.IOException;
import java.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	private static Logger LOG = LoggerFactory.getLogger(Utils.class);
	
	public static String getReaderString(Reader reader) {
		if (reader == null)
			return "";
		
		StringBuilder sb = new StringBuilder();
	    appendReaderStrings(reader, sb);			
	    return sb.toString();
	}

	public static void appendReaderStrings(Reader reader, StringBuilder sb) {
		try {
	    	char[] buffer = new char[1024];
	    	int numChars;
	    	
	    	while ((numChars = reader.read(buffer, 0, buffer.length)) > 0) {
	    	      sb.append(buffer, 0, numChars);
	    	}
	    	sb.append("\n");
	    } catch (IOException e) {
			LOG.warn("Reading error" + e);
		}
	}

}
