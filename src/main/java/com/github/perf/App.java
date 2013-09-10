package com.github.perf;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.slf4j.LoggerFactory;

import com.github.perf.util.Stat;

import ch.qos.logback.classic.Logger;

public class App {
	
	private static Reader[] NO_READERS = new Reader[0];
	
	static final String URL_NAME = "url";
	
	static final String USERS_NAME = "users";

	static final String ON_START_EVENT = "onStart";
	static final String ON_STOP_EVENT = "onStop";
	
	static final String BEFORE_HANDLER = "before";
	static final String AFTER_HANDLER = "after";
	
	static final Logger LOG = (Logger) LoggerFactory.getLogger(App.class);
	
	boolean isInitialized = false;
	private AppConf conf;
	private List<User> users = new ArrayList<User>(50);
	
	public App(Reader[] confReaders) {
		try {
			Stat.init();
			conf = AppConf.initConf(confReaders);
			initUsers();
			isInitialized = true;
			if (conf.isLocalStatEnabled())
				Stat.getLocalStat();
			Map<String, Object> remoteMap = conf.getRemoteStatHost();
			if (conf.isStatEnabled() && remoteMap != null) {
				String name = (String) remoteMap.get("name");
				String host = (String) remoteMap.get("host");
				Double port = (Double) remoteMap.get("port");
				if (name != null && host != null && port != null)
					Stat.getRemoteStat(name, host, port.intValue());
			}
		} catch (Throwable t) {
			LOG.warn("Configuration error", t);
		}
	}
	
	public static Reader[] getConfFiles(String[] strings) {
		if (strings != null) {
			Reader[] result = new Reader[strings.length];
			try {
				for (int i = 0; i < strings.length; i++)
					result[i] = new FileReader(strings[i]);
				return result;
			} catch (FileNotFoundException e) {
				LOG.warn("File not found " + e);
			}
		}
		return NO_READERS;
	}

	private void initUsers() {
		for (UserGroup group : conf.getUserGroups())
			for (Map<String, Object> user : group.getUsers()) {
				Boolean isPattern = (Boolean) user.get("pattern");
				if (isPattern != null && isPattern.booleanValue())
					initUsersByPattern(user, group);
				else {
					User anUser = new User(this, (String) user.get("name"), group);
					for (Map.Entry<String, Object> entry : user.entrySet()) {
						anUser.set(entry.getKey(), entry.getValue());
					}
					users.add(anUser);
				}
			}
	}

	private void initUsersByPattern(Map<String, Object> user, UserGroup group) {
		int first = ((Double) user.get("first")).intValue();
		int last = ((Double) user.get("last")).intValue();
		user.remove("pattern");
		user.remove("first");
		user.remove("last");

		int i = 0;
		int size = user.size();
		String[] fields = new String[size];
		String[] patterns = new String[size];
		Format[] fieldFormats = new Format[size];
		for (Map.Entry<String, Object> entry : user.entrySet()) {
			fields[i] = entry.getKey();
			patterns[i] = (String) entry.getValue();
			if (patterns[i] == null || patterns[i].trim().length() == 0 || patterns[i].indexOf('0') == -1)
				fieldFormats[i] = null;
			else
				fieldFormats[i] = new DecimalFormat(patterns[i]);
			i++;
		}
		
		try {
			if (first > 0 && last > 0)
				for (i = first; i < last + 1; i++) {
					User anUser = new User(this, fieldFormats[0].format(i), group);
					for (int j = 0; j < fieldFormats.length; j++) {
						if (fieldFormats[j] == null)
							anUser.set(fields[j], patterns[j]);
						else
							anUser.set(fields[j], fieldFormats[j].format(i));
					}
					users.add(anUser);
				}
		} catch (Exception e) {
			// catching runtime exceptions
			LOG.warn("User initialization error", e);
		}
	}

	AppConf getConf() {
		return conf;
	}
	
	List<User> getUsers() {
		return users;
	}
	
	public void execute() {
		if (!isInitialized) {
			LOG.warn("Application is not initialized, execution is not possible");
			return;
		}
		
		long start = System.currentTimeMillis();
		int size = users.size();
		if (LOG.isInfoEnabled())
			LOG.info("Starting " + size + " users");
		final CountDownLatch latch = new CountDownLatch(size);
		for (final User user : users) {
			user.preinit();
			Thread t = new Thread(new Runnable() {

				public void run() {
					user.execute();
					latch.countDown();
				}
				
			}, user.get("name").toString());
			t.start();
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			LOG.warn("Execution is interrupted", e);
		}
		if (conf.isActionStatEnabled())
			Stat.logAction(Stat.SYSTEM, "EXECUTE", (System.currentTimeMillis() - start));
		if (conf.doStatPlotting())
			Stat.plotting();
	}
	
	public static void main(String[] args) {
		if (args.length == 0)
			System.out.println("Usage: App js-files");
		else {
			App app = new App(getConfFiles(args));
			app.execute();
		}
	}
	
}
