package com.spicymango.fanfictionreader.fragments;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.provider.SqlConstants;
import com.spicymango.fanfictionreader.provider.StoryProvider;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class StoryDisplayFragment extends Fragment implements
		LoaderCallbacks<Spanned> {
	
	private static final int LOADER_ID = 0;
	private static final String EXTRA_URI = "extra_uri";
	
	private static final String SAVED_ID = "saved_id";
	private static final String SAVED_CURRENT = "saved_current";
	
	private TextView storyText;	
	private long mStoryId;
	private int mCurrentPage;

	@Override
	public Loader<Spanned> onCreateLoader(int arg0, Bundle arg1) {
		switch (arg0) {
		case LOADER_ID:
			return new StoryLoader(getActivity(), mStoryId, mCurrentPage);
		default:
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Spanned> arg0, Spanned arg1) {
		if (arg1 == null) {
			
		}else{
			storyText.setText(arg1, BufferType.SPANNABLE);
		}
	}

	@Override
	public void onLoaderReset(Loader<Spanned> arg0) {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_read_story, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		
		if (savedInstanceState == null) {
			parseUri((Uri) getArguments().getParcelable(EXTRA_URI));
		}else{
			mStoryId = savedInstanceState.getLong(SAVED_ID);
			mCurrentPage = savedInstanceState.getInt(SAVED_CURRENT);
		}
		
		getLoaderManager().initLoader(LOADER_ID, savedInstanceState, this);
		storyText = (TextView) getView().findViewById(R.id.read_story_text);
		super.onActivityCreated(savedInstanceState);
	}	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putLong(SAVED_ID, mStoryId);
		outState.putInt(SAVED_CURRENT, mCurrentPage);
		super.onSaveInstanceState(outState);
	}
	
	/**
	 * Parses the Uri.
	 * @param uri
	 * @return True if the Uri was valid, false otherwise.
	 */
	private boolean parseUri(Uri uri){
		Pattern filePattern = Pattern.compile("/s/(\\d++)/(\\d++)/");
		Matcher matcher = filePattern.matcher(uri.toString());
		if (matcher.find()) {
			mStoryId = Long.parseLong(matcher.group(1));
			mCurrentPage = Integer.parseInt(matcher.group(2));
			return true;
		}
		return false;
	}
	
	private static class StoryLoader extends AsyncTaskLoader<Spanned>{
		
		private String mTitle;
		private boolean mDataHasChanged;
		private boolean isInLibrary;
		
		private Spanned mData;
		
		private long mStoryId;
		private int mCurrentPage;
		private int mTotalPages;
		
		/**
		 * Creates a new Story Loader
		 * @param context The current context
		 * @param storyId The Id of the story
		 * @param pageNumber The page to load
		 */
		public StoryLoader(Context context, long storyId, int pageNumber) {
			super(context);
			mDataHasChanged = true;
			mTotalPages = 0;
			mStoryId = storyId;
			mCurrentPage = pageNumber;
		}
		
		@Override
		protected void onStartLoading() {
			if (mData != null) {
				deliverResult(mData);
			}	
			if (mDataHasChanged || mData == null) {
				forceLoad();
			}
			super.onStartLoading();
		}

		@Override
		public Spanned loadInBackground() {
			
			//First load
			if (mTotalPages == 0) {
				onFirstLoad();	
			}
			
			Spanned data;
			if (isInLibrary) {
				data = getStoryFromFile();
			}else{
				data = getStoryFromSite();
			}
			
			if (data != null) {
				mDataHasChanged = false;
			}
			
			return data;
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
		private Spanned getStoryFromSite() {
			try {
				org.jsoup.nodes.Document document = Jsoup.connect(assembleUrl()).get();
				if (mTotalPages == 0) {
					Element title = document.select("div#content div b").first();
					
					if (title == null) return null;
					
					mTitle = title.ownText();
					
					int totalPages;
					Element link = document.select(
							"body#top > div[align=center] > a").first();
					if (link != null)
						totalPages = Math.max(
								pageNumberOnline(link.attr("href"), 2),
								mCurrentPage);
					else {
						totalPages = 1;
					}
					mTotalPages = totalPages;
				}
				
				Elements storyText = document.select("div#storycontent");
				if (storyText == null) return null;
				
				return Html.fromHtml(storyText.html());
			} catch (IOException e) {
				return null;
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
		private static int pageNumberOnline(String url, int group) {
			final Pattern currentPageNumber = Pattern
					.compile("(?:/s/(\\d{1,10}+)/)(\\d++)(?:/)");
			Matcher matcher = currentPageNumber.matcher(url);
			if (matcher.find()) {
				return Integer.valueOf(matcher.group(group));
			} else {
				return 1;
			}
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
		private Spanned getStoryFromFile() {
			try {
				File file = new File(getContext().getFilesDir(), mStoryId + "_"
						+ mCurrentPage + ".txt");
				BufferedInputStream fin = new BufferedInputStream(
						new FileInputStream(file));
				byte[] buffer = new byte[(int) file.length()];
				fin.read(buffer);
				fin.close();
				return new SpannedString(new String(buffer));
			} catch (IOException e) {
				return null;
			}
		}
		
		/**
		 * Assembles an Url pointing to the FanFiction story.
		 * @return An Url corresponding to the FanFiction page
		 */
		private String assembleUrl(){
			return "https://m.fanfiction.net/s/" + mStoryId + "/"
					+ mCurrentPage + "/";
	
		}
		
		/**
		 * Executes methods that only need to be executed on the first load.
		 */
		private void onFirstLoad(){
			String[] projection = {SqlConstants.KEY_TITLE, SqlConstants.KEY_LENGHT};
			String[] selectionArgs = {String.valueOf(mStoryId)};
			Cursor c = getContext().getContentResolver().query(StoryProvider.CONTENT_URI, projection, SqlConstants.KEY_STORY_ID + " = ?", selectionArgs, null);
			if (c != null && c.moveToFirst()) {
				isInLibrary = true;
				mTitle = c.getString(c.getColumnIndex(SqlConstants.KEY_TITLE));
				mTotalPages = c.getInt(c.getColumnIndex(SqlConstants.KEY_LENGHT));
			}else{
				isInLibrary = false;
			}
			if (c != null) {
				c.close();
			}
		}

		@Override
		public void deliverResult(Spanned data) {
			if (data != null) {
				mData = data;
			}
			super.deliverResult(mData);
		}
	}
}