package com.spicymango.fanfictionreader.menu.reviewmenu;

import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.menu.BaseFragment;
import com.spicymango.fanfictionreader.menu.BaseLoader;
import com.spicymango.fanfictionreader.util.Sites;

import java.util.List;

/**
 * A class that is used to show FanFiction reviews.
 *
 * Created by Michael Chen on 01/17/2016.
 */
public class ReviewMenuActivity extends AppCompatActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_frame_layout);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		assert getSupportActionBar() != null;	// Make IntelliJ null check happy
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
			fr.replace(R.id.content_frame, new ReviewMenuFragment());
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

	public final static class ReviewMenuFragment extends BaseFragment<ReviewMenuItem>{
		private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		private static final int MATCHER_FF = 0;
		private static final int MATCHER_FP = 1;
		private static final int MATCHER_AO3 = 2;

		static{
			// FanFiction reviews
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "r/#/", MATCHER_FF);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "r/#/#/#/", MATCHER_FF);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "r/#/", MATCHER_FF);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "r/#/#/#/", MATCHER_FF);

			// FictionPress reviews
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "r/#/", MATCHER_FP);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "r/#/#/#/", MATCHER_FP);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "r/#/", MATCHER_FP);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "r/#/#/#/", MATCHER_FP);

			// Archive of Our Own Reviews
			URI_MATCHER.addURI(Sites.ARCHIVE_OF_OUR_OWN.AUTHORITY, "works/#", MATCHER_AO3);
		}

		private LoaderAdapter<ReviewMenuItem> mLoaderAdapter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			setTitle(R.string.menu_reviews_title);
			setHasOptionsMenu(true);

			// Display a textView if no reviews are found. This can occur when filtering by chapter
			final TextView empty = new TextView(getActivity());
			empty.setText(R.string.menu_reviews_no_reviews);
			empty.setGravity(Gravity.CENTER);
			empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			setEmptyView(empty);

			final Uri uri = getActivity().getIntent().getData();
			final int match = URI_MATCHER.match(uri);

			switch (match){
				case MATCHER_FF:
					mLoaderAdapter = new LoaderAdapter<ReviewMenuItem>() {
						@Override
						public BaseLoader<ReviewMenuItem> getNewLoader(Bundle args) {
							return new ReviewMenuLoaders.FanFictionReviewLoader(getActivity(), args, uri);
						}
					};
					break;
				case MATCHER_FP:
					break;
				case MATCHER_AO3:
					break;
			}

			getLoaderManager().initLoader(0, savedInstanceState, this);
		}

		@Override
		public void onLoadFinished(Loader<List<ReviewMenuItem>> loader, List<ReviewMenuItem> data) {
			super.onLoadFinished(loader, data);
			getActivity().supportInvalidateOptionsMenu();

			// If the loader succeeded,  update the subtitle
			if (mLoader instanceof ReviewMenuLoaders.TitleLoader) {
				ReviewMenuLoaders.TitleLoader titleLoader = (ReviewMenuLoaders.TitleLoader) mLoader;
				String subTitle = titleLoader.getTitle();
				if (subTitle != null){
					setSubTitle(subTitle);
				}
			}
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
				case R.id.filter:
					BaseLoader.Filterable filterable = (BaseLoader.Filterable) mLoader;
					filterable.onFilterClick(getActivity());
					return true;
				default:
					return super.onOptionsItemSelected(item);
			}
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.story_menu, menu);
		}

		@Override
		public void onPrepareOptionsMenu(Menu menu) {
			MenuItem filter = menu.findItem(R.id.filter);
			if (mLoader instanceof BaseLoader.Filterable) {
				// If the menu can be filtered, enable the icon if ready
				filter.setVisible(true);
				if (((BaseLoader.Filterable) mLoader).isFilterAvailable()) {
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
		public Loader<List<ReviewMenuItem>> onCreateLoader(int id, Bundle args) {
			return mLoaderAdapter.getNewLoader(args);
		}

		@Override
		protected BaseAdapter adapter(List<ReviewMenuItem> dataSet) {
			return new ReviewAdapter(getActivity(), dataSet);
		}
	}

}
