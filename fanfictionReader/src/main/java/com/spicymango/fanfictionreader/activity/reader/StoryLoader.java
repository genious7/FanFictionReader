package com.spicymango.fanfictionreader.activity.reader;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import com.spicymango.fanfictionreader.provider.SqlConstants;
import com.spicymango.fanfictionreader.util.Result;

public abstract class StoryLoader extends AsyncTaskLoader<StoryChapter> {
	private static final String STATE_DATA = "Data";
	
	public Result mResult;
	public boolean mScrollTo;
	
	private int mCurrentPage;
	private Cursor mCursor;
	private StoryChapter mData;
	private boolean mDataHasChanged;
	private boolean mFirstScroll;
	private final ContentObserver mObserver;
	private int mScrollOffset;
	private final long mStoryId;
	
	public StoryLoader(Context context, Bundle in, long storyId, int currentPage) {
		super(context);
		mStoryId = storyId;
		mObserver = new ForceLoadContentObserver();
		
		if (in != null && in.containsKey(STATE_DATA)) {
			//If reopening, don't scroll automatically
			mFirstScroll = false;
			mScrollTo = false;
			
			mData = in.getParcelable(STATE_DATA);
			mCurrentPage = mData.getChapterNumber();
			mDataHasChanged = false;
		}else{
			// If it is the first time opening the activity, scroll to the
			// remembered position if it exists.
			mFirstScroll = true;
			mScrollTo = true;
			
			mData = new StoryChapter();
			
			mCurrentPage = currentPage;
			mDataHasChanged = true;
			mScrollOffset = 0;
		}
	}
	

	@Override
	public void deliverResult(StoryChapter data) {
		if (mData.getStorySpans() == null) {
			StoryChapter tmp = new StoryChapter(mCurrentPage, false);
			tmp.setCurrentPage(mCurrentPage);
			tmp.setStoryTitle("");
			tmp.setStoryText("");
			super.deliverResult(tmp);
		}else{
			super.deliverResult(mData.clone());
		}
	}
	
	public final int getScrollOffset(){
		return mScrollOffset;
	}
	
	@Override
	public StoryChapter loadInBackground() {
		
		Thread.currentThread().setName("Story Loader Thread");
		
		syncSql();
		String html;
		
		if (mData.isInLibrary() && mData.getTotalChapters() >= mCurrentPage) {
			try {
				html = getStoryFromFile(mStoryId, mCurrentPage);
				if (html == null) {
					redownload(mStoryId, mCurrentPage);
					throw new FileNotFoundException();
				}
			} catch (FileNotFoundException e) {
				mResult = Result.ERROR_SD;
				return null;
			}
		} else {
			try {
				html = getStoryFromSite(mStoryId, mCurrentPage, mData);
				if (html == null) {
					mResult = Result.ERROR_PARSE;
					return null;
				}
			} catch (IOException e) {
				mResult = Result.ERROR_CONNECTION;
				return null;
			}
		}

		mData.setStoryText(html);
		mData.setCurrentPage(mCurrentPage);
		
		//Do not reset scroll to zero if opening a saved story
		if (mFirstScroll) {
			mScrollTo = true;
			mFirstScroll = false;
		}else if(mDataHasChanged) {
			mScrollTo = true;
			mScrollOffset = 0;
		}
		
		mDataHasChanged = false;
		
		mResult = Result.SUCCESS;
		return mData;
	}
	
	/**
	 * Loads the requested page
	 * @param page The page to load
	 */
	public final void loadPage(int page){
		mDataHasChanged = true;
		mCurrentPage = page;
		startLoading();
	}
	
	public void onSaveInstanceState(Bundle outState){
		if (mData.getStorySpans() != null) {
			outState.putParcelable(STATE_DATA, mData);
		}
	}
	
	private void syncSql(){
		//If the cursor is null, create a new cursor and register it
		if(mCursor == null){
			mCursor = getFromDatabase(mStoryId);
			mCursor.registerContentObserver(mObserver);
		//If the loader was triggered by the content observer, update the cursor
		}else if(!mDataHasChanged){
			mCursor.unregisterContentObserver(mObserver);
			mCursor.close();
			mCursor = getFromDatabase(mStoryId);
			mCursor.registerContentObserver(mObserver);
		}
		
		//If the story has been downloaded, update the values from here on every operation
		if(mCursor != null && mCursor.moveToFirst()){
			mData.setAuthorId(mCursor.getLong(mCursor.getColumnIndexOrThrow(SqlConstants.KEY_AUTHOR_ID)));
			mData.setStoryTitle(mCursor.getString(mCursor.getColumnIndexOrThrow(SqlConstants.KEY_TITLE)));
			mData.setTotalChapters(mCursor.getInt(mCursor.getColumnIndexOrThrow(SqlConstants.KEY_CHAPTER)));
			mData.setInLibrary(true);
			
			if(mFirstScroll){
				mCurrentPage = mCursor.getInt(mCursor.getColumnIndexOrThrow(SqlConstants.KEY_LAST));
				mScrollOffset = mCursor.getInt(mCursor.getColumnIndexOrThrow(SqlConstants.KEY_OFFSET));
			}
		}
	}
	
	
	protected abstract Cursor getFromDatabase(final long storyId);
	
	/**
	 * Gets the requested story from the file system
	 * @param storyId The id of the target story
	 * @param currentPage The target page number
	 * @return The html document containing the story
	 * @throws FileNotFoundException If the requested file is not found 
	 */
	protected abstract String getStoryFromFile(final long storyId,final int currentPage) throws FileNotFoundException;
	
	protected abstract String getStoryFromSite(final long storyId,final int currentPage, StoryChapter data) throws IOException;

	protected abstract void redownload(final long storyId, final int currentPage);
	
	/**
	 * Closes the cursor when finished
	 */
	@Override
	protected void onReset() {
		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.unregisterContentObserver(mObserver);
			mCursor.close();
		}
		super.onReset();
	}
	
	@Override
	protected final void onStartLoading() {
		if (mDataHasChanged) {
			mResult = Result.LOADING;
			deliverResult(mData);
			forceLoad();
		}else{
			mResult = Result.SUCCESS;
			deliverResult(mData);
		}
	}
}

