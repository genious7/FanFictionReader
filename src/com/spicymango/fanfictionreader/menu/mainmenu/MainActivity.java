package com.spicymango.fanfictionreader.menu.mainmenu;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.dialogs.AboutDialog;
import com.spicymango.fanfictionreader.menu.mainmenu.NavigationAdapter.DrawerItem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainActivity extends ActionBarActivity implements OnItemClickListener {
	private static final String FIRST_TIME_USER = "MainActivity.FIRST_TIME_USER";
	private static final int INTENT_SETTINGS = 0;
	
	//TODO: Move
	public static final String EXTRA_PREF = "Resume pref";
	public static final String EXTRA_RESUME_ID = "Resume Id";
	
	private DrawerLayout mDrawer;
	private ActionBarDrawerToggle mDrawerToggle;
	
	private static final DrawerItem[] NAVIGATION_DRAWER = {
		new DrawerItem(0, R.string.site_fanfiction),
		new DrawerItem(1, R.string.site_archive),
		new DrawerItem(2, R.string.site_fictionpress),
		new DrawerItem(3, R.string.menu_button_settings),
		new DrawerItem(4, R.string.menu_button_about)
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		//Set the orientation and theme for the whole activity
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_navigation_drawer);
		
		//Initialize settings to default values upon the first access to the application
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		//Fill the navigation drawer
		setNavigationDrawer();
		
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

		// Only add the new fragment on the first execution
		if (savedInstanceState == null) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content_frame, new FanFictionMain());
			ft.commit();
		}
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
		
		//Start with the navigation drawer open on the first use
		final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		if (prefs.getBoolean(FIRST_TIME_USER, true)) {
			prefs.edit().putBoolean(FIRST_TIME_USER, false).commit();
			mDrawer.openDrawer(GravityCompat.START);
		}	
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			return mDrawerToggle.onOptionsItemSelected(item);
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		//Close the navigation drawer on back
		if (mDrawer.isDrawerOpen(Gravity.START)) {
			mDrawer.closeDrawers();
		}else{
			super.onBackPressed();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,	long id) {
		FragmentTransaction ft;		
		switch ((int)id) {
		case 0: //FanFiction
			ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content_frame, new FanFictionMain());
			ft.commit();
			break;
		case 1: //Archive of Our Own
			ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content_frame, new ArchiveOfOurOwnMain());
			ft.commit();
			break;
		case 2: //FictionPress
			ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content_frame, new FictionPressMain());
			ft.commit();
			break;
		case 3:
			Intent i = new Intent(this, Settings.class);
			startActivityForResult(i, INTENT_SETTINGS);
			break;
		case 4:
			DialogFragment diag = new AboutDialog();
			diag.show(getSupportFragmentManager(), null);
			break;
		default:
			throw new RuntimeException("Unknown ID");
		}
	}
	
	/**
	 * Sets up the navigation drawer
	 */
	private void setNavigationDrawer(){	
		mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		
		//Fill the drawer listview
		final ListView lv = (ListView) findViewById(R.id.drawer_list);
		lv.setAdapter(new NavigationAdapter(this, NAVIGATION_DRAWER));
		lv.setOnItemClickListener(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		
		//Restart the activity after returning from settings in order to refresh the theme
		case INTENT_SETTINGS:
			Intent intent = getIntent();
			finish();
			startActivity(intent);
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}
}
