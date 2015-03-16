package com.spicymango.fanfictionreader;

import com.spicymango.fanfictionreader.menu.mainmenu.FanFictionFragment;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {
	private DrawerLayout mDrawer;
	private ActionBarDrawerToggle mDrawerToggle;
	
	//TODO: Move
	public static final String EXTRA_PREF = "Resume pref";
	public static final String EXTRA_RESUME_ID = "Resume Id";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		//Set the orientation and theme for the whole application
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigation_drawer);
		
		//Initialize settings to default values upon the first access to the application
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		//Fill the navigation drawer
		mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		
		//Set the toolbar as the action bar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.app_name, R.string.app_name);
		
		// Prevents the app from double loading when opened from the market or
		// from some launchers.
		// See https://code.google.com/p/android/issues/detail?id=2373
		if (!isTaskRoot()) {
			Intent intent = getIntent();
			String action = intent.getAction();
			if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && action != null
					&& action.equals(Intent.ACTION_MAIN)) {
				finish();
				return;
			}
		}
		
		//Set the default fragment to the FanFiction fragment
		//TODO: remember last opened fragment
		if(getSupportFragmentManager().findFragmentById(android.R.id.content) == null){
			FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			ft.replace(R.id.content_frame, new FanFictionFragment());
			ft.commit();
		}
		
	}	
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		//If the user presses the navigation icon, open the navigation drawer
		if (mDrawerToggle.onOptionsItemSelected(item))
			return true;
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		if (mDrawer.isDrawerOpen(Gravity.START)) {
			mDrawer.closeDrawers();
		}else{
			super.onBackPressed();
		}
	}
}
