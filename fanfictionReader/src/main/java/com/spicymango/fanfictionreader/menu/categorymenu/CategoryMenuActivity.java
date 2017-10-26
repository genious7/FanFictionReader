package com.spicymango.fanfictionreader.menu.categorymenu;

import java.util.List;

import org.apache.commons.lang3.text.WordUtils;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.menu.BaseFragment;
import com.spicymango.fanfictionreader.menu.categorymenu.CategoryMenuLoaders.*;
import com.spicymango.fanfictionreader.menu.communitymenu.CommunityMenuActivity;
import com.spicymango.fanfictionreader.menu.storymenu.StoryMenuActivity;
import com.spicymango.fanfictionreader.util.Sites;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;

public class CategoryMenuActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_frame_layout);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
			fr.replace(R.id.content_frame, new CategoryMenuFragment());
			fr.commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public final static class CategoryMenuFragment extends BaseFragment<CategoryMenuItem> {

		private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

		private static final String STATE_SORT = "STATE_SORT";
		private static final int FANFICTION_REGULAR = 0;
		private static final int FANFICTION_CROSSOVER = 1;
		private static final int FANFICTION_SUB_CATEGORY = 2;
		private static final int FANFICTION_COMMUNITY = 3;
		private static final int ARCHIVE_OF_OUR_OWN_NORMAL = 4;
		private static final int FICTIONPRESS_COMMUNITY = 5;

		static {
			// Archive Of Our Own
			URI_MATCHER.addURI(Sites.ARCHIVE_OF_OUR_OWN.AUTHORITY, "media/*/fandoms", ARCHIVE_OF_OUR_OWN_NORMAL);

			// FanFiction Mobile Site
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "crossovers/*/", FANFICTION_CROSSOVER);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "crossovers/*/#/", FANFICTION_SUB_CATEGORY);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "communities/*/", FANFICTION_COMMUNITY);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "*/", FANFICTION_REGULAR);
			// FanFiction Desktop Site
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "crossovers/*/", FANFICTION_CROSSOVER);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "crossovers/*/#/", FANFICTION_SUB_CATEGORY);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "communities/*/", FANFICTION_COMMUNITY);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "*/", FANFICTION_REGULAR);

			// FictionPress Mobile Site
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "communities/*/", FICTIONPRESS_COMMUNITY);
			// FictionPress Desktop Site
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "communities/*/", FICTIONPRESS_COMMUNITY);
		}

		private LoaderAdapter<CategoryMenuItem> mLoaderAdapter;
		private CategoryMenuAdapter mAdapter;
		private boolean mSortOrder;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			setHasOptionsMenu(true);

			final Uri uri = getActivity().getIntent().getData();
			int site = URI_MATCHER.match(uri);

			switch (site) {
			case ARCHIVE_OF_OUR_OWN_NORMAL:
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					final Intent i = new Intent(getActivity(), StoryMenuActivity.class);
					i.setData(getItem(position).mUri);
					startActivity(i);
				});
				mLoaderAdapter = args -> new ArchiveOfOurOwnCategoryLoader(getActivity(), args, uri);
				setTitle(R.string.menu_navigation_title_regular);
				String subTitle = WordUtils.capitalize(uri.getPathSegments().get(1), ' ', '.', '\'');
				setSubTitle(subTitle.replace("*a*", "&"));
				break;
			case FANFICTION_REGULAR:
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					final Intent i = new Intent(getActivity(), StoryMenuActivity.class);
					i.setData(getItem(position).mUri);
					startActivity(i);
				});
				mLoaderAdapter = args -> new FanFictionRegularCategoryLoader(getActivity(), args, uri);
				setTitle(R.string.menu_navigation_title_regular);
				setSubTitle(WordUtils.capitalize(uri.getPathSegments().get(0), ' ', '.', '\''));
				break;
			case FANFICTION_CROSSOVER:
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					final Intent i = new Intent(getActivity(), CategoryMenuActivity.class);
					i.setData(getItem(position).mUri);
					startActivity(i);
				});
				mLoaderAdapter = args -> new FanFictionRegularCategoryLoader(getActivity(), args, uri);
				setTitle(R.string.menu_navigation_title_crossover);
				setSubTitle(WordUtils.capitalize(uri.getPathSegments().get(1), ' ', '.', '\''));
				break;
			case FANFICTION_SUB_CATEGORY:
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					final Intent i = new Intent(getActivity(), StoryMenuActivity.class);
					i.setData(getItem(position).mUri);
					startActivity(i);
				});
				mLoaderAdapter = args -> new FanFictionSubCategoryLoader(getActivity(), args, uri);
				setTitle(R.string.menu_navigation_title_crossover);
				setSubTitle(WordUtils.capitalize(uri.getPathSegments().get(1), ' ', '.', '\''));
				break;
			case FANFICTION_COMMUNITY:
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					final Intent i = new Intent(getActivity(), CommunityMenuActivity.class);
					i.setData(getItem(position).mUri);
					startActivity(i);
				});
				mLoaderAdapter = args -> new FanFictionCommunityCategoryLoader(getActivity(), args, uri);
				setTitle(R.string.menu_navigation_title_community);
				setSubTitle(WordUtils.capitalize(uri.getPathSegments().get(1), ' ', '.', '\''));
				break;
			case FICTIONPRESS_COMMUNITY:
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					final Intent i = new Intent(getActivity(), CommunityMenuActivity.class);
					i.setData(getItem(position).mUri);
					startActivity(i);
				});
				mLoaderAdapter = args -> new FictionPressCommunityCategoryLoader(getActivity(), args, uri);
				setTitle(R.string.menu_navigation_title_community);
				setSubTitle(WordUtils.capitalize(uri.getPathSegments().get(1), ' ', '.', '\''));
				break;
			default:
				throw new IllegalArgumentException("The uri " + uri + " is invalid.");
			}

			if (savedInstanceState != null) {
				mSortOrder = savedInstanceState.getBoolean(STATE_SORT);
				mListView.setFastScrollEnabled(mSortOrder == CategoryMenuComparator.SORT_ALPHABETICAL);
			} else {
				mSortOrder = CategoryMenuComparator.SORT_VIEWS;
			}

			getLoaderManager().initLoader(0, mLoaderArgs, this);
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putBoolean(STATE_SORT, mSortOrder);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			// Inflate the menu
			inflater.inflate(R.menu.navigation_menu, menu);

			MenuItem filter = menu.findItem(R.id.filter);
			if (mLoader != null && mLoader instanceof Filterable) {
				filter.setVisible(true);

				// Set alpha, since the drawable's alpha may have been changed
				// by other activities
				filter.getIcon().setAlpha(255);
			} else {
				filter.setVisible(false);
			}
		}

		@Override
		public void onLoadFinished(Loader<List<CategoryMenuItem>> loader, List<CategoryMenuItem> data) {
			super.onLoadFinished(loader, data);
			mAdapter.sort(mSortOrder);
			getActivity().supportInvalidateOptionsMenu();
		}

		@Override
		public void onResume() {
			super.onResume();
			getActivity().supportInvalidateOptionsMenu();
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case R.id.navigation_library_sort_by_name:
				if (mSortOrder != CategoryMenuComparator.SORT_ALPHABETICAL) {
					mListView.setFastScrollEnabled(true);
					mSortOrder = CategoryMenuComparator.SORT_ALPHABETICAL;
					mAdapter.sort(mSortOrder);
				}
				return true;
			case R.id.navigation_library_sort_by_size:
				if (mSortOrder != CategoryMenuComparator.SORT_VIEWS) {
					mListView.setFastScrollEnabled(false);
					mSortOrder = CategoryMenuComparator.SORT_VIEWS;
					mAdapter.sort(mSortOrder);
				}
				return true;
			case R.id.filter:
				displayFilterDialog();
				return true;
			default:
				return super.onOptionsItemSelected(item);
			}
		}

		/**
		 * Displays the dialog that handles filtering
		 */
		private void displayFilterDialog() {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			final Filterable loader = (Filterable) mLoader;

			String[] filterList = loader.getFilterEntries();

			builder.setItems(filterList, (dialog, position) -> {
				loader.onFilterSelected(position);
				mSortOrder = CategoryMenuComparator.SORT_ALPHABETICAL;
			});
			builder.show();
		}

		@Override
		public Loader<List<CategoryMenuItem>> onCreateLoader(int id, Bundle args) {
			return mLoaderAdapter.getNewLoader(args);
		}

		@Override
		protected BaseAdapter adapter(List<CategoryMenuItem> dataSet) {
			return mAdapter = new CategoryMenuAdapter(getActivity(), dataSet);
		}

		protected interface Filterable {

			String[] getFilterEntries();

			void onFilterSelected(int position);

		}

	}
}
