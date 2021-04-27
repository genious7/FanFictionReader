package com.spicymango.fanfictionreader;

import android.app.Application;
import android.preference.PreferenceManager;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.spicymango.fanfictionreader.util.AndroidCookieStore;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;


/**
 * An application class that represents the FanFiction Application. Initialization for basic
 * components is done here. Created by Michael Chen on 11/07/2015.
 */
public class FanFictionApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		//Initialize settings to default values upon the first access to the application
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// Initialize the persistent cookie manager. This is used to store the Cloudflare cookie,
		// which is required to be able to browse the ff.net website.
		final CookieManager cookieManager = new CookieManager(new AndroidCookieStore(getApplicationContext()), CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(cookieManager);

		// Enable crash reporting if set
		FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(Settings.isCrashReportingEnabled(this));
	}

}
