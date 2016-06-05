package de.schulzhess.xymon;

import android.content.SharedPreferences;

public class Settings {
	public String serverUrl;
	
	public String username;
	
	public String password;
	
	public int updateInterval;
	
	// TODO: proxy params
	
	// TODO: vibrate on/off
	
	// TODO: Update only on wlan?
	
	public void saveState(SharedPreferences.Editor map) {
    	if (map != null) {
        	map.putString("serverUrl", serverUrl);
        	map.putString("u", username);
        	map.putString("p", password);
        	map.putInt("updateInterval", updateInterval);
        }
	}

	public void restoreState(SharedPreferences savedState) {
		if (savedState != null) {
	    	serverUrl = savedState.getString("serverUrl", "");
	    	username = savedState.getString("u", "");
	    	password = savedState.getString("p", "");
	    	updateInterval = savedState.getInt("updateInterval", 120);
		}
	}	
}
