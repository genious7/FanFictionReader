package com.spicymango.fanfictionreader.menu.authormenu;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.TabActivity;
import com.spicymango.fanfictionreader.menu.authormenu.AuthorProfileLoader.FanFictionProfileLoader;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog;
import com.spicymango.fanfictionreader.util.Result;
import com.spicymango.fanfictionreader.util.adapters.TextAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity used to show the author's profile and written stories
 * Created by Michael Chen on 01/26/2016.
 */
public class AuthorMenuActivity extends TabActivity implements FilterDialog.FilterListener{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addTab(R.string.menu_author_stories, AuthorStoryFragment.class);
		addTab(R.string.menu_author_profile, ProfileFragment.class);
	}

	@Override
	public void onFilter(int[] selected) {
		// Forward the filter to the currently selected fragment
		Fragment fragment = getCurrentFragment();

		if (fragment != null && fragment instanceof FilterDialog.FilterListener){
			FilterDialog.FilterListener listener = (FilterDialog.FilterListener) fragment;
			listener.onFilter(selected);
		}
	}

	public static final class ProfileFragment extends Fragment implements LoaderManager.LoaderCallbacks<Result>, View.OnClickListener {

		private long mAuthorId;
		private List<Spanned> mList;
		private BaseAdapter mAdapter;

		/**
		 * The retry button shown upon connection failure
		 */
		private View mNoConnectionBar;
		/**
		 * The progress spinner displayed while the loader loads
		 */
		private View mProgressBar;

		private FanFictionProfileLoader mLoader;

		private static long authorId(Uri uri) {
			String segment = uri.getPathSegments().get(1);
			return Long.parseLong(segment);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
								 Bundle savedInstanceState) {

			mList = new ArrayList<>();
			mAdapter = new TextAdapter(getActivity(), mList);

			View v = inflater.inflate(R.layout.activity_list_view, container, false);
			ListView listView = (ListView) v.findViewById(android.R.id.list);
			View footer = inflater.inflate(R.layout.footer_list, null);
			listView.addFooterView(footer, null, false);
			listView.setAdapter(mAdapter);

			View addPageBtn = footer.findViewById(R.id.story_load_pages);
			addPageBtn.setVisibility(View.GONE);
			mProgressBar = footer.findViewById(R.id.progress_bar);
			mNoConnectionBar = footer.findViewById(R.id.row_retry);
			View retryButton = mNoConnectionBar.findViewById(R.id.btn_retry);
			retryButton.setOnClickListener(this);

			mAuthorId = authorId(getActivity().getIntent().getData());
			getLoaderManager().initLoader(0, savedInstanceState, this);

			return v;
		}

		@Override
		public Loader<Result> onCreateLoader(int id, Bundle args) {
			return new FanFictionProfileLoader(getActivity(), args, mAuthorId);
		}

		@Override
		public void onLoadFinished(Loader<Result> loader, Result data) {
			mLoader = (FanFictionProfileLoader) loader;
			switch (data) {
				case LOADING:
					mProgressBar.setVisibility(View.VISIBLE);
					mNoConnectionBar.setVisibility(View.GONE);
					break;
				case ERROR_CONNECTION:
					mProgressBar.setVisibility(View.GONE);
					mNoConnectionBar.setVisibility(View.VISIBLE);
					break;
				case SUCCESS:
					mProgressBar.setVisibility(View.GONE);
					mNoConnectionBar.setVisibility(View.GONE);

					mList.clear();
					mList.addAll(mLoader.mData);
					mAdapter.notifyDataSetChanged();

					((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(mLoader.mAuthor);
					break;

				default:
					break;
			}
		}

		@Override
		public void onLoaderReset(Loader<Result> loader) {
		}

		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
		public void onSaveInstanceState(Bundle outState) {
			int currentapiVersion = android.os.Build.VERSION.SDK_INT;
			if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				if (!getActivity().isChangingConfigurations()) {
					mLoader.onSavedInstanceState(outState);
				}
			} else {
				mLoader.onSavedInstanceState(outState);
			}
			super.onSaveInstanceState(outState);
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.btn_retry:
					mLoader.startLoading();
					break;
				default:
					break;
			}
		}

	}


}
