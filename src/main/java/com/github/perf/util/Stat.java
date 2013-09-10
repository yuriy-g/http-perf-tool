package com.github.perf.util;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;

import com.github.perf.http.HttpClient.HttpMethod;

public class Stat extends Thread {

	public static final String SYSTEM = "SYSTEM";
	public static final String CPUMEM = "CPUMEM";
	public static final String HTTP = "HTTP";
	
	public static enum Result { OK, NOK };
	
	private static Logger LOG;
	private static Logger STATLOG;

	private static final String THREAD_NAME = "stat.";

	private String name;
	private static boolean isStatLogInitialized = false;
	private boolean isInitialized = false;
	private long interval = 1000;
	
	private final int availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	private RuntimeMXBean rtbean;
	@SuppressWarnings("restriction")
	private com.sun.management.OperatingSystemMXBean osbean;
	private MemoryMXBean membean;
	private List<GarbageCollectorMXBean> gcbeans;
	private List<MemoryPoolMXBean> mpbeans;
	private ThreadMXBean tbean;
	
	//private static final NumberFormat nf = NumberFormat.getInstance();
	private static ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {

		@Override
		public DateFormat get() {
			return super.get();
		}

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
		}

	};

	private Values curr = null;
	
	private static final String SEP = ",";
	private static final String HEADER = 
			"Time" + SEP +
			"User" + SEP +
			"CPU" + SEP + 
			"MemCommitted" + SEP + 
			"MemUsed" + SEP + 
			"MemMax" + SEP + 
			"Threads" + SEP;

	public static Stat getLocalStat() {
		Stat result = new Stat("app");
		result.start();
		return result;
	}

	public static Stat getRemoteStat(String name, String host,  int port) {
		Stat result = new Stat(host, port, name + "." + host + ":" + port);
		result.start();
		return result;
	}

	// required because otherwise StatPlotter depends on Stat class
	// without this dependency, logback is not used
	// it could be called from constructors (normally)
	// but also from App constructor, because AppConf uses STATLOG
	// but Stat constructors are called after AppConf checking
	public synchronized static void init() {
		if (!isStatLogInitialized) {
			LOG = LoggerFactory.getLogger(Stat.class);
			STATLOG = LoggerFactory.getLogger("stat");
			if (STATLOG.isTraceEnabled())
				STATLOG.trace(HEADER);
			isStatLogInitialized = true;
		}
	}
	
	private Stat(String name) {
		super(THREAD_NAME + (name == null ? "" : name));
		this.name = name;
		init();
		setDaemon(true);
		initLocalMXBeans();
		init(name);
	}
	
	private Stat(String host, int port, String name) {
		super(THREAD_NAME + (name == null ? "" : name));
		this.name = name;
		init();
		setDaemon(true);
		initRemoteMXBeans(host, port);
		init(name);
	}

	@SuppressWarnings("restriction")
	private void initLocalMXBeans() {
		try {
			rtbean = ManagementFactory.getRuntimeMXBean();
			OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean( );
			osbean = bean instanceof com.sun.management.OperatingSystemMXBean? (com.sun.management.OperatingSystemMXBean) bean : null;
			membean = ManagementFactory.getMemoryMXBean();
			gcbeans = ManagementFactory.getGarbageCollectorMXBeans();
			mpbeans = ManagementFactory.getMemoryPoolMXBeans();
			tbean = ManagementFactory.getThreadMXBean();
			isInitialized = true;
		} catch (Throwable t) {
			LOG.warn("MXBean initialization error: " + t);
		}
	}

	@SuppressWarnings("restriction")
	private void initRemoteMXBeans(String host, int port) {
		try {
			JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi"));
			MBeanServerConnection mbs = connector.getMBeanServerConnection();
			
			rtbean = ManagementFactory.newPlatformMXBeanProxy(mbs, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
			OperatingSystemMXBean bean = ManagementFactory.newPlatformMXBeanProxy(mbs, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
			osbean = bean instanceof com.sun.management.OperatingSystemMXBean? (com.sun.management.OperatingSystemMXBean) bean : null;
			membean = ManagementFactory.newPlatformMXBeanProxy(mbs, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
			ObjectName gcName = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");
			Set<ObjectName> queryNames = mbs.queryNames(gcName, null);
			gcbeans = new ArrayList<GarbageCollectorMXBean>(queryNames.size());
			for (ObjectName name : queryNames)
			    gcbeans.add(ManagementFactory.newPlatformMXBeanProxy(mbs, name.getCanonicalName(), GarbageCollectorMXBean.class));
			ObjectName mpName = new ObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",*");
			Set<ObjectName> queryNames2 = mbs.queryNames(mpName, null);
			mpbeans = new ArrayList<MemoryPoolMXBean>(queryNames2.size());
			for (ObjectName name : queryNames2)
			    mpbeans.add(ManagementFactory.newPlatformMXBeanProxy(mbs, name.getCanonicalName(), MemoryPoolMXBean.class));
			tbean = ManagementFactory.newPlatformMXBeanProxy(mbs, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
			isInitialized = true;
		} catch (Throwable t) {
			LOG.warn("MXBean initialization error: " + t);
		}
	}

	private void init(String name) {
		if (isInitialized) {
			logStartup(this);
			/*
			for (java.util.Map.Entry<String, String> prop : rtbean.getSystemProperties().entrySet()) {
				System.out.println(prop);
			}
			*/
		}		
	}

	public boolean isInitialized() {
		return isInitialized;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	private boolean exists() {
		int count = 0;
		long[] ids = tbean.getAllThreadIds();
		for (long id : ids) {
			ThreadInfo ti = tbean.getThreadInfo(id);
			if (ti != null && THREAD_NAME.equals(ti.getThreadName())) {
					count++;
					if (count > 1)
						return true;
			}
		}
		return false;
	}

	public void run() {
		if (isInitialized && !exists())
			while (!isInterrupted()) {
				update();
				try {
					sleep(interval);
				} catch (InterruptedException e) {
					break;
				}
			}
	}

	@SuppressWarnings("restriction")
	private void update() {
		if (curr == null)
			curr = new Values();
		
		boolean isFirst = curr.currNano == 0;
		
		long prevNano = curr.currNano;
		curr.currNano = System.nanoTime();
		long diffNano = curr.currNano - prevNano;
		curr.currTime = System.currentTimeMillis();
		
		long prevCpuTime = curr.currCpuTime;
		curr.currCpuTime = osbean == null ? 0L : osbean.getProcessCpuTime();
		long diffCpuTime = curr.currCpuTime - prevCpuTime;
		//curr.currUptime = rtbean.getUptime();
		
		curr.cpu = (float) diffCpuTime * 100 / (diffNano * availableProcessors);
		curr.heapMem = membean.getHeapMemoryUsage();
		curr.nonheapMem = membean.getNonHeapMemoryUsage();
		
		/*
		for (GarbageCollectorMXBean gcbean : gcbeans) {
			System.out.println(gcbean.getName() + SEP + gcbean.getCollectionCount() + SEP + gcbean.getCollectionTime());
		}
		for (MemoryPoolMXBean mpbean : mpbeans) {
			System.out.println(mpbean.getName() + SEP + mpbean.getUsage());
		}
		*/
		
		curr.threadCount = tbean.getThreadCount();
		
		if (!isFirst)
			log(curr.toString());
	}
	
	public Values getValues() {
		return curr;
	}
	
	public String getValuesString() {
		return curr == null ? "" : curr.toString();
	}

	public class Values {
		
		private long currNano;
		private long currTime;
		private long currCpuTime;
		private float cpu;
		MemoryUsage heapMem;
		MemoryUsage nonheapMem;
		int threadCount;
		
		public long getCurrTime() {
			return currTime;
		}
		
		public long getCurrCpuTime() {
			return currCpuTime;
		}
		
		public float getCpu() {
			return cpu;
		}
		
		public MemoryUsage getHeapMem() {
			return heapMem;
		}
		
		public long getInitHeapMem() {
			return heapMem == null ? 0 : heapMem.getInit();
		}
		
		public long getCommittedHeapMem() {
			return heapMem == null ? 0 : heapMem.getCommitted();
		}
		
		public long getMaxHeapMem() {
			return heapMem == null ? 0 : heapMem.getMax();
		}
		
		public long getUsedHeapMem() {
			return heapMem == null ? 0 : heapMem.getUsed();
		}
		
		public int getThreadCount() {
			return threadCount;
		}
		
		@Override
		public String toString() {
			return 
					formatDate(currTime) + SEP +
					CPUMEM + "." + name + SEP + 
					cpu + SEP +
					getUsedHeapMem() + SEP +
					getCommittedHeapMem() + SEP +
					getMaxHeapMem() + SEP +
					threadCount + SEP;
		}
		
		
	}
	
	public static String formatDate(long time) {
		return df.get().format(time);
	}
	
	public static Date parseDate(String source) throws ParseException {
		return df.get().parse(source);
	}
	
	public static void log(String txt) {
		if (STATLOG.isTraceEnabled())
			STATLOG.trace(txt);
	}

	private static void logStartup(Stat st) {
		if (STATLOG.isTraceEnabled()) {
			//STATLOG.trace("Spec" + SEP + st.rtbean.getSpecName() + SEP + st.rtbean.getSpecVendor() + SEP + st.rtbean.getSpecVersion());
			//STATLOG.trace("Vm" + st.rtbean.getVmName() + SEP + st.rtbean.getVmVendor() + SEP + st.rtbean.getVmVersion());
			STATLOG.trace(formatDate(st.rtbean.getStartTime()) + SEP + st.getName() + SEP + "START");
		}
	}

	public static void logRequest(String name, String url, HttpMethod method, Map<String, Object> httpParams) {
		if (STATLOG.isTraceEnabled())
			STATLOG.trace(formatDate(System.currentTimeMillis()) + SEP + name + SEP + HTTP + SEP + url + SEP + method + SEP + httpParams);
	}
	
	public static void logResponse(String name, String url, Result result) {
		if (STATLOG.isTraceEnabled())
			STATLOG.trace(formatDate(System.currentTimeMillis()) + SEP + name + SEP + HTTP + SEP + url + SEP + result);
	}
	
	public static void logAction(String name, String action) {
		if (STATLOG.isTraceEnabled())
			STATLOG.trace(formatDate(System.currentTimeMillis()) + SEP + name + SEP + action);
	}
	
	public static void logAction(String name, String action, long time) {
		if (STATLOG != null && STATLOG.isTraceEnabled())
			STATLOG.trace(formatDate(System.currentTimeMillis()) + SEP + name + SEP + action + SEP + time + "ms");
	}
	
	public static void logAction(String name, String action, Result result) {
		if (STATLOG.isTraceEnabled())
			STATLOG.trace(formatDate(System.currentTimeMillis()) + SEP + name + SEP + action + SEP + result);
	}

	public static void plotting() {
		Appender<ILoggingEvent> appender = ((ch.qos.logback.classic.Logger) STATLOG).getAppender("STAT");
		if (appender instanceof FileAppender<?>) {
			FileAppender<?> fileAppender = (FileAppender<?>) appender;
			try {
				StatPlotter.main(new String[] { fileAppender.getFile() });
			} catch (Exception e) {
				LOG.warn("Stat plotting error", e);
			}
		}
	}
	
}
