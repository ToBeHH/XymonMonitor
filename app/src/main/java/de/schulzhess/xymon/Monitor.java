package de.schulzhess.xymon;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


/**
 * Main class for the monitor
 * 
 * @author Tobias Schulz-Hess, dot knowledge
 * TODO: About dialog
 * TODO: Translations
 */
public class Monitor extends Activity {

	protected static final int NOTIFY_ID = 4242;

	private UpdateStatusThread thread;

	public static XymonStatus currentStatus;

	protected SharedPreferences prefs;

	protected NotificationManager notificationManager;
	
	protected boolean brightSetOn = false;

    static Context context;

	final static String UPDATE_INTENT = "de.schulzhess.xymon.UPDATE";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
        this.context = getApplicationContext();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		thread = new UpdateStatusThread(this);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(prefListener);

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		thread.start();

		if ("".equals(prefs.getString("serverUrl", ""))) {
			startActivity(new Intent(this, EditPreferences.class));
		}
	}

	@Override
	protected void onResume() {
        this.context = getApplicationContext();
		super.onResume();
		if (prefs.getBoolean("keepScreenOn", false)) {			
			findViewById(R.id.mainView).setKeepScreenOn(true);
			brightSetOn = true;
		} else {
			brightSetOn = false;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (brightSetOn || prefs.getBoolean("keepScreenOn", false)) {
			findViewById(R.id.mainView).setKeepScreenOn(false);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.option, menu);

		return (super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.prefs:
			startActivity(new Intent(this, EditPreferences.class));
			return true;
		case R.id.browser:
			Intent i = new Intent();
			i.setAction("android.intent.action.VIEW");
			i.addCategory("android.intent.category.BROWSABLE");
			i.setData(Uri.parse(prefs.getString("serverUrl", "")));
			startActivity(i);
			return true;
		case R.id.about:
			AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
			aboutBuilder.setTitle(R.string.about_title);
			aboutBuilder.setMessage(R.string.about_message);
			aboutBuilder.setPositiveButton(android.R.string.ok, null);
			AlertDialog about = aboutBuilder.create();
			about.show();
			return true;
		case R.id.pause:
			if (thread.isPaused()) {
				thread.unPause();
				item.setIcon(android.R.drawable.ic_media_pause);
				item.setTitle(R.string.menu_pause);
				thread.interrupt();
			} else {
				thread.pause();
				item.setIcon(android.R.drawable.ic_media_play);
				item.setTitle(R.string.menu_play);
			}
			return true;
		}

		return (super.onOptionsItemSelected(item));
	}

	@Override
	protected void onDestroy() {
		thread.halt();
		thread.interrupt();
		try {
			// wait for thread to finish
			thread.join();
		} catch (InterruptedException e) {
			// ignore
		}
        super.onDestroy();
	}

	public void updateView(final boolean _notify) {
		runOnUiThread(new Runnable() {
			boolean notify = _notify;

			public void run() {
				View mainView = findViewById(R.id.mainView);

				if (notify) {
					Intent i = new Intent();
					i.setClassName("de.schulzhess.xymon", "de.schulzhess.xymon.Monitor");
					i.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
					
					PendingIntent pi=PendingIntent.getActivity(mainView.getContext(), 0, i, 0);

					NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mainView.getContext())
							.setSmallIcon(currentStatus.getStatusIcon())
							.setContentTitle(getString(R.string.notification_title))
							.setContentText(getString(R.string.notification_content).replace("###", getString(currentStatus.getColorName())));
					mBuilder.setContentIntent(pi);

					NotificationManager mNotifyMgr =
							(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					mNotifyMgr.notify(1338, mBuilder.build());

					/*
					getString(R.string.app_name) + ": " +
									getString(R.string.notification_content).replace("###", getString(currentStatus.getColorName()))

					Notification note = new Notification(currentStatus.getStatusIcon(), ,
							System.currentTimeMillis());
					note.setLatestEventInfo(mainView.getContext(),
							getString(R.string.notification_title), 
							getString(R.string.notification_content).replace("###", getString(currentStatus.getColorName())), 
							pi);
					notificationManager.notify(1338, note);
					 */

					if (prefs.getBoolean("vibrate", false)) {
						Notification notification = new Notification();
						notification.vibrate = new long[] { 100, 250, 100, 500 };
						notificationManager.notify(NOTIFY_ID, notification);
					}
				}
				
				currentStatus.updateStatus(mainView);
				if (prefs.getBoolean("statusUpdate", false)) {
					Toast toast = Toast.makeText(mainView.getContext(),
							R.string.toast_status_updated, Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.BOTTOM, 0, 0);
					toast.show();
				}
			}
		});
	}

	private SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
				String key) {
			new Thread(){
				@Override
				public void run() {
					thread.updateStatus();
				}
			}.start();
		}
	};

}