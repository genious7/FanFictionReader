package com.crazymango.fanfictionreader.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.crazymango.fanfictionreader.DetailDisplay;
import com.crazymango.fanfictionreader.FilterMenu;
import com.crazymango.fanfictionreader.R;
import com.crazymango.fanfictionreader.Settings;
import com.crazymango.fanfictionreader.util.Parser;
import com.crazymango.fanfictionreader.util.Story;
import com.crazymango.fanfictionreader.util.StoryMenuAdapter;

import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
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

public class StoryMenuActivity extends ActionBarActivity implements LoaderCallbacks<List<Story>> , OnClickListener, OnItemClickListener, OnItemLongClickListener{
	
	private static final String FANFIC_AUTHORITY = "m.fanfiction.net";
	private static final String FANFIC_SCHEME = "https";
	private final static String STATE_CURRENT_FILTER = "CurrentFilter";
	private static final int STORY_LOADER = 0;
	
	private static final int URI_COMMUNITY_MENU = 3;
	private static final int URI_CROSSOVER_MENU = 1;
	private static final int URI_JUST_IN_MENU = 2;
	private static final UriMatcher URI_MATCHER = getUriMatcher();
	private static final int URI_NORMAL_MENU = 0;
	
	/**
	 * Retrieves the Uri Matcher for this activity
	 * @return The UriMatcher
	 */
	private static UriMatcher getUriMatcher(){
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);		
		matcher.addURI(FANFIC_AUTHORITY, "j/", URI_JUST_IN_MENU);
		matcher.addURI(FANFIC_AUTHORITY, "community/*/#/", URI_COMMUNITY_MENU);
		matcher.addURI(FANFIC_AUTHORITY, "community/*/#/#/#/#/#/#/#/#/", URI_COMMUNITY_MENU);
		matcher.addURI(FANFIC_AUTHORITY, "*/#/#/", URI_CROSSOVER_MENU);
		matcher.addURI(FANFIC_AUTHORITY, "*/*/", URI_NORMAL_MENU);
		return matcher;
	}
	
	/**
	 * Capitalizes the first letter of every word
	 * @param string
	 * @return
	 */
	public static final String capitalizeString(String string) {
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
	
	private int[] filter = {1,0,0,0,10,0,0,0,0,0,0,0,0,0};//Selected filter, default values applied.
	
	/**
	 * Contains the filter texts and keys
	 */
	private ArrayList<LinkedHashMap<String, Integer>> filterList; //Filter elements
	/**
	 * The listview's adapter
	 */
	private ArrayAdapter<Story> mAdapter;
	/**
	 * The button used to load the next page
	 */
	private Button mAddPageButton;
	/**
	 * The list of stories currently loaded
	 */
	private List<Story> mList;
	/**
	 * The StoryLoader that fills the list
	 */
	private StoryLoader mLoader;
	/**
	 * The retry button shown upon connection failure
	 */
	private View mNoConnectionBar;
	/**
	 * The progress spinner displayed while the loader loads
	 */
	private View mProgressBar;
	/**
	 * The uri of the currently loaded page
	 */
	private Uri mUri;
	/**
	 * The currently selected items in the filter
	 */
	private int[] selectedPositions = null;
	
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
	public Loader<List<Story>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case STORY_LOADER:
			mLoader = new StoryLoader(this,mUri, filter);
			return mLoader;
		default:
			return null;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.story_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem filter = menu.findItem(R.id.read_story_filter);
		if (filterList != null) {
			filter.setEnabled(true);
			filter.getIcon().setAlpha(255);
		}else{
			filter.setEnabled(false);
			filter.getIcon().setAlpha(64);	
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(FANFIC_SCHEME)
		.authority(FANFIC_AUTHORITY)
		.appendPath("s")//Story
		.appendPath("" + id)//Id
		.appendPath("1")//Chapter 1
		.appendPath("");//Adds the '/'
		
		Intent i = new Intent(this, StoryDisplayActivity.class);
		i.setData(builder.build());
		startActivity(i);	
		
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		Intent i = new Intent(this,DetailDisplay.class);
		i.putExtra(DetailDisplay.MAP,mList.get(position));
		startActivity(i);
		return true;
	}

	@Override
	public void onLoaderReset(Loader<List<Story>> loader) {
		mList = null;
	}

	@Override
	public void onLoadFinished(Loader<List<Story>> loader, List<Story> data) {
		mList.clear();
		mList.addAll(data);
		mAdapter.notifyDataSetChanged();
		
		mLoader = (StoryLoader) loader;
		if (filterList == null) {
			supportInvalidateOptionsMenu();
		}
		filterList = mLoader.filterList;
		
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

			if (mLoader.mCurrentPage < mLoader.mTotalPages) {
				String text = String.format(getString(R.string.menu_story_page_button), mLoader.mCurrentPage + 1, mLoader.mTotalPages);
				mAddPageButton.setVisibility(View.VISIBLE);
				mAddPageButton.setText(text);				
			}else{
				mAddPageButton.setVisibility(View.GONE);
			}
			
		}
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(FilterMenu.STATE_FILTER_LIST, filterList);
		outState.putIntArray(STATE_CURRENT_FILTER, filter);
		super.onSaveInstanceState(outState);
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.read_story_filter:
			Intent i = new Intent(this,FilterMenu.class);
			
			//Generate the ordered KeySet, as putExtra does not support ordered HashMaps
			ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(); 
			for (int j = 0; j < filterList.size(); j++) {
				keys.add(new ArrayList<String>(filterList.get(j).keySet()));
			}
			
			i.putExtra(FilterMenu.STATE_FILTER_LIST, filterList);//HashMap
			i.putExtra(FilterMenu.KEYSET, keys);//Ordered KeySet
			i.putExtra(FilterMenu.SELECTED_KEYS, selectedPositions);//Position selected on previous filter, may equal null 
			startActivityForResult(i, 1);	
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
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
				if (!filter.equals(mLoader.filter)) {
					mLoader.filter = filter;
					mLoader.mData = new ArrayList<Story>();
					mLoader.setUri(mUri);
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Settings.setOrientation(this);
		
		mUri = getIntent().getData();
		mList = new ArrayList<Story>();
		mAdapter = new StoryMenuAdapter(this, R.layout.story_menu_list_item, mList);
		
		ListView listView = (ListView)findViewById(R.id.list);
		listView.addFooterView(getLayoutInflater().inflate(R.layout.progress_bar, null),null,false);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setAdapter(mAdapter);
		
		mAddPageButton = (Button) findViewById(R.id.story_load_pages);
		mAddPageButton.setOnClickListener(this);
		mProgressBar = findViewById(R.id.progress_bar); 
		mNoConnectionBar = findViewById(R.id.row_no_connection);
		
		setTitle();
		
		if (savedInstanceState != null){
			filterList = (ArrayList<LinkedHashMap<String, Integer>>) savedInstanceState.getSerializable(FilterMenu.STATE_FILTER_LIST);
			filter = savedInstanceState.getIntArray(STATE_CURRENT_FILTER);
		}
		
		getSupportLoaderManager().initLoader(STORY_LOADER, null, this);
	}
	
	/**
	 * Sets the title and the sub title of the activity.
	 * @throws IllegalStateException Invalid Uri
	 */
	private void setTitle(){
		String subTitle;
		int title;
		
		switch (URI_MATCHER.match(mUri)) {
		case -1:	
			throw new IllegalStateException("The Uri " + mUri + " is invalid");
		case URI_NORMAL_MENU:
			title = R.string.menu_navigation_title_regular;
			subTitle = mUri.getLastPathSegment();
			break;
		case URI_CROSSOVER_MENU:
			title = R.string.menu_navigation_title_crossover;
			subTitle = mUri.getPathSegments().get(0);
			subTitle.replaceAll("-", " ");
			break;
		case URI_JUST_IN_MENU:
			title = R.string.menu_story_title_just_in;
			subTitle = "";
			break;
		case URI_COMMUNITY_MENU:
		default:
			title = R.string.menu_navigation_title_community;
			subTitle = mUri.getPathSegments().get(1);
			subTitle.replaceAll("-", " ");
			filter = new int[]{0,0,0,0,99,0,0,0,0,0,0,0,0,0};
			break;
		}
		setTitle(title);
		getSupportActionBar().setSubtitle(capitalizeString(subTitle));
	}

	private static class StoryLoader extends AsyncTaskLoader<List<Story>>{
		private static final  Pattern pattern = Pattern.compile(
				"/s/([\\d]++)/");
		
		private static final  Pattern pattern2 = Pattern.compile(
				"(?:&p=)(\\d{1,4}+)"//Normal
				+ "|(?:communit[^/]*+/(?:[^/]*+/){4})(\\d{1,4}+)"//Communities
				+ "|(?:&ppage=)(\\d{1,4}+)");//Search
		
		/**
		 * Gets the number of pages in the document
		 * @param document The parsed document
		 * @return The number of pages in the document
		 */
		public final static int getpageNumber(Document document){
			Elements elements = document.select("div#content a:matchesOwn(\\A(?i)last\\Z)");
			if (elements.isEmpty()){
				if (document.select("div#content a:matchesOwn(\\A(?i)next\\Z)").isEmpty())
					return 1;
				return 2;
			}
			return getpageNumber(elements.first().attr("href"));
			
		}
		
		/**
		 * Gets the page number in the url
		 * @param url The url to parse
		 * @return The current page
		 */
		private final static int getpageNumber(String url){
			Matcher matcher = pattern2.matcher(url);
			matcher.find();
			for (int i = 1; i < matcher.groupCount(); i++) {
				if (matcher.group(i) != null)
					return Integer.valueOf(matcher.group(i));
			}
			return 1;
		}
		
		private int[] filter;
		
		private ArrayList<LinkedHashMap<String, Integer>> filterList; //Filter elements
		
		private int mCurrentPage;
		
		private List<Story> mData;
		
		private boolean mHasChanged;
		
		private boolean mHasConnectionError;

		private int mTotalPages;

		private Uri mUri;

		/**
		 * Creates a new storyLoader
		 * @param context The context of the activity
		 * @param uri THe uri to use.
		 */
		public StoryLoader(Context context , Uri uri, int[] filter) {
			super(context);
			resetState();
			this.filter = filter;
			mUri = uri;
		}
		
		/**
		 * Delivers the data to the main activity
		 * @param data The list of Stories to deliver
		 */
		@Override
		public void deliverResult(List<Story> data) {
			if (isReset()) {
				return;
			}
			if (data == null) {
				if (mData == null) {
					mData = new ArrayList<Story>();
				}else{
					mData = new ArrayList<Story>(mData);
				}
			} else if(mData == data){
				mData = new ArrayList<Story>(data);
			}else{
				mData = data;
			}
			super.deliverResult(mData);
		}
		
		/**
		 * Finds if the last loading attempt had a connection error.
		 * @return True if there was a connection error, false otherwise
		 */
		public boolean hasConnectionError() {
			return mHasConnectionError;
		}
		
		/**
		 * Gets the current loader state
		 * @return True if the loader is currently running, false otherwise
		 */
		public boolean isRunning() {
			return mHasChanged & !mHasConnectionError;
		}
		
		/**
		 * Loads all the data asynchronously.
		 * @return A list of stories to load
		 */
		@Override
		public List<Story> loadInBackground() {
			try {
				Document document = Jsoup.connect(formatUri().toString()).get();
				
				if (mTotalPages == 0) {
					mTotalPages = Math.max(getpageNumber(document),mCurrentPage);
				}
				
				if (filterList==null) { //Load the filters if they aren't already loaded.
					filterList=Parser.Filter(document);
				}
				
				mHasConnectionError = false;
				mHasChanged = false;
				return parseStories(document);
				
			} catch (IOException e) {
				mHasConnectionError = true;
				return null;
			}
		}

		/**
		 * Loads the next page
		 */
		public void loadNextPage(){
			if (mCurrentPage < mTotalPages) {
				mCurrentPage++;
				mHasConnectionError = false;
				mHasChanged = true;
				startLoading();
			}
		}
		
		/**
		 * Sets a new Uri and executes the loader.
		 * @param uri The new Uri
		 * @return True if the Uri was different from the previous one, false otherwise.
		 */
		public boolean setUri(Uri uri) {
			mUri = uri;
			resetState();
			startLoading();
			return true;
		}
		
		
		/**
		 * Obtains a list of stories from the document
		 * @param document
		 * @return
		 */
		private List<Story> parseStories(Document document){
			
			ArrayList<Story> list;
			if (mCurrentPage == 1) {
				list = new ArrayList<Story>();
			}else{
				list = new ArrayList<Story>(mData);
			}
			
			Elements summaries = document.select("div#content div.bs");
			Elements titles = summaries.select("a[href~=(?i)/s/\\d+/1/.*]");
			Elements authors = summaries.select("a[href^=/u/]");
			Elements attribs = summaries.select("div.gray");

			Matcher matcher = pattern.matcher("");

			for (int i = 0; i < titles.size(); i++) {
				matcher.reset(titles.get(i).attr("href"));
				matcher.find();

				Elements dates = summaries.get(i).select("span[data-xutime]");
				long updateDate = 0;
				long publishDate = 0;

				if (dates.size() == 1) {
					updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
					publishDate = updateDate;
				} else if (dates.size() == 2) {
					updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
					publishDate = Long.parseLong(dates.last().attr("data-xutime")) * 1000;
				}

				Story TempStory = new Story(Integer.parseInt(matcher.group(1)),
						titles.get(i).ownText(), authors.get(i).ownText(), 0,
						summaries.get(i).ownText().replaceFirst("(?i)by\\s*", ""),
						attribs.get(i).text(), updateDate, publishDate);

				list.add(TempStory);
			}
			return list;
		}
		
		/**
		 * Resets the variables that represent the state of the loader
		 */
		private void resetState(){
			mHasChanged = true;
			mHasConnectionError = false;
			mCurrentPage = 1;
			mTotalPages = 0;
		}
		
		/**
		 * Adds the queries to the current Uri.
		 * @return The Uri with the query attached
		 */
		private Uri formatUri(){
			Uri.Builder builder = mUri.buildUpon();
			builder.query("");
			switch (URI_MATCHER.match(mUri)) {
			case URI_JUST_IN_MENU:
				builder.appendQueryParameter("s", filter[12] + "") 		//Type
						.appendQueryParameter("cid", filter[13] + "") 	//Category
						.appendQueryParameter("l", filter[5] + ""); 	//Language
				break;
			case URI_NORMAL_MENU:
			case URI_CROSSOVER_MENU:
				builder.appendQueryParameter("srt", filter[0] + "")		//Sort by
				 		.appendQueryParameter("t", filter[1] + "")		//Time range
				 		.appendQueryParameter("g1", filter[2] + "")		//Genre 1
				 		.appendQueryParameter("g2", filter[3] + "")		//Genre 2
				 		.appendQueryParameter("r", filter[4] + "")		//Rating
				 		.appendQueryParameter("lan", filter[5] + "")	//Language
				 		.appendQueryParameter("len", filter[6] + "")	//Length
				 		.appendQueryParameter("s", filter[7] + "")		//Status
				 		.appendQueryParameter("c1", filter[8] + "")		//Character 1
				 		.appendQueryParameter("c2", filter[9] + "")		//Character 2
				 		.appendQueryParameter("c3", filter[10] + "")	//Character 3
				 		.appendQueryParameter("c4", filter[11] + "")	//Character 4
				 		.appendQueryParameter("p", mCurrentPage + "");	//Current Page
				break;
			case URI_COMMUNITY_MENU:
				builder.appendEncodedPath(filter[4] + "")
						.appendEncodedPath(mCurrentPage + "")
						.appendEncodedPath(filter[2] + "")
						.appendEncodedPath(filter[6] + "")
						.appendEncodedPath(filter[7] + "")
						.appendEncodedPath(filter[1] + "")
						.appendEncodedPath("");
			default:
				break;
			}
			return builder.build();
			
		}
	
		/**
		 * Called automatically each time the loader starts
		 */
		@Override
		protected void onStartLoading() {
			deliverResult(mData);
			if (mHasChanged || mData == null) {
				 forceLoad();
			}
		}
	}
}
