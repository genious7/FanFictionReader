package com.spicymango.fanfictionreader;

import com.spicymango.fanfictionreader.util.FileHandler;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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
	
	private enum TextSize{
		SMALL(14,"S"),
		MEDIUM(18,"M"),
		LARGE(22,"L"),
		XLARGE(32,"XL");
		
		private int size;
		private String key;
		
		TextSize(int fontSize, String key){
			size = fontSize;
			this.key = key;
		}
	
		public static int getSize(String key){
			for (TextSize t : values()) {
				if (t.key.equals(key)) {
					return t.size;
				}
			}
			return MEDIUM.size;
		}
	}
	
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
		
		Preference installLocation = findPreference(getString(R.string.pref_loc));
		installLocation.setEnabled(FileHandler.isExternalStorageWritable());
		installLocation.setOnPreferenceChangeListener(this);
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
		return TextSize.getSize(textSize);
	}
	
	public static boolean isIncrementalUpdatingEnabled(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getBoolean(context.getString(R.string.pref_incremental_updating), true);
	}
	
	public static boolean isWakeLockEnabled(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getBoolean(context.getString(R.string.pref_wake_lock), true);
	}
	
	public static boolean shouldWriteToSD(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getString(context.getString(R.string.pref_loc), "ext").equals("ext");
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
		} else if (preference.getKey() == getString(R.string.pref_loc)){
			showMoveDialog();
		}
		return true;
	}

	private void showMoveDialog() {
		AlertDialog.Builder diag = new Builder(this);
		diag.setTitle(R.string.pref_loc_diag_title);
		diag.setMessage(R.string.pref_loc_diag);
		diag.setNeutralButton(android.R.string.ok, null);
		diag.show();		
	}

}

