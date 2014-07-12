package com.spicymango.fanfictionreader.filter;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class FilterSet implements Parcelable{
	
	private List<FilterObject> filter; 
	
	private FilterSet(Parcel in){
		in.readList(filter, getClass().getClassLoader());
	}
	
	public static final Parcelable.Creator<FilterSet> CREATOR = new Creator<FilterSet>() {
		
		@Override
		public FilterSet[] newArray(int size) {
			return new FilterSet[size];
		}
		
		@Override
		public FilterSet createFromParcel(Parcel source) {
			return new FilterSet(source);
		}
	}; 

	@Override
 	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(filter);
	}

	private static final class FilterObject implements Parcelable{
		
		@Override
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
