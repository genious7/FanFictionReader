package com.spicymango.fanfictionreader.menu.communitymenu;

import androidx.appcompat.widget.ThemedSpinnerAdapter.Helper;

/**
 * A small {@link Helper} Enum that contains the associated filter values for
 * the FanFiction community sort.
 * 
 * @author Michael Chen
 *
 */
enum SortBy {
	RANDOM(99), STAFF(1), STORIES(2), FOLLOWS(3), CREATE_DATE(4);

	private final int id;

	SortBy(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

}