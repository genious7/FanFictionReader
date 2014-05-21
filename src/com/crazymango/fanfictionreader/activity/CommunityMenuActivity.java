package com.crazymango.fanfictionreader.activity;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.crazymango.fanfictionreader.R;
import com.crazymango.fanfictionreader.Settings;
import com.crazymango.fanfictionreader.util.Parser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class CommunityMenuActivity extends ActionBarActivity implements LoaderCallbacks<List<CommunityItem>>, OnItemClickListener, OnItemLongClickListener, OnClickListener{

	/**
	 * The main loader
	 */
	private communityLoader mLoader;
	
	/**
	 * Main data list
	 */
	private List<CommunityItem> mList;
	/**
	 * References to hide and show these views
	 */
	private View mProgressBar, mReconectRow, mAddPage;
	private ArrayAdapter<CommunityItem> mAdapter;
	private String fanfiction_scheme;
	private String fafinction_authority;
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.retry_internet_connection:
			mLoader.startLoading();
			break;
		case R.id.story_load_pages:
			mLoader.loadNewPage();
			break;
		default:
			break;
		}
		
	}
	
	@Override
	public Loader<List<CommunityItem>> onCreateLoader(int arg0, Bundle arg1) {
		if (arg0 == 0) {
			mLoader = new communityLoader(this, arg1, getIntent().getData());
			return mLoader;
		}
		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.community_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent i = new Intent(this,StoryMenuActivity.class);
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(fanfiction_scheme)
		.authority(fafinction_authority)
		.encodedPath(mList.get((int)id).uri);
		i.setData(builder.build());
		startActivity(i);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void onLoaderReset(Loader<List<CommunityItem>> arg0) {
		
	}
	
	@Override
	public void onLoadFinished(Loader<List<CommunityItem>> arg0, List<CommunityItem> arg1) {
		mLoader = (communityLoader) arg0;
		mList.clear();
		mList.addAll(arg1);
		mAdapter.notifyDataSetChanged();
		
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
			break;
		default:
			SortBy temp = SortBy.get(item.getItemId());
			if (temp != null) {
				mLoader.setSort(temp);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
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
		mList = new ArrayList<CommunityItem>();
		mAdapter = new CommunityAdapter(this, mList);
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
		
		getSupportLoaderManager().initLoader(0, savedInstanceState, this);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		mLoader.onSavedInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	private static final class CommunityAdapter extends ArrayAdapter<CommunityItem>{
		
		
		public CommunityAdapter(Context context,
				List<CommunityItem> objects) {
			super(context, R.layout.community_menu_list_item, objects);
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				LayoutInflater inflater = ((Activity) getContext())
						.getLayoutInflater();
				convertView = inflater.inflate(R.layout.community_menu_list_item,
						parent, false);
				holder = new ViewHolder();
				holder.title = (TextView) convertView
						.findViewById(R.id.community_title);
				holder.summary = (TextView) convertView
						.findViewById(R.id.community_summary);
				holder.author = (TextView) convertView
						.findViewById(R.id.community_author);
				holder.stories = (TextView) convertView
						.findViewById(R.id.community_stories);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.title.setText(getItem(position).title);
			holder.summary.setText(getItem(position).summary);
			holder.author.setText(getItem(position).author);
			holder.stories.setText(String.valueOf(getItem(position).stories));

			return convertView;
		}

		/**
		 * A small helper class to hold the id's of the views
		 * @author Michael Chen
		 */
		private static final class ViewHolder{
			private TextView author;
			private TextView title;
			private TextView stories;
			private TextView summary;
		}
	}

	private final static class communityLoader extends AsyncTaskLoader<List<CommunityItem>>{
		/**
		 * Contains the communityItems Loaded
		 */
		private List<CommunityItem> mData;
		/**
		 * True if a reload should be made, false otherwise
		 */
		private boolean dataHasChanged;
		/**
		 * True if a connection error has occured, false otherwise.
		 */
		private boolean connectionError;
		
		private SortBy mSortBy;
		
		private Uri baseUri;
		
		private int mCurrentPage;
		
		private int mTotalPages;
		
		private static final String STATE_DATA = "Community Items Data";
		private static final String STATE_TOTAL = "Community Total Pages";
		private static final String STATE_CURRENT = "Community Current Pages";
		private static final String STATE_SORT_BY = "Community Sort By";
		
		public communityLoader(Context context, Bundle params, Uri uri) {
			super(context);
			if (params == null || !params.containsKey(STATE_DATA)) {
				mData = new ArrayList<CommunityItem>();
				mTotalPages = 0;
				mCurrentPage = 1;
				mSortBy = SortBy.FOLLOWS;
				dataHasChanged = true;
			} else {
				mData = params.getParcelableArrayList(STATE_DATA);
				mTotalPages = params.getInt(STATE_TOTAL, 0);
				mCurrentPage = params.getInt(STATE_CURRENT, 1);
				mSortBy = (SortBy) params.getSerializable(STATE_SORT_BY);
				dataHasChanged = false;
			}
			baseUri = uri;
			connectionError = false;
		}

		@Override
		public void deliverResult(List<CommunityItem> data) {
			if (data != null) {
				mData = data;
			}
			mData = new ArrayList<CommunityItem>(mData);
			super.deliverResult(mData);
		}

		/**
		 * Parses the communities in the list
		 * @param document The document to parse
		 * @return The list of communities
		 */
		public List<CommunityItem> getCommunities( Document document){	

			Elements base = document.select("div#content > div.bs");
			Elements title = base.select("a");
			Elements summary = base.select("div.z-padtop");
			
			List<CommunityItem> list = new ArrayList<CommunityItem>(base.size());
			
			
			String communityText = getContext().getString(R.string.menu_navigation_count_story);
			
			final String dateFormat = "MM-dd-yy";
			final DateFormat format = new SimpleDateFormat(dateFormat, Locale.US);
			
			for (int i = 0; i < base.size(); i++) {
				String titleField = title.get(i).ownText();
				String url = title.get(i).attr("href");

				String n_views = title.get(i).child(0).ownText().replaceAll("[() ]", "");
				n_views = String.format(communityText, n_views);
				
				String summaryField = summary.get(i).ownText();
				
				String attrib[] = summary.get(i).child(0).ownText().toString().split("\\s+-\\s+");
				String languague = attrib[0];
				int staff = Integer.parseInt(attrib[1].replaceAll("[\\D]", ""));
				int follows = Integer.parseInt(attrib[2].replaceAll("[\\D]", ""));
				
				Date date;
				try {
					date = format.parse(attrib[3].replaceAll("(?i)since:\\s*", ""));
				} catch (ParseException e) {
					e.printStackTrace();
					date = new Date();
				}

				String author = attrib[4].replaceAll("(?i)founder:\\s*", "");
				
				list.add(new CommunityItem(titleField, url, author, summaryField, n_views, languague, staff, follows, date));
			}
			return list;

		}
		
		public String getUri(){
			Uri.Builder builder = baseUri.buildUpon();
			builder.appendPath("0")
			.appendPath(mSortBy.getId() + "")
			.appendPath(mCurrentPage + "")
			.appendPath("");
			return builder.toString();
		}
		
		public boolean hasConnectionError() {
			return connectionError;
		}
		
		/**
		 * Finds whether the Loader is currently running
		 * @return True if it is currently executing, false otherwise
		 */
		public boolean isRunning(){
			return dataHasChanged && !connectionError;
		}

		@Override
		public List<CommunityItem> loadInBackground() {
			try {
				Document document = Jsoup.connect(getUri()).get();
				if (mTotalPages == 0) {
					mTotalPages = Parser.Pages(document);
				}
				if (mCurrentPage == 1) {
					mData = getCommunities(document);
				}else{
					mData.addAll(getCommunities(document));
				}
				dataHasChanged = false;
				return mData;
			} catch (IOException e) {
				connectionError = true;
				return null;
			}
		}
		
		public void loadNewPage(){
			mCurrentPage++;
			dataHasChanged = true;
			startLoading();
		}
		
		public void onSavedInstanceState(Bundle savedInstanceState){
			savedInstanceState.putParcelableArrayList(STATE_DATA, (ArrayList<? extends Parcelable>) mData);
			savedInstanceState.putInt(STATE_CURRENT, mCurrentPage);
			savedInstanceState.putInt(STATE_TOTAL, mTotalPages);			
			savedInstanceState.putSerializable(STATE_SORT_BY, mSortBy);
		}
		
		public void setSort(SortBy sortBy){
			if (mSortBy != sortBy) {
				mSortBy = sortBy;
				mTotalPages = 0;
				mCurrentPage = 1;
				dataHasChanged = true;
				mData.clear();
				startLoading();
			}
		}
		
		@Override
		protected void onStartLoading() {
			connectionError = false;
			deliverResult(mData);
			if (dataHasChanged) {
				forceLoad();
			}
		}
	}

	private enum SortBy{
		RANDOM (99, R.id.community_sort_random),
		STAFF (1, R.id.community_sort_staff),
		STORIES (2, R.id.community_sort_stories),
		FOLLOWS (3, R.id.community_sort_follows),
		CREATE_DATE (4, R.id.community_sort_random);
		
		public static final SortBy  get(int res){
			for (SortBy e : values()) {
				if (res == e.res) {
					return e;
				}
			}
			return null;
		}
		private final int id;
		
		private final int res;
		
		private SortBy(int id, int res) {
			this.id = id;
			this.res = res;
		}
		
		public int getId(){
			return id;
		}
		
	}
}

/**
 * Contains the parameters that define a community.
 * 
 * @author Michael Chen
 */
final class CommunityItem implements Parcelable {
	String title;
	String uri;
	String author;
	String summary;
	String stories;
	String languague;
	int staff;
	int follows;
	Date published;

	public static final Parcelable.Creator<CommunityItem> CREATOR = new Creator<CommunityItem>() {

		@Override
		public CommunityItem createFromParcel(Parcel source) {
			return new CommunityItem(source);
		}

		@Override
		public CommunityItem[] newArray(int size) {
			return new CommunityItem[size];
		}
	};

	/**
	 * A constructor used for creating a community item from a parcel.
	 * 
	 * @param in
	 */
	public CommunityItem(Parcel in) {
		title = in.readString();
		uri = in.readString();
		author = in.readString();
		summary = in.readString();
		stories = in.readString();
		languague = in.readString();
		staff = in.readInt();
		follows = in.readInt();
		published = new Date(in.readLong());
	}

	public CommunityItem(String title, String uri, String author,
			String summary, String stories, String languague, int staff,
			int follows, Date published) {
		super();
		this.title = title;
		this.uri = uri;
		this.author = author;
		this.summary = summary;
		this.stories = stories;
		this.languague = languague;
		this.staff = staff;
		this.follows = follows;
		this.published = published;
	}

	/**
	 * Always returns 0
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeString(uri);
		dest.writeString(author);
		dest.writeString(summary);
		dest.writeString(stories);
		dest.writeString(languague);
		dest.writeInt(staff);
		dest.writeInt(follows);
		dest.writeLong(published.getTime());
	}
}