package com.spicymango.fanfictionreader.activity.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.spicymango.fanfictionreader.LibraryDownloader;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.activity.LogInActivity;
import com.spicymango.fanfictionreader.activity.Site;
import com.spicymango.fanfictionreader.dialogs.ReviewDialog;
import com.spicymango.fanfictionreader.menu.mainmenu.MainActivity;
import com.spicymango.fanfictionreader.provider.SqlConstants;
import com.spicymango.fanfictionreader.provider.StoryProvider;
import com.spicymango.fanfictionreader.util.AsyncPost;
import com.spicymango.fanfictionreader.util.FileHandler;
import com.spicymango.fanfictionreader.util.adapters.TextAdapter;

public class StoryDisplayActivity extends AppCompatActivity implements LoaderCallbacks<StoryChapter>, OnClickListener{
	private static final String STATE_UPDATED = "HasUpdated";
	
	/**
	 * Opens the story with the selected id. If the story already exists in the
	 * library, the story will open in the last position read. Otherwise, it
	 * will open on the first page.
	 * 
	 * @param context The current context
	 * @param id The id of the story
	 * @param site The desired site
	 * @param autoUpdate True to update the story if on library, false otherwise
	 */
	public static void openStory(Context context, long id, Site site, boolean autoUpdate){
		Uri uri;
		Uri.Builder builder = new Uri.Builder();

		builder.scheme(autoUpdate ? Site.scheme : "file") // Scheme
				.authority(site.authorityMobile) // Authority
				.appendPath("s") // Story
				.appendPath(Long.toString(id)) // Id
				.appendPath("1") // Chapter 1
				.appendPath(""); // Adds the '/'
		uri = builder.build();
		
		Intent i = new Intent(context, StoryDisplayActivity.class);
		i.setData(uri);
		context.startActivity(i);	
	}
	
	/**
	 * The button at the footer
	 */
	private View btnFirst, btnPrev, btnNext, btnLast, progressBar, recconectBar, buttonBar;
	
	private TextView btnPage;
	private StoryChapter mData;
	private boolean fromBrowser = false;
	private BaseAdapter mAdapter;
	private long mAuthorId;
	private int mCurrentPage;
	private List<Spanned> mList;
	private ListView mListView;
	private StoryLoader mLoader;
	private boolean mHasUpdated;
	
	private Toolbar mToolbar;
	private boolean isToolBarVisible = true;
	
	/**
	 * Currently loaded site
	 */
	private Site mSite;
	private long mStoryId;
	
	private int mTotalPages;
	
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
			load(mTotalPages);
			break;
		case R.id.read_story_page_counter:
			chapterPicker();
			break;
		case R.id.btn_retry:
			mLoader.startLoading();
			break;
		}	
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		final int stepSize = 100;

		if (Settings.volumeButtonsScrollStory(this)) {
			if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
				mListView.smoothScrollBy(stepSize, 100);
				return true;
			} else if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
				mListView.smoothScrollBy(-stepSize, 100);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public Loader<StoryChapter> onCreateLoader(int id, Bundle args) {
		return new FanFictionLoader(this, args, mStoryId, mCurrentPage);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.read_story_menu, menu);
		menu.setGroupVisible(R.id.account_group, LogInActivity.isLoggedIn(this));
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public void onLoaderReset(Loader<StoryChapter> loader) {
	}

	@Override
	public void onLoadFinished(Loader<StoryChapter> loader, StoryChapter data) {
		
		mLoader = (StoryLoader) loader;
		
		switch (mLoader.mResult) {
		case LOADING:
			recconectBar.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			buttonBar.setVisibility(View.GONE);
			break;
		case ERROR_CONNECTION:
			recconectBar.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			buttonBar.setVisibility(View.GONE);
			break;
		case ERROR_SD:
			TextView btn = (TextView) findViewById(R.id.label_retry);
			btn.setText(R.string.error_sd);
			recconectBar.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			buttonBar.setVisibility(View.GONE);
			break;
		case SUCCESS:
			mData = data;
			mTotalPages = data.getTotalChapters();
			mCurrentPage = data.getChapterNumber();
			getSupportActionBar().setSubtitle(data.getStoryTitle());
			
			mList.clear();
			mList.addAll(data.getStorySpans());
			mAdapter.notifyDataSetChanged();											//Update story text
			
			if (mLoader.mScrollTo) {														//Scroll to desired position, if needed
				scrollTo(mLoader.getScrollOffset());
				mLoader.mScrollTo = false;
			}
			
			recconectBar.setVisibility(View.GONE);
			progressBar.setVisibility(View.GONE);
			buttonBar.setVisibility(View.VISIBLE);
			updatePageButtons(mCurrentPage, mTotalPages);								//Set button visibility
			
			if (fromBrowser && data.isInLibrary() && !mHasUpdated) {				//Update the story if required
				mHasUpdated = true;
				LibraryDownloader.download(this, mStoryId, mCurrentPage, getOffset());
			}
			
			supportInvalidateOptionsMenu();												//Required upon update to disable "update" button
		default:
			break;
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.read_story_menu_add:
			mHasUpdated = true;
			LibraryDownloader.download(this, mStoryId, mCurrentPage, getOffset());  
			supportInvalidateOptionsMenu();
			return true;
		case R.id.read_story_go_to:
			chapterPicker();
			return true;
		case R.id.follow:
		case R.id.favorite:	
			//TODO: Fictionpress and archieve of our own follows
			Uri.Builder builder = new Uri.Builder();
			builder.scheme(Site.scheme);
			builder.authority(Site.FANFICTION.authorityMobile);
			builder.appendEncodedPath("m");
			builder.appendEncodedPath("subs.php");
			builder.appendQueryParameter("uid", Long.toString(mAuthorId));
			builder.appendQueryParameter("sid", Long.toString(mStoryId));
			builder.appendQueryParameter("src", "s");
			builder.appendQueryParameter("ch", Long.toString(mCurrentPage));
			
			if (item.getItemId() == R.id.follow) {
				builder.appendQueryParameter("salert", "1");
				new AsyncPost(this,R.string.toast_added_follows, builder.build(), Method.GET).execute();
			}else{
				builder.appendQueryParameter("favs", "1");
				new AsyncPost(this,R.string.toast_added_favs, builder.build(), Method.GET).execute();
			}
			
			return true;
		case R.id.read_story_go_to_top:
			scrollTo(0);
			return true;
		case R.id.read_story_go_to_bottom:
			mListView.setSelection(mListView.getCount() - 1);
			return true;
		case R.id.review:
			ReviewDialog.review(this, mStoryId, mCurrentPage);
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
		if ( mData != null && mData.isInLibrary()) {
			item.setIcon(R.drawable.ic_action_refresh);
			item.setTitle(R.string.read_story_update);
			item.setTitleCondensed(getString(R.string.read_story_update_condensed));
		} else {
			item.setIcon(R.drawable.ic_action_download);
			item.setTitle(R.string.read_story_add);
			item.setTitleCondensed(getString(R.string.read_story_add_condensed));
		}

		if (mHasUpdated) {
			item.setEnabled(false);
			item.getIcon().setAlpha(64);
		}else{
			item.setEnabled(true);
			item.getIcon().setAlpha(255);
		}
		
		if (!mList.isEmpty()) {
			menu.setGroupEnabled(R.id.go_to_group, true);
		}	
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	/**
	 * Opens the dialog for the "go to chapter button"
	 */
	private void chapterPicker() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String[] Chapters = new String[mTotalPages];
		for (int i = 0; i < mTotalPages; i++) {
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
		builder.create();
		builder.show();
	}
	
	/**
	 * Gets the first visible character at the current scroll position
	 * @return The index of the first visible character
	 */
	private int getOffset(){
		int offset = 0;
		int firstPosition = mListView.getFirstVisiblePosition();
		for (int j = 0; j < firstPosition; j++) {
			offset += mList.get(j).length();
		}		
		return offset;
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

		mSite = Site.fromAuthority(uri.getAuthority());
		
		switch (mSite) {
		case FANFICTION:
		case FICTIONPRESS:
			// TODO: Make links work without chapter numbers
			Pattern filePattern = Pattern.compile("/s/(\\d++)/(\\d++)/");
			Matcher matcher = filePattern.matcher(uri.toString());
			if (matcher.find()) {
				fromBrowser = !uri.getScheme().equals("file");
				mStoryId = Integer.valueOf(matcher.group(1));
				mCurrentPage = Integer.valueOf(matcher.group(2));
				return true;
			}
			break;
		case ARCHIVE_OF_OUR_OWN:
			// TODO: Add archive of our own
			break;
		}
		return false;
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
			if (i >= mList.size()) {
				i = mList.size() - 1;
				break;
			}
		}

		mListView.setSelectionFromTop(i, 0);
	}

	/**
	 * Updates the buttons to reflect the current position along the story
	 * 
	 * @param currentPage The currently selected page
	 * @param totalPages The total number of pages
	 */
	@SuppressLint("SetTextI18n")
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
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private void showAndHideToolbar(boolean showToolbar) {
		// If the ToolBar visibility is not the desired visibility
		if (isToolBarVisible != showToolbar) {
			isToolBarVisible = showToolbar;

			if (showToolbar) {
				mToolbar.animate().translationY(0).alpha(1).setDuration(300)
						.setInterpolator(new DecelerateInterpolator());
			} else {
				mToolbar.animate().translationY(-mToolbar.getBottom()).alpha(1)
						.setDuration(300)
						.setInterpolator(new DecelerateInterpolator());
			}

		}
	}

	@SuppressLint("InflateParams")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndThemeNoActionBar(this);	//Sets the theme according to user settings
		super.onCreate(savedInstanceState);					//Super() constructor
		setContentView(R.layout.activity_list_toolbar);		//Set the layout

		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mList = new ArrayList<Spanned>();
		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setKeepScreenOn(Settings.isWakeLockEnabled(this));
		View footer = getLayoutInflater().inflate(R.layout.footer_read_story, null);
		mListView.addFooterView(footer, null, false);
		mListView.setDividerHeight(0);						//No line in between paragraphs
		mAdapter = new TextAdapter(this, mList);
		mListView.setAdapter(mAdapter);

		int currentApiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentApiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1){
			mListView.setOnScrollListener(new ListViewHider());
		}


		btnFirst = footer.findViewById(R.id.read_story_first);
		btnPrev = footer.findViewById(R.id.read_story_prev);
		btnNext = footer.findViewById(R.id.read_story_next);
		btnLast = footer.findViewById(R.id.read_story_last);
		btnPage = (Button)findViewById(R.id.read_story_page_counter);

		btnFirst.setOnClickListener(this);
		btnPrev.setOnClickListener(this);
		btnNext.setOnClickListener(this);
		btnLast.setOnClickListener(this);
		btnPage.setOnClickListener(this);

		progressBar = footer.findViewById(R.id.progress_bar);
		recconectBar = footer.findViewById(R.id.row_retry);
		buttonBar = footer.findViewById(R.id.buttonBar);

		//Creates a new activity, and sets the initial page and story Id
		if (!parseUri(getIntent().getData())) {
			Toast toast = Toast.makeText(this, R.string.error_parsing, Toast.LENGTH_SHORT);
			toast.show();
			finish();
			return;
		}

		if(savedInstanceState == null){
			mHasUpdated = false;
		}else{
			mHasUpdated = savedInstanceState.getBoolean(STATE_UPDATED);
		}

		getSupportLoaderManager().initLoader(0, savedInstanceState, this);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mLoader.onSaveInstanceState(outState);
		outState.putBoolean(STATE_UPDATED, mHasUpdated);
	}
	
	@Override
	protected void onStop() {
		//TODO: Archive of our own & FictionPress
		if (mData != null && mData.isInLibrary()) {
			ContentResolver resolver = getContentResolver();
			AsyncQueryHandler handler = new AsyncQueryHandler(resolver){};
			
			int offset = getOffset();
			
			ContentValues values = new ContentValues(1);
			values.put(SqlConstants.KEY_LAST, mCurrentPage);
			values.put(SqlConstants.KEY_OFFSET, offset);
			handler.startUpdate(0, null, StoryProvider.FF_CONTENT_URI, values,
					SqlConstants.KEY_STORY_ID + " = ?",
					new String[] { String.valueOf(mStoryId) });
			
			SharedPreferences preference = getSharedPreferences(MainActivity.EXTRA_PREF,MODE_PRIVATE);
			SharedPreferences.Editor editor = preference.edit();
			editor.putLong(MainActivity.EXTRA_RESUME_ID, mStoryId);
			editor.commit();
		}
		super.onStop();
	}

	private class ListViewHider implements OnScrollListener{
		private final int pixelThreshold;
		private int oldFirstVisibleItem;
		private int oldFirstVisibleItemY;
		private int yAccumulator;
		private int oldFirstVisibleItemHeigth;
		
		public ListViewHider() {
			oldFirstVisibleItem = 0;
			pixelThreshold = getResources().getDimensionPixelSize(R.dimen.main_menu_icon_size);
		}
				
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			
			//If there is no text, show the action bar
			if (visibleItemCount == 0){
				showAndHideToolbar(true);
				return;
			}
			
			final int dy;
			
			//Get the first visible item
			View firstItem = view.getChildAt(0);
			
			//If near the top, show the action bar regardless of scroll direction
			if (firstVisibleItem == 0 && firstItem.getTop() > 0) {
				showAndHideToolbar(true);
				return;
			}
			
			//If scrolling downwards, either the first visible item or the y offset will change
			//The first If statement handles when the first visible item changes
			if (firstVisibleItem > oldFirstVisibleItem) {
				dy = firstItem.getTop() - (oldFirstVisibleItemHeigth + oldFirstVisibleItemY);
			}
			//If scrolling within the same item, the distance scrolled is just the offset of the item
			else if (firstVisibleItem == oldFirstVisibleItem){
				dy = firstItem.getTop() - oldFirstVisibleItemY;
			}
			//If scrolling upwards and a new item is visible
			else{
				dy = firstItem.getTop() + firstItem.getHeight() - oldFirstVisibleItemY;
			} 
			
			// Compute how much the user has scrolled in a specific direction;
			// checks the sign bit to determine if direction has changed
			yAccumulator = (yAccumulator ^ dy) >> 31 == 0 ? yAccumulator + dy : dy ;
			
			if (yAccumulator > pixelThreshold) {
				showAndHideToolbar(true);
			} else if (yAccumulator < -pixelThreshold){
				showAndHideToolbar(false);
			}
			
			oldFirstVisibleItem = firstVisibleItem;
			oldFirstVisibleItemHeigth = firstItem.getHeight();
			oldFirstVisibleItemY = firstItem.getTop();
		}
	}
	
	private static class FanFictionLoader extends StoryLoader{

		public FanFictionLoader(Context context, Bundle in, long storyId,
				int currentPage) {
			super(context, in, storyId, currentPage);
		}

		@Override
		protected String getStoryFromFile(long storyId, int currentPage) {
			return FileHandler.getFile(getContext(), storyId, currentPage);
		}

		@Override
		protected String getStoryFromSite(long storyId, int currentPage, StoryChapter data) throws IOException {
			Uri.Builder builder = new Uri.Builder();
			builder.scheme(Site.scheme);
			builder.authority(Site.FANFICTION.authorityMobile);
			builder.appendEncodedPath("s");
			builder.appendEncodedPath(Long.toString(storyId));
			builder.appendEncodedPath(Integer.toString(currentPage));
			builder.appendEncodedPath("");
			
			Document document = Jsoup.connect(builder.toString()).timeout(10000).get();
			
			if (data.getTotalChapters() == 0) {
				Element title = document.select("div#content div b").first();
				if (title == null) return null;
				data.setStoryTitle(title.ownText());
				
				int totalPages;
				Element link = document.select("body#top div[align=center] > a:matches(^\\d++$)").first();
				if (link != null)
					totalPages = Math.max(
							pageNumberOnline(link.attr("href"), 2),
							currentPage);
				else {
					totalPages = 1;
				}
				data.setTotalChapters(totalPages);
				
				Element authorElement = document.select("input[name=uid]").first();
				long authorId = Long.parseLong(authorElement.attr("value"));
				data.setAuthorId(authorId);
			}
			
			Elements storyText = document.select("div#storycontent");
			if (storyText.isEmpty()) return null;
			return storyText.html();
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
		protected Cursor getFromDatabase(long storyId) {
			Cursor c = getContext().getContentResolver().query(StoryProvider.FF_CONTENT_URI, null,
			SqlConstants.KEY_STORY_ID + " = ?",
			new String[] {Long.toString(storyId)}, null);
			return c;
		}
	}
}
