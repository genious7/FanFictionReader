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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MenuItem;

import java.util.Locale;


public class Settings extends AppCompatActivity {

	public final static int SANS_SERIF = 0;
	public final static int SERIF = 1;

	/**
	 * An enum for the old text size format, before it was customizable. Retained only for
	 * compatibility
	 */
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

		if (savedInstanceState == null) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(android.R.id.content, new PrefsFragment()).commit();
		}
	}

	public static class PrefsFragment extends PreferenceFragment implements OnPreferenceChangeListener, OnPreferenceClickListener{
		private final static String CREATE_DIALOG = "CreateDialog";

		@Override
		public void onCreate(Bundle paramBundle) {
			super.onCreate(paramBundle);
			addPreferencesFromResource(R.xml.preferences);

			// Check if the orientation setting changes in order to update the activity orientation
			Preference orientationPref = findPreference(getString(R.string.pref_key_orientation));
			orientationPref.setOnPreferenceChangeListener(this);

			// Only display the save location setting if there is an external storage available
			final boolean isSdCardAvailable = FileHandler.isExternalStorageWritable(getActivity());
			Preference installLocation = findPreference(getString(R.string.pref_key_loc));
			installLocation.setEnabled(isSdCardAvailable);
			installLocation.setOnPreferenceChangeListener(this);

			// Check if the theme is changed in order to recreate the activity
			Preference themeChanged = findPreference(getString(R.string.pref_key_theme));
			themeChanged.setOnPreferenceChangeListener(this);

			// Check if the locale is changed in order to recreate the activity
			Preference localeChanged = findPreference(getString(R.string.pref_key_locale));
			localeChanged.setOnPreferenceChangeListener(this);

			// Check if the backup button is clicked
			Preference backup = findPreference(getString(R.string.pref_key_back_up));
			backup.setOnPreferenceClickListener(this);

			// Check if the restore button is clicked
			Preference restore = findPreference(getString(R.string.pref_key_restore));
			restore.setEnabled(RestoreDialog.findBackUpFile(getActivity()) != null);
			restore.setOnPreferenceClickListener(this);

			// Check if the font button is clicked
			Preference fontDiag = findPreference(getString(R.string.pref_key_text_size));
			fontDiag.setOnPreferenceClickListener(this);

			if (getActivity().getIntent().getBooleanExtra(CREATE_DIALOG, false)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.diag_theme_warning);
				builder.setPositiveButton(android.R.string.ok,	new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
				builder.show();
			}
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			switch (preference.getKey()) {
				case "Application Orientation":
					// If the requested orientation changes, update the orientation
					String value = (String) newValue;
					switch (value){
						case "A":
							getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
							break;
						case "H":
							getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
							break;
						default:
							getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
							break;
					}
					return true;
				case "Save Location":
					// Warn the user that changing the location does not move old stories
					showMoveDialog();
					return true;
				case "Application Theme":
					// Saves the preference, then reopens the settings file if
					// the new value is different.
					String currentValue = preference.getSharedPreferences()
							.getString(getString(R.string.pref_key_theme), "D");

					if (currentValue.equals(newValue)) {
						return false;
					} else {
						Editor editor = preference.getEditor();
						editor.putString(preference.getKey(), (String) newValue);
						editor.commit();

						Intent i = getActivity().getIntent();
						//Display imperfect theme dialog.
						if (newValue.equals("DD")) {
							i.putExtra(CREATE_DIALOG, true);
						}
						getActivity().finish();
						startActivity(i);
						return false;
					}
				case "Locale":
					// Save the preference. If the selected locale is different from the current
					// one, recreate the activity
					String currentLocale = preference.getSharedPreferences()
							.getString(getString(R.string.pref_key_locale), "auto");

					if (currentLocale.equals(newValue)) {
						return false;
					} else {
						Editor editor = preference.getEditor();
						editor.putString(preference.getKey(), (String) newValue);
						editor.commit();

						Intent i = getActivity().getIntent();
						getActivity().finish();
						startActivity(i);
						return false;
					}
				default:
					return true;
			}
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

	/**
	 * Gets the font size that should be used to display the story text. The font size is returned
	 * as an integer. The font size should be interpreted as scale independent pixels, SP.
	 * @param context The current context
	 * @return The font size, as an integer
	 */
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

	/**
	 * Checks if the stories should be updated incrementally. If enabled, only new chapters should
	 * be downloaded. If false, every old chapter should be redownloaded in order to account for
	 * possible changes.
	 * @param context The current context
	 * @return True if chapters should be updated incrementally, false otherwise
	 */
	public static boolean isIncrementalUpdatingEnabled(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getBoolean(context.getString(R.string.pref_incremental_updating), true);
	}

	/**
	 * Checks if the volume keys should be used to scroll in stories. If true, the volume keys
	 * should be able to scroll a chapter.
	 * @param context The current context
	 * @return True if volume key scrolling is enabled
	 */
	public static boolean volumeButtonsScrollStory(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getBoolean(context.getString(R.string.pref_key_volume_scroll), false);
	}

	/**
	 * Checks if the wake lock should be enabled. If true, the device should not dim the screen
	 * due to inactivity while displaying a story.
	 * @param context The current context
	 * @return True if the wake lock should be enabled
	 */
	public static boolean isWakeLockEnabled(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getBoolean(context.getString(R.string.pref_key_wake_lock), true);
	}

	public static boolean shouldWriteToSD(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPref.getString(context.getString(R.string.pref_key_loc), "ext").equals("ext");
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
	 * Sets the orientation of the activity based on the current user settings.
	 * @param activity The application whose settings should be changed
	 * @param sharedPref The application's default SharedPreferences
	 */
	private static void setOrientation(Activity activity, SharedPreferences sharedPref){
		String orientation = sharedPref.getString(activity.getString(R.string.pref_key_orientation), "A");
		switch (orientation){
			case "A":	// Automatic orientation
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
				break;
			case "H":	// Landscape orientation
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				break;
			default:	// Vertical orientation
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;
		}
	}

	/**
	 * Sets the application locale, overriding the default if required by the user
	 *
	 * @param context    The current context
	 * @param sharedPref The application's default SharedPreferences
	 */
	private static void setLocale(Context context, SharedPreferences sharedPref) {
		// Obtain the required locale
		final Locale locale;
		String languageCode = sharedPref.getString(context.getString(R.string.pref_key_locale),
												   context.getString(R.string.pref_locale_default));
		if (languageCode.equals(context.getString(R.string.pref_locale_default))) {
			locale = Locale.getDefault();
		} else {
			locale = new Locale(languageCode);
		}

		// Set the locale
		Resources res = context.getResources();
		DisplayMetrics metrics = res.getDisplayMetrics();
		Configuration configuration = res.getConfiguration();
		configuration.locale = locale;
		res.updateConfiguration(configuration, metrics);
	}

	/**
	 * Sets the orientation of the activity based on current settings
	 *
	 * @param activity The activity to set
	 */
	public static void setOrientationAndTheme(Activity activity) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
		setOrientation(activity, sharedPref);
		setLocale(activity, sharedPref);

		String theme = sharedPref.getString(activity.getString(R.string.pref_key_theme), "D");
		switch (theme) {
			case "DD":
				activity.setTheme(R.style.AppActionBar_Darker);
				break;
			case "D":
				activity.setTheme(R.style.AppActionBar);
				break;
			default:
				activity.setTheme(R.style.AppActionBarLight);
				break;
		}
	}

 	public static int getDialogTheme(Context context){
 		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
 		String theme = sharedPref.getString(context.getString(R.string.pref_key_theme), "D");
		if (theme.equals("DD") || theme.equals("D")){
			return R.style.DialogDark;
		}else{
			return R.style.DialogLight;
		}
 	}

 	/**
 	 * Sets the orientation of the activity based on current settings
	 * @param activity The activity to set
	 */
 	public static void setOrientationAndThemeNoActionBar(Activity activity){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
		setOrientation(activity, sharedPref);
		setLocale(activity, sharedPref);

		String theme = sharedPref.getString(activity.getString(R.string.pref_key_theme), "D");
		switch (theme){
			case "DD":	//Materials Darker
				activity.setTheme(R.style.MaterialDarker);
				break;
			case "D":	//Materials Dark
				activity.setTheme(R.style.MaterialDark);
				break;
			default:	//Materials Light
				activity.setTheme(R.style.MaterialLight);
				break;
		}
	}
}

