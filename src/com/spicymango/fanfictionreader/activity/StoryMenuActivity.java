package com.spicymango.fanfictionreader.activity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.jsoup.nodes.Document;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.dialogs.DetailDialog;
import com.spicymango.fanfictionreader.filter.FilterDialog;
import com.spicymango.fanfictionreader.util.BaseActivity;
import com.spicymango.fanfictionreader.util.BaseLoader;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.Story;
import com.spicymango.fanfictionreader.util.adapters.StoryMenuAdapter;

import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Toast;

public class StoryMenuActivity extends BaseActivity<Story>{

	private static final String FANFIC_AUTHORITY = "m.fanfiction.net";
	
	private static final int URI_COMMUNITY_MENU = 3;
	private static final int URI_CROSSOVER_MENU = 1;
	private static final int URI_JUST_IN_MENU = 2;
	private static final UriMatcher URI_MATCHER = getUriMatcher();
	private static final int URI_NORMAL_MENU = 0;
	
	private Uri mUri;
	
	private StoryLoader mLoader;
	
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
		matcher.addURI(FANFIC_AUTHORITY, "*/#/", URI_NORMAL_MENU);
		matcher.addURI(FANFIC_AUTHORITY, "*/*/", URI_NORMAL_MENU);
		return matcher;
	}

	@Override
	public Loader<List<Story>> onCreateLoader(int id, Bundle args) {
		return new StoryLoader(this, args, mUri);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		StoryDisplayActivity.openStory(this, id, true);	
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		DetailDialog.show(this, mList.get(position));
		return true;
	}

	@Override
	protected BaseAdapter getAdapter() {
		return new StoryMenuAdapter(this, mList);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.story_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.read_story_filter:
			FilterDialog.show(this, mLoader.filterList, mLoader.filter);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}	
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem filter = menu.findItem(R.id.read_story_filter);
		if (mLoader == null || mLoader.filterList == null) {
			filter.setEnabled(false);
			filter.getIcon().setAlpha(64);
		}else{
			filter.setEnabled(true);
			filter.getIcon().setAlpha(255);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public void onLoadFinished(Loader<List<Story>> loader, List<Story> data) {
		super.onLoadFinished(loader, data);
		mLoader = (StoryLoader) loader;
		supportInvalidateOptionsMenu();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode==1){//Filter Menu
			if (resultCode==RESULT_CANCELED) {
				//Dialog cancelled
				Toast toast = Toast.makeText(this, getResources().getString(R.string.dialog_cancelled), Toast.LENGTH_SHORT);
				toast.show();
			}else if (resultCode == RESULT_OK) {
				int[] filter = data.getIntArrayExtra(FilterDialog.RESULT);
				if (filter != mLoader.filter) {
					mLoader.filter = filter;
					mLoader.resetState();
					mLoader.startLoading();
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUri = getIntent().getData();
		setTitle();
		getSupportLoaderManager().initLoader( 0, savedInstanceState, this);
	}
	
	/**
	 * Sets the title and the sub title of the activity.
	 * @throws IllegalStateException Invalid Uri
	 */
	private void setTitle(){
		String subTitle;
		int title;
		
		switch (URI_MATCHER.match(mUri)) {	
		case URI_NORMAL_MENU:
			title = R.string.menu_navigation_title_regular;
			subTitle = mUri.getLastPathSegment();
			break;
		case URI_CROSSOVER_MENU:
			title = R.string.menu_navigation_title_crossover;
			subTitle = mUri.getPathSegments().get(0);
			break;
		case URI_JUST_IN_MENU:
			title = R.string.menu_story_title_just_in;
			subTitle = "";
			break;
		case URI_COMMUNITY_MENU:
			title = R.string.menu_navigation_title_community;
			subTitle = mUri.getPathSegments().get(1);
			break;
		default:
				throw new IllegalStateException("The Uri " + mUri + " is invalid");	
		}
		subTitle = subTitle.replaceAll("-", " ");
		setTitle(title);
		getSupportActionBar().setSubtitle(capitalizeString(subTitle));
	}
	
	private static final class StoryLoader extends BaseLoader<Story>{
		private static final String STATE_FILTER = "filter";
		private static final String STATE_FILTER_LIST = "filter list";
		
		private int[] filter;
		private ArrayList<LinkedHashMap<String, Integer>> filterList; //Filter elements
		private Uri mUri;
		
		@SuppressWarnings("unchecked")
		public StoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);
			mUri = uri;
			
			if (savedInstanceState != null && savedInstanceState.containsKey(STATE_FILTER)) {
				filter = savedInstanceState.getIntArray(STATE_FILTER);
				filterList = (ArrayList<LinkedHashMap<String, Integer>>) savedInstanceState.getSerializable(STATE_FILTER_LIST);
			}else{
				switch (URI_MATCHER.match(mUri)) {
				case URI_COMMUNITY_MENU:
					filter = new int[]{0,0,0,0,99,0,0,0,0,0,0,0,0,0};
					break;
				default:
					filter = new int[]{1,0,0,0,10,0,0,0,0,0,0,0,0,0};
					break;
				}
			}
		}
		@Override
		public void onSavedInstanceState(Bundle outState) {
			outState.putIntArray(STATE_FILTER, filter);
			outState.putSerializable(STATE_FILTER_LIST, filterList);
			super.onSavedInstanceState(outState);
		}
		@Override
		protected Uri formatUri(int currentPage) {
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
				 		.appendQueryParameter("p", currentPage + "");	//Current Page
				break;
			case URI_COMMUNITY_MENU:
				builder.appendEncodedPath(filter[4] + "")				//Rating
						.appendEncodedPath(filter[0] + "")				//Sort Options
						.appendEncodedPath(currentPage + "")			//Current Page
						.appendEncodedPath(filter[2] + "")				//Genre
						.appendEncodedPath(filter[6] + "")				//Length
						.appendEncodedPath(filter[7] + "")				//Status
						.appendEncodedPath(filter[1] + "")				//Time Range
						.appendEncodedPath("");
			default:
				break;
			}
			return builder.build();
		}

		@Override
		protected int getTotalPages(Document document) {
			return Math.max(Parser.getpageNumber(document), getCurrentPage());
		}

		@Override
		protected boolean load(Document document, List<Story> list) {
			// Load the filters if they aren't already loaded.
			if (filterList == null) {
				filterList = Parser.Filter(document);
			}
			return Parser.Stories(document, list);
		}
		
	}	
}

