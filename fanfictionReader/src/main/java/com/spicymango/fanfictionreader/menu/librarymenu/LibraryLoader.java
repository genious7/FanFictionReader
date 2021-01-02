package com.spicymango.fanfictionreader.menu.librarymenu;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.CursorLoader;
import android.text.TextUtils;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.BaseLoader;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.SpinnerData;
import com.spicymango.fanfictionreader.provider.SqlConstants;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A loader that gets the user's library.
 * <p/>
 * Created by Michael Chen on 05/07/2016.
 */
class LibraryLoader extends CursorLoader implements SqlConstants, BaseLoader.Filterable {
	/** A simple pattern used to generate the list of available fandoms */
	private static final Pattern CROSSOVER_PATTERN = Pattern.compile("(.+)(?<!Rosario) \\+ (.+) Crossover");

	/** The arrayList that backs up the filter*/
	private ArrayList<SpinnerData> mFilterData;
	private static final String STATE_FILTER = "STATE_FILTER";

	/** True if the fandom list has been loaded, false otherwise*/
	private boolean mFandomsLoaded;
	private static final String STATE_FANDOMS = "STATE FANDOMS";

	private String mQuery;
	private static final String STATE_QUERY = "STATE QUERY";

	public LibraryLoader(Context context, Uri uri, String[] projection, Bundle saveInstanceState) {
		super(context, uri, projection, null, null, null);

		// Initialize Filter if necessary, otherwise, load it from the saved instance state
		if (saveInstanceState != null && saveInstanceState.containsKey(STATE_FILTER)){
			mFilterData = saveInstanceState.getParcelableArrayList(STATE_FILTER);
			mFandomsLoaded = saveInstanceState.getBoolean(STATE_FANDOMS);
			mQuery = saveInstanceState.getString(STATE_QUERY);
		} else{
			createFilter();
			mFandomsLoaded = false;
			mQuery = null;
		}

		setSqlWhere();
		setSortOrder(getSqlOrderBy());
	}

	@Override
	public Cursor loadInBackground() {
		final Cursor c = super.loadInBackground();

		if (!mFandomsLoaded && c != null) {
			final Matcher matcher = CROSSOVER_PATTERN.matcher("");

			// Add the 'View all fandoms' option to the filter
			final String all = getContext().getString(R.string.menu_library_filter_all);

			/* The list of Fandoms in the user's library*/
			final Set<String> mFandoms = new TreeSet<>(new FilterComparator(all));
			mFandoms.add(all);

			if (c.moveToFirst()) {
				final int index = c.getColumnIndex(SqlConstants.KEY_CATEGORY);
				do {
					final String category = c.getString(index);

					matcher.reset(category);
					if (matcher.find()){
						mFandoms.add(matcher.group(1));
						mFandoms.add(matcher.group(2));
					} else{
						mFandoms.add(category);
					}

				} while (c.moveToNext());
			}
			prepareFilter(mFandoms);
			mFandomsLoaded = true;
		}

		return c;
	}

	public void onSaveInstanceState(Bundle in){
		in.putParcelableArrayList(STATE_FILTER, mFilterData);
		in.putBoolean(STATE_FANDOMS, mFandomsLoaded);
		in.putString(STATE_QUERY, mQuery);
	}

	@Override
	public void filter(int[] filterSelected) {
		for (int i = 0; i < filterSelected.length; i++) {
			mFilterData.get(i).setSelected(filterSelected[i]);
		}

		setSqlWhere();
		setSortOrder(getSqlOrderBy());
		onContentChanged();
	}

	@Override
	public boolean isFilterAvailable() {
		return mFandomsLoaded;
	}

	@Override
	public void onFilterClick(FragmentActivity activity) {
		// Create and display a new filter
		final FilterDialog.Builder builder = new FilterDialog.Builder();
		builder.addSingleSpinner(activity.getString(R.string.filter_category), mFilterData.get(0));
		builder.addSingleSpinner(activity.getString(R.string.filter_sort), mFilterData.get(1));
		builder.addSingleSpinner(activity.getString(R.string.filter_length), mFilterData.get(2));
		builder.addSingleSpinner(activity.getString(R.string.filter_type), mFilterData.get(3));
		builder.addSingleSpinner(activity.getString(R.string.filter_status), mFilterData.get(4));
		builder.show((LibraryMenuActivity) activity);
	}

	/**
	 * Initializes the static filters such as word count and sort order
	 */
	private void createFilter(){
		mFilterData = new ArrayList<>();

		// Filter by fandom
		mFilterData.add(new SpinnerData(KEY_CATEGORY, new ArrayList<>(), new ArrayList<>(), 0));

		// Filter by sort order
		final String[] sortByLabel = getContext().getResources().getStringArray(R.array.menu_library_sort_by);
		final String[] sortBySQL = {
				KEY_UPDATED + " DESC",
				KEY_PUBLISHED + " DESC",
				KEY_TITLE + " COLLATE NOCASE ASC",
				KEY_AUTHOR + " COLLATE NOCASE ASC",
				KEY_FAVORITES + " DESC",
				KEY_FOLLOWERS + " DESC",
				KEY_REVIEWS + " DESC",
				//We subtract 1 from the last chapter and chapter so chapter 1 always starts at 0 and the last chapter always ends at 1
				//When the percentage is the same we will use the default sort behavior(update date). (Read Percentage Filter)
				"(CAST(" + KEY_LAST + " - 1 AS FLOAT) / (" + KEY_CHAPTER + " - 1)) ASC, " + KEY_UPDATED + " DESC",
				KEY_ADDED + " DESC",
				KEY_LAST_READ + " DESC"
		};
		mFilterData.add(new SpinnerData("SortBy", sortByLabel, sortBySQL, 0));

		// Filter by words
		final String[] wordsLabel = getContext().getResources().getStringArray(R.array.menu_library_filter_words);
		final String[] wordsSql = { "", " < 1000", " < 5000", " > 1000", " > 5000", " > 10000", " > 20000", " > 40000",
									" > 60000", " > 100000" };
		mFilterData.add(new SpinnerData(KEY_LENGTH, wordsLabel, wordsSql, 0));

		// Filter by type (regular vs. crossover)
		final String[] typeLabel = getContext().getResources().getStringArray(R.array.menu_library_filter_type);
		final String[] typeSql = { "", " NOT LIKE '%Crossover' ", " LIKE '%Crossover' " };
		mFilterData.add(new SpinnerData(KEY_CATEGORY, typeLabel, typeSql, 0));

		// Filter if complete
		String[] statusLabel = getContext().getResources().getStringArray(R.array.menu_library_filter_status);
		String[] statusSql = { "", " != 0", " = 0" };
		mFilterData.add(new SpinnerData(KEY_COMPLETE, statusLabel, statusSql, 0));
	}

	/**
	 * Prepares the filter based on the available Fandoms.
	 * @param fandoms A set containing the available fandoms.
	 */
	private void prepareFilter(Set<String> fandoms) {
		final List<String> fandomList = mFilterData.get(0).getLabels();
		fandomList.clear();
		fandomList.addAll(fandoms);

		final List<String> fandomFilter = mFilterData.get(0).getFilters();
		assert fandomFilter != null;	// Keep IntelliJ null-check happy
		fandomFilter.clear();
		fandomFilter.addAll(fandoms);
	}

	/**
	 * Refreshes the sql where statement and its corresponding selectionArgs.
	 */
	private void setSqlWhere() {
		final ArrayList<String> sqlWhere = new ArrayList<>();
		final ArrayList<String> sqlWhereArgs = new ArrayList<>();

		// Filter by word count
		if (mFilterData.get(2).getSelected() != 0) {
			sqlWhere.add(mFilterData.get(2).getName() + mFilterData.get(2).getCurrentFilter());
		}

		// Filter by story type
		if (mFilterData.get(3).getSelected() != 0) {
			sqlWhere.add(mFilterData.get(3).getName() + mFilterData.get(3).getCurrentFilter());
		}

		// Filter by story status
		if (mFilterData.get(4).getSelected() != 0) {
			sqlWhere.add(mFilterData.get(4).getName() + mFilterData.get(4).getCurrentFilter());
		}

		// Filter by fandom
		if (mFilterData.get(0).getSelected() != 0) {
			sqlWhere.add(mFilterData.get(0).getName() + " LIKE ?");
			sqlWhereArgs.add("%" + mFilterData.get(0).getCurrentFilter() + "%");
		}

		// Search
		if (!TextUtils.isEmpty(mQuery)) {
			sqlWhere.add(FTS_TABLE + " MATCH ?");
			sqlWhereArgs.add(mQuery);
		}

		setSelection(TextUtils.join(" AND ", sqlWhere));
		setSelectionArgs(sqlWhereArgs.toArray(new String[sqlWhereArgs.size()]));
	}

	/**
	 * Gets the SQL order by statement based on the currently selected filter.
	 * @return The sql order by statement, or null
	 */
	@Nullable
	private String getSqlOrderBy(){
		return mFilterData == null ? null : mFilterData.get(1).getCurrentFilter();
	}

	void onSearch(final String query){
		if (mQuery == null || !mQuery.equals(query)){
			mQuery = query;
			setSqlWhere();
			onContentChanged();
		}
	}

	/**
	 * A custom comparator that ensures that the "All Crossovers" entry is sorted at the top of the
	 * Fandom list
	 */
	private static class FilterComparator implements Comparator<String> {

		/** A collator that is used to sort the fandoms correctly */
		private static final Collator COLLATOR = Collator.getInstance(Locale.US);

		static{
			COLLATOR.setStrength(Collator.PRIMARY);
		}

		private final String all;

		public FilterComparator(String all) {
			this.all = all;

		}

		@Override
		public int compare(String lhs, String rhs) {
			if (lhs.equals(all)) {
				return -1;
			} else if (rhs.equals(all)) {
				return 1;
			} else {
				return COLLATOR.compare(lhs, rhs);
			}
		}
	}
}
