package com.spicymango.fanfictionreader.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Created by Michael Chen on 12/17/2015.
 */
public class BackupAgent  extends BackupAgentHelper{
	private static String PREF_DEF = "com.spicymango.fanfictionreader_preferences";

	// Unique keys for the helpers
	private static String KEY_PREF = "prefs";

	@Override
	public void onCreate() {
		super.onCreate();

		// Back up all the preferences
		SharedPreferencesBackupHelper prefHelper = new SharedPreferencesBackupHelper(this, PREF_DEF);
		addHelper(KEY_PREF, prefHelper);
	}
}
