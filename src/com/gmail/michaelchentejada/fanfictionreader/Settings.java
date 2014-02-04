package com.gmail.michaelchentejada.fanfictionreader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class Settings extends PreferenceActivity {
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
	
	protected static int fontSize(Context context){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		String textSize = sharedPref.getString(context.getResources().getString(R.string.pref_text_size), "");
		if (textSize.equals("S")) {
			return 14;
		}else if (textSize.equals("M")) {
			return 18;
		} else {
			return 22;
		}
	}
}
