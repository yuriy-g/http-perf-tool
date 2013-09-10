package com.github.perf.util;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlParser {

	private static Logger LOG = LoggerFactory.getLogger(HtmlParser.class);
	
	private HTMLEditorKit kit = new HTMLEditorKit();
	private HTMLDocument doc;
	
	public HtmlParser() {
		kit = new HTMLEditorKit();
	}
	
	public void parse(String content) {
		Reader reader = new StringReader(content);
		doc = (HTMLDocument) kit.createDefaultDocument();
		doc.putProperty("IgnoreCharsetDirective", new Boolean(true));
		try {
			kit.read(reader, doc, 0);
		} catch (Exception e) {
			LOG.warn("HTML parsing error", e);
		}
	}

	public Collection<String> getLinks() {
		Set<String> links = new HashSet<String>(); 
		getLinks(links, "link", "href");
		getLinks(links, "script", "src");
		getLinks(links, "img", "src");
		return links;
	}
	
	private void getLinks(Collection<String> links, String tagName, String targetAttr) {
		if (doc == null)
			return;
		HTMLDocument.Iterator it = doc.getIterator(HTML.getTag(tagName));
		while (it.isValid()) {
			 AttributeSet attrSet = it.getAttributes();
			 Enumeration<?> e = attrSet.getAttributeNames();
			 while (e.hasMoreElements()) {
				 Object name = e.nextElement();
				 if (name.toString().equalsIgnoreCase(targetAttr))
					 links.add((String) attrSet.getAttribute(name));
			 }
			 it.next();
		}
	}

	public static class Element {
	
		public static final Element NULL = new Element(null, null);
		
		private HtmlParser parser;
		private javax.swing.text.Element el;
		
		public Element(javax.swing.text.Element element, HtmlParser parser) {
			this.el = element;
			this.parser = parser;
		}

		public Element id(String id) {
			return el == null ? NULL : parser.id(el, id);
		}

		public Element name(String name) {
			return el == null ? NULL : parser.name(el, name);
		}

		public String text() {
			return el == null ? null : parser.text(el);
		}

		public String val() {
			return el == null ? null : HtmlParser.attr(el, HTML.Attribute.VALUE);
		}
		
		public boolean isNull() {
			return el == null;
		}
		
	}

	public Element root() {
		return new Element(doc.getDefaultRootElement(), this);
	}
	
	private static String attr(javax.swing.text.Element el, Object attr) {
		Object value = el != null ? el.getAttributes().getAttribute(attr) : null;
		return value != null ? value.toString() : null;
	}
	
	private Element el(javax.swing.text.Element el, Object attr, Object value) {
		return new Element(doc.getElement(el, attr, value), this);
	}

	private Element id(javax.swing.text.Element el, String id) {
		return el(el, HTML.Attribute.ID, id);
	}

	private Element name(javax.swing.text.Element el, String name) {
		return el(el, HTML.Attribute.NAME, name);
	}
	
	private String text(javax.swing.text.Element el) {
		try {
			return doc.getText(el.getStartOffset(), el.getEndOffset() - el.getStartOffset());
		} catch (BadLocationException e) {
			return null;
		}
	}
	
}
