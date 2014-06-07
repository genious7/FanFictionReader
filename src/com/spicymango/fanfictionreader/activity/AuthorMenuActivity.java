/**
 * 
 */
package com.spicymango.fanfictionreader.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.DetailDisplay;
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
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

/**
 * Represents the menu to see stories by author
 * @author Michael Chen
 */
public class AuthorMenuActivity extends ActionBarActivity implements LoaderCallbacks<List<Story>>, OnClickListener, OnItemClickListener, OnItemLongClickListener{
	private BaseAdapter mAdapter;
	private List<Story> mList;
	
	/**
	 * The button used to load the next page
	 */
	private Button mAddPageButton;
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
	
	private AuthorLoader mLoader;
	private List<Map<Integer, String>> filterList;
	
	@Override
	public Loader<List<Story>> onCreateLoader(int arg0, Bundle arg1) {
		return new AuthorLoader(this, authorId(mUri)); 
	}

	@Override
	public void onLoadFinished(Loader<List<Story>> loader, List<Story> data) {
		mList.clear();
		mList.addAll(data);
		mAdapter.notifyDataSetChanged();
		
		mLoader =  (AuthorLoader) loader;
		getSupportActionBar().setSubtitle(mLoader.mAuthor);
		
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
	public void onLoaderReset(Loader<List<Story>> arg0) {
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Settings.setOrientation(this);
		
		mUri = getIntent().getData();
		mList = new ArrayList<Story>();
		mAdapter = new StoryMenuAdapter(this, mList);
		
		ListView listView = (ListView)findViewById(R.id.list);
		listView.addFooterView(getLayoutInflater().inflate(R.layout.progress_bar, null),null,false);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setAdapter(mAdapter);
		
		mAddPageButton = (Button) findViewById(R.id.story_load_pages);
		mAddPageButton.setOnClickListener(this);
		mProgressBar = findViewById(R.id.progress_bar); 
		mNoConnectionBar = findViewById(R.id.row_no_connection);
		
		Button retryButton = (Button) findViewById(R.id.retry_internet_connection);
		retryButton.setOnClickListener(this);
		
		getSupportLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.retry_internet_connection:
			mLoader.startLoading();
			break;
		case R.id.story_load_pages:
			mLoader.loadNextPage();
		default:
			break;
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(getString(R.string.fanfiction_scheme))
		.authority(getString(R.string.fanfiction_authority))
		.appendPath("s")//Story
		.appendPath("" + id)//Id
		.appendPath("1")//Chapter 1
		.appendPath("");//Adds the '/'
		
		Intent i = new Intent(this, StoryDisplayActivity.class);
		i.setData(builder.build());
		startActivity(i);	
	}
	
	private static long authorId(Uri uri){
		String segment = uri.getPathSegments().get(1);
		return Long.parseLong(segment);
	}

	private static final class AuthorLoader extends AsyncTaskLoader<List<Story>>{
		public List<Map<Integer, String>> filterList;
		private int mCurrentPage, mTotalPages;
		private long mAuthorId;
		private boolean mDataHasChanged, mConnectionError;
		private List<Story> mData;
		private String mAuthor;
		
		public void loadNextPage(){
			mDataHasChanged = true;
			mCurrentPage++;
			startLoading();
		}
		
		public AuthorLoader(Context context, long authorId) {
			super(context);
			mCurrentPage = 1;
			mTotalPages = 0;
			mDataHasChanged = true;
			mConnectionError = false;
			mAuthorId = authorId;
			mData = new ArrayList<Story>();
		}
		
		public boolean isRunning() {
			return !mConnectionError && mDataHasChanged;
		}

		public boolean hasConnectionError() {
			return mConnectionError;
		}

		@Override
		protected void onStartLoading() {
			mConnectionError = false;
			deliverResult(mData);
			if (mDataHasChanged) {
				forceLoad();
			}
		}

		@Override
		public void deliverResult(List<Story> data) {
			if (data != null && data != mData) {
				mData = data;
			} else {
				mData = new ArrayList<Story>(mData);
			}
			super.deliverResult(mData);
		}
		
		@Override
		public List<Story> loadInBackground() {
			try {
				Document document = Jsoup.connect(getUri().toString()).get();
				List<Story> list;
				
				if (mTotalPages == 0) {
					mTotalPages = Math.max(Parser.getpageNumber(document),mCurrentPage);
				}
				
				if (mCurrentPage == 1) {
					list = new ArrayList<Story>();
				}else{
					list = new ArrayList<Story>(mData);
				}
				
				list.addAll(Stories(document));
				mDataHasChanged = false;
				return list;
			} catch (IOException e) {
				mConnectionError = true;
				return null;
			}
		}
		
		private static final  Pattern pattern = Pattern.compile(
				"/s/([\\d]++)/");
		
		private ArrayList<Story> Stories(Document document) {

			Elements summaries = document.select("div#content div.bs");
			summaries.select("b").unwrap();
			
			Elements titles = summaries.select("a[href~=(?i)/s/\\d+/1/.*]");
			Elements attribs = summaries.select("div.gray");
			
			mAuthor = getAuthor(document);

			ArrayList<Story> list = new ArrayList<Story>();
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
						titles.get(i).ownText(), mAuthor, mAuthorId,
						summaries.get(i).ownText().replaceFirst("(?i)by\\s*", ""),
						attribs.get(i).ownText(), updateDate, publishDate);

				list.add(TempStory);
			}
			return list;
		}
		
		private String getAuthor(Document document){
			Elements author = document.select("div#content div b");
			if (author.isEmpty()) {
				return "";
			}else{
				return author.first().ownText();
			}
		}

		/**
		 * Returns the Uri for the selected author, page, and sort option
		 * @return
		 */
		private Uri getUri() {
			Uri.Builder builder = new Uri.Builder();
			builder.scheme("https")
			.authority("m.fanfiction.net")
			.appendPath("u")
			.appendPath(mAuthorId + "")
			.appendPath("")
			.appendQueryParameter("a", "s")
			.appendQueryParameter("p", mCurrentPage + "");
			return builder.build();
		}
		
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		Intent i= new Intent(this, DetailDisplay.class);
		i.putExtra(DetailDisplay.MAP, mList.get(position));
		i.putExtra(DetailDisplay.EXTRA_AUTHOR, true);
		startActivity(i);
		return true;
	}



}
