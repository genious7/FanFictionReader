package com.spicymango.fanfictionreader.activity.reader;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;

import com.spicymango.fanfictionreader.util.Parser;

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
	
	private long authorId;
	
	private int currentPage, totalPages;
	
	private String storyTitle;
	private List<Spanned> storyText;
	
	private boolean inLibrary;
	
	/**
	 * Creates a new StoryObject representing the story
	 */
	public StoryObject() {
	}
	
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
		authorId = in.readLong();
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

	public long getAuthorId() {
		return authorId;
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

	public void setAuthorId(long authorId) {
		this.authorId = authorId;
	}

	/**
	 * @param currentPage
	 *            the currentPage to set
	 */
	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	public void setStoryText(List<Spanned> spanned){
		this.storyText = spanned;
	}
	
	/**
	 * @param storyText
	 *            the storyText to set
	 */
	public void setStoryText(Spanned storyText) {
		this.storyText = Parser.split(storyText);
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
		dest.writeLong(authorId);
		
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
		tmp.setAuthorId(this.getAuthorId());
		return tmp;
	}

	public void setInLibrary(boolean inLibrary) {
		this.inLibrary = inLibrary;
	}

}