package com.spicymango.fanfictionreader.menu.storymenu.FilterDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An object that represents a spinner element. Each object will pair a list of
 * display values with a list of internal filters. The object also stores which
 * object is currently selected.
 * 
 * @author Michael Chen
 *
 */
public class SpinnerData implements Parcelable {
	private final String mName;
	private List<String> mLabels;
	private List<String> mFilters;
	private int mSelected;

	public SpinnerData(Parcel in) {
		// Create the new lists
		mLabels = new ArrayList<>();
		mFilters = new ArrayList<>();

		// Load the parcel
		mName = in.readString();
		in.readStringList(mLabels);
		in.readStringList(mFilters);
		mSelected = in.readInt();
	}

	/**
	 * Creates a new {@link SpinnerData} object
	 * 
	 * @param name
	 *            This object's name
	 * @param labels
	 *            The user-viewable spinner labels
	 * @param filters
	 *            The data corresponding to each spinner selection
	 * @param selected
	 *            The currently selected spinner object
	 */
	public SpinnerData(@Nullable String name, @NonNull List<String> labels, @Nullable List<String> filters,
			int selected) {
		mName = name;
		mLabels = labels;
		mFilters = filters;
		mSelected = selected;
	}

	/**
	 * Creates a new {@link SpinnerData} object
	 * 
	 * @param name
	 *            This object's name
	 * @param labels
	 *            The user-viewable spinner labels
	 * @param filters
	 *            The data corresponding to each spinner selection
	 * @param selected
	 *            The currently selected spinner object
	 */
	public SpinnerData(@Nullable String name, @NonNull String[] labels, @Nullable String[] filters, int selected) {
		mName = name;
		mLabels = Arrays.asList(labels);
		mFilters = filters == null ? null : Arrays.asList(filters);
		mSelected = selected;
	}

	public final static Creator<SpinnerData> CREATOR = new Creator<SpinnerData>() {
		@Override
		public SpinnerData createFromParcel(Parcel source) {
			return new SpinnerData(source);
		}

		@Override
		public SpinnerData[] newArray(int size) {
			return new SpinnerData[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		dest.writeStringList(mLabels);
		dest.writeStringList(mFilters);
		dest.writeInt(mSelected);
	}

	/**
	 * Gets this object's name
	 * 
	 * @return
	 */
	public @Nullable String getName() {
		return mName;
	}

	/**
	 * Gets a list of the user viewable spinner labels.
	 * 
	 * @return
	 */
	public @NonNull List<String> getLabels() {
		return mLabels;
	}

	/**
	 * Sets the user-viewable spinner labels
	 * 
	 * @param labels
	 *            A list of strings
	 */
	public void setLabels(@NonNull List<String> labels) {
		mLabels = labels;
	}

	/**
	 * Gets the list of non-user visible filter strings
	 */
	public @Nullable List<String> getFilters() {
		return mFilters;
	}

	/**
	 * Sets the list of non-user visible filter strings.
	 * 
	 * @param list
	 */
	public void setFilters(@Nullable List<String> list) {
		mFilters = list;
	}

	/**
	 * Gets the active filter, based on the current selection.
	 * 
	 * @return The active filter, or "0" if the list of filter values is not
	 *         set.
	 */
	public String getCurrentFilter() {
		if (mFilters == null || mFilters.isEmpty()) {
			return "0";
		} else {
			return mFilters.get(mSelected);
		}
	}

	/**
	 * Sets the currently selected element
	 * 
	 * @param selected
	 *            The position of the selected element
	 */
	public void setSelected(int selected) {
		// Check that the requested selection is valid. Note that 0 is always
		// valid, as it is the default position.
		if (selected > 0 && selected >= mLabels.size()) {
			throw new IndexOutOfBoundsException(
					"The requested position is greater than the number of spinner positions");
		}

		// Set the requested selection
		mSelected = selected;
	}

	/**
	 * Gets the element set as currently selected.
	 * 
	 * @return
	 */
	public int getSelected() {
		return mSelected;
	}
}