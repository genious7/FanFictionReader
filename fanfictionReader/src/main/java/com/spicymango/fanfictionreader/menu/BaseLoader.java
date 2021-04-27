package com.spicymango.fanfictionreader.menu;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import com.spicymango.fanfictionreader.util.Result;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.AsyncTaskLoader;

/**
 * An loader of {@link Parcelable} objects that caches the objects across state
 * changes.
 * 
 * @author Michael Chen
 * 
 * @param <T>
 *            A {@link Parcelable} type.
 */
public abstract class BaseLoader<T extends Parcelable> extends
		AsyncTaskLoader<List<T>> {
	private final static String STATE_LOADER = "STATE LOADER";
	private final static String STATE_CURRENT = "STATE CURRENT PAGE";
	private final static String STATE_DATA = "STATE CURRENT DATA";
	private final static String STATE_STATUS = "STATE STATUS";
	private final static String STATE_TOTAL = "STATE TOTAL PAGES";
	private String htmlFromWebView;
	
	/**
	 * An interface that must be implemented by any loader that offers
	 * filtering capabilities.
	 * 
	 * @author Michael Chen
	 *
	 */
	public interface Filterable {
		/**
		 * Called whenever the filter button is clicked
		 * @param activity The calling activity.
		 */
		void onFilterClick(FragmentActivity activity);

		/**
		 * Determines whether the filter can be displayed
		 * 
		 * @return True if the filter is available, false otherwise
		 */
		boolean isFilterAvailable();

		/**
		 * Called whenever a new filter is available. For checkboxes, the
		 * value will be zero when the check box is unchecked. For
		 * spinners, the integer returned is the selected position
		 * 
		 * @param filterSelected The selected positions for the filter.
		 */
		void filter(int[] filterSelected);
	}

	/**
	 * The page that is currently loaded
	 */
	private int mCurrentPage;

	/**
	 * A list containing the loaded data
	 */
	private ArrayList<T> mData;

	/**
	 * A list containing the old loaded data.
	 */
	private ArrayList<T> mDataOld;

	/**
	 * The current status of the loader
	 */
	private Result mStatus;

	/**
	 * The maximum number of pages
	 */
	private int mTotalPages;
	
	/**
	 * A boolean that tells if a loading bar will be shown between transitions.
	 */
	private boolean mDisableProgressBar;

	public BaseLoader(Context context, Bundle savedInstanceState) {
		super(context);

		mData = new ArrayList<>();
		mDataOld = new ArrayList<>();
		mDisableProgressBar = false;

		final String key = STATE_LOADER + getId();
		
		if (savedInstanceState == null || !savedInstanceState.containsKey(key)) {
			resetState();
		} else {
			Bundle state = savedInstanceState.getBundle(key);
			mTotalPages = state.getInt(STATE_TOTAL, 0);
			mCurrentPage = state.getInt(STATE_CURRENT, 1);
			mStatus = (Result) state.getSerializable(STATE_STATUS);
			mData = state.getParcelableArrayList(STATE_DATA);
		}
	}
	
	public final void disableProgressBar(){
		mDisableProgressBar = true;
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
	 * Gets the currently loaded page
	 */
	public final int getCurrentPage() {
		return mCurrentPage;
	}

	public final Result getState() {
		return mStatus;
	}

	/**
	 * Gets the total number of pages available
	 * 
	 * @return The total number of pages
	 */
	public final int getTotalPages() {
		return mTotalPages;
	}

	/**
	 * Finds if the last execution of the loader failed due to a connection
	 * error
	 * 
	 * @return True if a connection error occurred, false otherwise
	 */
	public final boolean hasConnectionError() {
		return mStatus == Result.ERROR_CONNECTION;
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
	 * Finds if the last execution of the loader failed due to a parsing error
	 * 
	 * @return True if a parsing error occurred, false otherwise
	 */
	public final boolean hasParseError() {
		return mStatus == Result.ERROR_PARSE;
	}

	/**
	 * Finds whether the loader is currently loading.
	 * 
	 * @return True if the loader is executing, false otherwise
	 */
	public final boolean isRunning() {
		return mStatus == Result.LOADING;
	}

	public final void setHtmlFromWebView(String html){
		htmlFromWebView = html;
	}

	@Override
	public final List<T> loadInBackground() {

		Document document;
		final Uri uri = getUri(mCurrentPage);

		if (htmlFromWebView == null) {

			// Check for Internet connection errors
			//try {
				if (uri == null) {
					// Assign the document an empty value
					document = new Document("");
				} else {
					mStatus = Result.ERROR_CLOUDFLARE_CAPTCHA;
					return null;
				}
		} else if (htmlFromWebView.equalsIgnoreCase("404")){
			mStatus = Result.ERROR_CONNECTION;
			htmlFromWebView = null;
			return null;
		} else{
			assert uri != null;
			document = Jsoup.parse(htmlFromWebView, uri.toString());
			htmlFromWebView = null;
		}

		// Check for parsing errors
		if (load(document, mDataOld)) {
			mStatus = Result.SUCCESS;

			// Check the total page number if required
			if (mTotalPages == 0) {
				mTotalPages = getTotalPages(document);
			}

			return mDataOld;
		} else {
			mStatus = Result.ERROR_PARSE;
			return null;
		}
	}

	/**
	 * Loads the next page
	 */
	public final void loadNextPage() {
		// Load the next age if it exists.
		if (mCurrentPage < mTotalPages) {
			htmlFromWebView = null;
			mCurrentPage++;
			mStatus = Result.LOADING;
			startLoading();
		} else {
			throw new IllegalStateException(
					"BaseLoader Attempted to load page number "
							+ (mCurrentPage + 1) + " when only " + mTotalPages
							+ " pages exist.");
		}
	}

	public final void saveInstanceState(Bundle outState) {
		Bundle bundle = new Bundle();
		bundle.putInt(STATE_CURRENT, mCurrentPage);
		bundle.putInt(STATE_TOTAL, mTotalPages);
		bundle.putParcelableArrayList(STATE_DATA, mData);
		bundle.putSerializable(STATE_STATUS, mStatus);
		outState.putBundle(STATE_LOADER + getId(), bundle);
		onSaveInstanceState(outState);
	}
	
	/**
	 * @param savedInstanceState  The bundle in which additional settings must be saved
	 */
	protected void onSaveInstanceState(Bundle savedInstanceState){
		
	}

	/**
	 * Resets the loader to its default state
	 */
	public void resetState() {
		htmlFromWebView = null;
		mCurrentPage = 1;
		mTotalPages = 0;
		mData.clear();
		mDataOld.clear();
		mStatus = Result.LOADING;
	}

	/**
	 * Swaps both lists
	 */
	private void swapLists() {
		ArrayList<T> tmp = mDataOld;
		mDataOld = mData;
		mData = tmp;
	}

	/**
	 * Parse the total number of pages from the document
	 * 
	 * @param document
	 *            The HTML of the web page
	 * @return The total number of pages
	 */
	protected abstract int getTotalPages(Document document);

	/**
	 * Gets the {@link Uri} that the loader should load. May be null if a connection is not
	 * required. If a null pointer is returned, the load method will be called with an empty
	 * document
	 *
	 * @param currentPage The page that should be loaded
	 * @return The complete Uri, including filters
	 */
	@Nullable
	protected abstract Uri getUri(int currentPage);

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
		// Reload the data if required.
		if (mStatus == Result.SUCCESS) {
			deliverResult(mData);
		} else {
			mStatus = mDisableProgressBar ? Result.LOADING_HIDE_PROGRESS : Result.LOADING;
			deliverResult(mData);
			forceLoad();
		}
	}
}
