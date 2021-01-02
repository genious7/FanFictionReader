package com.spicymango.fanfictionreader.menu;

import java.util.ArrayList;
import java.util.List;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.util.Result;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public abstract class BaseActivity<T extends Parcelable> extends
		AppCompatActivity implements LoaderCallbacks<List<T>>, OnClickListener,
		OnItemClickListener, OnItemLongClickListener {

	/**
	 * The listview's adapter
	 */
	private BaseAdapter mAdapter;
	/**
	 * The button used to load the next page
	 */
	private Button mAddPageButton;
	/**
	 * The list of stories currently loaded
	 */
	protected List<T> mList;
	/**
	 * The StoryLoader that fills the list
	 */
	protected BaseLoader<T> mLoader;
	/**
	 * The retry button shown upon connection failure
	 */
	private View mErrorBar;
	/**
	 * The progress spinner displayed while the loader loads
	 */
	private View mProgressBar;
	
	private TextView mRetryLabel;

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.story_load_pages:
			mLoader.loadNextPage();
			break;
		case R.id.btn_retry:
			mLoader.startLoading();
			break;
		default:
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<List<T>> loader) {
		mList = null;
	}

	@Override
	public void onLoadFinished(Loader<List<T>> loader, List<T> data) {
		mList.clear();
		mList.addAll(data);
		mAdapter.notifyDataSetChanged();
		mLoader = (BaseLoader<T>) loader;
		
		Result state = mLoader.getState();		
		switch (state) {
		case LOADING:
			mProgressBar.setVisibility(View.VISIBLE);
			mAddPageButton.setVisibility(View.GONE);
			mErrorBar.setVisibility(View.GONE);
			break;
		case ERROR_CONNECTION:
			mRetryLabel.setText(R.string.error_connection);
			mProgressBar.setVisibility(View.GONE);
			mAddPageButton.setVisibility(View.GONE);
			mErrorBar.setVisibility(View.VISIBLE);
			break;
		case ERROR_PARSE:
			mRetryLabel.setText(R.string.error_parsing_mini);
			mProgressBar.setVisibility(View.GONE);
			mAddPageButton.setVisibility(View.GONE);
			mErrorBar.setVisibility(View.VISIBLE);
			break;
		default:
			mProgressBar.setVisibility(View.GONE);
			mErrorBar.setVisibility(View.GONE);
			
			if (mLoader.hasNextPage()) {
				String text = String.format(
						getString(R.string.menu_story_page_button),
						mLoader.getCurrentPage() + 1, mLoader.getTotalPages());
				mAddPageButton.setVisibility(View.VISIBLE);
				mAddPageButton.setText(text);
			} else {
				mAddPageButton.setVisibility(View.GONE);
			}
			
			break;
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			if (!isChangingConfigurations()) {
				mLoader.saveInstanceState(outState);
			}
		} else {
			mLoader.saveInstanceState(outState);
		}

		super.onSaveInstanceState(outState);
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_toolbar);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mList = new ArrayList<>();
		mAdapter = getAdapter();

		ListView listView = (ListView) findViewById(android.R.id.list);
		View footer = getLayoutInflater().inflate(R.layout.footer_list, null);
		listView.addFooterView(footer, null, false);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setAdapter(mAdapter);

		mAddPageButton = (Button) findViewById(R.id.story_load_pages);
		mAddPageButton.setOnClickListener(this);
		mProgressBar = findViewById(R.id.progress_bar);
		mErrorBar = findViewById(R.id.row_retry);

		mRetryLabel = (TextView) footer.findViewById(R.id.label_retry);
		View retryButton = footer.findViewById(R.id.btn_retry);
		retryButton.setOnClickListener(this);
	}

	protected abstract BaseAdapter getAdapter();
}
