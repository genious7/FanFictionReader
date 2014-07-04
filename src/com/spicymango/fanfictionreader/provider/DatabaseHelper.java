package com.spicymango.fanfictionreader.provider;

import java.util.ArrayList;
import java.util.List;

import com.spicymango.fanfictionreader.util.Story;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper  implements SqlConstants{
	
	private static final int DATABASE_VERSION = 4; //Database version 3
	private static final String DATABASE_NAME = "library.db";
	
	//The name of the table
	protected static final String TABLE_LIBRARY = "library";
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		 String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_LIBRARY + "("
	                + KEY_STORY_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE,"
	                + KEY_TITLE + " TEXT NOT NULL,"
	                + KEY_AUTHOR + " TEXT NOT NULL,"
	                + KEY_AUTHOR_ID + " TEXT,"
	                + KEY_RATING + " TEXT,"
	                + KEY_GENRE + " TEXT,"
	                + KEY_LANGUAGUE + " TEXT," 
	                + KEY_CATEGORY + " TEXT,"
	                + KEY_CHAPTER + " INTEGER,"
	                + KEY_LENGHT + " INTEGER,"
	                + KEY_FAVORITES + " INTEGER,"
	                + KEY_FOLLOWERS + " INTEGER,"
	                + KEY_PUBLISHED + " INTEGER,"
	                + KEY_UPDATED + " INTEGER,"
	                + KEY_SUMMARY + " TEXT NOT NULL,"
	                + KEY_LAST + " INTEGER"
	                + ")";
		 db.execSQL(CREATE_CONTACTS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int arg2) {
		if (oldVersion < 3) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIBRARY);
			onCreate(db);
		}
		if (oldVersion < 4) {
			db.execSQL("UPDATE " + TABLE_LIBRARY + " SET " + KEY_LENGHT + " = REPLACE(" + KEY_LENGHT+ ",',','')");
			db.execSQL("UPDATE " + TABLE_LIBRARY + " SET " + KEY_FAVORITES + " = REPLACE(" + KEY_FAVORITES+ ",',','')");
			db.execSQL("UPDATE " + TABLE_LIBRARY + " SET " + KEY_FOLLOWERS + " = REPLACE(" + KEY_FOLLOWERS+ ",',','')");
			
			db.execSQL("CREATE TABLE IF NOT EXISTS " +"tmp" + "("
	                + KEY_STORY_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE,"
	                + KEY_TITLE + " TEXT NOT NULL,"
	                + KEY_AUTHOR + " TEXT NOT NULL,"
	                + KEY_AUTHOR_ID + " TEXT,"
	                + KEY_RATING + " TEXT,"
	                + KEY_GENRE + " TEXT,"
	                + KEY_LANGUAGUE + " TEXT," 
	                + KEY_CATEGORY + " TEXT,"
	                + KEY_CHAPTER + " INTEGER,"
	                + KEY_LENGHT + " INTEGER,"
	                + KEY_FAVORITES + " INTEGER,"
	                + KEY_FOLLOWERS + " INTEGER,"
	                + KEY_PUBLISHED + " INTEGER,"
	                + KEY_UPDATED + " INTEGER,"
	                + KEY_SUMMARY + " TEXT NOT NULL,"
	                + KEY_LAST + " INTEGER"
	                + ")");
			db.execSQL("INSERT INTO tmp SELECT * FROM " + TABLE_LIBRARY);
			db.execSQL("DROP TABLE " + TABLE_LIBRARY);
			db.execSQL("ALTER TABLE tmp RENAME TO " + TABLE_LIBRARY);
		}
		
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	/**
	 * Obtains the last chapter read in the selected story
	 * 
	 * @param id
	 *            The id of the story
	 * @return The last chapter read, or -1 if the story is not in the database.
	 */
	public int getLastChapterRead(long id){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LIBRARY, new String[] { KEY_LAST },
				KEY_STORY_ID + "=?", new String[] { String.valueOf(id) }, null,
				null, null);
		
		int last;
		
		if (cursor == null || !cursor.moveToFirst()){
	    	last = -1;
		}else{
			int columnIndex = cursor.getColumnIndex(KEY_LAST);
			last = cursor.getInt(columnIndex);
		}
		cursor.close();
		return last;
	}
	
	/**
	 * Gets the story with the requested id
	 * @param id The id of the story
	 * @return The story, or null if it doesn't exist.
	 */
	public Story getStory(long id) {
	    SQLiteDatabase db = this.getReadableDatabase();
	 
	    Cursor cursor = db.query(TABLE_LIBRARY, 
	    		new String[] { KEY_STORY_ID, KEY_TITLE, KEY_AUTHOR, KEY_AUTHOR_ID, KEY_RATING,
	    		KEY_GENRE, KEY_LANGUAGUE, KEY_CATEGORY, KEY_CHAPTER, KEY_LENGHT, KEY_FAVORITES,
	    		KEY_FOLLOWERS, KEY_PUBLISHED, KEY_UPDATED, KEY_SUMMARY}, KEY_STORY_ID + "=?",
	            new String[] { String.valueOf(id) }, null, null, null);
	    
	    if (cursor == null || !cursor.moveToFirst()){
	    	cursor.close();
	    	return null;
	    }
	 
		Story story = new Story(cursor);
		
		cursor.close();
		
	    return story;
	}

	/**
	 * Obtains all the stories from the library.
	 * 
	 * @return A list of all the stories, or an empty list if none are
	 *         available.
	 */
	public List<Story> getAllStories() {		
		// Select All Query
		//String selectQuery = "SELECT  * FROM " + TABLE_LIBRARY;

		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LIBRARY, new String[] { KEY_STORY_ID,
				KEY_TITLE, KEY_AUTHOR, KEY_AUTHOR_ID, KEY_RATING, KEY_GENRE,
				KEY_LANGUAGUE, KEY_CATEGORY, KEY_CHAPTER, KEY_LENGHT,
				KEY_FAVORITES, KEY_FOLLOWERS, KEY_PUBLISHED, KEY_UPDATED,
				KEY_SUMMARY }, null, null, null, null, null);
		
		List<Story> storyList = new ArrayList<Story>(cursor.getCount());
		
		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Story story = new Story(cursor);
				storyList.add(story);
			} while (cursor.moveToNext());
		}
		cursor.close();
		
		return storyList;
	}
	
	/**
	 * Obtains the total number of stories present.
	 * @return The number of stories
	 */
	public int getStoryCount() {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LIBRARY, new String[] { KEY_STORY_ID },
				null, null, null, null, null);
		int count = cursor.getCount();
		cursor.close();
		// return count
		return count;
	}
}
