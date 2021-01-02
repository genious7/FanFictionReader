package com.spicymango.fanfictionreader.menu.reviewmenu;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import java.util.Date;

/**
 * An object that corresponds to a single review
 *
 * Created by Michael Chen on 01/17/2016.
 */
public class ReviewMenuItem implements Parcelable {
	private final String mAuthor;
	private final String mText;
	private final Date  mDate;
	private final int mChapter;

	public static final Creator CREATOR = new Creator() {
		@Override
		public Object createFromParcel(Parcel source) {
			return new ReviewMenuItem(source);
		}

		@Override
		public Object[] newArray(int size) {
			return new ReviewMenuItem[size];
		}
	};

	/**
	 * Creates a new ReviewMenuItem object, which represents a single review.
	 * @param author The name of the reviewer
	 * @param text The review text
	 * @param date The date the review was posted
	 * @param chapter The chapter that was reviewed
	 */
	public ReviewMenuItem(@NonNull String author, @NonNull String text, @NonNull Date date,
						  int chapter) {
		mAuthor = author;
		mText = text;
		mDate = date;
		mChapter = chapter;
	}

	/**
	 * Restores a ReviewMenuItem from a parcel
	 *
	 * @param in
	 */
	private ReviewMenuItem(Parcel in){
		mAuthor = in.readString();
		mText = in.readString();
		mDate = new Date(in.readLong());
		mChapter = in.readInt();
	}

	@NonNull
	public String getAuthor(){
		return mAuthor;
	}

	@NonNull
	public String getText(){
		return mText;
	}

	@NonNull
	public Date getDate(){
		return mDate;
	}

	public int getChapter(){
		return mChapter;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mAuthor);
		dest.writeString(mText);
		dest.writeLong(mDate.getTime());
		dest.writeInt(mChapter);
	}
}
