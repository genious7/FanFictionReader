package com.spicymango.fanfictionreader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;


public class Settings extends PreferenceActivity implements OnPreferenceChangeListener{
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);
		
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB){
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		addPreferencesFromResource(R.xml.preferences);
		
		Preference orientationPref = findPreference(getString(R.string.pref_orientation));
		orientationPref.setOnPreferenceChangeListener(this);
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public static int fontSize(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		String textSize = sharedPref.getString(context.getString(R.string.pref_text_size), "");
		if (textSize.equals("S")) {
			return 14;
		}else if (textSize.equals("M")) {
			return 18;
		} else {
			return 22;
		}
	}
	
	public static boolean isIncrementalUpdatingEnabled(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getBoolean(context.getString(R.string.pref_incremental_updating), true);
	}
	
	/**
	 * Sets the orientation of the activity based on current settings
	 * @param activity The activity to set
	 */
	public static void setOrientationAndTheme(Activity activity){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
		String orientation = sharedPref.getString(activity.getString(R.string.pref_orientation), "A");
		if (orientation.equals("A")) {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
		}else if (orientation.equals("H")) {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		
		String theme = sharedPref.getString(activity.getString(R.string.pref_theme), "D");
		if (theme.equals("D")) {
			activity.setTheme(R.style.AppActionBar);
		}else{
			activity.setTheme(R.style.AppActionBarLight);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		
		if (preference.getKey() == getString(R.string.pref_orientation)) {
			String value = (String) newValue;
			if (value.equals("A")) {
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			} else if (value.equals("H")) {
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			} else {
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
		}
		return true;
	}

}

