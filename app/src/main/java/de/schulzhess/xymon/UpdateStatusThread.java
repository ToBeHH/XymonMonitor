package de.schulzhess.xymon;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.apache.http.conn.ssl.SSLSocketFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class UpdateStatusThread extends Thread {
    private static final int DEFAULT_BUFFER_SIZE = 1024;
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
     */
    public UpdateStatusThread(Monitor activity) {
        super();
        this.mainThread = activity;
    }

    public XymonStatus retrieveStatus() {
        Log.d(XymonStatus.TAG, "Starting to retrieve status...");
        XymonStatus result = new XymonStatus();
        result.overallStatus = XymonStatus.UNKNOWN;
        result.detailedStatus = "No Data";

        // check, if we are allowed to connect
        if (mainThread.prefs.getBoolean("wifionly", false)) {
            WifiManager wifiManager = (WifiManager) mainThread.getSystemService(Activity.WIFI_SERVICE);
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
        if (!(url.endsWith(".html") || url.endsWith(".htm"))) {
            if (url.endsWith("/")) {
                url = url + "bb2.html";
            } else {
                url = url + "/bb2.html";
            }
        }
        Log.d(XymonStatus.TAG, "Getting xymon status from url: " + url);

        HttpURLConnection urlConnection = null;
        try {

            URL url2 = new URL(url);
            if (mainThread.prefs.getBoolean("selfsigned", false) && url.startsWith("https")) {
                try {
                    SSLContext context = SSLContext.getInstance("TLS");
                    Log.d(XymonStatus.TAG, "Using empty Trust all Keystore for SSL connection (because we are in test mode)");
                    context.init(null, new TrustManager[]{new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }

                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }
                    }}, new java.security.SecureRandom());

                    urlConnection = (HttpsURLConnection) url2.openConnection();
                    ((HttpsURLConnection) urlConnection).setHostnameVerifier(new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });
                    // set this for all kind of connections:
                    HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });
                    ((HttpsURLConnection) urlConnection).setSSLSocketFactory(context.getSocketFactory());

                } catch (Exception ioe) {
                    Log.e(XymonStatus.TAG, "Cannot use ignore all keystore");
                    urlConnection = (HttpsURLConnection) url2.openConnection();
                    ((HttpsURLConnection) urlConnection).setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
                }
            } else {
                Log.d(XymonStatus.TAG, "Using DEFAULT connection for url " + url);
                if (url.startsWith("https")) {
                    urlConnection = (HttpsURLConnection) url2.openConnection();
                    ((HttpsURLConnection) urlConnection).setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
                } else {
                    urlConnection = (HttpURLConnection) url2.openConnection();
                }
            }

            final String username = mainThread.prefs.getString("username", "");
            final String password = mainThread.prefs.getString("password", "");
            if (null != username && !"".equals(username)) {
                try {
                    urlConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes((username + ":" + password).getBytes(), Base64.DONT_BREAK_LINES));
                /*Authenticator.setDefault(new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password.toCharArray());

                    }
                });*/
                } catch (IllegalArgumentException iae) {
                    Log.e(XymonStatus.TAG, "Error while setting username and password", iae);
                    result.detailedStatus = mainThread.getString(R.string.error_password);
                    result.lastChecked = new Date();
                    return result;
                }
            }
            InputStreamReader in = new InputStreamReader(new BufferedInputStream(urlConnection.getInputStream()));
            Log.d(XymonStatus.TAG, "Got connection");
            StringWriter writer = new StringWriter();
            char[] buffer = new char[DEFAULT_BUFFER_SIZE];
            long count = 0;
            int n = 0;
            while (-1 != (n = in.read(buffer))) {
                writer.write(buffer, 0, n);
                count += n;
            }
            in.close();
            writer.close();
            String data = writer.toString();
            Log.d(XymonStatus.TAG, "Status retrieved, size: " + data.length());

/*
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
			*/

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
        } catch (IOException e) {
            Log.e(XymonStatus.TAG, "Exception while loading data from URL", e);
            result.overallStatus = XymonStatus.EXCEPTION;
            result.detailedStatus = e.getLocalizedMessage();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        result.lastChecked = new Date();
        return result;
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            if (paused) {
                if (((new Date()).getTime() - lastChecked.getTime()) > 3600000) {
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

        // update widgets
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(Monitor.context);
        ComponentName widgetComponent = new ComponentName(Monitor.context, WidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        Intent update = new Intent();
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        Monitor.context.sendBroadcast(update);
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

