package de.dotknowledge.xymon;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class UpdateStatusThread extends Thread {
	protected static Pattern COLOR_PATTERN = Pattern
			.compile("BODY BGCOLOR=\"([^\"]+)\" BACKGROUND");

	protected static Pattern NAME_PATTERN = Pattern
			.compile("A NAME=\"([^\"]+)\"");

	protected static Pattern TITLE_PATTERN = Pattern
			.compile("TITLE=\"([^\"]+)\"");
	
	private Boolean isRunning = null;
		
	public Monitor mainThread;

	private boolean paused;
	
	private Date lastChecked;
	
	/**
	 * @param view
	 * @param settings
	 */
	public UpdateStatusThread(Monitor activity) {
		super();
		this.mainThread = activity;
	}

	public XymonStatus retrieveStatus() {
		XymonStatus result = new XymonStatus();
		result.overallStatus = XymonStatus.UNKNOWN;
		result.detailedStatus = "No Data";
		
		// check, if we are allowed to connect
		if (mainThread.prefs.getBoolean("wifionly", false)) {
			WifiManager wifiManager = (WifiManager)mainThread.getSystemService(Activity.WIFI_SERVICE);
			boolean error = false;
			if (wifiManager.isWifiEnabled()) {
				WifiInfo info = wifiManager.getConnectionInfo();
				if (info.getLinkSpeed() <= 0) {
					error = true;
				}
			} else {
				error = true;
			}
			if (error) {
				result.overallStatus = XymonStatus.PURPLE;
				result.detailedStatus = mainThread.getString(R.string.error_nowifinetwork);
				result.lastChecked = new Date();
				return result;
			}
		}
		
		// calculate URL
		String url = mainThread.prefs.getString("serverUrl", "");
		if ("".equals(url)) {
			result.detailedStatus = mainThread.getString(R.string.error_nourl);
			result.lastChecked = new Date();
			return result;
		}
		if (url.endsWith("/")) {
			url = url + "bb2.html";
		} else {
			url = url + "/bb2.html";
		}

		// perform the request
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_0);
			HttpProtocolParams.setContentCharset(params, "UTF_8");
			HttpProtocolParams.setUseExpectContinue(params, false);
			httpClient.setParams(params);

			HttpGet request = new HttpGet(url);
			String username = mainThread.prefs.getString("username", "");
			String password = mainThread.prefs.getString("password", "");
			if (null != username && !"".equals(username)) {
				request
						.addHeader("Authorization",
								"Basic "
										+ Base64.encodeBytes((username
												+ ":" + password)
												.getBytes()));
			}
			String data;
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			data = httpClient.execute(request, responseHandler);

			Matcher colorMatcher = COLOR_PATTERN.matcher(data);
			if (colorMatcher.find()) {
				result.overallStatus = colorMatcher.group(1);
			} else {
				result.detailedStatus = mainThread.getString(R.string.error_invalidurl);
				result.lastChecked = new Date();
				return result;
			}
			lastChecked = new Date();

			result.detailedStatus = data;
			result.detailedStatus = result.detailedStatus
					.substring(result.detailedStatus
							.indexOf("TABLE SUMMARY=\"Group Block\" BORDER=0 CELLPADDING=2") + 10);
			result.detailedStatus = result.detailedStatus
					.substring(result.detailedStatus.indexOf("TR") + 10);
			int i = result.detailedStatus.indexOf("TR") + 3;
			String temp = result.detailedStatus.substring(i,
					result.detailedStatus.indexOf("/TABLE", i));
			String[] tempRE = temp.split("/TR");
			result.detailedStatus = "";

			for (int j = 0; j < tempRE.length; j++) {
				String line = "";
				Matcher nameMatcher = NAME_PATTERN.matcher(tempRE[j]);
				if (nameMatcher.find()) {
					line = nameMatcher.group(1) + ": ";
					Matcher titleMatcher = TITLE_PATTERN.matcher(tempRE[j]);
					while (titleMatcher.find()) {
						String alert = titleMatcher.group(1);
						if (!alert.contains("green")) {
							alert = alert.replaceAll(":red:", ":" + mainThread.getString(R.string.color_red) + ":");
							alert = alert.replaceAll(":yellow:", ":" + mainThread.getString(R.string.color_yellow) + ":");
							alert = alert.replaceAll(":blue:", ":" + mainThread.getString(R.string.color_blue) + ":");
							alert = alert.replaceAll(":purple:", ":" + mainThread.getString(R.string.color_purple) + ":");
							line = line + alert + ", ";
						}
					}
					result.detailedStatus = result.detailedStatus
							+ line.substring(0, line.length() - 2) + "\n";
				}
			}
		} catch (ClientProtocolException e) {
			result.overallStatus = XymonStatus.EXCEPTION;
			result.detailedStatus = e.getLocalizedMessage();
		} catch (IOException e) {
			result.overallStatus = XymonStatus.EXCEPTION;
			result.detailedStatus = e.getLocalizedMessage();
		}

		result.lastChecked = new Date();
		return result;
	}
	
	@Override
	public void run() {
		isRunning = true;
		while (isRunning) {
			if (paused) {
				if ( ((new Date()).getTime() - lastChecked.getTime() ) > 3600000) {
					// last update is older than one hour
					Monitor.currentStatus.overallStatus = XymonStatus.PURPLE;
					mainThread.updateView(true);
				}
			} else {
				updateStatus();
			}
			
			try {
				int updateInterval = 120;
				try {
					updateInterval = Integer.parseInt(mainThread.prefs.getString("updateInterval", "120"));
				} catch (NumberFormatException nfe) {
					// ignore;
				}
				Thread.sleep(updateInterval * 1000);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		isRunning = null;
	}

	public void updateStatus() {
		XymonStatus s = retrieveStatus();
		
		boolean notify = false;
		
		if (Monitor.currentStatus != null && XymonStatus.isColor(s.overallStatus) && !s.overallStatus.equals(Monitor.currentStatus.overallStatus)) {
			notify = true;	
		}
		
		Monitor.currentStatus = s;
		mainThread.updateView(notify);
	}
	
	public void halt() {
		isRunning = false;
	}

	public void pause() {
		paused = true;		
	}

	public void unPause() {
		paused = false;		
	}
	
	public boolean isPaused() {
		return paused;
	}
}

