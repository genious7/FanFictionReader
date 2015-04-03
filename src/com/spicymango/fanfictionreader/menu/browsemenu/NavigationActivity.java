package com.spicymango.fanfictionreader.menu.browsemenu;

import com.spicymango.fanfictionreader.Settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;

public class NavigationActivity extends ActionBarActivity {
	public final static String EXTRA_SITE = "NavigationActivity.Extra Site";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);
		
		//Set fragment only on first execution
		if (savedInstanceState == null) {
			
			//Fetch the site id
			final int id = getIntent().getIntExtra(EXTRA_SITE, -1);
			
			//Sanity check
			if (id == -1)
				throw new RuntimeException("NavigationActivity - No site specified");
			
			Bundle args = new Bundle();
			args.putInt(EXTRA_SITE, id);
			
			Fragment frag = new ToggleFragment();
			frag.setArguments(args);
			
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(frag, null);
			ft.commit();			
		}
	}
	
}
