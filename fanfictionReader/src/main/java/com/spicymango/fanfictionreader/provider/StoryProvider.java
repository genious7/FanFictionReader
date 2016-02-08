/**
 * 
 */
package com.spicymango.fanfictionreader.provider;

import com.spicymango.fanfictionreader.activity.Site;

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
	private static final String BASE_PATH_FF = "library";
	private static final String BASE_PATH_FP = "fictionPressLibrary";
	
	/**
	 * {@link Uri} for the FanFiction content provider
	 * <p>
	 */
	public static final Uri FF_CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + BASE_PATH_FF);
	/**
	 * {@link Uri} for the FictionPress content provider
	 * <p>
	 */
	public static final Uri FP_CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + BASE_PATH_FP);
	
	private static final String STORIES_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.spicymango.fanfictionreader.stories";
	private static final String STORY_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.spicymango.fanfictionreader.story";
	
	private static final int GET_ALL = 0x0;
	private static final int GET_ONE = 0x1;
	private static final int FANFICTION = 0x0;
	private static final int FICTIONPRESS = 0x2;
	
	private static final UriMatcher uriMatcher = getUriMatcher();
	
	/**
	 * Gets a UriMatcher for the provider
	 * @return
	 */
	private static UriMatcher getUriMatcher() {
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		matcher.addURI(AUTHORITY, BASE_PATH_FF, GET_ALL | FANFICTION);
		matcher.addURI(AUTHORITY, BASE_PATH_FF + "/#", GET_ONE | FANFICTION);

		matcher.addURI(AUTHORITY, BASE_PATH_FP, GET_ALL | FICTIONPRESS);
		matcher.addURI(AUTHORITY, BASE_PATH_FP + "/#", GET_ONE | FICTIONPRESS);
		return matcher;
	}

	private DatabaseHelper db;

	private static String getTable(int id){
		switch (id & ~0x01) {
		case FANFICTION:
			return DatabaseHelper.FANFICTION_TABLE;
		case FICTIONPRESS:
			return DatabaseHelper.FICTIONPRESS_TABLE;
		default:
			throw new IllegalArgumentException("StoryProvider - getTable: Table does not exist");
		}
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final SQLiteDatabase sqlDB = db.getWritableDatabase();
		final String tableId = getTable(uriMatcher.match(uri));
		final int rowsDeleted;
		
		switch (uriMatcher.match(uri)) {
		case GET_ALL:
			rowsDeleted = sqlDB.delete(tableId, selection,
					selectionArgs);
			break;
		case GET_ONE:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB
						.delete(tableId, KEY_STORY_ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(tableId, KEY_STORY_ID + "=" + id
						+ " and " + selection, selectionArgs);
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

	@Override
	public String getType(Uri uri) {
		final int uriType = uriMatcher.match(uri);
		switch (uriType & 0x01) {
		case GET_ALL:
			return STORIES_MIME_TYPE;
		case GET_ONE:
			return STORY_MIME_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final int uriType = uriMatcher.match(uri);
		final String tableId = getTable(uriType);
		final SQLiteDatabase sqlDB = db.getWritableDatabase();
		
		final long id;
		
		switch (uriType & 0x1) {
		case GET_ALL:
			id = sqlDB.insert(tableId, null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		Uri itemUri = ContentUris.withAppendedId(uri, id);
		getContext().getContentResolver().notifyChange(itemUri, null);
		return itemUri;
	}

	@Override
	public boolean onCreate() {
		db = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		final Cursor c;
		final SQLiteDatabase data = db.getReadableDatabase();
		final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		
		final int uriType = uriMatcher.match(uri);
		queryBuilder.setTables(getTable(uriType));

		switch (uriMatcher.match(uri) & 0x01) {
		case GET_ALL:
			if (TextUtils.isEmpty(sortOrder)) {	//Default sort order
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

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		final SQLiteDatabase sqlDB = db.getWritableDatabase();
		final int rowsUpdated;
		final int uriType = uriMatcher.match(uri);
		final String tableId = getTable(uriType);
		
		switch (uriType & 0x01) {
		case GET_ALL:
			rowsUpdated = sqlDB.update(tableId, values, selection,
					selectionArgs);
			break;
		case GET_ONE:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(tableId, values, KEY_STORY_ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(tableId, values, KEY_STORY_ID + "="
						+ id + " AND " + selection, selectionArgs);
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
	 * @param site The current site
	 * @param storyId The id of the story
	 * @return The last chapter read, or 1 if the story doesn't exist.
	 */
	public static int lastChapterRead(Context context, Site site, long storyId) {
		return getIntField(context, site, storyId, SqlConstants.KEY_LAST, 1);
	}
	
	/**
	 * Obtains the total number of chapters
	 * @param context The current context
	 * @param site The current site
	 * @param storyId The id of the story
	 * @return The total number of chapters, or 1 if the story doesn't exist.
	 */
	public static int numberOfChapters(Context context, Site site, long storyId) {
		return getIntField(context, site, storyId, SqlConstants.KEY_CHAPTER, 1);
	}
	
	/**
	 * Gets an integer field
	 * @param context The current context
	 * @param site	The current site
	 * @param storyId	The Id of the desired story
	 * @param field		The desired field
	 * @param defautlt	The default value if the field does not exist.
	 * @return The requested parameter
	 */
	private static int getIntField(Context context, Site site, long storyId, String field,int defautlt){
		ContentResolver resolver = context.getContentResolver();
		Cursor c = resolver.query(site.content_uri,
				new String[] { field },
				SqlConstants.KEY_STORY_ID + " = ?",
				new String[] { Long.toString(storyId) }, null);
		int value;
		int index;
		if (c == null || !c.moveToFirst() || (index = c.getColumnIndex(SqlConstants.KEY_CHAPTER)) == -1) {
			value = defautlt;
		} else {
			value = c.getInt(index);
		}
		c.close();
		return value;
	}
}
