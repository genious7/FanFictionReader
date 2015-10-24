package com.spicymango.fanfictionreader.menu.categorymenu;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Sorts the menuObjects either by name or stories
 * 
 * @author Michael Chen
 *
 */
class CategoryMenuComparator implements Comparator<CategoryMenuItem> {
	public static final boolean SORT_ALPHABETICAL = false;
	public static final boolean SORT_VIEWS = true;

	private static final Collator COLLATOR = Collator.getInstance(Locale.US);
	private boolean sortBy = true;

	static {
		COLLATOR.setStrength(Collator.PRIMARY);
	}

	/**
	 * True to sort by title, false to sort by views
	 * 
	 * @param SortBy
	 */
	public CategoryMenuComparator() {
		sortBy = SORT_VIEWS;
	}

	public void setSortType(boolean sortType) {
		sortBy = sortType;
	}

	@Override
	public int compare(CategoryMenuItem arg0, CategoryMenuItem arg1) {
		if (sortBy == SORT_ALPHABETICAL) {
			if (arg0.mSortInt == Integer.MAX_VALUE) {
				return -1;
			} else if (arg1.mSortInt == Integer.MAX_VALUE) {
				return 1;
			} else {
				return COLLATOR.compare(arg0.mTitle, arg1.mTitle);
			}
		} else {
			return -((Integer) arg0.mSortInt).compareTo(arg1.mSortInt);
		}
	}
}
