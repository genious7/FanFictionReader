package com.spicymango.fanfictionreader.util;

import android.provider.BaseColumns;

public interface SqlConstants {

	// Columns of the table
	public static final String KEY_STORY_ID = BaseColumns._ID;
	public static final String KEY_TITLE = "title";
	public static final String KEY_AUTHOR = "author";
	public static final String KEY_AUTHOR_ID = "authorId";
	public static final String KEY_RATING = "rating";
	public static final String KEY_GENRE = "genre";
	public static final String KEY_LANGUAGUE = "languague";
	public static final String KEY_CATEGORY = "category";
	public static final String KEY_CHAPTER = "chapter";
	public static final String KEY_LENGHT = "lenght";
	public static final String KEY_FAVORITES = "favorites";
	public static final String KEY_FOLLOWERS = "follows";
	public static final String KEY_UPDATED = "updated";
	public static final String KEY_PUBLISHED = "published";
	public static final String KEY_SUMMARY = "summary";
	public static final String KEY_LAST = "lastChapter";
}
