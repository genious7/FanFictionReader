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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.spicymango.fanfictionreader.BuildConfig;
import com.spicymango.fanfictionreader.activity.Site;

/**
 * @author Michael Chen
 * 
 */
public class StoryProvider extends ContentProvider implements SqlConstants {
	private static final String AUTHORITY = BuildConfig.provider_authority;
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
	
	private static final int GET_ALL = 			0b00;
	private static final int GET_ONE = 			0b01;
	private static final int GET_MASK = 		0b01;
	private static final int FANFICTION = 		0b00;
	private static final int FICTIONPRESS = 	0b10;
	private static final int SITE_MASK = 		0b10;
	
	private static final UriMatcher URI_MATCHER;

	static{
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, BASE_PATH_FF, GET_ALL | FANFICTION);
		URI_MATCHER.addURI(AUTHORITY, BASE_PATH_FF + "/#", GET_ONE | FANFICTION);
		URI_MATCHER.addURI(AUTHORITY, BASE_PATH_FP, GET_ALL | FICTIONPRESS);
		URI_MATCHER.addURI(AUTHORITY, BASE_PATH_FP + "/#", GET_ONE | FICTIONPRESS);
	}

	private DatabaseHelper db;

	private static String getTable(int id){
		switch (id & SITE_MASK) {
		case FANFICTION:
			return DatabaseHelper.FANFICTION_TABLE;
		case FICTIONPRESS:
			return DatabaseHelper.FICTIONPRESS_TABLE;
		default:
			throw new IllegalArgumentException("StoryProvider - getTable: Table does not exist");
		}
	}

	/**
	 * Gets the name of the full text search table
	 * @param id The name of the site being searched
	 * @return The name of the full text search table
	 */
	private static String getFtsTable(int id){
		switch (id & SITE_MASK) {
			case FANFICTION:
				return DatabaseHelper.FANFICTION_TABLE_FTS;
			default:
				throw new IllegalArgumentException("StoryProvider - getTable: Table does not exist");
		}
	}

	@Override
	public int delete(@NonNull Uri uri, @Nullable String selection,
					  @Nullable String[] selectionArgs) {

		final int uriType = URI_MATCHER.match(uri);
		final SQLiteDatabase sqlDB = db.getWritableDatabase();
		final String tableId = getTable(URI_MATCHER.match(uri));
		final int rowsDeleted;

		if ((uriType & GET_MASK) == GET_ALL) {
			// When deleting multiple records, simply use the selection and selectionArgs
			rowsDeleted = sqlDB.delete(tableId, selection, selectionArgs);
		} else {
			final String id = uri.getLastPathSegment();

			if (selectionArgs == null) {
				// If the selectionArgs is empty, just set it to the id
				selectionArgs = new String[]{id};
			} else {
				// If the selectionArgs is not empty, add the id parameter to the end
				final String[] tmp = new String[selectionArgs.length + 1];
				System.arraycopy(selectionArgs, 0, tmp, 0, selectionArgs.length);
				selectionArgs = tmp;
				selectionArgs[selectionArgs.length - 1] = id;
			}

			if (TextUtils.isEmpty(selection)) {
				// If there is no where clause, set it to search by id
				selection = KEY_STORY_ID + "= ?";
			} else {
				// If there is a where clause, append the id comparison to the end of the query
				selection = selection + " AND " + KEY_STORY_ID + "= ?";
			}

			rowsDeleted = sqlDB.delete(tableId, selection, selectionArgs);
		}

		if (rowsDeleted > 0) {
			// If the dataSet changed, notify any observers
			assert getContext() != null;    // Keeps Android Studio warnings happy
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return rowsDeleted;
	}

	@Override
	public String getType(@NonNull Uri uri) {
		final int uriType = URI_MATCHER.match(uri);
		switch (uriType & GET_MASK) {
		case GET_ALL:
			return STORIES_MIME_TYPE;
		case GET_ONE:
			return STORY_MIME_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		final int uriType = URI_MATCHER.match(uri);
		final String tableId = getTable(uriType);
		final SQLiteDatabase sqlDB = db.getWritableDatabase();

		if ((uriType & GET_MASK) == GET_ONE){
			// Insert operations cannot be applied to URI's that select a single record
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		final long id = sqlDB.insert(tableId, null, values);

		// Generate the uri of the new item
		final Uri itemUri = ContentUris.withAppendedId(uri, id);

		// Notify that the data set changed
		assert getContext() != null;	// Keeps Android Studio warnings happy
		getContext().getContentResolver().notifyChange(itemUri, null);

		return itemUri;
	}

	@Override
	public boolean onCreate() {
		db = new DatabaseHelper(getContext());
		return true;
	}

	/**
	 * Queries the database. If a full text search is performed, the query is modified as required.
	 * @param uri The content provider uri for the base (non-full text search) table
	 * @param projection The required table columns
	 * @param selection The Where statement
	 * @param selectionArgs The Where arguments
	 * @param sortOrder The sort order
	 * @return A cursor containing the requested columns
	 */
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
						@Nullable String[] selectionArgs, @Nullable String sortOrder) {
		final SQLiteDatabase data = db.getReadableDatabase();
		final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		final int uriType = URI_MATCHER.match(uri);
		final String baseTable = getTable(uriType);

		// If only one element is being fetched, add the appropriate check to the WHERE clause
		if ((uriType & GET_MASK) == GET_ONE) {
			queryBuilder.appendWhere(KEY_STORY_ID + "=");
			queryBuilder.appendWhereEscapeString(uri.getLastPathSegment());
		}

		// If multiple elements are being fetched, set the default sort order if none are specified
		if ((uriType & GET_MASK) == GET_ALL && TextUtils.isEmpty(sortOrder)) {
			sortOrder = KEY_TITLE + " COLLATE NOCASE ASC";
		}

		if (selection != null && selection.contains("MATCH")) {
			// Join the full text search table to the regular table since a full text search is being performed
			final String ftsTable = getFtsTable(uriType);
			queryBuilder.setTables(baseTable + " JOIN " + ftsTable + " ON " + KEY_STORY_ID + "=" + ftsTable + "." + KEY_FTS_ID);

			// Replace the full text search keyword on the WHERE statement by the table's actual name
			selection = selection.replace(FTS_TABLE, ftsTable);
		} else {
			// Use the base table only since a full text search is not required
			queryBuilder.setTables(baseTable);
		}

		final Cursor c = queryBuilder.query(data, projection, selection, selectionArgs,
											null, null, sortOrder);

		assert getContext() != null;// Keeps Android Studio warnings happy
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
					  @Nullable String[] selectionArgs) {

		final SQLiteDatabase sqlDB = db.getWritableDatabase();
		final int rowsUpdated;
		final int uriType = URI_MATCHER.match(uri);
		final String tableId = getTable(uriType);
		
		switch (uriType & GET_MASK) {
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
			assert getContext() != null;	// Keeps Android Studio warnings happy
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
