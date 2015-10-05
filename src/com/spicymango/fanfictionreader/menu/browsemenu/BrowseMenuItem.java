package com.spicymango.fanfictionreader.menu.browsemenu;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

final class BrowseMenuItem implements Parcelable {

	public final String title;
	public final Uri uri;

	public static final Parcelable.Creator<BrowseMenuItem> CREATOR = new Parcelable.Creator<BrowseMenuItem>() {
		public BrowseMenuItem createFromParcel(Parcel in) {
			return new BrowseMenuItem(in);
		}

		public BrowseMenuItem[] newArray(int size) {
			return new BrowseMenuItem[size];
		}
	};

	public BrowseMenuItem(Parcel in) {
		title = in.readString();
		uri = in.readParcelable(Uri.class.getClassLoader());
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeParcelable(uri, 0);
	}

	public BrowseMenuItem(final String title,final Uri uri) {
		this.title = title;
		this.uri = uri;
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
