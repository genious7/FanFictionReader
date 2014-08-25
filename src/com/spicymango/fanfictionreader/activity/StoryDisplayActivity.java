package com.spicymango.fanfictionreader.activity;

import info.piwai.android.JellyBeanSpanFixTextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;

import com.spicymango.fanfictionreader.LibraryDownloader;
import com.spicymango.fanfictionreader.MainMenu;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.provider.SqlConstants;
import com.spicymango.fanfictionreader.provider.StoryProvider;
import com.spicymango.fanfictionreader.util.FileHandler;
import com.spicymango.fanfictionreader.util.Parser;

public class StoryDisplayActivity extends ActionBarActivity implements LoaderCallbacks<StoryObject>, OnClickListener{
	public static final String EXTRA_OFFSET = "Offset";
	
	private View btnFirst, btnPrev, btnNext, btnLast, progressBar, recconectBar, buttonBar;
	/**
	 * The buttons at to change the current page
	 */
	private TextView btnPage;
	private StoryObject data;
	private int mCurrentPage;
	private StoryLoader mLoader;
	private long mStoryId;
	private int totalPages;
	private BaseAdapter mAdapter;
	private List<Spanned> mList;
	private ListView mListView;
	boolean fromBrowser = false;
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.read_story_first:	
			load(1);
			break;
		case R.id.read_story_prev:
			load(mCurrentPage - 1);
			break;
		case R.id.read_story_next:
			load(mCurrentPage + 1);
			break;
		case R.id.read_story_last:
			load(totalPages);
			break;
		case R.id.read_story_page_counter:
			chapterPicker();
			break;
		case R.id.retry_internet_connection:
			mLoader.startLoading();
			break;
		}	
	}
	
	@Override
	public Loader<StoryObject> onCreateLoader(int id, Bundle args) {
		int offset = getIntent().getIntExtra(EXTRA_OFFSET, 0);
		return new StoryLoader(this, args, mStoryId, mCurrentPage, offset);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.read_story_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public void onLoaderReset(Loader<StoryObject> loader) {
	}

	@Override
	public void onLoadFinished(Loader<StoryObject> loader, StoryObject data) {
		
		mLoader = (StoryLoader) loader;
		
		this.data = data;
		totalPages = data.getTotalPages();
		mCurrentPage = data.getCurrentPage();
		getSupportActionBar().setSubtitle(data.getStoryTitle());
		
		mList.clear();
		mList.addAll(data.getStoryText());
		mAdapter.notifyDataSetChanged();
		
		if (mLoader.isRunning()) {
			recconectBar.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			buttonBar.setVisibility(View.GONE);
		}else if(mLoader.connectionError){
			recconectBar.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			buttonBar.setVisibility(View.GONE);
		}else if(mLoader.sdCardError){
			TextView btn = (TextView) findViewById(R.id.label_retry);
			btn.setText(R.string.error_sd);
			recconectBar.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			buttonBar.setVisibility(View.GONE);
		}else{
			recconectBar.setVisibility(View.GONE);
			progressBar.setVisibility(View.GONE);
			buttonBar.setVisibility(View.VISIBLE);
			updatePageButtons(mCurrentPage, totalPages);
		}
		
		if (mLoader.scrollTo) {
			scrollTo(mLoader.characterOffset);
			mLoader.scrollTo = false;
		}
		
		if (fromBrowser && data.isInLibrary() && !mLoader.hasUpdated) {
			mLoader.hasUpdated = true;
			LibraryDownloader.download(this, mStoryId, mCurrentPage);
		}
		supportInvalidateOptionsMenu();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.read_story_menu_add:
			mLoader.hasUpdated = true;
			LibraryDownloader.download(this, mStoryId, mCurrentPage);  
			supportInvalidateOptionsMenu();
			return true;
		case R.id.read_story_go_to:
			chapterPicker();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.read_story_menu_add);
		if ( mLoader != null && mLoader.mData != null && mLoader.mData.isInLibrary()) {
			item.setIcon(R.drawable.ic_action_refresh);
			item.setTitle(R.string.read_story_update);
			item.setTitleCondensed(getString(R.string.read_story_update_condensed));
		} else {
			item.setIcon(R.drawable.ic_action_download);
			item.setTitle(R.string.read_story_add);
			item.setTitleCondensed(getString(R.string.read_story_add_condensed));
		}

		if (mLoader.hasUpdated) {
			item.setEnabled(false);
			item.getIcon().setAlpha(64);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	private void chapterPicker() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String[] Chapters = new String[totalPages];
		for (int i = 0; i < totalPages; i++) {
			Chapters[i] = getResources().getString(R.string.read_story_chapter)
					+ (i + 1);
		}
		builder.setItems(Chapters, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (mCurrentPage != which + 1) {
					load(which + 1);
				}
			}
		});
		builder.setInverseBackgroundForced(true);
		builder.create();
		builder.show();
	}
	
	private void load(int page){
		if (mLoader != null) {
			mLoader.loadPage(page);
		}
	}
	
	/**
	 * Parses the Uri.
	 * @param uri
	 * @return True if the Uri was valid, false otherwise.
	 */
	private boolean parseUri(Uri uri){
		String scheme = uri.getScheme();
		if (scheme.equals("https")||scheme.equals("http")) {
			Pattern filePattern = Pattern.compile("/s/(\\d++)/(\\d++)/");
			Matcher matcher = filePattern.matcher(uri.toString());
			if (matcher.find()) {
				mStoryId = Integer.valueOf(matcher.group(1));
				mCurrentPage = Integer.valueOf(matcher.group(2));		
				fromBrowser = true;
				return true;
			}
			
		}else if(scheme.equals("file")){
			Pattern filePattern = Pattern.compile("(\\d++)_(\\d++).txt");
			Matcher matcher = filePattern.matcher(uri.getLastPathSegment());
			if (matcher.find()) {
				mStoryId = Integer.valueOf(matcher.group(1));
				mCurrentPage = Integer.valueOf(matcher.group(2));
				return true;
			}
		}
		Toast toast = Toast.makeText(this, R.string.error_parsing, Toast.LENGTH_SHORT);
		toast.show();
		return false;
	}
	
	private void updatePageButtons(int currentPage, int totalPages){
		if (currentPage == totalPages) {
			btnNext.setEnabled(false);
			btnLast.setEnabled(false);
		} else {
			btnNext.setEnabled(true);
			btnLast.setEnabled(true);
		}
		if (currentPage == 1) {
			btnPrev.setEnabled(false);
			btnFirst.setEnabled(false);
		} else {
			btnPrev.setEnabled(true);
			btnFirst.setEnabled(true);
		}
		if (totalPages == 1) {
			btnPage.setEnabled(false);
		}
		btnPage.setText(currentPage + "/" + totalPages);
	}
	
	@SuppressLint("InflateParams")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);//Super() constructor first
		setContentView(R.layout.activity_list_view);//Set the layout
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		mList = new ArrayList<Spanned>();
		mListView = (ListView) findViewById(R.id.list);
		mListView.setKeepScreenOn(Settings.isWakeLockEnabled(this));
		View footer = getLayoutInflater().inflate(R.layout.footer_read_story, null);
		mListView.addFooterView(footer);
		mListView.setDividerHeight(0);
		mAdapter = new StoryAdapter(this, mList);
		mListView.setAdapter(mAdapter);

		btnFirst = footer.findViewById(R.id.read_story_first);
		btnPrev = footer.findViewById(R.id.read_story_prev);
		btnNext = footer.findViewById(R.id.read_story_next);
		btnLast = footer.findViewById(R.id.read_story_last);
		btnPage = (Button)findViewById(R.id.read_story_page_counter);
		progressBar = footer.findViewById(R.id.progress_bar);
		recconectBar = footer.findViewById(R.id.row_no_connection);
		buttonBar = footer.findViewById(R.id.buttonBar);
		btnPage.setOnClickListener(this);
		
		//Creates a new activity, and sets the initial page and story Id
		if (!parseUri(getIntent().getData())) {
			finish();
			return;
		}
		
		getSupportLoaderManager().initLoader(0, savedInstanceState, this);

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mLoader.onSaveInstanceState(outState);
	}
	
	/**
	 * Gets the first visible character at the current scroll position
	 * @return The index of the first visible character
	 */
	private int getCharacterOffset(){
		int offset = 0;
		int firstPosition = mListView.getFirstVisiblePosition();
		for (int j = 0; j < firstPosition; j++) {
			offset += mList.get(j).length();
		}		
		return offset;
	}
	
	/**
	 * Scrolls to the selected position
	 * 
	 * @param offset
	 */
	private void scrollTo(final int offset) {
		
		if (mList.isEmpty()) {
			return;
		}
		
		int i = 0;
		int j = 0;

		while (j + mList.get(i).length() <= offset) {
			j += mList.get(i).length();
			i++;
		}
		
		if (i >= mList.size()) {
			i = mList.size() - 1;
		}

		mListView.setSelectionFromTop(i, 0);
	}
	
	@Override
	protected void onStop() {
		if (data != null && data.isInLibrary()) {
			ContentResolver resolver = getContentResolver();
			AsyncQueryHandler handler = new AsyncQueryHandler(resolver){};
			
			int offset = getCharacterOffset();
			
			ContentValues values = new ContentValues(1);
			values.put(SqlConstants.KEY_LAST, mCurrentPage);
			values.put(SqlConstants.KEY_OFFSET, offset);
			handler.startUpdate(0, null, StoryProvider.CONTENT_URI, values,
					SqlConstants.KEY_STORY_ID + " = ?",
					new String[] { String.valueOf(mStoryId) });
			
			SharedPreferences preference = getSharedPreferences(MainMenu.EXTRA_PREF,MODE_PRIVATE);
			SharedPreferences.Editor editor = preference.edit();
			editor.putLong(MainMenu.EXTRA_RESUME_ID, mStoryId);
			editor.putInt(MainMenu.EXTRA_RESUME_CHAPTER, mCurrentPage);
			editor.putInt(MainMenu.EXTRA_RESUME_OFFSET, offset);
			editor.commit();
		}
		super.onStop();
	}
	
	private static class StoryAdapter extends ArrayAdapter<Spanned>{
		private final int padding;
		
		public StoryAdapter(Context context, List<Spanned> objects) {
			super(context, 0, objects);
			padding = getContext().getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view;
			if (convertView == null) {
				view = new JellyBeanSpanFixTextView(getContext());
				view.setTextSize(TypedValue.COMPLEX_UNIT_SP, Settings.fontSize(getContext()));
				view.setPadding(padding, 0, padding, 0);
			}else{
				view = (TextView) convertView;
			}
			view.setText(getItem(position), BufferType.SPANNABLE);
			return view;
		}		
		
		@Override
		public boolean isEnabled(int position) {
			return false;
		}
	}
	
	private static class StoryLoader extends AsyncTaskLoader<StoryObject>{
		private static final String STATE_DATA = "Data extra";
		private static final String STATE_UPDATED = "Updated extra";
		private static final String STATE_SCROLL = "Scroll";
		private boolean connectionError;
		private boolean sdCardError;
		Cursor cursor;
		
		private int currentPage;
		private StoryObject mData;
		private boolean dataHasChanged;
		private boolean hasUpdated;
		private final ContentObserver observer;
		private boolean scrollTo;
		private boolean firstScroll;
		
		private final long storyId;
		private int characterOffset;
		
		public StoryLoader(Context context, Bundle in, long storyId, int currentPage, int offset) {
			super(context);
			this.storyId = storyId;
			observer = new ForceLoadContentObserver();
			scrollTo = false;
			if (in != null && in.containsKey(STATE_DATA)) {
				firstScroll = false;
				characterOffset = in.getInt(STATE_SCROLL);
				mData = in.getParcelable(STATE_DATA);
				this.currentPage = mData.getCurrentPage();
				hasUpdated = in.getBoolean(STATE_UPDATED);
				dataHasChanged = false;
			}else{
				firstScroll = true;
				this.currentPage = currentPage;
				dataHasChanged = true;
				hasUpdated = false;
			}
		}
		
		@Override
		public void deliverResult(StoryObject data) {
			if (mData == null || mData.getStoryText() == null) {
				StoryObject tmp = new StoryObject(currentPage, false);
				tmp.setCurrentPage(currentPage);
				tmp.setStoryTitle("");
				tmp.setStoryText(Html.fromHtml(""));
				super.deliverResult(tmp);
			}else{
				super.deliverResult(mData.clone());
			}
		}
		
		public boolean isRunning(){
			return dataHasChanged && !connectionError && !sdCardError;
		}
		
		@Override
		public StoryObject loadInBackground() {
			// Fills data from SQLite
			// if this is the first
			// instance.
			mData = fillFromSql(storyId);
			Spanned storyText;
			
			if (mData.isInLibrary() && mData.getTotalPages() >= currentPage) {
				storyText = FileHandler.getFile(getContext(), storyId, currentPage);
				if (storyText == null) {
					Log.d("1", "");
					sdCardError = true;
					return null;
				}
			} else {
				storyText = getStoryFromSite(storyId, currentPage);
				if (storyText == null) {
					connectionError = true;
					return null;
				}
			}

			mData.setStoryText(storyText);
			mData.setCurrentPage(currentPage);

			if (firstScroll) {
				scrollTo = true;
				firstScroll = false;
			} else if (dataHasChanged) {
				scrollTo = true;
				characterOffset = 0;
			}
			dataHasChanged = false;
			return mData;
			
		}
		
		/**
		 * Reads the SQLite database and fills the data into the mResult field.
		 * 
		 * @param storyId
		 *            The numerical Id of the story
		 */
		private StoryObject fillFromSql(long storyId) {
			if (mData == null) {
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
				ContentResolver resolver = getContext().getContentResolver();
				String[] projection = {SqlConstants.KEY_TITLE, SqlConstants.KEY_CHAPTER, SqlConstants.KEY_LAST}; 
				cursor = resolver.query(StoryProvider.CONTENT_URI, projection, SqlConstants.KEY_STORY_ID + " = ?", new String[]{storyId + ""}, null);			
				StoryObject tmpObj;
				if (cursor.moveToFirst()) {
					cursor.registerContentObserver(observer);
					tmpObj = new StoryObject(cursor.getInt(1), true);
					tmpObj.setStoryTitle(cursor.getString(0));	
					currentPage = cursor.getInt(2);
				} else {
					tmpObj = new StoryObject(0, false);
				}
				return tmpObj;
				
			}else if(mData.isInLibrary() && hasUpdated){
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
				ContentResolver resolver = getContext().getContentResolver();
				String[] projection = {SqlConstants.KEY_CHAPTER}; 
				cursor = resolver.query(StoryProvider.CONTENT_URI, projection, SqlConstants.KEY_STORY_ID + " = ?", new String[]{storyId + ""}, null);			
				cursor.moveToFirst();
				mData.setTotalPages(cursor.getInt(0));				
				cursor.close();
			}
			return mData;
		}

		
		/**
		 * Reads the story from the web site
		 * 
		 * @param storyId
		 *            The numerical id of the story
		 * @param pageNumber
		 *            The chapter to fetch
		 * @param tmpObj
		 * @return The story text as a Spanned Object
		 */
		private Spanned getStoryFromSite(long storyId, int pageNumber) {
			try {
				String url = "https://m.fanfiction.net/s/" + storyId + "/"
						+ pageNumber + "/";
				org.jsoup.nodes.Document document = Jsoup.connect(url).get();
				if (mData.getTotalPages() == 0) {
					
					Element title = document.select("div#content div b").first();
					if (title == null) return null;
					mData.setStoryTitle(title.ownText());
					
					int totalPages;
					Element link = document.select("body#top div[align=center] > a:matches(^\\d++$)").first();
					if (link != null)
						totalPages = Math.max(
								pageNumberOnline(link.attr("href"), 2),
								pageNumber);
					else {
						totalPages = 1;
					}
					mData.setTotalPages(totalPages);
				}
				
				Elements storyText = document.select("div#storycontent");
				if (storyText.isEmpty()) return null;
				return Html.fromHtml(storyText.html());
				
			} catch (IOException e) {
				return null;
			}
		}
		
		private void loadPage(int page){
			dataHasChanged = true;
			currentPage = page;
			startLoading();
		}
		
		private void onSaveInstanceState(Bundle outState){
			if (mData.getStoryText() != null) {
				outState.putParcelable(STATE_DATA, mData);
			}
			outState.putBoolean(STATE_UPDATED, hasUpdated);
		}
		
		@Override
		protected void onReset() {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			super.onReset();
		}

		/**
		 * Extracts the page number or the story id from a url
		 * 
		 * @param url
		 *            The string containing the url that needs to be parsed
		 * @param group
		 *            One for the story id, two for the page number.
		 * @return Either the story id or the page number
		 */
		private int pageNumberOnline(String url, int group) {
			final Pattern currentPageNumber = Pattern
					.compile("(?:/s/(\\d{1,10}+)/)(\\d++)(?:/)");
			Matcher matcher = currentPageNumber.matcher(url);
			if (matcher.find()) {
				return Integer.valueOf(matcher.group(group));
			} else {
				return 1;
			}
		}
		
		@Override
		protected void onStartLoading() {
			connectionError = false;
			sdCardError = false;
			deliverResult(mData);
			if (dataHasChanged || mData == null) {
				forceLoad();
			}
		}

	}
	
}

class StoryObject implements Parcelable{
	public final static Parcelable.Creator<StoryObject> CREATOR = new Creator<StoryObject>() {

		@Override
		public StoryObject createFromParcel(Parcel source) {
			return new StoryObject(source);
		}

		@Override
		public StoryObject[] newArray(int size) {
			return new StoryObject[size];
		}
	};
	private int currentPage;
	private final boolean inLibrary;
	private List<Spanned> storyText;
	private String storyTitle;

	private int totalPages;

	/**
	 * Creates a new StoryObject representing the story
	 * 
	 * @param totalPages
	 *            The total number of chapters in the story
	 * @param inLibrary
	 *            True if the story is in the library
	 */
	public StoryObject(int totalPages, boolean inLibrary) {
		this.totalPages = totalPages;
		this.inLibrary = inLibrary;
	}

	private StoryObject(Parcel in) {
		inLibrary = in.readByte() != 0;
		currentPage = in.readInt();
		totalPages = in.readInt();
		storyTitle = in.readString();
		List<String> spans = new ArrayList<String>();
		in.readStringList(spans);
		
		storyText = new ArrayList<Spanned>(spans.size());
		for (String string : spans) {
			storyText.add(Html.fromHtml(string));
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * @return the currentPage
	 */
	public int getCurrentPage() {
		return currentPage;
	}

	/**
	 * @return the storyText
	 */
	public List<Spanned> getStoryText() {
		return storyText;
	}

	/**
	 * @return the storyTitle
	 */
	public String getStoryTitle() {
		return storyTitle;
	}

	/**
	 * @return the totalPages
	 */
	public int getTotalPages() {
		return totalPages;
	}

	/**
	 * @return the inLibrary
	 */
	public boolean isInLibrary() {
		return inLibrary;
	}

	/**
	 * @param currentPage
	 *            the currentPage to set
	 */
	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	/**
	 * @param storyText
	 *            the storyText to set
	 */
	public void setStoryText(Spanned storyText) {
		this.storyText = Parser.split(storyText);
	}
	
	public void setStoryText(List<Spanned> spanned){
		this.storyText = spanned;
	}

	/**
	 * @param storyTitle
	 *            the storyTitle to set
	 */
	public void setStoryTitle(String storyTitle) {
		this.storyTitle = storyTitle;
	}

	/**
	 * @param totalPages
	 *            the totalPages to set
	 */
	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByte((byte) (inLibrary ? 1 : 0));
		dest.writeInt(currentPage);
		dest.writeInt(totalPages);
		dest.writeString(storyTitle);
		
		List<String> spans = new ArrayList<String>();
		for (Spanned span : storyText) {
			spans.add(Html.toHtml(span));
		}
		dest.writeStringList(spans);
	}

	@Override
	protected StoryObject clone() {
		StoryObject tmp = new StoryObject(this.getTotalPages(),
				this.isInLibrary());
		tmp.setCurrentPage(this.getCurrentPage());
		tmp.setStoryTitle(this.getStoryTitle());
		tmp.setStoryText(this.getStoryText());
		return tmp;
	}

}
