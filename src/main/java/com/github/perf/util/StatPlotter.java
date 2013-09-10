package com.github.perf.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.perf.util.Stat.Result;
import com.github.plot.Plot;

public class StatPlotter {

	private static class CpuMemData {
		private List<Double> time = new ArrayList<Double>(100);
		private List<Double> cpu = new ArrayList<Double>(100);
		private List<Double> memCommitted = new ArrayList<Double>(100);
		private List<Double> memUsed = new ArrayList<Double>(100);
		private List<Double> memMax = new ArrayList<Double>(100);
		private List<Double> threads = new ArrayList<Double>(100);		
	}
	
	private static class CpuMemIndex {
		private Map<String, CpuMemData> hosts = new HashMap<String, CpuMemData>(5);
	}
	
	private static class Action {
		private long time;
		private Stat.Result result = Stat.Result.OK;
	}
	
	private static class UserActions {
		private int index;
		private List<Action> actions = new ArrayList<Action>(10);
	}
	
	private static class UserData {
		private Map<String, UserActions> users = new HashMap<String, UserActions>(100);
		List<Double> xOk = new ArrayList<Double>(100);
		List<Double> yOk = new ArrayList<Double>(100);
		List<Double> xNok = new ArrayList<Double>(100);
		List<Double> yNok = new ArrayList<Double>(100);
	}
	
	private static final String SEP = ",";
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1)
			System.out.println("Usage: CsvToPlot <csv-file>");
		else
			parseCsv(args[0]);
	}

	private static void parseCsv(String csvFileName) throws IOException, ParseException {
		System.out.println("Parsing " + csvFileName);
		BufferedReader br = new BufferedReader(new FileReader(csvFileName));
		try {
			long minTime = Long.MAX_VALUE, maxTime = 0;
			CpuMemIndex cmData = new CpuMemIndex();
			UserData uData = new UserData();
			String line;
			int i = 0, j = 0;
			while ((line = br.readLine()) != null) {
				i++;
				if (i == 1)
					continue;
				String[] cols = line.split(SEP);
				long time = cols.length > 0 ? Stat.parseDate(cols[0]).getTime() : null;
				String userName = cols.length > 1 ? cols[1] : null;
				if (userName != null) {
					if (userName.startsWith(Stat.CPUMEM) && cols.length > 6) {
						String[] names = userName.split("\\.");
						String hostName = names == null || names.length < 2 ? null : names[1];
						CpuMemData cmIdx = cmData.hosts.get(hostName);
						if (cmIdx == null) {
							cmIdx = new CpuMemData();
							cmData.hosts.put(hostName, cmIdx);
						}
						cmIdx.time.add((double) time);
						if (time > maxTime)
							maxTime = time;
						if (time < minTime)
							minTime = time;
						cmIdx.cpu.add(Double.parseDouble(cols[2]));
						cmIdx.memCommitted.add(Double.parseDouble(cols[3]));
						cmIdx.memUsed.add(Double.parseDouble(cols[4]));
						cmIdx.memMax.add(Double.parseDouble(cols[5]));
						cmIdx.threads.add(Double.parseDouble(cols[6]));
					} else if (Stat.SYSTEM.equals(userName)) {	
					} else if (userName.startsWith("stat.")) {
					} else {
						UserActions idx = uData.users.get(userName);
						if (idx == null) {
							idx = new UserActions();
							idx.index = j++;
							uData.users.put(userName, idx);
						}
						String actionName = cols.length > 2 ? cols[2] : null;
						if (actionName != null) {
							if (Stat.HTTP.equals(actionName)) {
							} else {
								String res = cols.length > 3 ? cols[3] : null;
								Action action = null;
								if (res == null) {
									action = new Action();
									action.time = time;
									if (time > maxTime)
										maxTime = time;
									if (time < minTime)
										minTime = time;
									idx.actions.add(action);
								} else
									action = idx.actions.get(idx.actions.size() - 1);
								if (res != null && "NOK".equals(res))
									action.result = Result.NOK;
							}
						}
					}
				}
			}
			for (UserActions user : uData.users.values()) {
				for (Action action : user.actions) {
					if (action.result == Stat.Result.OK) {
						uData.xOk.add((double) action.time);
						uData.yOk.add((double) user.index);
					} else {
						uData.xNok.add((double) action.time);
						uData.yNok.add((double) user.index);
					}
				}
			}
			String csvName = csvFileName;
			if (csvName.endsWith(".csv"))
				csvName = csvName.substring(0, csvName.length() - 4);
			for (Map.Entry<String, CpuMemData> entry : cmData.hosts.entrySet())
				saveCpuMemPlot(csvName, entry.getKey(), entry.getValue(), minTime, maxTime);
			saveUserPlot(csvName, uData, minTime, maxTime);
		} finally {
			br.close();
		}
	}
	
	private static void saveCpuMemPlot(String csvName, String name, CpuMemData data, long minTime, long maxTime) throws IOException {
		String fileName = csvName + "_cpumem_" + name;
		String fileExt = "png";
		System.out.println("Generating " + fileName + "." + fileExt);
		Plot plot = Plot.plot(Plot.plotOpts().
				title("CPU/Mem (" + name + ")").
				width(1000).
				height(600).
				legend(Plot.LegendFormat.TOP)).
				xAxis("t", Plot.axisOpts().
						range(minTime, maxTime).
						format(Plot.AxisFormat.TIME_HMS)).
				yAxis("threads", Plot.axisOpts().
						format(Plot.AxisFormat.NUMBER_INT)).
				yAxis("%", Plot.axisOpts().
						range(0, 100)).
				yAxis("bytes", Plot.axisOpts().
						format(Plot.AxisFormat.NUMBER_KGM)).
						
				series("threads", Plot.data().
						xy(data.time, data.threads), 
						Plot.seriesOpts().
							yAxis("threads").
							color(Color.yellow).
							lineWidth(3)).
		
				series("mem committed", Plot.data().
						xy(data.time, data.memCommitted), 
						Plot.seriesOpts().
							yAxis("bytes").
							line(Plot.Line.DASHED).
							lineWidth(2).
							lineDash(new float[] { 2.0f, 2.0f }).
							marker(Plot.Marker.CIRCLE).
							markerSize(4).
							color(Color.BLUE)).
						
				series("mem used", Plot.data().
						xy(data.time, data.memUsed), 
						Plot.seriesOpts().
							yAxis("bytes").
							line(Plot.Line.DASHED).
							lineWidth(2).
							lineDash(new float[] { 2.0f, 5.0f }).
							areaColor(new Color(0, 0, 0xff, 30)).
							color(Color.BLUE)).
							
				series("mem max", Plot.data().
						xy(data.time, data.memMax), 
						Plot.seriesOpts().
							yAxis("bytes").
							line(Plot.Line.SOLID).
							lineWidth(1).
							color(Color.BLUE)).
		
				series("cpu", Plot.data().
						xy(data.time, data.cpu), 
						Plot.seriesOpts().
							yAxis("%").
							color(Color.RED).
							lineWidth(3));
		
		plot.save(fileName, fileExt);
	}

	private static void saveUserPlot(String name, UserData data, long minTime, long maxTime) throws IOException {
		String fileName = name + "_user_activity";
		String fileExt = "png";
		System.out.println("Generating " + fileName + "." + fileExt);
		Plot plot = Plot.plot(Plot.plotOpts().
				title("User activity").
				width(1000).
				height(600)).
				xAxis("t", Plot.axisOpts().
						range(minTime, maxTime).
						format(Plot.AxisFormat.TIME_HMS)).
				yAxis("users", Plot.axisOpts().
						format(Plot.AxisFormat.NUMBER_INT)).
				series("ok", Plot.data().
						xy(data.xOk, data.yOk), 
						Plot.seriesOpts().
							line(Plot.Line.NONE).
							color(Color.GREEN).
							marker(Plot.Marker.SQUARE).
							markerSize(4).
							markerColor(Color.GREEN)).
				series("nok", Plot.data().
						xy(data.xNok, data.yNok), 
						Plot.seriesOpts().
							line(Plot.Line.NONE).
							color(Color.RED).
							marker(Plot.Marker.SQUARE).
							markerSize(4).
							markerColor(Color.RED));
		plot.save(fileName, fileExt);
	}

}
