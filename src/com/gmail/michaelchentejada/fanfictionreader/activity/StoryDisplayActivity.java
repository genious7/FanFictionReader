package com.gmail.michaelchentejada.fanfictionreader.activity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.gmail.michaelchentejada.fanfictionreader.LibraryDownloader;
import com.gmail.michaelchentejada.fanfictionreader.R;
import com.gmail.michaelchentejada.fanfictionreader.Settings;
import com.gmail.michaelchentejada.fanfictionreader.util.Story;
import com.gmail.michaelchentejada.fanfictionreader.util.DatabaseHelper;

/**
 * 
 * @author Michael Chen
 */
public class StoryDisplayActivity extends ActionBarActivity implements OnClickListener {
	private final static String STATE_CURRENT_PAGE = "com.gmail.michaelchentejada.fanfictionreader.activity.StoryDisplayActivity.currentPage";
	
	private Button btnFirst;
	private Button btnLast;
	private Button btnNext;
	
	private Button btnPageSelect;
	
	private Button btnPrev;
	/**The currently loaded page*/
	private int mCurrentPage;
	private boolean mInLibrary = false;
	/**The AsyncTask used to load the story*/
	private StoryLoader mLoader;
	/**The id of the current story*/
	private long mStoryId;
	private int mTotalPages;
	private ScrollView scrollview;
	private TextView textViewStory;
	
	/**
	 * Handles the Buttons that change pages.
	 * @param v The view identifying the button clicked.
	 */
	public void changePage(View v) {
		switch (v.getId()) {
		case R.id.read_story_first:	
			refreshList(1);
			break;
		case R.id.read_story_prev:
			refreshList(mCurrentPage - 1);
			break;
		case R.id.read_story_next:
			refreshList(mCurrentPage + 1);
			break;
		case R.id.read_story_last:
			refreshList(mTotalPages);
			break;
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.read_story_page_counter:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String[] Chapters = new String[mTotalPages];
			for (int i = 0; i < mTotalPages; i++) {
				Chapters[i] = getResources().getString(R.string.read_story_chapter) + (i+1);
			}
			builder.setItems(Chapters, new DialogInterface.OnClickListener() {
		           @Override
				public void onClick(DialogInterface dialog, int which) {
			           if (mCurrentPage != which + 1) {
						 refreshList(which + 1);
			           }
			       }
			});
			builder.setInverseBackgroundForced(true);
			builder.create();
			builder.show();
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.read_story_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.read_story_menu_update:
		case R.id.read_story_menu_add:
			Intent i = new Intent(this, LibraryDownloader.class);
			i.putExtra(LibraryDownloader.EXTRA_LAST_PAGE, mCurrentPage);
            i.putExtra(LibraryDownloader.EXTRA_STORY_ID, mStoryId);
            startService(i);   
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (menu != null) {
			if (mInLibrary) {
				menu.findItem(R.id.read_story_menu_add).setVisible(false);
				menu.findItem(R.id.read_story_menu_update).setVisible(true);
			} else {
				menu.findItem(R.id.read_story_menu_add).setVisible(true);
				menu.findItem(R.id.read_story_menu_update).setVisible(false);
			}
		}
		if (mLoader != null && mLoader.isFinished()) {
			getSupportActionBar().setSubtitle(mLoader.mResult.getStoryTitle());
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return mLoader;
	}
	
	public void toast(String text){
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
		toast.show();
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
		toast(getString(R.string.dialog_unspecified));
		Log.e("parseUri", "The URI " + uri.toString() + " is invalid.");
		return false;
	}
	
	private void refreshList(int pageNumber) {
		StoryObject tmp = mLoader.mResult;
		mLoader = new StoryLoader(this, tmp);
		mLoader.execute(mStoryId, (long) pageNumber);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);//Super() constructor first
		setContentView(R.layout.activity_read_story);//Set the layout
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		textViewStory = (TextView)findViewById(R.id.read_story_text);
		textViewStory.setTextSize(TypedValue.COMPLEX_UNIT_SP, Settings.fontSize(this));
		btnFirst = (Button)findViewById(R.id.read_story_first);
		btnPrev = (Button)findViewById(R.id.read_story_prev);
		btnNext = (Button)findViewById(R.id.read_story_next);
		btnLast = (Button)findViewById(R.id.read_story_last);
		btnPageSelect = (Button)findViewById(R.id.read_story_page_counter);
		scrollview = (ScrollView)findViewById(R.id.read_story_scrollview);
		btnPageSelect.setOnClickListener(this);
		
		if (!parseUri(getIntent().getData())) {
			finish();
			return;
		}
		
		if (savedInstanceState != null) {
			mCurrentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE);
		}
		
		mLoader = (StoryLoader)getLastCustomNonConfigurationInstance();
		if (mLoader == null) {
			mLoader = new StoryLoader(this);
			mLoader.execute(mStoryId,(long)mCurrentPage);
		} else{
			mLoader.mActivity = new WeakReference<StoryDisplayActivity>(this);
			if (mLoader.isFinished()) {
				mLoader.updateUI();
			}	
		}
		
	}
	
	@Override
	protected void onDestroy() {
		mLoader.mActivity.clear();
		super.onDestroy();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_CURRENT_PAGE, mCurrentPage);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Loads the story.
	 * @author Michael Chen
	 */
	private static class StoryLoader extends
			AsyncTask<Long, Void, StoryObject> {
		private Context mAppContext;
		private boolean mIsFinished;
		private boolean mResetScrollBar = false;
		private StoryObject mResult;
		WeakReference<StoryDisplayActivity> mActivity;

		/**
		 * Creates a new story loader AsyncTask
		 * 
		 * @param activity
		 *            The current instance of the activity
		 */
		public StoryLoader(StoryDisplayActivity activity) {
			this(activity, null);
		}

		/**
		 * Creates a new story recycling the previous results
		 * 
		 * @param activity
		 *            The current activity
		 * @param object
		 *            The object from the previous instance
		 */
		public StoryLoader(StoryDisplayActivity activity, StoryObject object) {
			super();
			mAppContext = activity.getApplicationContext();
			mActivity = new WeakReference<StoryDisplayActivity>(activity);
			mResult = object;

		}

		/**
		 * Determines whether the asyncTask has completed successfully.
		 * 
		 * @return True if the asyncTask is finished
		 */
		public boolean isFinished() {
			return mIsFinished;
		}

		/**
		 * Updates the UI and the fields
		 */
		public void updateUI() {
			if (mActivity.get() != null) {
				final StoryDisplayActivity t = mActivity.get();
				if (mResult == null) {
					t.finish();
					return;
				}
				int currentPage = mResult.getCurrentPage();

				t.mCurrentPage = currentPage;

				t.mTotalPages = mResult.getTotalPages();
				t.mInLibrary = mResult.isInLibrary();
				t.textViewStory.setText(mResult.getStoryText());
				t.btnPageSelect.setText(
						currentPage + "/" + mResult.getTotalPages(),
						BufferType.SPANNABLE);

				if (mResetScrollBar) {
					t.scrollview.post(new Runnable() {
						@Override
						public void run() {
							t.scrollview.scrollTo(0, 0);
						}
					});
					mResetScrollBar = false;
				}

				if (mResult.getTotalPages() == currentPage) {
					t.btnNext.setEnabled(false);
					t.btnLast.setEnabled(false);
				} else {
					t.btnNext.setEnabled(true);
					t.btnLast.setEnabled(true);
				}
				if (currentPage == 1) {
					t.btnPrev.setEnabled(false);
					t.btnFirst.setEnabled(false);
				} else {
					t.btnPrev.setEnabled(true);
					t.btnFirst.setEnabled(true);
				}
				
				t.supportInvalidateOptionsMenu();

				hideDialog(t);
			}
			mActivity.clear();
		}

		/**
		 * Reads the SQLite database and fills the data into the mResult field.
		 * 
		 * @param storyId
		 *            The numerical Id of the story
		 */
		private StoryObject fillFromSql(long storyId) {
			if (mResult == null) {
				// Finds the number of pages from the database.
				DatabaseHelper db = new DatabaseHelper(mAppContext);
				Story story = db.getStory(storyId);
				db.close();
				if (story == null) {
					return new StoryObject(0, false);
				} else {
					StoryObject tmpObj = new StoryObject(
							story.getChapterLenght(), true);
					tmpObj.setStoryTitle(story.getName());
					return tmpObj;
				}
			}
			return mResult.clone();
		}

		/**
		 * Reads the story from the memory
		 * 
		 * @param storyId
		 *            The id of the story
		 * @param pageNumber
		 *            The page number that should be loaded
		 * @return The story as a Spanned element
		 */
		private Spanned getStoryFromFile(long storyId, int pageNumber) {
			try {
				File file = new File(mAppContext.getFilesDir(), storyId + "_"
						+ pageNumber + ".txt");
				BufferedInputStream fin = new BufferedInputStream(
						new FileInputStream(file));
				byte[] buffer = new byte[(int) file.length()];
				fin.read(buffer);
				fin.close();
				return new SpannedString(new String(buffer));
			} catch (IOException e) {
				Log.e("loadFromFile", "Story #" + storyId + " not found.", e);
				return null;
			}
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
		private Spanned getStoryFromSite(long storyId, int pageNumber,
				StoryObject tmpObj) {
			try {
				org.jsoup.nodes.Document document = Jsoup.connect(
						"https://m.fanfiction.net/s/" + storyId + "/"
								+ pageNumber + "/").get();
				if (tmpObj.getTotalPages() == 0) {
					Element title = document.select("div#content div b")
							.first();
					if (title == null) {
						return null;
					}
					tmpObj.setStoryTitle(title.ownText());
					int totalPages;
					Element link = document.select(
							"body#top > div[align=center] > a").first();
					if (link != null)
						totalPages = Math.max(
								pageNumberOnline(link.attr("href"), 2),
								pageNumber);
					else {
						totalPages = 1;
					}
					tmpObj.setTotalPages(totalPages);
				}
				Elements storyText = document.select("div#storycontent");
				if (storyText == null) {
					return null;
				}
				return Html.fromHtml(storyText.html());
			} catch (IOException e) {
				return null;
			}
		}

		private void hideDialog(FragmentActivity activity) {
			DialogFragment prev = (DialogFragment) activity
					.getSupportFragmentManager().findFragmentByTag("progress");
			if (prev != null) {

				prev.dismiss();
			}
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

		private void showProgressDialog(FragmentActivity activity) {
			FragmentTransaction ft = activity.getSupportFragmentManager()
					.beginTransaction();
			Fragment prev = activity.getSupportFragmentManager()
					.findFragmentByTag("progress");
			if (prev != null) {
				ft.remove(prev);
			}
			DialogFragment progressDialog = new customProgressDialog();
			progressDialog.show(ft, "progress");
		}

		@Override
		protected StoryObject doInBackground(Long... params) {
			long storyId = params[0];
			int currentPage = (int) (long) params[1];
			StoryObject tmpObj = fillFromSql(storyId);// Fills data from SQLite
														// if this is the first
														// instance.
			if (tmpObj.isInLibrary()) {
				tmpObj.setStoryText(getStoryFromFile(storyId, currentPage));
			} else {
				tmpObj.setStoryText(getStoryFromSite(storyId, currentPage,
						tmpObj));
			}
			tmpObj.setCurrentPage(currentPage);
			return tmpObj;
		}

		@Override
		protected void onCancelled() {
			mAppContext = null;
			mActivity.clear();// Clear the reference to the activity
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(StoryObject result) {
			super.onPostExecute(result);
			if (result.getStoryText() == null) {
				Toast toast = Toast.makeText(mAppContext,
						mAppContext.getString(R.string.dialog_internet),
						Toast.LENGTH_SHORT);
				toast.show();
			} else {
				mResult = result;
				mResetScrollBar = true;
			}
			mAppContext = null;
			mIsFinished = true;
			updateUI();
		}

		@Override
		protected void onPreExecute() {
			mIsFinished = false;
			showProgressDialog(mActivity.get());
			super.onPreExecute();
		}

		public static class customProgressDialog extends DialogFragment {
			WeakReference<StoryDisplayActivity> mActivity;
			
			@Override
			public void onCancel(DialogInterface dialog) {
				if (mActivity != null) {
					mActivity.get().mLoader.cancel(false);
					if (mActivity.get().mLoader.mResult == null) {
						mActivity.get().finish();
					}
				}
				super.onCancel(dialog);
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				ProgressDialog progressDialog = new ProgressDialog(
						getActivity());
				progressDialog.setTitle("");
				progressDialog.setMessage(getResources().getString(
						R.string.dialog_loading));
				return progressDialog;
			}
			
			@Override
			public void onAttach(Activity activity) {
				mActivity = new WeakReference<StoryDisplayActivity>((StoryDisplayActivity) activity);
				super.onAttach(activity);
			}
			
			@Override
			public void onDetach() {
				mActivity.clear();
				super.onDetach();
			}
		}
	}

	private static class StoryObject{
		private int currentPage;
		private final boolean inLibrary;
		private Spanned storyText;
		private String storyTitle;
		private int totalPages;
		/**
		 * Creates a new StoryObject representing the story
		 * @param totalPages The total number of chapters in the story
		 * @param inLibrary True if the story is in the library
		 */
		public StoryObject(int totalPages, boolean inLibrary) {
			this.totalPages = totalPages;
			this.inLibrary = inLibrary;
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
		public Spanned getStoryText() {
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
		 * @param currentPage the currentPage to set
		 */
		public void setCurrentPage(int currentPage) {
			this.currentPage = currentPage;
		}
		/**
		 * @param storyText the storyText to set
		 */
		public void setStoryText(Spanned storyText) {
			this.storyText = storyText;
		}
		/**
		 * @param storyTitle the storyTitle to set
		 */
		public void setStoryTitle(String storyTitle) {
			this.storyTitle = storyTitle;
		}
		/**
		 * @param totalPages the totalPages to set
		 */
		public void setTotalPages(int totalPages) {
			this.totalPages = totalPages;
		}	
		
		@Override
		protected StoryObject clone() {
			StoryObject tmp = new StoryObject(this.getTotalPages(), this.isInLibrary());
			tmp.setCurrentPage(this.getCurrentPage());
			tmp.setStoryTitle(this.getStoryTitle());
			tmp.setStoryText(this.getStoryText());
			return tmp;
		}
	}
}
