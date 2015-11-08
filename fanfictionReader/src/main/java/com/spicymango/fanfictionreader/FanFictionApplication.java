package com.spicymango.fanfictionreader;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

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
	}
}
