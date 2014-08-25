/**
 * Coded by Michael Chen Tejada
 */
package com.spicymango.fanfictionreader.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.util.MenuObject;
import com.spicymango.fanfictionreader.util.Parser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Represents the menu's employed to navigate in between categories
 * 
 * @author Michael Chen
 */
public class NavigationMenuActivity extends ActionBarActivity implements LoaderCallbacks<List<MenuObject>>, OnItemClickListener, OnClickListener{
	private static final int COMMUNITY_MENU = 3;
	private static final int CROSSOVER_MENU = 1;
	/**
	 * Contains the FanFiction web site authority
	 * <p>
	 * <b>Value:</b> {@value}
	 */
	private static final String FANFIC_AUTHORITY = "m.fanfiction.net";
	private static final int LOADER_MENU = 1;
	private static final int NORMAL_MENU = 0;
	private static final String STATE_FILTER = "Filter";
	private static final String STATE_LIST = "List";	
	private static final String STATE_SORT = "Sort";
	private static final int SUB_CATEGORY_MENU = 2;
	
	private static final UriMatcher URIMATCHER = getUriMatcher();
	
	private static final String capitalizeString(String string) {
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
	
	/**
	 * Gets the matcher that checks for suitable <code>URI's</code>.<p>
	 * <ul><li>fanfiction.net/#cat/ -- Normal Menu </li>
	 * <li>fanfiction.net/crossover/#cat/ -- Crossover Menu</li>
	 * <li>fanfiction.net/crossover/#name/#code -- Crossover Sub-Menu</li>
	 * <li>fanfiction.net/communities/#cat/ -- Communities Menu</li></ul>
	 */	
	private static UriMatcher getUriMatcher(){
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);		
		matcher.addURI(FANFIC_AUTHORITY, "crossovers/*/", CROSSOVER_MENU);
		matcher.addURI(FANFIC_AUTHORITY, "crossovers/*/#/", SUB_CATEGORY_MENU);
		matcher.addURI(FANFIC_AUTHORITY, "communities/*/", COMMUNITY_MENU);
		matcher.addURI(FANFIC_AUTHORITY, "*/", NORMAL_MENU);
		return matcher;
	}
	
	private NavigationMenuAdapter mAdapter;
	private int mCurrentFilter;
	private ArrayList<MenuObject> mList;
	private ListView mListView;
	private MenuLoader mLoader;
	private View mProgressBar;
	private View mRetryBar;
	private boolean mSort;
	private Uri mUri;

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.retry_internet_connection:
			mLoader.startLoading();
			break;
		default:
			break;
		}
		
	}
	
	@Override
	public Loader<List<MenuObject>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case LOADER_MENU:
			mLoader = new MenuLoader(this, mUri, mList);
			return mLoader;
		default:
			return null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.navigation_menu, menu);
		return true;
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long id) {
		Intent i = null;
		
		Uri uri = Uri.parse(mList.get(arg2).getUri());
		
		switch (URIMATCHER.match(mUri)) {
		case NORMAL_MENU:
		case SUB_CATEGORY_MENU:
			i = new Intent(this,StoryMenuActivity.class);
			break;
		case CROSSOVER_MENU:
			i = new Intent(this,NavigationMenuActivity.class);
			break;
		case COMMUNITY_MENU:
			i = new Intent(this,CommunityMenuActivity.class);
			break;
		default:
			return;
		}
		i.setData(uri);		
		startActivity(i);
	}
	
	@Override
	public void onLoaderReset(Loader<List<MenuObject>> loader) {
		mList.clear();
	}
	
	@Override
	public void onLoadFinished(Loader<List<MenuObject>> loader, List<MenuObject> data) {
		mList.clear();
		mList.addAll(data);
		mAdapter.sort(mSort);
		mAdapter.notifyDataSetChanged();	
		mLoader = (MenuLoader) loader;
		
		if (mLoader.hasConnectionError()) {
			mProgressBar.setVisibility(View.GONE);
			mRetryBar.setVisibility(View.VISIBLE);
			Toast toast = Toast.makeText(this, R.string.error_connection, Toast.LENGTH_SHORT);
			toast.show();
		}else if(mLoader.isRunning()){
			mProgressBar.setVisibility(View.VISIBLE);
			mRetryBar.setVisibility(View.GONE);
		}else{
			mProgressBar.setVisibility(View.GONE);
			mRetryBar.setVisibility(View.GONE);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.navigation_library_sort_by_name:
			mSort = true;
			mAdapter.sort(true);
			return true;
		case R.id.navigation_library_sort_by_size:
			mSort = false;
			mAdapter.sort(false);
			return true;
		case R.id.filter:
			displayFilterDialog();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return false;
		}
	}
	
	/**
	 * Displays the dialog that handles filtering
	 */
	private void displayFilterDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		String[] filterList;
		if (URIMATCHER.match(mUri) == SUB_CATEGORY_MENU) {
			filterList = getResources().getStringArray(R.array.menu_navigation_filter_crossover);
		} else {
			filterList = new String[28];
			filterList[0] = getString(R.string.menu_navigation_filter_top_200);
			filterList[1] = "#";
			for (int i = 0; i < 26; i++) {
				filterList[2 + i] = "" + (char)((int)('A') + i);
			}
		}
		
		
		builder.setItems(filterList, new DialogInterface.OnClickListener() {
	           @Override
			public void onClick(DialogInterface dialog, int position) {
					if (position < 28 && mCurrentFilter != position) {
						mCurrentFilter = position;
						Uri.Builder builder = mUri.buildUpon();
						builder.query("");
						if (URIMATCHER.match(mUri) == SUB_CATEGORY_MENU) {
							builder.appendQueryParameter("pcategoryid", "" + sortKey());
						}else{
							builder.appendQueryParameter("l", "" + sortKey());
						}
						
						mUri = builder.build();
						mSort = true;
						mLoader.setUri(mUri);
					}
		       }
		});
		builder.setInverseBackgroundForced(true);
		builder.create();
		builder.show();
	}
	
	/**
	 * Sets the title and the sub title of the activity.
	 * @throws IllegalStateException Invalid Uri
	 */
	private void setTitle() {
		final int title;
		final String subTitle;
		switch (URIMATCHER.match(mUri)) {
		case -1:
			throw new IllegalStateException("The Uri " + mUri.toString()
					+ " is invalid");
		case CROSSOVER_MENU:
		case SUB_CATEGORY_MENU:
			title = R.string.menu_navigation_title_crossover;
			subTitle = mUri.getPathSegments().get(1);
			break;
		case COMMUNITY_MENU:
			title = R.string.menu_navigation_title_community;
			subTitle = mUri.getPathSegments().get(1);
			break;
		case NORMAL_MENU:
		default:
			title = R.string.menu_navigation_title_regular;
			subTitle = mUri.getPathSegments().get(0);
			break;
		}
		setTitle(title);
		getSupportActionBar().setSubtitle(capitalizeString(subTitle));
	}
	
	/**
	 * Retrieves the sort key for the current filter
	 * @return
	 */
	private String sortKey(){
		switch (URIMATCHER.match(mUri)) {
		case SUB_CATEGORY_MENU:
			if (mCurrentFilter == 0) {
				return "0";
			}else if (mCurrentFilter < 5){
				return "20"+mCurrentFilter;
			}else if (mCurrentFilter == 5) {
				return "209";
			}else if (mCurrentFilter == 6) {
				return "211";
			}else if (mCurrentFilter == 7){
				return "205";
			}else if (mCurrentFilter == 8){
				return "207";
			}else{
				return "208";
			}
		default:
			String selector = "";
			if (mCurrentFilter == 0) {
				selector = "";
			}else if (mCurrentFilter == 1){
				selector = "1";
			}else{
				selector = "" + (char) ('a'+mCurrentFilter-2);
			}
			return selector;
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		if (savedInstanceState != null) {
			mList = savedInstanceState.getParcelableArrayList(STATE_LIST);
			mSort = savedInstanceState.getBoolean(STATE_SORT);
			mCurrentFilter = savedInstanceState.getInt(STATE_FILTER);
		}else{
			mList = new ArrayList<MenuObject>();
		}
		
		View footer = getLayoutInflater().inflate(R.layout.footer_list, null);
		mAdapter = new NavigationMenuAdapter(this, R.layout.category_menu_list_item, mList);
		mUri = getIntent().getData();
		setTitle();
		
		mListView = (ListView)findViewById(R.id.list);
		mListView.addFooterView(footer, null, false);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		
		mProgressBar = findViewById(R.id.progress_bar);
		mRetryBar = findViewById(R.id.row_no_connection);
		Button retryButton = (Button) findViewById(R.id.retry_internet_connection);
		retryButton.setOnClickListener(this);
		View addPageButton = findViewById(R.id.story_load_pages);
		addPageButton.setVisibility(View.GONE);

		getSupportLoaderManager().initLoader(LOADER_MENU, null, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelableArrayList(STATE_LIST, mList);
		outState.putBoolean(STATE_SORT, mSort);
		outState.putInt(STATE_FILTER, mCurrentFilter);
		getIntent().setData(mUri);
		super.onSaveInstanceState(outState);
	}
	
	/**
	 * Sorts the menuObjects either by name or stories
	 * @author Michael Chen
	 *
	 */
	private static class ListComparator implements Comparator<MenuObject> {

		private boolean sortBy = true;
		
		/**
		 * True to sort by title, false to sort by views
		 * @param SortBy
		 */
		public ListComparator(boolean SortBy) {
			sortBy = SortBy;
		}
		
		@Override
		public int compare(MenuObject arg0, MenuObject arg1) {
			if (sortBy){
				if (arg0.getSortInt() == Integer.MAX_VALUE) {
					return -1;
				}else if(arg1.getSortInt() == Integer.MAX_VALUE){
					return 1;
				}
				return arg0.getTitle().compareTo(arg1.getTitle());
			}else{
				return -((Integer)arg0.getSortInt()).compareTo(arg1.getSortInt());
			}	
		}
	}
	
	private static class MenuLoader extends AsyncTaskLoader<List<MenuObject>>{
		private boolean mConnectionError;
		private boolean mHasChanged;
		private List<MenuObject> mList;
		private Uri mUri; 
	
		/**
		 * Creates a new MenuLoader instance.
		 * @param context The context of the activity
		 * @param uri The Uri to display
		 * @param list A previously loaded list if it exists, null otherwise
		 */
		public MenuLoader(Context context, Uri uri, List<MenuObject> list) {
			super(context);
			mUri = uri;
			if (list == null || list.isEmpty()){
				mList = new ArrayList<MenuObject>();
				mHasChanged = true;
			}else{
				mList = list;
				mHasChanged = false;
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void deliverResult(List<MenuObject> data) {
			if (isReset()) {
				return;
			}
			if (data == null || mList == data) {
				mList = new ArrayList<MenuObject>(mList);
			}else{
				mList = data;
			}
			super.deliverResult(mList);
		}
		
		/**
		 * Determines if an internet connection error has occured
		 * @return True if an error has occured, false otherwise
		 */
		public boolean hasConnectionError(){
			return mConnectionError;
		}
		
		/**
		 * Determines if the loader is running 
		 * @return
		 */
		public boolean isRunning(){
			return mHasChanged && !mConnectionError;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public List<MenuObject> loadInBackground() {
			
			try {
				Document document = Jsoup.connect(mUri.toString()).get();
				Elements categories = document.select("div#content > div.bs > a");
				
				ArrayList<MenuObject> tmpList = new ArrayList<MenuObject>(categories.size() + 1);
				String formatString;
				
				switch (URIMATCHER.match(mUri)) {
				case COMMUNITY_MENU:
					formatString = getContext().getString(R.string.menu_navigation_count_community);
					break;
				case SUB_CATEGORY_MENU:		
					MenuObject tmpObject = new MenuObject(
							getCrossoverName(document),
							"",
							getCrossoverUri(document), 
							Integer.MAX_VALUE);
					
					tmpList.add(tmpObject);
					
				default://Intentional fall through
					formatString = getContext().getString(R.string.menu_navigation_count_story);
				}
				
				for (Element element : categories) {
					if (element.children().isEmpty()) {throw new IOException();}
					String count = element.child(0).ownText();
					MenuObject tmpObject = new MenuObject(
							element.ownText(),
							String.format(formatString, count),
							element.attr("abs:href"), 
							Parser.parseInt(count));
					tmpList.add(tmpObject);
				}
				mHasChanged = false;
				mConnectionError = false;
				return tmpList;
				
			} catch (Exception iOException) {
				mConnectionError = true;
				return null;
			}
		}

		/**
		 * Sets a new {@link Uri} and starts loading if the new Uri does not
		 * match the prexisting one.
		 * 
		 * @param uri
		 *            The new Uri
		 */
		public void setUri(Uri uri){
			if (uri.equals(mUri)) {
				return;
			}
			mUri = uri;
			mHasChanged = true;
			mList = new ArrayList<MenuObject>();
			startLoading();
		}

		/**
		 * Gets the "all crossover" text for the current document
		 * @param document The document to fetch the information
		 * @return The requested text, or null if the link does not exist
		 */
		private String getCrossoverName(Document document){
			Elements url = document.select("div#content > center > a");
			if (url == null || url.first() == null) {
				return null;
			}
			return url.first().ownText();
		}
		
		/**
		 * Gets the "all crossover" url for the current document
		 * @param document The document to fetch the information
		 * @return The requested url, or null if the link does not exist
		 */
		private String getCrossoverUri(Document document){
			Elements url = document.select("div#content > center > a");
			if (url == null || url.first() == null) {
				return null;
			}
			return url.first().attr("abs:href");
		}
		
		@Override
		protected void onStartLoading() {
			mConnectionError = false;
			deliverResult(mList);
			if (mHasChanged) {
				forceLoad();
			}			
		}

	}

	private static class NavigationMenuAdapter extends ArrayAdapter<MenuObject>{
		int layoutResourceId;
			
		/**
		 * Initializes the adapter
		 * @param context The current context
		 * @param layoutResourceId The resource ID for a layout file
		 * @param data The objects to represent in the list view
		 */
		public NavigationMenuAdapter(Context context, int layoutResourceId, List<MenuObject> data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId=layoutResourceId;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			MenuObject menuRow = getItem(position);
			MenuItemHolder holder = null;
			
			if(convertView == null)
	        {
	            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
	            convertView = inflater.inflate(layoutResourceId, parent, false);
	           
	            holder = new MenuItemHolder();
	            holder.txtTitle = (TextView)convertView.findViewById(R.id.category_menu_title);
	            holder.txtViews = (TextView)convertView.findViewById(R.id.category_menu_views);
	            convertView.setTag(holder);
	        }
	        else
	        {
	            holder = (MenuItemHolder)convertView.getTag();
	        }
	       
	        
	        holder.txtTitle.setText(menuRow.getTitle());
	        holder.txtViews.setText(menuRow.getViews());
	       
	        return convertView;
		}
		
		/**
		 * Sorts the adapter either by views or by title
		 * @param sortBy True to sort by title, false to sort by views
		 */
		public void sort(boolean sortBy) {
			super.sort(new ListComparator(sortBy));
		}
		
		/**
		 * A cache of the TextViews. Provides a speed improvement.
		 * @author Michael Chen
		 */
		static class MenuItemHolder
	    {
	        TextView txtTitle;
	        TextView txtViews;
	    }
	}
}
