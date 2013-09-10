package com.github.perf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.perf.http.HttpClient;
import com.github.perf.util.HtmlParser;
import com.github.perf.util.Stat;

public class User implements Comparable<User> {

	private static final String AUTHENTICATED_FIELD = "authenticated";
	
	private Logger log;
	private Logger dumplog;

	private String name;
	private UserGroup group;
	private Vars vars = new Vars();
	// arguments is used for each call from a script
	// as all calls are not parallel for the same agent
	// the same 'args' field is used (less memory allocations)
	private Vars args = new Vars();
	
	private App app;
	private HttpClient client;
	private ScriptEnv scriptEnv;
	private HtmlParser parser = new HtmlParser();
	
	public User(App app, String name, UserGroup group) {
		this(name);
		this.app = app;
		this.group = group;
	}
	
	public User(String name) {
		this.name = name;
		vars.set("name", name);
		log = LoggerFactory.getLogger("user." + name);
		dumplog = LoggerFactory.getLogger("dump." + name);
	}
	
	private String getName() {
		return name;
	}

	Object get(String key) {
		return vars.get(key);
	}

	private boolean getBoolean(String key) {
		Boolean b = (Boolean) get(key);
		return b != null && b.booleanValue();
	}
	
	void set(String key, Object value) {
		vars.set(key, value);
	}
	
	Object getArg(String key) {
		return args.get(key);
	}

	private void setArg(String key, Object value) {
		args.set(key, value);
	}
	
	private void clearArgs() {
		args.clear();
	}
	
	private boolean checkClient() {
		if (client == null) {
			log.warn("Client is not initialized or not started");
			return false;
		} else
			return true;
	}

	public void preinit() {
		if (app.getConf().isActionStatEnabled())
			Stat.logAction(name, "PREINIT");
	}
	
	private void init() {
		if (app.getConf().isActionStatEnabled())
			Stat.logAction(name, "INIT");
		long start = System.currentTimeMillis();
		try {
			client = HttpClient.getClient(app.getConf().isContentCached());
			if (checkClient()) {
				client.start();
			}
		} catch (Exception e) {
			log.warn("Initialization error", e);
			client = null;
		}	
		if (app.getConf().isActionStatEnabled())
			Stat.logAction(name, "INIT", (System.currentTimeMillis() - start));
	}
	
	private void start() throws Exception {
		if (log.isInfoEnabled())
			log.info("Starting");
		if (app.getConf().isActionStatEnabled())
			Stat.logAction(name, "START");
		boolean isOk = false;
		try {
			// script env is needed earlier than script execution, because handlers could be executed on start
			scriptEnv = ScriptEnv.getScriptEnv();
			scriptEnv.set("userObj", this.new UserObject());
			try {
				scriptEnv.eval(ScriptEnv.getScript(app.getConf().getMappings()));
			} catch (Exception e) {
				log.warn("Script initializing error", e);
			}		
			String startAction = app.getConf().getEvents().get(App.ON_START_EVENT);
			executeActions(startAction);
			isOk = true;
		} finally {
			if (app.getConf().isActionStatEnabled())
				Stat.logAction(name, "START", isOk ? Stat.Result.OK : Stat.Result.NOK);
		}
	}

	private void stop() throws Exception {
		if (log.isInfoEnabled())
			log.info("Stopping");
		if (app.getConf().isActionStatEnabled())
			Stat.logAction(name, "STOP");
		boolean isOk = false;
		try {
			String stopAction = app.getConf().getEvents().get(App.ON_STOP_EVENT);
			if (getBoolean(AUTHENTICATED_FIELD))
				executeActions(stopAction);
			isOk = true;
		} finally {
			if (app.getConf().isActionStatEnabled())
				Stat.logAction(name, "STOP", isOk ? Stat.Result.OK : Stat.Result.NOK);
		}
	}

	private void uninit() {
		if (app.getConf().isActionStatEnabled())
			Stat.logAction(name, "UNINIT");
		long start = System.currentTimeMillis();
		if (checkClient())
			try {
				client.stop();
			} catch (Exception e) {
				log.warn("Unitialization error", e);
			}
		if (app.getConf().isActionStatEnabled())
			Stat.logAction(name, "UNINIT", (System.currentTimeMillis() - start));
	}
	
	public void execute() {
		init();
		long start = System.currentTimeMillis();
		if (app.getConf().isActionStatEnabled())
			Stat.logAction(name, "EXECUTE");
		try {
			start();
			try {
				executeScript();
			} finally {
				stop();
			}
		} catch (Exception e) {
			log.warn("Execution error", e);
		}
		if (app.getConf().isActionStatEnabled())
			Stat.logAction(name, "EXECUTE", (System.currentTimeMillis() - start));
		uninit();
	}
	
	private void executeScript() {
		boolean authenticated = getBoolean(AUTHENTICATED_FIELD);
		String script = group.getFunc();
		if (script == null && log.isInfoEnabled())
			log.info("No script to execute");
		if (!authenticated && log.isInfoEnabled())
			log.info("User not authenticated");
		if (script == null || !authenticated)
			return;
		if (log.isInfoEnabled())
			log.info("Executing script");
		try {
			Object func = scriptEnv.eval(script);
			scriptEnv.set("run_func", func);
			scriptEnv.eval("run_func()");
		} catch (Exception e) {
			log.warn("Script executing error", e);
		}
	}

	private void executeAction(final String actionName) throws Exception {
		if (checkClient()) {
			final AppConf conf = app.getConf();
			final Action action = conf.getMappings().get(actionName);
			if (action == null)
				return;
			
			if (log.isInfoEnabled())
				log.info("Executing action: " + actionName);
			if (conf.isActionStatEnabled())
				Stat.logAction(name, actionName);
			executeHandler(actionName, action, App.BEFORE_HANDLER);
			String uri = action.getUri();
			boolean isOk = false; 
			try {
				if (uri == null)
					postExecuteAction(actionName, action, null, null);
				else
					executeActionRequest(actionName, uri);
				isOk = true;
			} finally {
				if (!isOk && conf.isActionStatEnabled())
					Stat.logAction(name, actionName, Stat.Result.NOK);
			}
		}
	}

	private void executeActionRequest(final String actionName, final String uri) throws Exception {
		final AppConf conf = app.getConf();
		final Action action = conf.getMappings().get(actionName);
		final String url = conf.getUrl() + "/" + uri;
		String content = null;			
		final Map<String, Object> httpParams = action.getHttpParams(this);
		
		if (dumplog.isTraceEnabled())
			dumplog.trace("Requesting " + url + " " + action.getMethod() + " " + httpParams);
		if (conf.isHttpStatEnabled())
			Stat.logRequest(name, url, action.getMethod(), httpParams);
		final boolean[] result = { false };
		if (action.isAsync()) {
			client.request(url, action.getMethod(), httpParams, new HttpClient.HttpListener() {

				@Override
				public void onComplete(String content) {
					postExecuteAction(actionName, action, httpParams, content);
					if (conf.isHttpStatEnabled())
						Stat.logResponse(name, url, result[0] ? Stat.Result.OK : Stat.Result.NOK);
					if (conf.isActionStatEnabled())
						Stat.logAction(name, actionName, Stat.Result.NOK);
				}

				@Override
				public void onContent(String content) {
				}

				@Override
				public void onFailure(Throwable failure) {
					result[0] = false;
					log.warn(actionName + " action execution error", failure);
				}

				@Override
				public void onSuccess() {
					result[0] = true;
				}
					
			});
		}
		else {
			Stat.Result res = Stat.Result.NOK;
			try {
				content = client.request(url, action.getMethod(), httpParams);
				res = Stat.Result.OK;
			} finally {
				if (conf.isHttpStatEnabled())
					Stat.logResponse(name, url, res);
			}
			postExecuteAction(actionName, action, httpParams, content);
		}
	} 

	private void postExecuteAction(String actionName, Action action, Map<String, Object> httpParams, String content) {
		if (action.getUri() != null)
			if (dumplog.isTraceEnabled()) {
				dumplog.trace(actionName + " " + httpParams + "\n-----\n" + content + "\n-----");;
			}
		
		executeAfterActionHandler(actionName, action, content);
		
		if (content != null)
			retrieveLinks(action);
		if (app.getConf().isActionStatEnabled())
			Stat.logAction(name, actionName, Stat.Result.OK);
	}

	private void executeAfterActionHandler(String actionName, Action action, String content) {
		if (content != null) {
			parser.parse(content);
			scriptEnv.set("content", content);
			scriptEnv.set("$", parser.root());
			executeHandler(actionName, action, App.AFTER_HANDLER);
			scriptEnv.set("content", null);
			scriptEnv.set("$", null);
		}
	}

	private void retrieveLinks(Action action) {
		boolean doRetrieveLinks = app.getConf().doRetrieveLinks();
		Collection<String> links = parser.getLinks();
		Collection<String> links2 = action.getAdditionalLinks();
		if (links2 != null)
			links.addAll(links2);
		for (String link : links)
			if (doRetrieveLinks)
				retrieveLink(link);
	}

	private void retrieveLink(final String link) {
		final AppConf conf = app.getConf();
		String url = conf.getUrl() + "/" + link;
		if (app.getConf().isHttpStatEnabled())
			Stat.logRequest(name, url, HttpClient.HttpMethod.GET, null);
		final boolean[] result = { false };
		client.request(url, new HttpClient.HttpListener() {

			@Override
			public void onComplete(String content) {
				if (conf.isHttpStatEnabled())
					Stat.logResponse(name, link, result[0] ? Stat.Result.OK : Stat.Result.NOK);				
			}

			@Override
			public void onContent(String content) {
			}

			@Override
			public void onFailure(Throwable failure) {
				result[0] = false;
				log.warn(link + " retrieving error", failure);
			}

			@Override
			public void onSuccess() {
				result[0] = true;
			}
				
		});
	}

	private void executeActions(String actionNames) throws Exception {
		if (actionNames != null) {
			String[] actionNameArray = actionNames.split(",");
			for (String actionName : actionNameArray)
				executeAction(actionName);
		}
	}
	
	private Object executeHandler(String actionName, Action action, String eventName) {
		if (action == null || action.getHandlers() == null)
			return null;
		
		String handler = action.getHandlers().get(eventName);
		if (handler != null) {
			String actionEventName = actionName + "_" + eventName;
			if (log.isInfoEnabled())
				log.info("Executing handler: " + actionEventName);			
			try {
				String handlerName = "func_" + actionEventName;
				Object value = scriptEnv.eval(handlerName + "()");
				return value;
			} catch (Exception e) {
				log.warn(actionEventName + " handler execution error", e);
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("User [");
		for (Entry<String, Object> entry : vars.entrySet())
			sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
		return sb.append("]").toString();
	}
	
	private static int compareTo(String s1, String s2) {
		return s1 == null ? 
				(s2 == null ? 0 : -1) : 
				(s2 == null ? 1 : s1.compareTo(s2)); 
	}
	
	public int compareTo(User o) {
		if (o == null)
			return 1;
		int nameCmp = compareTo(getName().toString(), o.getName().toString());
		return nameCmp;
	}
	
	// restricted object for scripting and actions
	public class UserObject {
		
		public Object get(String key) {
			return User.this.get(key);
		}

		public boolean getBoolean(String key) {
			return User.this.getBoolean(key);
		}
		
		public void set(String key, Object value) {
			User.this.set(key, value);
		}

		public Set<Entry<String, Object>> entrySet() {
			return User.this.vars.entrySet();
		}
		
		public Object getArg(String key) {
			return User.this.getArg(key);
		}

		public void setArg(String key, Object value) {
			User.this.setArg(key, value);
		}

		public void clearArgs() {
			User.this.clearArgs();
		}
		
		public void executeAction(String actionName) throws Exception {
			User.this.executeAction(actionName);
			//User.this.clearArgs();
		}

		public HtmlParser getParser() {
			return User.this.parser;
		}
	}
	
	public class Vars {

		private Map<String, Object> values = new HashMap<String, Object>();

		public Object get(String key) {
			return values.get(key);
		}
		
		public void set(String key, Object value) {
			values.put(key, value);
		}

		public void clear() {
			values.clear();
		}

		public Set<Entry<String, Object>> entrySet() {
			return values.entrySet();
		}

	}
	
}
