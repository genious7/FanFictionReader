package com.spicymango.fanfictionreader.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.spicymango.fanfictionreader.R;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

/**
 * Represents the basic loader used throughout the application
 * 
 * @author Michael Chen
 * 
 * @param <T>
 */
public abstract class BaseLoader<T extends Parcelable> extends
		AsyncTaskLoader<List<T>> {
	
	private final static String STATE_CURRENT = "STATE CURRENT PAGE";
	private final static String STATE_TOTAL = "STATE TOTAL PAGES";
	private final static String STATE_DATA = "STATE CURRENT DATA";
	private final static String STATE_CONNECTION = "STATE CONNECTION";
	private final static String STATE_CHANGED = "STATE CHANGED";

	/**
	 * The page that is currently loaded
	 */
	private int mCurrentPage;

	/**
	 * A list containing the loaded data
	 */
	private ArrayList<T> mData;

	/**
	 * Shows if data has changed.
	 */
	protected boolean mDataHasChanged;

	/**
	 * A list containing the old loaded data
	 */
	private ArrayList<T> mDataOld;

	/**
	 * Stores whether if the last execution of the loader failed due to a
	 * connection error.
	 */
	private boolean mHasConnectionError;

	/**
	 * The total number of pages
	 */
	private int mTotalPages;

	/**
	 * Contains the base Uri for the FanFiction site
	 */
	protected final Uri BASE_URI;

	public BaseLoader(Context context, Bundle savedInstanceState) {
		super(context);
		
		mDataOld = new ArrayList<T>();
		
		if (savedInstanceState == null) {
			mTotalPages = 0;
			mCurrentPage = 1;
			mHasConnectionError = false;
			mDataHasChanged = true;
			mData = new ArrayList<T>();
		} else {
			mTotalPages = savedInstanceState.getInt(STATE_TOTAL, 0);
			mCurrentPage = savedInstanceState.getInt(STATE_CURRENT, 1);
			mHasConnectionError = savedInstanceState.getBoolean(
					STATE_CONNECTION, false);
			mDataHasChanged = savedInstanceState
					.getBoolean(STATE_CHANGED, true);

			if (savedInstanceState.containsKey(STATE_DATA)) {
				mData = savedInstanceState.getParcelableArrayList(STATE_DATA);
			}else{
				mData = new ArrayList<T>();
			}
		}
		
		// Generates the base Uri
		String scheme = context.getString(R.string.fanfiction_scheme);
		String authority = context.getString(R.string.fanfiction_authority);
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(scheme);
		builder.authority(authority);
		BASE_URI = builder.build();
	}
	
	public void onSavedInstanceState(Bundle outState){
		outState.putInt(STATE_CURRENT, mCurrentPage);
		outState.putInt(STATE_TOTAL, mTotalPages);
		outState.putParcelableArrayList(STATE_DATA, mData);
		outState.putBoolean(STATE_CONNECTION, mHasConnectionError);
		outState.putBoolean(STATE_CHANGED, mDataHasChanged);
	}

	@Override
	public final void deliverResult(List<T> data) {
		// Do not return any value if the Loader is not started
		if (isReset()) {
			return;
		}

		// Swaps the lists if necessary in order to ensure that a different list
		// is returned every time
		if (data == null || data == mData) {
			mDataOld.clear();
			mDataOld.addAll(mData);
		}
		swapLists();

		super.deliverResult(mData);
	}

	/**
	 * Finds if the last execution of the loader failed due to a connection
	 * error
	 * 
	 * @return True if a connection error occurred, false otherwise
	 */
	public final boolean hasConnectionError() {
		return mHasConnectionError;
	}

	/**
	 * Finds if another page is available
	 * 
	 * @return True if there is one or more pages remaining
	 */
	public final boolean hasNextPage() {
		return mCurrentPage < mTotalPages;
	}

	/**
	 * Finds whether the loader is currently loading.
	 * 
	 * @return True if the loader is executing, false otherwise
	 */
	public final boolean isRunning() {
		return mDataHasChanged && !mHasConnectionError;
	}

	@Override
	public final List<T> loadInBackground() {
		try {
			Document document = Jsoup.connect(
					formatUri(mCurrentPage).toString()).get();

			if (mCurrentPage == 1) {
				mDataOld.clear();
			}
			
			if (mTotalPages == 0) {
				mTotalPages = getTotalPages(document);
			}

			if (load(document, mDataOld)) {
				mDataHasChanged = false;
				return mDataOld;
			} else {
				mHasConnectionError = true;
				return null;
			}

		} catch (IOException e) {
			mHasConnectionError = true;
			return null;
		}
	}

	/**
	 * Loads the next page
	 */
	public final void loadNextPage() {
		if (mCurrentPage < mTotalPages) {
			mCurrentPage++;
			mDataHasChanged = true;
			startLoading();
		} else {
			Log.e("BaseLoader - loadNextPage", "Attempted to load page number "
					+ (mCurrentPage + 1) + " when only " + mTotalPages
					+ " pages exist.");
		}
	}

	/**
	 * Swaps both lists
	 */
	private final void swapLists() {
		ArrayList<T> tmp = mDataOld;
		mDataOld = mData;
		mData = tmp;
	}

	/**
	 * Adds qualifiers to the Uri
	 * 
	 * @param currentPage
	 *            The page that should be loaded
	 * @return The complete Uri, including filters
	 */
	protected abstract Uri formatUri(int currentPage);

	/**
	 * Parse the total number of pages from the document
	 * 
	 * @param document
	 *            The HTML of the web page
	 * @return The total number of pages
	 */
	protected abstract int getTotalPages(Document document);

	/**
	 * Loads the data
	 * 
	 * @param document
	 *            The HTML of the web page
	 * @param list
	 *            The list objects must be added to
	 * @return True if the load succeeded, false if an error occurred
	 */
	protected abstract boolean load(Document document, List<T> list);

	@Override
	protected final void onStartLoading() {
		mHasConnectionError = false;
		deliverResult(mData);
		if (mDataHasChanged) {
			forceLoad();
		}
	}

	/**
	 * Gets the currently loaded page
	 */
	public final int getCurrentPage(){
		return mCurrentPage;
	}
	
	/**
	 * Gets the total number of pages available
	 * @return The total number of pages
	 */
	public final int getTotalPages(){
		return mTotalPages;
	}

	/**
	 * Resets the loader
	 */
	public void resetState(){
		mCurrentPage = 1;
		mTotalPages = 0;
		mData.clear();
		mDataHasChanged = true;
	}
}
