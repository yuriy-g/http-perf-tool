package com.github.perf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.github.perf.http.HttpClient.HttpMethod;

public class Action {

	private String uri;
	private HttpMethod method = HttpMethod.GET;
	private boolean isAsync = false;
	private Map<String, Param> params = new HashMap<String, Param>(3);
	private Map<String, String> handlers;
	private Collection<String> additionalLinks;
	
	public Action() {}
	
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	public boolean isAsync() {
		return isAsync;
	}

	public void setAsync(boolean isAsync) {
		this.isAsync = isAsync;
	}

	public Param getParam(String key) {
		return params.get(key);
	}
	
	public void setParam(String key, String paramString) {
		params.put(key, createParam(paramString));
	}
	
	private Param createParam(String paramString) {
		if (paramString == null)
			return null;
		return new Param(paramString);
	}

	public Map<String, Param> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		if (params != null) {
			for (Map.Entry<String, String> paramEntry : params.entrySet())
				setParam(paramEntry.getKey(), paramEntry.getValue());
		}
	}

	public enum ParamType { UNDEFINED, STRING, USER, ARG };
	
	public class Param {
		
		private ParamType type;
		private String value;
		
		private Param(String param) {
			String[] paramArray = param.split("\\.");
			if (paramArray.length == 1) {
				type = ParamType.STRING;
				value = paramArray[0];
			} else {
				type = parse(paramArray[0]);
				value = paramArray[1];
				if (ParamType.UNDEFINED.equals(type))
					value = param;
			}
		}

		private ParamType parse(String string) {
			if ("user".equalsIgnoreCase(string))
				return ParamType.USER;
			else if ("arg".equalsIgnoreCase(string))
				return ParamType.ARG;
			else return ParamType.UNDEFINED;
		}

		public ParamType getType() {
			return type;
		}

		public String getValue() {
			return value;
		}
		
		public Object eval(User user) {
			switch (type) {
			case STRING: return value;
			case USER: return user.get(value);
			case ARG: return user.getArg(value);
			default: return null;
			}
		}
		
	}

	public Map<String, Object> getHttpParams(User user) {
		if (params.size() > 0) {
			Map<String, Object> result = new HashMap<String, Object>();
			for (Map.Entry<String, Param> paramEntry : params.entrySet()) {
				Object value = paramEntry.getValue().eval(user);
				if (value != null)
					result.put(paramEntry.getKey(), value);
			}
			return result;
		}
		else
			return null;
	}

	public Map<String, String> getHandlers() {
		return handlers;
	}

	public void setHandlers(Map<String, String> handlers) {
		this.handlers = handlers;
	}

	public Collection<String> getAdditionalLinks() {
		return additionalLinks;
	}

	public void setAdditionalLinks(Collection<String> additionalLinks) {
		this.additionalLinks = additionalLinks;
	}

}
