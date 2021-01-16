package com.spicymango.fanfictionreader.menu.storymenu;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.text.WordUtils;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.activity.Site;
import com.spicymango.fanfictionreader.activity.reader.StoryDisplayActivity;
import com.spicymango.fanfictionreader.dialogs.DetailDialog;
import com.spicymango.fanfictionreader.menu.BaseFragment;
import com.spicymango.fanfictionreader.menu.BaseLoader.Filterable;
import com.spicymango.fanfictionreader.menu.storymenu.StoryMenuLoaders.*;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog.FilterListener;
import com.spicymango.fanfictionreader.util.Sites;
import com.spicymango.fanfictionreader.util.Story;

import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;

public class StoryMenuActivity extends AppCompatActivity implements FilterListener {
	
	private StoryMenuFragment mFragment; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_frame_layout);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

		final String tag = StoryMenuFragment.class.getName();
		if (savedInstanceState == null) {
			mFragment = new StoryMenuFragment();
			FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
			fr.replace(R.id.content_frame, mFragment, tag);
			fr.commit();
		} else {
			mFragment = (StoryMenuFragment) getSupportFragmentManager().findFragmentByTag(tag);
			if (mFragment == null) {
				throw new RuntimeException("StoryMenuActivity - onCreate: mFragment == null");
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home){
			onBackPressed();
			return true;
		}{
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onFilter(int[] selected) {
		mFragment.onFilter(selected);
	}
	
	public static final class StoryMenuFragment extends BaseFragment<Story>  implements FilterListener {
		// ArchiveOfOurOwn
		private static final int URI_AO3_NORMAL_MENU = 7;
		private static final int URI_AO3_COLLECTION_MENU = 8;
		// FanFiction
		private static final int URI_FF_NORMAL_MENU = 0;
		private static final int URI_FF_CROSSOVER_MENU = 1;
		private static final int URI_FF_JUST_IN_MENU = 2;
		private static final int URI_FF_COMMUNITY_MENU = 3;
		// FictionPress
		private static final int URI_FP_NORMAL_MENU = 4;
		private static final int URI_FP_JUST_IN_MENU = 5;
		private static final int URI_FP_COMMUNITY_MENU = 6;

		private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

		static {
			// Initializes the UriMatcher.

			// Archive Of Our Own Sites
			URI_MATCHER.addURI(Sites.ARCHIVE_OF_OUR_OWN.AUTHORITY, "tags/*/works", URI_AO3_NORMAL_MENU);
			URI_MATCHER.addURI(Sites.ARCHIVE_OF_OUR_OWN.AUTHORITY, "collections/*/works", URI_AO3_COLLECTION_MENU);

			// FanFiction Mobile Sites
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "j/", URI_FF_JUST_IN_MENU);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "community/*/#/", URI_FF_COMMUNITY_MENU);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "community/*/#/#/#/#/#/#/#/#/", URI_FF_COMMUNITY_MENU);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "*/#/#/", URI_FF_CROSSOVER_MENU);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "*/#/", URI_FF_NORMAL_MENU);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "*/*/", URI_FF_NORMAL_MENU);
			// FanFiction Desktop Sites
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "j/", URI_FF_JUST_IN_MENU);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "community/*/#/", URI_FF_COMMUNITY_MENU);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "community/*/#/#/#/#/#/#/#/#/", URI_FF_COMMUNITY_MENU);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "*/#/#/", URI_FF_CROSSOVER_MENU);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "*/#/", URI_FF_NORMAL_MENU);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "*/*/", URI_FF_NORMAL_MENU);

			// FictionPress Mobile Sites
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "j/", URI_FP_JUST_IN_MENU);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "community/*/#/", URI_FP_COMMUNITY_MENU);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "*/*/", URI_FP_NORMAL_MENU);
			// FictionPress Desktop Sites
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "j/", URI_FP_JUST_IN_MENU);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "community/*/#/", URI_FP_COMMUNITY_MENU);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "*/*/", URI_FP_NORMAL_MENU);
		}

		private LoaderAdapter<Story> mLoaderAdapter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			setHasOptionsMenu(true);

			final Uri uri = requireActivity().getIntent().getData();
			String subTitle;

			switch (URI_MATCHER.match(uri)) {
			case URI_AO3_NORMAL_MENU:
				setTitle(R.string.menu_navigation_title_regular);
				subTitle = uri.getPathSegments().get(1);
				mLoaderAdapter = args -> new AO3RegularStoryLoader(getActivity(), args, uri);
				break;
			case URI_AO3_COLLECTION_MENU:
				setTitle(R.string.menu_navigation_title_community);
				subTitle = uri.getPathSegments().get(1);
				break;
			case URI_FF_NORMAL_MENU:
				setTitle(R.string.menu_navigation_title_regular);
				subTitle = uri.getLastPathSegment();
				mLoaderAdapter = args -> new FFRegularStoryLoader(getActivity(), args, uri);
				mListView.setOnItemClickListener((parent, view, position, id) -> StoryDisplayActivity.openStory(getActivity(), id, Site.FANFICTION, true));
				break;
			case URI_FF_CROSSOVER_MENU:
				setTitle(R.string.menu_navigation_title_crossover);
				subTitle = uri.getPathSegments().get(0);
				mLoaderAdapter = args -> new FFRegularStoryLoader(getActivity(), args, uri);
				mListView.setOnItemClickListener((parent, view, position, id) -> StoryDisplayActivity.openStory(getActivity(), id, Site.FANFICTION, true));
				break;
			case URI_FF_JUST_IN_MENU:
				setTitle(R.string.menu_story_title_just_in);
				subTitle = "";
				mLoaderAdapter = args -> new FFJustInStoryLoader(getActivity(), args, uri);
				mListView.setOnItemClickListener((parent, view, position, id) -> StoryDisplayActivity.openStory(getActivity(), id, Site.FANFICTION, true));
				break;
			case URI_FF_COMMUNITY_MENU:
				setTitle(R.string.menu_navigation_title_community);
				subTitle = uri.getPathSegments().get(1).replace('-', ' ');
				mLoaderAdapter = args -> new FFCommunityStoryLoader(getActivity(), args, uri);
				mListView.setOnItemClickListener((parent, view, position, id) -> StoryDisplayActivity.openStory(getActivity(), id, Site.FANFICTION, true));
				break;
			case URI_FP_NORMAL_MENU:
				setTitle(R.string.menu_navigation_title_regular);
				subTitle = uri.getLastPathSegment();
				mLoaderAdapter = args -> new FPRegularStoryLoader(getActivity(), args, uri);
				break;
			case URI_FP_JUST_IN_MENU:
				setTitle(R.string.menu_story_title_just_in);
				subTitle = "";
				break;
			case URI_FP_COMMUNITY_MENU:
				setTitle(R.string.menu_navigation_title_community);
				subTitle = uri.getLastPathSegment();
				break;
			default:
				throw new IllegalArgumentException("The uri " + uri + " is invalid.");
			}

			mListView.setOnItemLongClickListener((parent, view, position, id) -> {
				DetailDialog.show(getActivity(), (Story) parent.getItemAtPosition(position));
				return true;
			});
			setSubTitle(WordUtils.capitalize(subTitle));
			LoaderManager.getInstance(this).initLoader(0, mLoaderArgs, this);
		}

		@Override
		public void onResume() {
			super.onResume();
			requireActivity().invalidateOptionsMenu();
		}

		@Override
		public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.story_menu, menu);
		}

		@Override
		public void onPrepareOptionsMenu(Menu menu) {
			MenuItem filter = menu.findItem(R.id.filter);
			if (mLoader instanceof Filterable) {
				// If the menu can be filtered, enable the icon if ready
				filter.setVisible(true);
				if (((Filterable) mLoader).isFilterAvailable()) {
					filter.setEnabled(true);
					filter.getIcon().setAlpha(255);
				} else {
					filter.setEnabled(false);
					filter.getIcon().setAlpha(64);
				}
			} else {
				// If the menu cannot be filtered, hide the icon.
				filter.setVisible(false);
			}
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			if (item.getItemId() == R.id.filter){
				Filterable filterable = (Filterable) mLoader;
				filterable.onFilterClick(getActivity());
				return true;
			} else{
				return super.onOptionsItemSelected(item);
			}
		}

		@NonNull
		@Override
		public Loader<List<Story>> onCreateLoader(int id, Bundle args) {
			return mLoaderAdapter.getNewLoader(args);
		}

		@Override
		public void onLoadFinished(@NonNull Loader<List<Story>> loader, List<Story> data) {
			super.onLoadFinished(loader, data);
			requireActivity().invalidateOptionsMenu();
		}

		@Override
		protected BaseAdapter adapter(List<Story> dataSet) {
			return new StoryMenuAdapter(getActivity(), dataSet);
		}

		@Override
		public void onFilter(int[] selected) {
			Filterable filterable = (Filterable) mLoader;
			filterable.filter(selected);
		}		
	}
}
