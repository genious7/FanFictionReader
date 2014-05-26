package com.spicymango.fanfictionreader.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.DetailDisplay;
import com.spicymango.fanfictionreader.FilterMenu;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.Story;
import com.spicymango.fanfictionreader.util.StoryMenuAdapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

/**
 * An activity used to search for stories
 * @author Michael Chen
 */
public class SearchActivity extends ActionBarActivity implements
		OnItemLongClickListener, OnItemClickListener, LoaderCallbacks<List<Story>>,
		OnQueryTextListener, OnClickListener {
	
	private static final int LOADER_ID = 0;
	
	private static String fanfiction_scheme;
	private static String fafinction_authority;
	
	/**
	 * An array adapter used to display the stories on the screen
	 */
	private ArrayAdapter<Story> mAdapter;
	/**
	 * The list of stories to display
	 */
	private List<Story> mList;
	/**
	 * The list of filter values
	 */
	private ArrayList<LinkedHashMap<String, Integer>> filterList;
	/**
	 * The currently selected filter by key
	 */
	private int[] filter = {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	/**
	 * The currently selected filter by position
	 */
	private int[] selectedPositions = null;
	/**
	 * References to hide and show these views
	 */
	private View mProgressBar, mReconectRow, mAddPage;
	
	private SearchLoader mLoader; 
	
	@Override
	public void onClick(View v) {
		if (v == mAddPage) {
			mLoader.loadNextPage();
		}		
	}

	@Override
	public Loader<List<Story>> onCreateLoader(int arg0, Bundle arg1) {
		if (arg0 == LOADER_ID) {
			return new SearchLoader(this, filter);
		}
		return null;
	}

	/**
	 * Initializes the menu on the action bar.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search_menu, menu);

		SearchView sView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
		sView.setOnQueryTextListener(this);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent i = new Intent(this, StoryDisplayActivity.class);
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(fanfiction_scheme)
				.authority(fafinction_authority)
				.appendPath("s")
				.appendPath(id + "")
				.appendPath("1")
				.appendPath("");
		i.setData(builder.build());
		startActivity(i);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		Intent i = new Intent(this, DetailDisplay.class);
		i.putExtra(DetailDisplay.MAP, mList.get(position));
		startActivity(i);
		return true;
	}
	
	
	@Override
	public void onLoaderReset(Loader<List<Story>> arg0) {
		mList.clear();
	}
	
	@Override
	public void onLoadFinished(Loader<List<Story>> arg0, List<Story> arg1) {
		mLoader = (SearchLoader) arg0;
		mList.clear();
		mList.addAll(arg1);
		mAdapter.notifyDataSetChanged();
		filterList = mLoader.mFilterList;
		
		if (mLoader.isRunning()) {
			mProgressBar.setVisibility(View.VISIBLE);
			mAddPage.setVisibility(View.GONE);
			mReconectRow.setVisibility(View.GONE);
		}else if (mLoader.hasConnectionError()) {
			mProgressBar.setVisibility(View.GONE);
			mAddPage.setVisibility(View.GONE);
			mReconectRow.setVisibility(View.VISIBLE);
		}else{
			mProgressBar.setVisibility(View.GONE);
			mReconectRow.setVisibility(View.GONE);

			if (mLoader.mCurrentPage < mLoader.mTotalPages) {
				String text = String.format(getString(R.string.menu_story_page_button), mLoader.mCurrentPage + 1, mLoader.mTotalPages);
				mAddPage.setVisibility(View.VISIBLE);
				((Button)mAddPage).setText(text);				
			}else{
				mAddPage.setVisibility(View.GONE);
			}
			
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.filter:
			Intent i = new Intent(this, FilterMenu.class);

			// Generate the ordered KeySet, as putExtra does not support ordered
			// HashMaps
			ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>();
			for (int j = 0; j < filterList.size(); j++) {
				keys.add(new ArrayList<String>(filterList.get(j).keySet()));
			}

			i.putExtra(FilterMenu.STATE_FILTER_LIST, filterList);// HashMap
			i.putExtra(FilterMenu.KEYSET, keys);// Ordered KeySet
			i.putExtra(FilterMenu.SELECTED_KEYS, selectedPositions);// Position
																	// selected
																	// on
																	// previous
																	// filter,
																	// may equal
																	// null
			startActivityForResult(i, 1);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onQueryTextChange(String arg0) {
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String arg0) {
		mLoader.search(arg0);
		return false;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode==1){//Filter Menu
			if (resultCode==RESULT_CANCELED) {
				//Dialog cancelled
				Toast toast = Toast.makeText(this, getResources().getString(R.string.dialog_cancelled), Toast.LENGTH_SHORT);
				toast.show();
			}else if (resultCode == RESULT_OK) {
				filter = data.getIntArrayExtra(FilterMenu.STATE_FILTER_LIST);
				selectedPositions = data.getIntArrayExtra(FilterMenu.SELECTED_KEYS);
				mLoader.setFilter(filter);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Settings.setOrientation(this);
		
		fanfiction_scheme = getString(R.string.fanfiction_scheme);
		fafinction_authority = getString(R.string.fanfiction_authority);
		
		//Initialize variables
		ListView listView = (ListView)findViewById(R.id.list);
		mList = new ArrayList<Story>();
		mAdapter = new StoryMenuAdapter(this, mList);
		View footer = getLayoutInflater().inflate(R.layout.progress_bar, null);
		listView.addFooterView(footer,null,false);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setAdapter(mAdapter);
		
		
		
		mAddPage = (Button) footer.findViewById(R.id.story_load_pages);
		mAddPage.setOnClickListener(this);
		mProgressBar = footer.findViewById(R.id.progress_bar); 
		mReconectRow = footer.findViewById(R.id.row_no_connection);
		View retryButton = findViewById(R.id.retry_internet_connection);
		retryButton.setOnClickListener(this);

		getSupportLoaderManager().initLoader(LOADER_ID, null, this);
	}

	private static class SearchLoader extends AsyncTaskLoader<List<Story>>{
		/**
		 * The data that is loaded
		 */
		private List<Story> mData;
		/**
		 * True if the loader should reload, false otherwise
		 */
		private boolean dataHasChanged;
		/**
		 * True if a connection error has occurred, false otherwise
		 */
		private boolean connectionError;
		/**
		 * Represents the currently loaded page
		 */
		private int mCurrentPage;
		/**
		 * The total number of pages for the current result
		 */
		private int mTotalPages;
		
		/**
		 * A variable to store the current query
		 */
		private String mQuery;
		
		/**
		 * The list of filter values
		 */
		private ArrayList<LinkedHashMap<String, Integer>> mFilterList;
		
		private int[] filter;
		
		private Uri mUri;
		
		public SearchLoader(Context context, int[] currentFilter) {
			super(context);
			mData = new ArrayList<Story>();
			mUri = Uri.EMPTY;
			filter = currentFilter;
			dataHasChanged = false;
			connectionError = false;
			mTotalPages = 0;
			mCurrentPage = 1;
		}
		
		@Override
		public void deliverResult(List<Story> data) {		
			if (data == mData || data == null) {
				mData = new ArrayList<Story>(mData);
			}else{
				mData = data;
			}
			super.deliverResult(mData);
		}

		/**
		 * Determines if an Internet connection error has occurred.
		 * @return True if an error has occurred, false otherwise.
		 */
		public boolean hasConnectionError(){
			return connectionError;
		}
		
		/**
		 * Determines if the loader is currently running
		 * @return True if it is running, false otherwise
		 */
		public boolean isRunning(){
			return dataHasChanged && !connectionError;
		}
		
		@Override
		public List<Story> loadInBackground() {
			try {
				Document document = Jsoup.connect(mUri.toString()).get();
				
				if (mFilterList==null) { //Load the filters if they aren't already loaded.
					mFilterList = SearchFilter(document);
				}
				
				List<Story> tmpList;
				if (mCurrentPage == 1) {
					tmpList = new ArrayList<Story>();
					mTotalPages = getpageNumber(document);
				}else{
					tmpList = new ArrayList<Story>(mData);
				}
				tmpList.addAll(Parser.Stories(document));
				
				connectionError = false;
				dataHasChanged = false;
				return tmpList;
			} catch (IOException e) {
				connectionError = true;
			}
			return null;
		}
		
		/**
		 * Builds the {@link Uri} used for the query, with the appropriate filters applied
		 * @return The {@link Uri} requested
		 */
		private Uri assembleUri(){
			Uri.Builder builder = new Uri.Builder();
			builder.scheme(fanfiction_scheme)
					.authority(fafinction_authority)
					.path("search.php")
					.appendQueryParameter("type", "story")
					.appendQueryParameter("ready", "1")
					.appendQueryParameter("keywords", mQuery)
					.appendQueryParameter("categoryid", filter[13] + "")
					.appendQueryParameter("genreid", filter[2] + "")
					.appendQueryParameter("languageid", filter[5] + "")
					.appendQueryParameter("censorid", filter[4] + "")
					.appendQueryParameter("statusid", filter[5] + "")
					.appendQueryParameter("ppage", mCurrentPage + "")
					.appendQueryParameter("words", filter[6] + "");
			return builder.build();
		}
		
		public void search(String query){
			mQuery = query;
			Uri tmpUri = assembleUri();
			if (!mUri.equals(tmpUri)) {
				mUri = tmpUri;
				mData.clear();
				dataHasChanged = true;
				connectionError = false;
				mCurrentPage = 1;
				mTotalPages = 0;
				startLoading();
			}
		}
		
		public void setFilter(int[] filter){
			this.filter = filter;
			search(mQuery);
		}
		
		public void loadNextPage(){
			mCurrentPage++;
			mUri = assembleUri();
			dataHasChanged = true;
			connectionError = false;
			startLoading();
		}
	
		@Override
		protected void onStartLoading() {
			deliverResult(mData);
			if (dataHasChanged) {
				forceLoad();
			}
		}
		
		/**
		 * Loads the filters for the search menu.
		 * @param document
		 * @return
		 */
		private static ArrayList<LinkedHashMap<String, Integer>> SearchFilter(Document document){
			Elements form = document.select("div#content form > div#drop_m > select");
			
			Elements[] filter = {
					form.select("[title=sort options] > option"),
					form.select("[title=time range options] > option"),
					form.select("[name=genreid] > option"),
					form.select("[title=genre 2 filter] > option"),
					form.select("[name=censorid] > option"),
					form.select("[title=language filter] > option"),
					form.select("[name=words] > option"),
					form.select("[name=statusid] > option"),
					form.select("[title=character 1 filter] > option"),
					form.select("[title=character 2 filter] > option"),
					form.select("[title=character 3 filter] > option"),
					form.select("[title=character 4 filter] > option"),
					form.select("[name=s]:not([title]) > option"),
					form.select("[name=categoryid] > option"),
					form.select("[name=l] > option"),};
			
			ArrayList<LinkedHashMap<String, Integer>> list = new ArrayList<LinkedHashMap<String,Integer>>();		
			LinkedHashMap<String, Integer> TempMap = new LinkedHashMap<String, Integer>();
			
			for (Elements j : filter) {
				for (Element k : j) {
					TempMap.put(k.ownText(), Integer.valueOf(k.attr("value")));
				}
				list.add(TempMap);
				TempMap = new LinkedHashMap<String,Integer>();
			}
			return list;
		}
	
		private static final Pattern pattern = Pattern
				.compile("(?:&ppage=)(\\d{1,4}+)");// Search
		
		/**
		 * Gets the number of pages in the document
		 * @param document The parsed document
		 * @return The number of pages in the document
		 */
		private final static int getpageNumber(Document document){
			Elements elements = document.select("div#content a:matchesOwn(\\A(?i)last)");
			if (elements.isEmpty()){
				if (document.select("div#content a:matchesOwn(\\A(?i)next)").isEmpty())
					return 1;
				return 2;
			}
			return getpageNumber(elements.first().attr("href"));
			
		}
	
		/**
		 * Gets the page number from the Url
		 * @param url The Url to parse
		 * @return The current page
		 */
		private final static int getpageNumber(String url){
			Matcher matcher = pattern.matcher(url);
			matcher.find();
			for (int i = 1; i < matcher.groupCount() + 1; i++) {
				if (matcher.group(i) != null)
					return Integer.valueOf(matcher.group(i));
			}
			return 1;
		}
	}

}