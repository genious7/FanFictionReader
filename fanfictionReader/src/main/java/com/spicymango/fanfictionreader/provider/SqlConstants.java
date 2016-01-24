package com.spicymango.fanfictionreader.provider;

import android.provider.BaseColumns;

public interface SqlConstants {

	// Columns of the table
	String KEY_STORY_ID = BaseColumns._ID;
	String KEY_TITLE = "title";
	String KEY_AUTHOR = "author";
	String KEY_AUTHOR_ID = "authorId";
	String KEY_RATING = "rating";
	String KEY_GENRE = "genre";
	String KEY_LANGUAGE = "languague";
	String KEY_CATEGORY = "category";
	String KEY_CHAPTER = "chapter";
	String KEY_LENGTH = "lenght";
	String KEY_FAVORITES = "favorites";
	String KEY_FOLLOWERS = "follows";
	String KEY_UPDATED = "updated";
	String KEY_PUBLISHED = "published";
	String KEY_SUMMARY = "summary";
	String KEY_LAST = "lastChapter";
	String KEY_COMPLETE = "completed";
	String KEY_OFFSET = "characterOffset";
	String KEY_CHARACTERS = "characters";
	String KEY_REVIEWS = "reviews";
}
