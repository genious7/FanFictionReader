package com.spicymango.fanfictionreader;

import com.spicymango.fanfictionreader.dialogs.BackUpDialog;
import com.spicymango.fanfictionreader.dialogs.FontDialog;
import com.spicymango.fanfictionreader.dialogs.RestoreDialog;
import com.spicymango.fanfictionreader.dialogs.RestoreDialogConfirmation;
import com.spicymango.fanfictionreader.util.FileHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;


public class Settings extends ActionBarActivity {
	
	public final static int SANS_SERIF = 0;
	public final static int SERIF = 1;
	
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportFragmentManager().beginTransaction()
				.replace(android.R.id.content, new PrefsFragment()).commit();

	}
	
	public static class PrefsFragment extends PreferenceFragment implements OnPreferenceChangeListener, OnPreferenceClickListener{
		
		@Override
		public void onCreate(Bundle paramBundle) {
			super.onCreate(paramBundle);
			addPreferencesFromResource(R.xml.preferences);
			
			Preference orientationPref = findPreference(getString(R.string.pref_orientation));
			orientationPref.setOnPreferenceChangeListener(this);
			
			final boolean isSdCardAvailable = FileHandler.isExternalStorageWritable(getActivity());
			Preference installLocation = findPreference(getString(R.string.pref_loc));
			installLocation.setEnabled(isSdCardAvailable);
			installLocation.setOnPreferenceChangeListener(this);
						
			Preference backup = findPreference(getString(R.string.pref_key_back_up));			
			Preference restore = findPreference(getString(R.string.pref_key_restore));
			restore.setEnabled(RestoreDialog.findBackUpFile(getActivity()) != null);
			Preference fontDiag = findPreference(getString(R.string.pref_key_text_size));
			backup.setOnPreferenceClickListener(this);
			restore.setOnPreferenceClickListener(this);
			fontDiag.setOnPreferenceClickListener(this);
		}
		
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			
			if (preference.getKey() == getString(R.string.pref_orientation)) {
				String value = (String) newValue;
				if (value.equals("A")) {
					getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
				} else if (value.equals("H")) {
					getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				} else {
					getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
			} else if (preference.getKey().equals(getString(R.string.pref_loc))){
				showMoveDialog();
			}
			return true;
		}
		
		private void showMoveDialog() {
			AlertDialog.Builder diag = new Builder(getActivity());
			diag.setTitle(R.string.pref_loc_diag_title);
			diag.setMessage(R.string.pref_loc_diag);
			diag.setNeutralButton(android.R.string.ok, null);
			diag.show();		
		}

		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (preference.getKey().equals(getString(R.string.pref_key_back_up))) {
				DialogFragment diag = new BackUpDialog();
				diag.show(getFragmentManager(), diag.getClass().getName());
			}else if (preference.getKey().equals(getString(R.string.pref_key_restore))){
				DialogFragment diag = new RestoreDialogConfirmation();
				diag.show(getFragmentManager(), diag.getClass().getName());
			}else if(preference.getKey().equals(getString(R.string.pref_key_text_size))){
				DialogFragment diag = new FontDialog();
				diag.show(getFragmentManager(), diag.getClass().getName());
			}
			return false;
		}
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
		
		int textSize;
		try {
			//Try to get the text size as an integer
			textSize = sharedPref.getInt(context.getString(R.string.pref_key_text_size), 14);
		} catch (ClassCastException e) {
			//Else, get the old format and convert it to the new one
			textSize = TextSize.getSize(sharedPref.getString(context.getString(R.string.pref_key_text_size), ""));
			Editor editor = sharedPref.edit();
			editor.putInt(context.getString(R.string.pref_key_text_size), textSize);
			editor.commit();
		}
		
		return textSize;
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
	
	public static int getTypeFaceId(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getInt(context.getString(R.string.pref_key_type_face), 0);
	}
	
	public static Typeface getTypeFace(Context context){
		switch (getTypeFaceId(context)) {
		case SANS_SERIF:
			return Typeface.SANS_SERIF;
		case SERIF:
			return Typeface.SERIF;
		default:
			return Typeface.DEFAULT;
		}
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
 	
 	/**
 	 * Sets the orientation of the activity based on current settings
	 * @param activity The activity to set
	 */
 	public static void setOrientationAndThemeNoActionBar(Activity activity){
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
			activity.setTheme(R.style.Theme_AppCompat_NoActionBar);
		}else{
			activity.setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
		}
	}
}

