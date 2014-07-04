/**
 * 
 */
package com.spicymango.fanfictionreader.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * @author Michael Chen
 * 
 */
public class StoryProvider extends ContentProvider implements SqlConstants {
	private static final String AUTHORITY = "com.spicymango.fanfictionreader.provider";
	private static final String BASE_PATH = "library";
	
	/**
	 * {@link Uri} for the content provider
	 * <p>
	 * <i>content://com.spicymango.fanfictionreader.provider/
	 * library</i>
	 */
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + BASE_PATH);
	
	private static final String STORIES_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.spicymango.fanfictionreader.stories";
	private static final String STORY_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.spicymango.fanfictionreader.story";
	
	private static final int GET_ALL = 0;
	private static final int GET_ONE = 1;
	
	private static final UriMatcher uriMatcher = getUriMatcher();
	
	/**
	 * Gets a UriMatcher for the provider
	 * @return
	 */
	private final static UriMatcher getUriMatcher() {
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		matcher.addURI(AUTHORITY, BASE_PATH, GET_ALL);
		matcher.addURI(AUTHORITY, BASE_PATH + "/#", GET_ONE);
		return matcher;
	}

	private DatabaseHelper db;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase sqlDB = db.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriMatcher.match(uri)) {
		case GET_ALL:
			rowsDeleted = sqlDB.delete(DatabaseHelper.TABLE_LIBRARY, selection,
					selectionArgs);
			break;
		case GET_ONE:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(DatabaseHelper.TABLE_LIBRARY,
						KEY_STORY_ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(DatabaseHelper.TABLE_LIBRARY,
						KEY_STORY_ID + "=" + id + " and " + selection,
						selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		if (rowsDeleted > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return rowsDeleted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		int uriType = uriMatcher.match(uri);
		switch (uriType) {
		case GET_ALL:
			return STORIES_MIME_TYPE;
		case GET_ONE:
			return STORY_MIME_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = uriMatcher.match(uri);
		SQLiteDatabase sqlDB = db.getWritableDatabase();
		long id = 0;
		switch (uriType) {
		case GET_ALL:
			id = sqlDB.insert(DatabaseHelper.TABLE_LIBRARY, null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		Uri itemUri = ContentUris.withAppendedId(uri, id);
		getContext().getContentResolver().notifyChange(itemUri, null);
		return itemUri;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		db = new DatabaseHelper(getContext());
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 * java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		Cursor c;
		SQLiteDatabase data = db.getReadableDatabase();
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(DatabaseHelper.TABLE_LIBRARY);

		switch (uriMatcher.match(uri)) {
		case GET_ALL:
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = KEY_TITLE + " COLLATE NOCASE ASC";
			}
			break;
		case GET_ONE:
			queryBuilder.appendWhere(KEY_STORY_ID + "="
					+ uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI" + uri);
		}
		c = queryBuilder.query(data, projection, selection, selectionArgs,
				null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 * android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		SQLiteDatabase sqlDB = db.getWritableDatabase();
		int rowsUpdated = 0;

		switch (uriMatcher.match(uri)) {
		case GET_ALL:
			rowsUpdated = sqlDB.update(DatabaseHelper.TABLE_LIBRARY, values,
					selection, selectionArgs);
			break;
		case GET_ONE:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(DatabaseHelper.TABLE_LIBRARY,
						values, KEY_STORY_ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(DatabaseHelper.TABLE_LIBRARY,
						values, KEY_STORY_ID + "=" + id + " AND " + selection,
						selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		if (rowsUpdated > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return rowsUpdated;
	}
	
	/**
	 * Obtains the last chapter read by the user
	 * @param context The current context
	 * @param storyId The id of the story
	 * @return The last chapter read, or 1 if the story doesn't exist.
	 */
	public static int lastChapterRead(Context context, long storyId) {
		ContentResolver resolver = context.getContentResolver();
		Cursor c = resolver.query(StoryProvider.CONTENT_URI,
				new String[] { SqlConstants.KEY_LAST },
				SqlConstants.KEY_STORY_ID + " = ?",
				new String[] { String.valueOf(storyId) }, null);
		int last;
		if (c == null || !c.moveToFirst()) {
			last = 1;
		} else {
			int index = c.getColumnIndex(SqlConstants.KEY_LAST);
			last = c.getInt(index);
		}
		c.close();
		return last;
	}
	
	/**
	 * Obtains the total number of chapters
	 * @param context The current context
	 * @param storyId The id of the story
	 * @return The total number of chapters, or 1 if the story doesn't exist.
	 */
	public static int numberOfChapters(Context context, long storyId) {
		ContentResolver resolver = context.getContentResolver();
		Cursor c = resolver.query(StoryProvider.CONTENT_URI,
				new String[] { SqlConstants.KEY_CHAPTER },
				SqlConstants.KEY_STORY_ID + " = ?",
				new String[] { String.valueOf(storyId) }, null);
		int last;
		if (c == null || !c.moveToFirst()) {
			last = 1;
		} else {
			int index = c.getColumnIndex(SqlConstants.KEY_CHAPTER);
			last = c.getInt(index);
		}
		c.close();
		return last;
	}
}
