/**
 * 
 */
package com.spicymango.fanfictionreader.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.activity.reader.StoryDisplayActivity;
import com.spicymango.fanfictionreader.dialogs.DetailDialog;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.Result;
import com.spicymango.fanfictionreader.util.Story;
import com.spicymango.fanfictionreader.util.TabListener;
import com.spicymango.fanfictionreader.util.adapters.StoryMenuAdapter;
import com.spicymango.fanfictionreader.util.adapters.TextAdapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

public class AuthorMenuActivity extends AppCompatActivity{
	private static final String STATE_TAB = "Tab selected";
	private static final String EXTRA_ID = "authorId";

	private long mAuthorId;
	private ActionBar actionBar;
	
	private static long authorId(Uri uri) {
		String segment = uri.getPathSegments().get(1);
		return Long.parseLong(segment);
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
		
		//Set the author Id
	    Uri uri =  getIntent().getData();
	    mAuthorId = authorId(uri);
	    
	    Bundle bundle = new Bundle();
	    bundle.putLong(EXTRA_ID, mAuthorId);
	    //Create the parameters to instantiate the fragment
		
		// setup action bar tabs
		actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayHomeAsUpEnabled(true);

		Tab tab = actionBar
				.newTab()
				.setText(R.string.menu_author_stories)
				.setTabListener(
						new TabListener(this, StoriesFragment.class, bundle));
		actionBar.addTab(tab);

		tab = actionBar
				.newTab()
				.setText(R.string.menu_author_profile)
				.setTabListener(
						new TabListener(this, ProfileFragment.class, bundle));
		actionBar.addTab(tab);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		actionBar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_TAB));
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_TAB, actionBar.getSelectedNavigationIndex());
		super.onSaveInstanceState(outState);
	}
	
	public final static class StoriesFragment extends Fragment implements LoaderCallbacks<Result>,OnItemClickListener, OnItemLongClickListener, OnClickListener{
		
		private long mAuthorId;
		private List<Story> mList;
		private BaseAdapter mAdapter;
		
		/**
		 * The retry button shown upon connection failure
		 */
		private View mNoConnectionBar;
		/**
		 * The progress spinner displayed while the loader loads
		 */
		private View mProgressBar;
		/**
		 * The button used to load the next page
		 */
		private Button mAddPageButton;

		private AuthorLoader mLoader;
		
		
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			StoryDisplayActivity.openStory(getActivity(), id, Site.FANFICTION, true);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			
			mList = new ArrayList<Story>();
			mAdapter = new StoryMenuAdapter(getActivity(), mList);
			
			View v = inflater.inflate(R.layout.activity_list_view, container, false);
			ListView listView = (ListView) v.findViewById(android.R.id.list);
			View footer = inflater.inflate(R.layout.footer_list, null);
			listView.addFooterView(footer, null, false);
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			listView.setAdapter(mAdapter);
			
			mAddPageButton = (Button) footer.findViewById(R.id.story_load_pages);
			mAddPageButton.setOnClickListener(this);
			mProgressBar = footer.findViewById(R.id.progress_bar); 
			mNoConnectionBar = footer.findViewById(R.id.row_retry);
			View retryButton = mNoConnectionBar.findViewById(R.id.btn_retry);
			retryButton.setOnClickListener(this);
			
			mAuthorId = getArguments().getLong(EXTRA_ID);
			getLoaderManager().initLoader(0, savedInstanceState, this);
			
			return v;
		}
	
		@Override
		public Loader<Result> onCreateLoader(int id, Bundle args) {
			return new AuthorLoader(getActivity(), args, mAuthorId);
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			DetailDialog.show((AppCompatActivity) getActivity(), mList.get(position));
			return true;
		}
		
		@Override
		public void onLoadFinished(Loader<Result> loader, Result data) {
			mLoader = (AuthorLoader) loader;
			switch (data) {
			case LOADING:
				mProgressBar.setVisibility(View.VISIBLE);
				mAddPageButton.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.GONE);
				break;
			case ERROR_CONNECTION:
				mProgressBar.setVisibility(View.GONE);
				mAddPageButton.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.VISIBLE);
				break;
			case SUCCESS:
				mProgressBar.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.GONE);

				mList.clear();
				mList.addAll(mLoader.mData);
				mAdapter.notifyDataSetChanged();

				((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(mLoader.mAuthor);

				if (mLoader.hasNextPage()) {
					String text = String.format(
							getString(R.string.menu_story_page_button),
							mLoader.mCurrentPage + 1,
							mLoader.mTotalPages);
					mAddPageButton.setVisibility(View.VISIBLE);
					mAddPageButton.setText(text);
				} else {
					mAddPageButton.setVisibility(View.GONE);
				}
				break;

			default:
				break;
			}
		}

		@Override
		public void onLoaderReset(Loader<Result> loader) {
		}

		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
		public void onSaveInstanceState(Bundle outState) {
			int currentapiVersion = android.os.Build.VERSION.SDK_INT;
			if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				if (!getActivity().isChangingConfigurations()) {
					mLoader.onSavedInstanceState(outState);
				}
			} else {
				mLoader.onSavedInstanceState(outState);
			}
			super.onSaveInstanceState(outState);
		}

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

		private static class AuthorLoader extends AsyncTaskLoader<Result> {
			private final static String STATE_CURRENT = "STATE CURRENT PAGE STORIES";
			private final static String STATE_TOTAL = "STATE TOTAL PAGES STORIES";
			private final static String STATE_DATA = "STATE CURRENT DATA STORIES";
			private final static String STATE_CHANGED = "STATE CHANGED STORIES";
			private static final String STATE_AUTHOR = "STATE AUTHOR STORIES";
			
			
			public String mAuthor;
			private long mAuthorId;
			private ArrayList<Story> mData;
			private int mCurrentPage, mTotalPages;
			private boolean mDataHasChanged;
			private Uri BASE_URI;

			public AuthorLoader(Context context, Bundle args, long authorId) {
				super(context);
				
				mAuthorId = authorId;
				
				if (args != null && args.containsKey(STATE_DATA)) {
					mTotalPages = args.getInt(STATE_TOTAL, 0);
					mCurrentPage = args.getInt(STATE_CURRENT, 1);
					mDataHasChanged = args.getBoolean(STATE_CHANGED, true);
					mData = args.getParcelableArrayList(STATE_DATA);
					mAuthor = args.getString(STATE_AUTHOR);
				} else {
					mData = new ArrayList<Story>();
					mDataHasChanged = true;
					mCurrentPage = 1;
					mTotalPages = 0;
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
				outState.putBoolean(STATE_CHANGED, mDataHasChanged);
				outState.putString(STATE_AUTHOR, mAuthor);
			}
			
			public void loadNextPage() {
				if (mCurrentPage < mTotalPages) {
					mCurrentPage++;
					mDataHasChanged = true;
					startLoading();
				} else {
					Log.e("BaseLoader-loadNextPage", "Attempted to load page number "
							+ (mCurrentPage + 1) + " when only " + mTotalPages
							+ " pages exist.");
				}
			}

			public boolean hasNextPage() {
				return mCurrentPage < mTotalPages;
			}

			@Override
			protected void onStartLoading() {
				if (mDataHasChanged) {
					deliverResult(Result.LOADING);
					forceLoad();
				}else{
					deliverResult(Result.SUCCESS);
				}
			}

			@Override
			public Result loadInBackground() {
				try {
					
					Document document = Jsoup.connect(
							formatUri(mCurrentPage).toString()).timeout(10000).get();
					
					if (mAuthor == null) {
						mAuthor = getAuthor(document);
					}

					if (mCurrentPage == 1) {
						mData.clear();
					}
					
					if (mTotalPages == 0) {
						mTotalPages = Math.max(Parser.getPageNumber(document),mCurrentPage);
					}

					if (load(document, mData)) {
						mDataHasChanged = false;
						return Result.SUCCESS;
					} else {
						return Result.ERROR_PARSE;
					}

				} catch (IOException e) {
					return Result.ERROR_CONNECTION;
				}
			}
			
			@Override
			public void deliverResult(Result data) {
				super.deliverResult(data);
			}
			
			protected Uri formatUri(int currentPage) {
				Uri.Builder builder = BASE_URI.buildUpon();
				builder.appendPath("u").appendPath(mAuthorId + "")
						.appendPath("").appendQueryParameter("a", "s")
						.appendQueryParameter("p", currentPage + "");
				return builder.build();
			}
			
			private static final Pattern pattern = Pattern
					.compile("/s/([\\d]++)/");

			protected boolean load(Document document, List<Story> list) {

				Elements summaries = document.select("div#content div.bs");

				Matcher storyIdMatcher = pattern.matcher("");

				for (Element element : summaries) {
					element.select("b").unwrap();
					Element title = element.select("a[href~=(?i)/s/\\d+/1/.*]")
							.first();
					Element attribs = element.select("div.gray").first();
					Elements dates = element.select("span[data-xutime]");

					if (title == null || attribs == null || dates == null)
						return false;

					storyIdMatcher.reset(title.attr("href"));
					storyIdMatcher.find();

					long updateDate = 0;
					long publishDate = 0;

					updateDate = Long.parseLong(dates.first().attr(
							"data-xutime")) * 1000;
					publishDate = Long.parseLong(dates.last().attr(
							"data-xutime")) * 1000;

					boolean complete;
					Elements imgs = element.select("img.mm");
					complete = !imgs.isEmpty();
					
					Story.Builder builder = new Story.Builder();
					builder.setId(Long.parseLong(storyIdMatcher.group(1)));
					builder.setName(title.ownText());
					builder.setAuthor(mAuthor);
					builder.setAuthorId(mAuthorId);
					builder.setSummary(element.ownText().replaceFirst("(?i)by\\s*", ""));
					builder.setFanFicAttributes(attribs.text());
					builder.setUpdateDate(updateDate);
					builder.setPublishDate(publishDate);
					builder.setCompleted(complete);

					list.add(builder.build());
				}
				return true;
			}
			
			private String getAuthor(Document document) {
				Elements author = document.select("div#content div b");
				if (author.isEmpty()) {
					return "";
				} else {
					return author.first().ownText();
				}
			}
			
		}

	}
	
	public static final class ProfileFragment extends Fragment implements LoaderCallbacks<Result>, OnClickListener{
		
		private long mAuthorId;
		private List<Spanned> mList;
		private BaseAdapter mAdapter;
		
		/**
		 * The retry button shown upon connection failure
		 */
		private View mNoConnectionBar;
		/**
		 * The progress spinner displayed while the loader loads
		 */
		private View mProgressBar;

		private ProfileLoader mLoader;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			
			mList = new ArrayList<Spanned>();
			mAdapter = new TextAdapter(getActivity(), mList);
			
			View v = inflater.inflate(R.layout.activity_list_view, container, false);
			ListView listView = (ListView) v.findViewById(android.R.id.list);
			View footer = inflater.inflate(R.layout.footer_list, null);
			listView.addFooterView(footer, null, false);
			listView.setAdapter(mAdapter);
			
			View addPageBtn = (Button) footer.findViewById(R.id.story_load_pages);
			addPageBtn.setVisibility(View.GONE);
			mProgressBar = footer.findViewById(R.id.progress_bar); 
			mNoConnectionBar = footer.findViewById(R.id.row_retry);
			View retryButton = mNoConnectionBar.findViewById(R.id.btn_retry);
			retryButton.setOnClickListener(this);
			
			mAuthorId = getArguments().getLong(EXTRA_ID);
			getLoaderManager().initLoader(0, savedInstanceState, this);
			
			return v;
		}
	
		@Override
		public Loader<Result> onCreateLoader(int id, Bundle args) {
			return new ProfileLoader(getActivity(), args, mAuthorId);
		}
		
		@Override
		public void onLoadFinished(Loader<Result> loader, Result data) {
			mLoader = (ProfileLoader) loader;
			switch (data) {
			case LOADING:
				mProgressBar.setVisibility(View.VISIBLE);
				mNoConnectionBar.setVisibility(View.GONE);
				break;
			case ERROR_CONNECTION:
				mProgressBar.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.VISIBLE);
				break;
			case SUCCESS:
				mProgressBar.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.GONE);

				mList.clear();
				mList.addAll(mLoader.mData);
				mAdapter.notifyDataSetChanged();

				((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(mLoader.mAuthor);
				break;

			default:
				break;
			}
		}

		@Override
		public void onLoaderReset(Loader<Result> loader) {
		}

		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
		public void onSaveInstanceState(Bundle outState) {
			int currentapiVersion = android.os.Build.VERSION.SDK_INT;
			if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
				if (!getActivity().isChangingConfigurations()) {
					mLoader.onSavedInstanceState(outState);
				}
			} else {
				mLoader.onSavedInstanceState(outState);
			}
			super.onSaveInstanceState(outState);
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btn_retry:
				mLoader.startLoading();
				break;
			default:
				break;
			}
		}

		private static class ProfileLoader extends AsyncTaskLoader<Result> {
			private final static String STATE_DATA = "STATE CURRENT DATA PROFILE";
			private final static String STATE_CHANGED = "STATE CHANGED PROFILE";
			private static final String STATE_AUTHOR = "STATE AUTHOR PROFILE";
			
			
			public String mAuthor;
			private long mAuthorId;
			private ArrayList<Spanned> mData;
			private boolean mDataHasChanged;
			private Uri BASE_URI;

			public ProfileLoader(Context context, Bundle args, long authorId) {
				super(context);
				
				mAuthorId = authorId;
				
				if (args != null && args.containsKey(STATE_DATA)) {
					mDataHasChanged = args.getBoolean(STATE_CHANGED, true);					
					mAuthor = args.getString(STATE_AUTHOR);
					
					List<String> spans = args.getStringArrayList(STATE_DATA);
					mData = new ArrayList<Spanned>(spans.size());
					for (String string : spans) {
						mData.add(Html.fromHtml(string));
					}
				} else {
					mData = new ArrayList<Spanned>();
					mDataHasChanged = true;
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
				
				ArrayList<String> spans = new ArrayList<String>();
				for (Spanned span : mData) {
					spans.add(Html.toHtml(span));
				}
				outState.putStringArrayList(STATE_DATA, spans);
				
				outState.putBoolean(STATE_CHANGED, mDataHasChanged);
				outState.putString(STATE_AUTHOR, mAuthor);
			}

			@Override
			protected void onStartLoading() {
				if (mDataHasChanged) {
					deliverResult(Result.LOADING);
					forceLoad();
				}else{
					deliverResult(Result.SUCCESS);
				}
			}

			@Override
			public Result loadInBackground() {
				try {
					
					Document document = Jsoup.connect(
							formatUri().toString()).timeout(10000).get();
					
					if (mAuthor == null) {
						mAuthor = getAuthor(document);
					}

					if (load(document, mData)) {
						mDataHasChanged = false;
						return Result.SUCCESS;
					} else {
						return Result.ERROR_PARSE;
					}

				} catch (IOException e) {
					return Result.ERROR_CONNECTION;
				}
			}
			
			@Override
			public void deliverResult(Result data) {
				super.deliverResult(data);
			}
			
			protected Uri formatUri() {
				Uri.Builder builder = BASE_URI.buildUpon();
				builder.appendPath("u").appendPath(mAuthorId + "")
						.appendPath("").appendQueryParameter("a", "b");
				return builder.build();
			}
			
			protected boolean load(Document document, List<Spanned> list) {

				Elements summaries = document.select("div#content > div");
				
				if (summaries.size() < 3) {
					SpannedString string = new SpannedString(getContext()
							.getString(R.string.menu_author_no_profile));
					list.add(string);
					return true;
				}
				
				Element txtElement = summaries.get(2);				
				Spanned txt = Html.fromHtml(txtElement.html());
				list.addAll(Parser.split(txt));
				
				return true;
			}
			
			private String getAuthor(Document document) {
				Elements author = document.select("div#content div b");
				if (author.isEmpty()) {
					return "";
				} else {
					return author.first().ownText();
				}
			}
			
		}

	}	
}