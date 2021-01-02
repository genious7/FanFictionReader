package com.spicymango.fanfictionreader;

import android.app.Application;
import android.preference.PreferenceManager;


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
	}
}
