package com.spicymango.fanfictionreader.menu;

import java.util.ArrayList;
import java.util.List;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.util.Result;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public abstract class BaseFragment<T extends Parcelable> extends Fragment
		implements LoaderCallbacks<List<T>>, OnClickListener {

	protected ListView mListView;
	private BaseAdapter mAdapter;

	private List<T> mList;

	// Views located in the footer
	private Button mAddPageButton;
	private View mErrorBar;
	private View mProgressBar;
	private TextView mRetryLabel;

	/**
	 * The most recently used {@link BaseLoader}
	 */
	protected BaseLoader<T> mLoader;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.activity_list_view, container, false);

		mList = new ArrayList<>();

		mListView = (ListView) v.findViewById(android.R.id.list);

		// Set the footer and its associated variables
		View footer = inflater.inflate(R.layout.footer_list, mListView, false);
		mProgressBar = footer.findViewById(R.id.progress_bar);
		mErrorBar = footer.findViewById(R.id.row_retry);
		mRetryLabel = (TextView) footer.findViewById(R.id.label_retry);
		mAddPageButton = (Button) footer.findViewById(R.id.story_load_pages);
		mAddPageButton.setOnClickListener(this);
		final View retryButton = footer.findViewById(R.id.btn_retry);
		retryButton.setOnClickListener(this);

		mListView.addFooterView(footer, null, false);

		mAdapter = adapter(mList);
		mListView.setAdapter(mAdapter);

		return v;
	}

	@Override
	public void onLoaderReset(Loader<List<T>> loader) {
		mList = null;
	}

	@Override
	public void onLoadFinished(Loader<List<T>> loader, List<T> data) {
		mLoader = (BaseLoader<T>) loader;

		mList.clear();
		mList.addAll(data);
		mAdapter.notifyDataSetChanged();

		// Get the current state. Use LOADING if the loader is not
		// available yet.
		Result state = mLoader.getState();

		switch (state) {
		case LOADING:
			mProgressBar.setVisibility(View.VISIBLE);
			mErrorBar.setVisibility(View.GONE);
			mAddPageButton.setVisibility(View.GONE);
			break;
		case LOADING_HIDE_PROGRESS:
			mProgressBar.setVisibility(View.GONE);
			mErrorBar.setVisibility(View.GONE);
			mAddPageButton.setVisibility(View.GONE);
			break;
		case ERROR_CONNECTION:
			mProgressBar.setVisibility(View.GONE);
			mErrorBar.setVisibility(View.VISIBLE);
			mAddPageButton.setVisibility(View.GONE);
			mRetryLabel.setText(R.string.error_connection);
			break;
		case ERROR_PARSE:
			mProgressBar.setVisibility(View.GONE);
			mErrorBar.setVisibility(View.VISIBLE);
			mAddPageButton.setVisibility(View.GONE);
			mRetryLabel.setText(R.string.error_parsing_mini);
			break;
		case SUCCESS:
			mProgressBar.setVisibility(View.GONE);
			mErrorBar.setVisibility(View.GONE);

			if (mLoader.hasNextPage()) {
				String text = getString(R.string.menu_story_page_button, mLoader.getCurrentPage() + 1,
						mLoader.getTotalPages());
				mAddPageButton.setVisibility(View.VISIBLE);
				mAddPageButton.setText(text);
			} else {
				mAddPageButton.setVisibility(View.GONE);
			}
		default:
			break;
		}

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mLoader.saveInstanceState(outState);
	}

	protected abstract BaseAdapter adapter(List<T> dataset);

	protected final void setSubTitle(@StringRes int title) {
		((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(title);
	}

	protected final void setSubTitle(String title) {
		((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(title);
	}

	protected final void setTitle(@StringRes int title) {
		getActivity().setTitle(title);
	}

	@Override
	public void onClick(View v) {
		if (mLoader == null) return;

		switch (v.getId()) {
		case R.id.btn_retry:
			mLoader.startLoading();
			break;
		case R.id.story_load_pages:
			mLoader.loadNextPage();
		default:
			break;
		}
	}

	protected T getItem(int position) {
		return mList.get(position);
	}

	/**
	 * Gets a loader
	 * @author Michael Chen
	 *
	 * @param <T> The type of data handled by the loader.
	 */
	protected interface LoaderAdapter<T extends Parcelable> {
		BaseLoader<T> getNewLoader(Bundle args);
	}
}
