package com.spicymango.fanfictionreader.menu.categorymenu;

import com.spicymango.fanfictionreader.util.Parser;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

final class CategoryMenuItem implements Parcelable {

	/**
	 * Used for parceling.
	 */
	public static final Parcelable.Creator<CategoryMenuItem> CREATOR = new Parcelable.Creator<CategoryMenuItem>() {
		@Override
		public CategoryMenuItem createFromParcel(Parcel source) {
			return new CategoryMenuItem(source);
		}

		@Override
		public CategoryMenuItem[] newArray(int size) {
			return new CategoryMenuItem[size];
		}
	};

	public final int mSortInt;
	public final String mTitle;
	public final Uri mUri;
	public final String mViews;

	/**
	 * Creates a new menu object
	 * 
	 * @param in The parcel to employ
	 */
	private CategoryMenuItem(Parcel in) {
		mTitle = in.readString();
		mViews = in.readString();
		mUri = in.readParcelable(Uri.class.getClassLoader());
		mSortInt = in.readInt();
	}

	/**
	 * Creates a new category menu object
	 * 
	 * @param mTitle The title to display
	 * @param mViews The total number of views as a string
	 * @param mUri The target url
	 */
	public CategoryMenuItem(String title, String views, Uri uri) {
		mTitle = title;
		mViews = views;
		mUri = uri;

		//Unsortable items should be in the top
		if (views.length() == 0) {
			this.mSortInt = Integer.MAX_VALUE;
		} else {
			this.mSortInt = Parser.parseInt(views);
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mTitle);
		dest.writeString(mViews);
		dest.writeParcelable(mUri, 0);
		dest.writeInt(mSortInt);
	}

	@Override
	public String toString() {
		return mTitle;
	}
}
