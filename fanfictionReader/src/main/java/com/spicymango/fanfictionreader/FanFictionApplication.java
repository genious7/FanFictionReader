package com.spicymango.fanfictionreader;

import android.app.Application;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.spicymango.fanfictionreader.menu.DataFragment;

import java.io.File;

import io.fabric.sdk.android.Fabric;

/**
 * An application class that represents the FanFiction Application. Initialization for basic
 * components is done here. Created by Michael Chen on 11/07/2015.
 */
public class FanFictionApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// Set up Crashlytics, disabled for debug builds
		Crashlytics crashlyticsKit = new Crashlytics.Builder()
				.core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
				.build();

		// Initialize Fabric with the debug-disabled crashlytics.
		Fabric.with(this, crashlyticsKit);

		//Initialize settings to default values upon the first access to the application
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// Clear the cache folder on app start
		clearSavedInstanceStateCacheDirectory();
	}

	/**
	 * Clears all the savedInstanceState cache files. If the cache folder does not exist (such as
	 * in a new install), this function will create the cache folder.
	 * <p>
	 * These files are used to store data instead of
	 * the savedInstanceState bundle whenever the data may exceed 1 MB, which is the limit before
	 * android Nougat throws a TransactionTooLargeException.
	 * </p>
	 * <p>
	 * The cache folder is cleared every time the app restarts in order to prevent it from growing
	 * too large. The DataFragment deletes cache files when destroyed. However, since the onDestroy()
	 * method is not guaranteed to be called, the cache files are also cleared here.
	 * </p>
	 */
	private void clearSavedInstanceStateCacheDirectory() {
		final File cacheFolder = new File(getCacheDir(), DataFragment.SAVED_INSTANCE_STATE_CACHE_PATH);

		if (cacheFolder.exists()) {
			// If the directory exists, clear all cache files.
			for (File file : cacheFolder.listFiles()){
				if (!file.isDirectory()){
					file.delete();
				}
			}
		} else {
			// If the directory does not exist, create it
			cacheFolder.mkdir();
		}
	}
}
