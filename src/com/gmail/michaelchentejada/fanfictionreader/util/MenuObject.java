package com.gmail.michaelchentejada.fanfictionreader.util;

import android.os.Parcel;
import android.os.Parcelable;

public class MenuObject implements Parcelable {

	/**
	 * Used for parceling.
	 */
	public static final Parcelable.Creator<MenuObject> CREATOR = new Parcelable.Creator<MenuObject>() {
		
		@Override
		public MenuObject createFromParcel(Parcel source) {
			return new MenuObject(source);
		}

		@Override
		public MenuObject[] newArray(int size) {
			return new MenuObject[size];
		}
	};
	private final int mSortInt;
	private final String mTitle;
	private final String mUri;
	
	private final String mViews;

	/**
	 * Creates a new menu object
	 * @param in The parcel to employ
	 */
	public MenuObject(Parcel in) {
		mTitle = in.readString();
		mViews = in.readString();
		mUri = in.readString();
		mSortInt = in.readInt();
	}

	/**
	 * @param mTitle
	 * @param mViews
	 * @param mUri
	 * @param mSortInt
	 */
	public MenuObject(String title, String views, String string, int sortInt) {
		this.mTitle = title;
		this.mViews = views;
		this.mUri = string;
		this.mSortInt = sortInt;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	/**
	 * @return the SortInt
	 */
	public int getSortInt() {
		return mSortInt;
	}

	/**
	 * @return the Title
	 */
	public String getTitle() {
		return mTitle;
	}

	/**
	 * @return the Uri
	 */
	public String getUri() {
		return mUri;
	}

	/**
	 * @return the Views
	 */
	public String getViews() {
		return mViews;
	}
	
	
	public boolean hasViewsField(){
		return !(mViews == "");
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mTitle);
		dest.writeString(mViews);
		dest.writeString(mUri);
		dest.writeInt(mSortInt);
	}
}
