package de.dotknowledge.xymon;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Class to call the edit preferences screen
 * 
 * @author Tobias Schulz-Hess, dot knowledge
 *
 */
public class EditPreferences extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
	}
}