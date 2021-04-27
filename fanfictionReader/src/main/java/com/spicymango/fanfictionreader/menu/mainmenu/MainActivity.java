package com.spicymango.fanfictionreader.menu.mainmenu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.dialogs.AboutDialog;

public class MainActivity extends AppCompatActivity implements OnNavigationItemSelectedListener {
	private static final String FIRST_TIME_USER = "MainActivity.FIRST_TIME_USER";
	private static boolean ENABLE_DRAWER = false;
	
	protected static final int INTENT_SETTINGS = 0;
	
	//TODO: Move
	public static final String EXTRA_PREF = "Resume pref";
	public static final String EXTRA_RESUME_ID = "Resume Id";
	
	private DrawerLayout mDrawer;
	private ActionBarDrawerToggle mDrawerToggle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Set the orientation and theme for the whole activity
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_navigation_drawer);

		//Set the toolbar as the action bar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		//Fill the navigation drawer
		setNavigationDrawer();
		
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.app_name, R.string.app_name);
		
		if (!ENABLE_DRAWER) {
			mDrawer.setEnabled(false);
			mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
			mDrawerToggle.setDrawerIndicatorEnabled(false);
		}		
		
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
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean(FIRST_TIME_USER, true) && ENABLE_DRAWER) {
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
		if (mDrawer.isDrawerOpen(GravityCompat.START)) {
			mDrawer.closeDrawers();
		}else{
			super.onBackPressed();
		}
	}

	/**
	 * Sets up the navigation drawer
	 */
	private void setNavigationDrawer() {
		mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

		final NavigationView view = (NavigationView) findViewById(R.id.drawer_list);
		view.setNavigationItemSelectedListener(this);
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

	@Override
	public boolean onNavigationItemSelected(MenuItem arg0) {
		FragmentTransaction ft;		
		switch (arg0.getItemId()) {
		case R.id.fanfiction: //FanFiction
			ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content_frame, new FanFictionMain());
			ft.commit();
			arg0.setChecked(true);
			break;
		case R.id.archive_of_our_own: //Archive of Our Own
			ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content_frame, new ArchiveOfOurOwnMain());
			ft.commit();
			arg0.setChecked(true);
			break;
		case R.id.fictionpress: //FictionPress
			ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.content_frame, new FictionPressMain());
			ft.commit();
			arg0.setChecked(true);
			break;
		case R.id.settings:
			Intent i = new Intent(this, Settings.class);
			startActivityForResult(i, INTENT_SETTINGS);
			break;
		case R.id.about:
			DialogFragment diag = new AboutDialog();
			diag.show(getSupportFragmentManager(), null);
			break;
		default:
			throw new RuntimeException("Unknown ID");
		}
		
		mDrawer.closeDrawers();		
		return true;
	}
}
