package com.github.perf;

import java.util.Map;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class ScriptEnv {

	private static final Logger LOG = (Logger) LoggerFactory.getLogger("scripting");
	
	private static ScriptEngineManager factory;
    
	private javax.script.ScriptEngine engine;

	static {
		factory = new ScriptEngineManager();		
	}

	// 1 engine per one ScriptEnv by design
	// Reasons are (a) the most of memory of ScriptEngine is ScriptContext (according with heap dump)
	// when you call eval with new bindings new ScriptContext is created to do that
	private ScriptEnv() {
		engine = factory.getEngineByName("JavaScript");
	}
	
	public static ScriptEnv getScriptEnv() {
		return new ScriptEnv();
	}
	
	public Object get(String key) {
		return engine.get(key);
	}
	
	public void set(String key, Object value) {
		engine.put(key, value);
	}

	public Object eval(String script) throws Exception {
		try {
			if (LOG.isTraceEnabled())
				LOG.trace("Evaluating:\n\n\n" + script + "\n\n");
			return engine.eval(script);
		} catch (ScriptException se) {
			if (isScriptOnly(se))
				printLine(script, se);
			else
				throw se;
			return null;
		}
	}
	
	private boolean isScriptOnly(ScriptException se) {
		for (Throwable t = se.getCause(); t != null; t = t.getCause()) {
			if (t != null && 
					(t instanceof java.util.concurrent.ExecutionException ||
					t instanceof java.net.ConnectException))
				return false;
		}
		return true;
	}

	private void printLine(String script, ScriptException se) throws ScriptException {
		String msg = se.getMessage();
		int idx1 = msg.indexOf('#');
		int idx2 = idx1 == -1 ? -1 : msg.indexOf(')', idx1);
		String l = idx1 != -1 && idx2 != -1 ? msg.substring(idx1 + 1, idx2) : null;
		int li = l != null ? Integer.parseInt(l) : -1;
		String line = "";
		if (li != -1) {
			idx1 = 0;
			for (int i = 0; i < li - 1; i++) {
				idx1 = script.indexOf('\n', idx1 + 1);
				if (idx1 == -1)
					break;
			}
			idx2 = idx1 == -1 ? -1 : script.indexOf('\n', idx1 + 1);
			if (idx1 != -1 && idx2 != -1)
				line = script.substring(idx1 + 1, idx2);
		}
		if (LOG.isInfoEnabled()) {
			LOG.info(msg + "\n\t" + line);
		}
	}

	static String getScript(Map<String, Action> mappings) {
		if (mappings == null)
			return "";
		
		StringBuilder sb = new StringBuilder(1024);		
		inject(sb, mappings);
		return sb.toString();
	}

	private static void inject(StringBuilder script, Map<String, Action> actions) {
		//script.append("importPackage(com.github.perf);\n\n");
		for (Map.Entry<String, Action> aEntry : actions.entrySet()) {
			String actionName = aEntry.getKey();
			script.append("function ").append(actionName).append("(args) {\n");
			script.append("\t").append("if (args)\n");
			script.append("\t\t").append("for (var key in args)\n");
			script.append("\t\t\t").append("userObj.setArg(key, args[key]);\n");
			// synchronizing js user and java userObj before action
			script.append("\t").append("for (var key in user)\n");
			script.append("\t\t").append("userObj.set(key, user[key]);\n");
			script.append("\t").append("userObj.executeAction('" + actionName + "');\n");
			script.append("\tuserObj.clearArgs();\n");
			script.append("}\n\n");
			
			Map<String, String> handlers = aEntry.getValue().getHandlers();
			if (handlers != null)
				for (Map.Entry<String, String> hEntry : handlers.entrySet()) {
					script.append("function func_").append(actionName).append("_").append(hEntry.getKey());
					script.append("() {\n").append("\tvar func = ").append(hEntry.getValue());
					script.append("\tfunc();\n");
					// synchronizing js user and java userObj after handler
					script.append("\t").append("for (var key in user)\n");
					script.append("\t\t").append("userObj.set(key, user[key]);\n");
					script.append("}\n\n");
				}
		}
		script.append("\n\n");
		script.append("var user = {};\n");
		script.append("var it = userObj.entrySet().iterator();\n");
		script.append("while (it.hasNext()) {\n");
		script.append("\tvar entry = it.next();\n");
		script.append("\tuser[entry.getKey()] = entry.getValue();\n");
		script.append("}\n");
		script.append("\n\n");
	}

}
