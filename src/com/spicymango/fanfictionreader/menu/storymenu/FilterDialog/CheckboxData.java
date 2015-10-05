package com.spicymango.fanfictionreader.menu.storymenu.FilterDialog;

import android.os.Parcel;
import android.os.Parcelable;

public class CheckboxData implements Parcelable {
	public static final Creator<CheckboxData> CREATOR = new Creator<CheckboxData>() {
		@Override
		public CheckboxData createFromParcel(Parcel source) {
			return new CheckboxData(source);
		}

		@Override
		public CheckboxData[] newArray(int size) {
			return new CheckboxData[size];
		}
	};
	private final String mName;
	private boolean mSelected;

	private String mTag;

	public CheckboxData(Parcel in) {
		mName = in.readString();
		mTag = in.readString();
		mSelected = in.readInt() != 0;
	}

	public CheckboxData(String name, String tag, boolean selected) {
		mName = name;
		mTag = tag;
		mSelected = selected;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public boolean getSelected() {
		return mSelected;
	}

	public String getTag() {
		return mTag;
	}

	public void setSelected(boolean selected) {
		mSelected = selected;
	}

	public void setTag(String tag) {
		mTag = tag;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		dest.writeString(mTag);
		dest.writeInt(mSelected ? 1 : 0);
	}

}
