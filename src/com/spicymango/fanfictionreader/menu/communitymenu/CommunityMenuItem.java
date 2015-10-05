package com.spicymango.fanfictionreader.menu.communitymenu;

import java.util.Date;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contains the parameters that define a community.
 * 
 * @author Michael Chen
 */
final class CommunityMenuItem implements Parcelable {
	protected String title;
	protected Uri uri;
	protected String author;
	protected String summary;
	protected String stories;
	private String languague;
	private int staff;
	private int follows;
	private Date published;

	public static final Parcelable.Creator<CommunityMenuItem> CREATOR = new Creator<CommunityMenuItem>() { // NO_UCD (unused code)

		@Override
		public CommunityMenuItem createFromParcel(Parcel source) {
			return new CommunityMenuItem(source);
		}

		@Override
		public CommunityMenuItem[] newArray(int size) {
			return new CommunityMenuItem[size];
		}
	};

	/**
	 * A constructor used for creating a community item from a parcel.
	 * 
	 * @param in
	 */
	private CommunityMenuItem(Parcel in) {
		title = in.readString();
		uri = in.readParcelable(Uri.class.getClassLoader());
		author = in.readString();
		summary = in.readString();
		stories = in.readString();
		languague = in.readString();
		staff = in.readInt();
		follows = in.readInt();
		published = new Date(in.readLong());
	}

	public CommunityMenuItem(String title, Uri url, String author,
			String summary, String stories, String languague, int staff,
			int follows, Date published) {
		super();
		this.title = title;
		this.uri = url;
		this.author = author;
		this.summary = summary;
		this.stories = stories;
		this.languague = languague;
		this.staff = staff;
		this.follows = follows;
		this.published = published;
	}

	/**
	 * Always returns 0
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeParcelable(uri, flags);
		dest.writeString(author);
		dest.writeString(summary);
		dest.writeString(stories);
		dest.writeString(languague);
		dest.writeInt(staff);
		dest.writeInt(follows);
		dest.writeLong(published.getTime());
	}
}