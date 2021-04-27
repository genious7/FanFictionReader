package com.spicymango.fanfictionreader.menu;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity that automatically adds a tab layout; each tab will be linked to a single fragment.
 * Created by Michael Chen on 01/26/2016.
 */
public abstract class TabActivity extends AppCompatActivity{
	private static final String STATE_SELECTED_TAB = "selected tab";
	private static final String STATE_TOTAL_TABS = "number of tabs";

	/**
	 * A list of the fragment classes, ordered in the same order the tabs are displayed
	 */
	private List<Class<? extends Fragment>> mFragmentClasses;

	/**
	 * A list of the arguments for the different fragments. If a fragment has no arguments, the list
	 * must contain a null element for that element
	 */
	private List<Bundle> mFragmentArguments;

	private TabLayout mTabLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tab_layout);

		// Set the action bar
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		assert getSupportActionBar() != null; // Make IntelliJ null check happy
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Set the tab properties
		mTabLayout = findViewById(R.id.tab_layout);
		mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
		mTabLayout.addOnTabSelectedListener(mTabListener);

		// Instantiate Fields
		mFragmentClasses = new ArrayList<>();
		mFragmentArguments = new ArrayList<>();
	}

	/**
	 * Adds a new tab to the {@link TabLayout}. The target fragment will be instantiated when the
	 * Tab is selected by the user.
	 *
	 * @param tabTitle The text to be displayed in the tab
	 * @param fragment The fragment that should be instantiated when the tab is selected
	 * @param arguments The fragment's arguments
	 */
	protected final void addTab(@StringRes int tabTitle, Class<? extends Fragment>fragment, Bundle arguments) {
		// Store the fragment type so that it can be instantiated later on
		mFragmentClasses.add(fragment);

		// Store the fragment arguments
		mFragmentArguments.add(arguments);

		// Create and add the new tab
		TabLayout.Tab tab = mTabLayout.newTab();
		tab.setText(tabTitle);
		mTabLayout.addTab(tab);
	}

	/**
	 * Adds a new tab to the {@link TabLayout}. The target fragment will be instantiated when the
	 * Tab is selected by the user.
	 *
	 * @param tabTitle The text to be displayed in the tab
	 * @param fragment The fragment that should be instantiated when the tab is selected
	 */
	protected final void addTab(@StringRes int tabTitle, Class<? extends Fragment> fragment) {
		addTab(tabTitle, fragment, null);
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SELECTED_TAB, mTabLayout.getSelectedTabPosition());
		outState.putInt(STATE_TOTAL_TABS, mTabLayout.getTabCount());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// Select the tab that was previously selected
		final int selectedTabIndex = savedInstanceState.getInt(STATE_SELECTED_TAB);
		final TabLayout.Tab selectedTab = mTabLayout.getTabAt(selectedTabIndex);
		if (selectedTab == null) throw new IllegalStateException("The number of tabs cannot change");
		selectedTab.select();

		// When the activity is recreated, all hidden fragments are shown again. To fix this, hide
		// every fragment except the currently selected one
		int totalFragments = savedInstanceState.getInt(STATE_TOTAL_TABS);

		// All tabs must be added in the onCreate. Dynamically adding and removing tabs is not supported.
		if (totalFragments != mTabLayout.getTabCount())
			throw new IllegalStateException("The number of tabs cannot change");

		FragmentManager manager = getSupportFragmentManager();
		for (int i = 0; i < totalFragments; i++){
			if (i == selectedTabIndex) continue;

			Fragment fr = manager.findFragmentByTag(Integer.toString(i));
			if (fr != null){
				FragmentTransaction ft = manager.beginTransaction();
				ft.hide(fr);
				ft.commit();
			}
		}
	}

	/**
	 * Gets the currently selected fragment. If no fragments have been defined, this returns null
	 * @return The currently selected fragment
	 */
	@Nullable
	protected final Fragment getCurrentFragment(){
		final int selectedPosition = mTabLayout.getSelectedTabPosition();
		if (selectedPosition == -1)
			return null;
		else{
			final FragmentManager manager = getSupportFragmentManager();
			final String fragmentTag = Integer.toString(selectedPosition);
			return manager.findFragmentByTag(fragmentTag);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home){
			onBackPressed();
			return true;
		} else{
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * A simple {@link TabLayout.OnTabSelectedListener} that will change the currently shown
	 * fragment whenever a tab is selected.
	 */
	private final TabLayout.OnTabSelectedListener mTabListener = new TabLayout.OnTabSelectedListener() {
		@Override
		public void onTabSelected(TabLayout.Tab tab) {
			// Get the position of the tab, which is used as a tag for the fragment and as the index
			// to determine what type of fragment to instantiate
			final int tabPosition = tab.getPosition();
			final String fragmentTag = Integer.toString(tabPosition);

			// Try to obtain the fragment, if it exists
			final FragmentManager manager = getSupportFragmentManager();
			Fragment fragment = manager.findFragmentByTag(fragmentTag);

			final FragmentTransaction ft = manager.beginTransaction();

			if (fragment == null){
				// The fragment does not exist. Create a new instance of the fragment
				final String fragmentName = mFragmentClasses.get(tabPosition).getName();
				final Bundle fragmentArgs = mFragmentArguments.get(tabPosition);

				final FragmentFactory fragmentFactory = manager.getFragmentFactory();
				fragment = fragmentFactory.instantiate(ClassLoader.getSystemClassLoader(), fragmentName);
				fragment.setArguments(fragmentArgs);
				ft.add(R.id.content_frame, fragment, fragmentTag);
			} else{
				// The fragment already exists. Just show it
				ft.show(fragment);
			}
			ft.commit();
		}

		@Override
		public void onTabUnselected(TabLayout.Tab tab) {
			// If something is not being unselected, do nothing. Note that tab may be null if opening
			// an activity.
			if (tab == null) {
				return;
			}

			// Hide the existing fragment
			final int tabPosition = tab.getPosition();
			final String fragmentTag = Integer.toString(tabPosition);

			// Try to obtain the current fragment, if it exists
			final FragmentManager manager = getSupportFragmentManager();
			final Fragment selectedFragment = manager.findFragmentByTag(fragmentTag);

			// If a fragment was returned, hide it
			if (selectedFragment != null){
				final FragmentTransaction ft = manager.beginTransaction();
				ft.hide(selectedFragment);
				ft.commit();
			}
		}

		@Override
		public void onTabReselected(TabLayout.Tab tab) {

		}
	};
}
