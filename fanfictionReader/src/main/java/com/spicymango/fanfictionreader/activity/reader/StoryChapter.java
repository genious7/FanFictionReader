package com.spicymango.fanfictionreader.activity.reader;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import android.text.Spanned;

import com.spicymango.fanfictionreader.util.HtmlParser;
import com.spicymango.fanfictionreader.util.Parser;

import java.util.List;

/**
 * A parcelable class that represents a single story chapter
 */
class StoryChapter implements Parcelable, Cloneable {
	public final static Parcelable.Creator<StoryChapter> CREATOR = new Creator<StoryChapter>() {

		@Override
		public StoryChapter createFromParcel(Parcel source) {
			return new StoryChapter(source);
		}

		@Override
		public StoryChapter[] newArray(int size) {
			return new StoryChapter[size];
		}
	};

	private long mAuthorId;
	private int mChapterNumber, mTotalChapters;
	private String mStoryTitle;
	private String mTextHtml;
	private List<Spanned> mTextSpans;
	private boolean mIsInLibrary;

	/**
	 * Creates a new StoryChapter representing the story
	 */
	public StoryChapter() {
	}

	/**
	 * Creates a new StoryChapter representing the story
	 *
	 * @param mTotalChapters The total number of chapters in the story
	 * @param inLibrary      True if the story is in the library
	 */
	public StoryChapter(int mTotalChapters, boolean inLibrary) {
		this.mTotalChapters = mTotalChapters;
		this.mIsInLibrary = inLibrary;
	}

	private StoryChapter(Parcel in) {
		mIsInLibrary = in.readByte() != 0;
		mChapterNumber = in.readInt();
		mTotalChapters = in.readInt();
		mStoryTitle = in.readString();
		mAuthorId = in.readLong();

		// Create the spanned story text from the html
		setStoryText(in.readString());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByte((byte) (mIsInLibrary ? 1 : 0));
		dest.writeInt(mChapterNumber);
		dest.writeInt(mTotalChapters);
		dest.writeString(mStoryTitle);
		dest.writeLong(mAuthorId);
		dest.writeString(mTextHtml);
	}

	public long getAuthorId() {
		return mAuthorId;
	}

	public void setAuthorId(long authorId) {
		this.mAuthorId = authorId;
	}

	/**
	 * @return This chapter's number
	 */
	public int getChapterNumber() {
		return mChapterNumber;
	}

	@Nullable
	public String getStoryHtml() {
		return mTextHtml;
	}

	/**
	 * @return the mTextSpans
	 */
	@Nullable
	public List<Spanned> getStorySpans() {
		return mTextSpans;
	}

	/**
	 * @return the mStoryTitle
	 */
	@Nullable
	public String getStoryTitle() {
		return mStoryTitle;
	}

	/**
	 * @param storyTitle the mStoryTitle to set
	 */
	public void setStoryTitle(@Nullable String storyTitle) {
		this.mStoryTitle = storyTitle;
	}

	/**
	 * @return The total number of chapters
	 */
	public int getTotalChapters() {
		return mTotalChapters;
	}

	/**
	 * @param totalChapters the mTotalChapters to set
	 */
	public void setTotalChapters(int totalChapters) {
		this.mTotalChapters = totalChapters;
	}

	/**
	 * @return If the current chapter is in the library
	 */
	public boolean isInLibrary() {
		return mIsInLibrary;
	}

	/**
	 * Sets whether the current chapter has been downloaded to the library
	 *
	 * @param isInLibrary
	 */
	public void setInLibrary(boolean isInLibrary) {
		this.mIsInLibrary = isInLibrary;
	}

	/**
	 * @param currentChapter This chapter's number
	 */
	public void setCurrentPage(int currentChapter) {
		this.mChapterNumber = currentChapter;
	}

	/**
	 * @param html The chapter text, as raw html
	 */
	public void setStoryText(@Nullable String html) {
		mTextHtml = html;
		if (html == null) {
			mTextSpans = null;
		} else {
			mTextSpans = Parser.split(HtmlParser.fromHtml(mTextHtml));
		}
	}

	/**
	 * Creates a shallow copy of the StoryChapter
	 * @return
	 */
	@Override
	protected StoryChapter clone() {
		StoryChapter tmp = new StoryChapter();
		tmp.mIsInLibrary = mIsInLibrary;
		tmp.mChapterNumber = mChapterNumber;
		tmp.mTotalChapters = mTotalChapters;
		tmp.mAuthorId = mAuthorId;
		tmp.mStoryTitle = mStoryTitle;
		tmp.mTextHtml = mTextHtml;
		tmp.mTextSpans = mTextSpans;
		return tmp;
	}

}