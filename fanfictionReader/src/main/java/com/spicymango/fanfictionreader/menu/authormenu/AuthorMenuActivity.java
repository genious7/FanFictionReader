package com.spicymango.fanfictionreader.menu.authormenu;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.TabActivity;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog;

/**
 * An activity used to show the author's profile and written stories
 * Created by Michael Chen on 01/26/2016.
 */
public class AuthorMenuActivity extends TabActivity implements FilterDialog.FilterListener{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Add the fragment that displays the author's stories as the first tab
		Bundle arguments = new Bundle();
		arguments.putInt(AuthorStoryFragment.EXTRA_LOADER_ID, AuthorStoryFragment.LOADER_STORIES);
		addTab(R.string.menu_author_stories, AuthorStoryFragment.class, arguments);

		// Display the author's profile as the second tab
		addTab(R.string.menu_author_profile, ProfileFragment.class);

		// Display the author's favorite stories as the third tab
		arguments = new Bundle();
		arguments.putInt(AuthorStoryFragment.EXTRA_LOADER_ID, AuthorStoryFragment.LOADER_FAVORITES);
		addTab(R.string.menu_author_favorites, AuthorStoryFragment.class, arguments);
	}

	@Override
	public void onFilter(int[] selected) {
		// Forward the filter to the currently selected fragment
		Fragment fragment = getCurrentFragment();

		if (fragment instanceof FilterDialog.FilterListener){
			FilterDialog.FilterListener listener = (FilterDialog.FilterListener) fragment;
			listener.onFilter(selected);
		}
	}


}
