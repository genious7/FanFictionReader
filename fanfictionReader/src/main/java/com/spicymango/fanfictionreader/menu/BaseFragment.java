package com.spicymango.fanfictionreader.menu;

import java.util.ArrayList;
import java.util.List;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.util.Result;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

public abstract class BaseFragment<T extends Parcelable> extends Fragment
		implements LoaderCallbacks<List<T>>, OnClickListener {

	/**
	 * Used to avoid a TransactionTooLargeException, the data is stored in a persistent fragment
	 */
	private DataFragment mDataFragment;

	/**
	 * The loader's saved instance state.
	 */
	protected Bundle mLoaderArgs;

	protected ListView mListView;
	private BaseAdapter mAdapter;

	private List<T> mList;

	// Views located in the footer
	private Button mAddPageButton;
	private View mErrorBar;
	private View mProgressBar;
	private TextView mRetryLabel;
	private FrameLayout mEmptyView;

	/**
	 * The most recently used {@link BaseLoader}
	 */
	protected BaseLoader<T> mLoader;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mList = new ArrayList<>();

		// Get the listView and the empty view
		View v = inflater.inflate(R.layout.activity_list_view, container, false);
		mListView = (ListView) v.findViewById(android.R.id.list);
		mEmptyView = (FrameLayout) v.findViewById(R.id.empty);

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
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// SavedInstanceState throws an TransactionTooLargeException when saving data,
		// so we save the data in a persistent fragment instead.
		// Create the persistent DataFragment if it doesn't exist and recover the saved bundle.
		mDataFragment = (DataFragment) getFragmentManager().findFragmentByTag("DATA");
		if (mDataFragment == null){
			mDataFragment = new DataFragment();
			getFragmentManager().beginTransaction().add(mDataFragment,"DATA").commit();
		}
		mLoaderArgs = mDataFragment.getData();
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

		// Make all the views invisible, then make the views visible as required.
		mProgressBar.setVisibility(View.GONE);
		mErrorBar.setVisibility(View.GONE);
		mAddPageButton.setVisibility(View.GONE);
		mEmptyView.setVisibility(View.GONE);

		// Unlike the footer, the listView should be visible by default. It is only
		// removed in order to show the empty view.
		mListView.setVisibility(View.VISIBLE);

		switch (state) {
			case LOADING:
				// Only the progress bar should be visible
				mProgressBar.setVisibility(View.VISIBLE);
				break;
			case LOADING_HIDE_PROGRESS:
				// No views should be visible. To be used for short local loads only
				break;
			case ERROR_CONNECTION:
				// Display a connection error
				mErrorBar.setVisibility(View.VISIBLE);
				mRetryLabel.setText(R.string.error_connection);
				break;
			case ERROR_PARSE:
				// Display a parsing error
				mErrorBar.setVisibility(View.VISIBLE);
				mRetryLabel.setText(R.string.error_parsing_mini);
				break;
			case SUCCESS:
				// If the loader has additional pages, display the button
				if (mLoader.hasNextPage()) {
					String text = getString(R.string.menu_story_page_button, mLoader.getCurrentPage() + 1,
											mLoader.getTotalPages());
					mAddPageButton.setVisibility(View.VISIBLE);
					mAddPageButton.setText(text);
				}

				// If an empty view has been set and the data set is empty, show the empty view.
				if (data.isEmpty() && mEmptyView.getChildCount() > 0){
					mListView.setVisibility(View.GONE);
					mEmptyView.setVisibility(View.VISIBLE);
				}
			default:
				break;
		}

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Since the savedInstanceState throws an error with large bundles, the state is saved
		// in a persistent fragment instead.
		final Bundle savedState = new Bundle();
		mLoader.saveInstanceState(savedState);
		mDataFragment.saveData(savedState);
	}

	protected abstract BaseAdapter adapter(List<T> dataSet);

	/**
	 * Sets the subTitle of the action bar.
	 * @param title The subTitle
	 */
	protected final void setSubTitle(@StringRes int title) {
		final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		if (actionBar != null) {
			actionBar.setSubtitle(title);
		} else {
			Log.e(this.getClass().getName(), "The subtitle cannot be changed because getSupportActionBar is null");
		}
	}

	/**
	 * Sets the subTitle of the action bar.
	 * @param title The subTitle
	 */
	protected final void setSubTitle(String title) {
		final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		if (actionBar != null) {
			actionBar.setSubtitle(title);
		} else {
			Log.e(this.getClass().getName(), "The subtitle cannot be changed because getSupportActionBar is null");
		}
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

	/**
	 * Sets a view that will be displayed in the center of the screen when the loader succeeds but
	 * returns an empty data set.
	 *
	 * @param emptyView The view that should be displayed upon an empty data set.
	 */
	public final void setEmptyView(View emptyView){
		mEmptyView.removeAllViews();
		mEmptyView.addView(emptyView);
	}
}
