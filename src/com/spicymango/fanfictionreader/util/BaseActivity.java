package com.spicymango.fanfictionreader.util;

import java.util.ArrayList;
import java.util.List;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

public abstract class BaseActivity<T extends Parcelable> extends
		ActionBarActivity implements LoaderCallbacks<List<T>>, OnClickListener,
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
	private View mNoConnectionBar;
	/**
	 * The progress spinner displayed while the loader loads
	 */
	private View mProgressBar;
	/**
	 * Contains the base Uri.
	 */
	protected Uri BaseUri;
	/**
	 * Capitalizes the first letter of every word
	 * @param string The String to capitalize
	 * @return
	 */
	protected static final String capitalizeString(String string) {
		  char[] chars = string.toCharArray();
		  boolean found = false;
		  for (int i = 0; i < chars.length; i++) {
		    if (!found && Character.isLetter(chars[i])) {
		      chars[i] = Character.toUpperCase(chars[i]);
		      found = true;
		    } else if (Character.isWhitespace(chars[i]) || chars[i]=='.' || chars[i]=='\'') { // You can add other chars here
		      found = false;
		    }
		  }
		  return String.valueOf(chars);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.story_load_pages:
			mLoader.loadNextPage();
			break;
		case R.id.retry_internet_connection:
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
		
		if (mLoader.isRunning()) {
			mProgressBar.setVisibility(View.VISIBLE);
			mAddPageButton.setVisibility(View.GONE);
			mNoConnectionBar.setVisibility(View.GONE);
		}else if (mLoader.hasConnectionError()) {
			mProgressBar.setVisibility(View.GONE);
			mAddPageButton.setVisibility(View.GONE);
			mNoConnectionBar.setVisibility(View.VISIBLE);
		}else{
			mProgressBar.setVisibility(View.GONE);
			mNoConnectionBar.setVisibility(View.GONE);
			
			if (mLoader.hasNextPage()) {
				String text = String.format(getString(R.string.menu_story_page_button), mLoader.getCurrentPage() + 1, mLoader.getTotalPages());
				mAddPageButton.setVisibility(View.VISIBLE);
				mAddPageButton.setText(text);				
			}else{
				mAddPageButton.setVisibility(View.GONE);
			}
			
		}
		
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB){
			if (!isChangingConfigurations()) {
				mLoader.onSavedInstanceState(outState);
			}
		}else{
			mLoader.onSavedInstanceState(outState);
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
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Uri.Builder builder = new Uri.Builder();
		builder.scheme(getString(R.string.fanfiction_scheme))
				.authority(getString(R.string.fanfiction_authority));
		BaseUri = builder.build();

		mList = new ArrayList<T>();
		mAdapter = getAdapter();
		
		ListView listView = (ListView) findViewById(R.id.list);
		View footer = getLayoutInflater().inflate(R.layout.footer_list, null);
		listView.addFooterView(footer, null, false);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setAdapter(mAdapter);
		
		mAddPageButton = (Button) findViewById(R.id.story_load_pages);
		mAddPageButton.setOnClickListener(this);
		mProgressBar = findViewById(R.id.progress_bar); 
		mNoConnectionBar = findViewById(R.id.row_no_connection);
		
		View retryButton = findViewById(R.id.retry_internet_connection);
		retryButton.setOnClickListener(this);
	}
	
	protected abstract BaseAdapter getAdapter();
}
