package com.github.perf;

import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.github.perf.http.HttpClient;
import com.github.perf.util.Stat;
import com.github.perf.util.Utils;

public class AppConf {

	private static final Logger LOG = (Logger) LoggerFactory
			.getLogger(AppConf.class);

	private Map<String, Object> stat = new HashMap<String, Object>(0);
	private Map<String, Object> connection = new HashMap<String, Object>(0);
	private Map<String, String> events = new HashMap<String, String>(0);
	private Map<String, Action> mappings = new HashMap<String, Action>(0);
	private UserGroup[] userGroups = new UserGroup[0];

	public void setStat(Map<String, Object> stat) {
		this.stat = stat;
	}

	public boolean isStatEnabled() {
		Boolean bool = stat == null ? null : (Boolean) stat.get("enabled");
		return bool == null ? false : bool.booleanValue();
	}

	public boolean isLocalStatEnabled() {
		Boolean bool = stat == null ? null : (Boolean) stat.get("local");
		return bool == null ? false : isStatEnabled() && bool.booleanValue();
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getRemoteStatHost() {
		return stat == null ? null : (Map<String, Object>) stat.get("remote");
	}

	public boolean isHttpStatEnabled() {
		Boolean bool = stat == null ? null : (Boolean) stat.get("http");
		return bool == null ? false : isStatEnabled() && bool.booleanValue();
	}

	public boolean isActionStatEnabled() {
		Boolean bool = stat == null ? null : (Boolean) stat.get("actions");
		return bool == null ? false : isStatEnabled() && bool.booleanValue();
	}

	public boolean doStatPlotting() {
		Boolean bool = stat == null ? null : (Boolean) stat.get("plot");
		return bool == null ? false : isStatEnabled() && bool.booleanValue();
	}
	
	public void setConnection(Map<String, Object> connection) {
		this.connection = connection;
	}

	public String getUrl() {
		return connection == null ? "" : (String) connection.get("url");
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getMandatoryHttpParams() {
		return connection == null ? null : (Map<String, String>) connection
				.get("mandatoryHttpParams");
	}

	public boolean doRetrieveLinks() {
		Object b = connection == null ? false : connection.get("retrieveLinks");
		return b == null ? false : (Boolean) b;
	}

	public boolean isContentCached() {
		Object b = connection == null ? false : connection.get("contentCached");
		return b == null ? false : (Boolean) b;
	}

	public Map<String, String> getEvents() {
		return events;
	}

	public void setEvents(Map<String, String> events) {
		this.events = events;
	}

	public Map<String, Action> getMappings() {
		return mappings;
	}

	@SuppressWarnings("unchecked")
	public void setMappings(Map<String, Map<String, Object>> mappings) {
		this.mappings = new HashMap<String, Action>();
		if (mappings == null)
			return;

		for (Map.Entry<String, Map<String, Object>> mappingEntry : mappings
				.entrySet()) {
			Action action = new Action();
			this.mappings.put(mappingEntry.getKey(), action);
			Map<String, Object> actionMap = mappingEntry.getValue();
			for (Map.Entry<String, Object> actionEntry : actionMap.entrySet()) {
				String actionKey = actionEntry.getKey();
				Object actionValue = actionEntry.getValue();
				if ("uri".equalsIgnoreCase(actionKey))
					action.setUri((String) actionValue);
				else if ("method".equalsIgnoreCase(actionKey))
					action.setMethod(HttpClient.parse((String) actionValue));
				else if ("params".equalsIgnoreCase(actionKey))
					action.setParams((Map<String, String>) actionValue);
				else if ("handlers".equalsIgnoreCase(actionKey))
					action.setHandlers((Map<String, String>) actionValue);
				else if ("additionalLinks".equalsIgnoreCase(actionKey))
					action.setAdditionalLinks((Collection<String>) actionValue);
				else if ("async".equalsIgnoreCase(actionKey))
					action.setAsync((Boolean) actionValue);
			}
			Map<String, String> connParams = getMandatoryHttpParams();
			if (connParams != null)
				for (Map.Entry<String, String> paramEntry : connParams
						.entrySet())
					action.setParam(paramEntry.getKey(), paramEntry.getValue());
		}
	}

	public UserGroup[] getUserGroups() {
		return userGroups;
	}

	@SuppressWarnings("unchecked")
	public void setUserGroups(List<Map<String, Object>> userGroups) {
		if (userGroups != null) {
			int size = userGroups.size();
			this.userGroups = new UserGroup[size];
			for (int i = 0; i < size; i++) {
				UserGroup group = new UserGroup();
				this.userGroups[i] = group;
				for (Map.Entry<String, Object> groupEntry : userGroups.get(i)
						.entrySet()) {
					String key = groupEntry.getKey();
					Object value = groupEntry.getValue();
					if ("users".equalsIgnoreCase(key))
						group.setUsers((List<Map<String, Object>>) value);
					else if ("run".equalsIgnoreCase(key))
						group.setFunc((String) value);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static AppConf initConf(Reader[] confReaders) {
		long start = System.currentTimeMillis();
		AppConf conf = new AppConf();

		if (confReaders == null)
			return conf;

		StringBuilder script = new StringBuilder(1024);
		for (Reader reader : confReaders)
			Utils.appendReaderStrings(reader, script);
		script.append(injectConf());

		ScriptEnv confEnv = ScriptEnv.getScriptEnv();
		try {
			confEnv.eval(script.toString());
		} catch (Exception e) {
			LOG.warn("Conf script error", e);
			return conf;
		}

		conf.setStat((Map<String, Object>) confEnv.get("statMap"));
		conf.setConnection((Map<String, Object>) confEnv.get("connectionMap"));
		conf.setUserGroups((List<Map<String, Object>>) confEnv.get("groupList"));
		conf.setEvents((Map<String, String>) confEnv.get("eventMap"));
		conf.setMappings((Map<String, Map<String, Object>>) confEnv
				.get("mappingMap"));

		if (conf.isActionStatEnabled())
			Stat.logAction(Stat.SYSTEM, "CONF_INIT", (System.currentTimeMillis() - start));

		return conf;
	}

	private static String injectConf() {
		return "\n\n\n" + "statMap = new java.util.HashMap()\n"
				+ "if (typeof(stat) != 'undefined')\n"
				+ "	for (var key in stat) {\n" + "		if (key === 'remote') {\n"
				+ "			hostMap = new java.util.HashMap()\n"
				+ "			statMap.put(key, hostMap)\n"
				+ "			for (var hostKey in stat[key])\n"
				+ "				hostMap.put(hostKey, stat[key][hostKey])\n"
				+ "		} else\n" + "			statMap.put(key, stat[key])\n"
				+ "	}\n"
				+ "connectionMap = new java.util.HashMap()\n"
				+ "for (var key in connection) {\n"
				+ "	if (key === 'mandatoryHttpParams') {\n"
				+ "		paramMap = new java.util.HashMap()\n"
				+ "		connectionMap.put(key, paramMap)\n"
				+ "		for (var paramKey in connection[key])\n"
				+ "			paramMap.put(paramKey, connection[key][paramKey])\n"
				+ "	} else\n"
				+ "		connectionMap.put(key, connection[key])\n"
				+ "}\n"
				+ "groupList = new java.util.ArrayList(groups.length)\n"
				+ "for (var i = 0; i < groups.length; i++) {\n"
				+ "	var group = new java.util.HashMap()\n"
				+ "	groupList.add(group)\n"
				+ "	for (var groupKey in groups[i]) {\n"
				+ "		if (groupKey === 'users') {\n"
				+ "			var users = groups[i][groupKey]\n"
				+ "			var userList = new java.util.ArrayList(users.length)\n"
				+ "			group.put(groupKey, userList)\n"
				+ "			for (var j = 0; j < users.length; j++) {\n"
				+ "				var map = new java.util.HashMap()\n"
				+ "				userList.add(map)\n"
				+ "				for (var key in users[j])\n"
				+ "					map.put(key, users[j][key])\n"
				+ "			}\n"
				+ "		} else if (groupKey === 'run') {\n"
				+ "			var func = groups[i][groupKey]\n"
				+ "			var funcString = func ? func.toString() : func\n"
				+ "			group.put(groupKey, funcString)\n"
				+ "		} else println('Unknown group key: ' + groupKey);\n"
				+ "	}\n"
				+ "}\n"
				+ "eventMap = new java.util.HashMap()\n"
				+ "for (var key in events) eventMap.put(key, events[key])\n"
				+ "mappingMap = new java.util.HashMap()\n"
				+ "for (var key in mapping)  {\n"
				+ "	var actionMap = new java.util.HashMap()\n"
				+ "	mappingMap.put(key, actionMap)\n"
				+ "	var action = mapping[key]\n"
				+ "	for (var actionKey in action) {\n"
				+ "		if (actionKey === 'params') {\n"
				+ "			var paramMap = new java.util.HashMap()\n"
				+ "			actionMap.put(actionKey, paramMap)\n"
				+ "			var params = action[actionKey]\n"
				+ "			for (var paramKey in params)\n"
				+ "				paramMap.put(paramKey, params[paramKey])\n"
				+ "		} else if (actionKey == 'handlers') {\n"
				+ "			var handlers = action[actionKey]\n"
				+ "			var handlerMap = new java.util.HashMap()\n"
				+ "			actionMap.put(actionKey, handlerMap)\n"
				+ "			for (var handlerKey in handlers) {\n"
				+ "				var func = handlers[handlerKey]\n"
				+ "				var funcString = func ? func.toString() : func\n"
				+ "				handlerMap.put(handlerKey, funcString)\n"
				+ "			}\n"
				+ "		} else if (actionKey == 'additionalLinks') {\n"
				+ "			var links = action[actionKey]\n"
				+ "			var linkList = new java.util.ArrayList(links.length)\n"
				+ "			actionMap.put(actionKey, linkList)\n"
				+ "			for (var i = 0; i < links.length; i++)\n"
				+ "				linkList.add(links[i])\n"
				+
				"		} else\n"
				+ "			actionMap.put(actionKey, action[actionKey])\n" + "	}\n"
				+ "}\n";
	}

}
