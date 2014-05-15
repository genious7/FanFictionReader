package com.gmail.michaelchentejada.fanfictionreader.util;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper  implements SqlConstants{
	
	private static final int DATABASE_VERSION = 2; //Database ver. 2
	protected static final String DATABASE_NAME = "library.db";
	
	//The name of the table
	protected static final String TABLE_LIBRARY = "library";
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		 String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_LIBRARY + "("
	                + KEY_STORY_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE,"
	                + KEY_TITLE + " TEXT,"
	                + KEY_AUTHOR + " TEXT,"
	                + KEY_AUTHOR_ID + " TEXT,"
	                + KEY_RATING + " TEXT,"
	                + KEY_GENRE + " TEXT,"
	                + KEY_LANGUAGUE + " TEXT," 
	                + KEY_CATEGORY + " TEXT,"
	                + KEY_CHAPTER + " INTEGER,"
	                + KEY_LENGHT + " TEXT,"
	                + KEY_FAVORITES + " TEXT,"
	                + KEY_FOLLOWERS + " TEXT,"
	                + KEY_PUBLISHED + " INTEGER,"
	                + KEY_UPDATED + " INTEGER,"
	                + KEY_SUMMARY + " TEXT"
	                + ")";
		 db.execSQL(CREATE_CONTACTS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		arg0.execSQL("DROP TABLE IF EXISTS " + TABLE_LIBRARY);
		onCreate(arg0);
	}

	public void addStory(Story story){
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_STORY_ID, story.getId());
		values.put(KEY_TITLE, story.getName());
		values.put(KEY_AUTHOR, story.getAuthor());
		values.put(KEY_AUTHOR_ID, story.getAuthor_id());
		values.put(KEY_RATING, story.getRating());
		values.put(KEY_GENRE, story.getGenre());
		values.put(KEY_LANGUAGUE, story.getlanguage());
		values.put(KEY_CATEGORY, story.getCategory());
		values.put(KEY_CHAPTER, story.getChapterLenght());
		values.put(KEY_LENGHT, story.getWordLenght());
		values.put(KEY_FAVORITES, story.getFavorites());
		values.put(KEY_FOLLOWERS, story.getFollows());
		values.put(KEY_PUBLISHED, story.getPublished().getTime());
		values.put(KEY_UPDATED, story.getUpdated().getTime());
		values.put(KEY_SUMMARY, story.getSummary());
		db.insert(TABLE_LIBRARY, null, values);
	    db.close();
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
	            new String[] { String.valueOf(id) }, null, null, null, null);
	    if (cursor == null || !cursor.moveToFirst())
	    	return null;
	 
		Story story = new Story(cursor.getInt(0), cursor.getString(1),
				cursor.getString(2), cursor.getInt(3), cursor.getString(14),
				cursor.getString(7), cursor.getString(4), cursor.getString(6),
				cursor.getString(5), cursor.getInt(8), cursor.getString(9),
				cursor.getString(10), cursor.getString(11), cursor.getLong(13),
				cursor.getLong(12));
		cursor.close();
		
	    return story;
	}
	
	/**
	 * Finds if a story is already in the library
	 * @param id The id of the story
	 * @return True if it exists, false otherwise
	 */
	public boolean hasStory(long id){
		SQLiteDatabase db = this.getReadableDatabase();
		 
	    Cursor cursor = db.query(TABLE_LIBRARY, 
	    		new String[] { KEY_STORY_ID, KEY_TITLE, KEY_AUTHOR, KEY_AUTHOR_ID, KEY_RATING,
	    		KEY_GENRE, KEY_LANGUAGUE, KEY_CATEGORY, KEY_CHAPTER, KEY_LENGHT, KEY_FAVORITES,
	    		KEY_FOLLOWERS, KEY_PUBLISHED, KEY_UPDATED, KEY_SUMMARY}, KEY_STORY_ID + "=?",
	            new String[] { String.valueOf(id) }, null, null, null, null);
	    return cursor.moveToFirst();
	}

	public List<Story> getAllStories() {
		List<Story> storyList = new ArrayList<Story>();
		    // Select All Query
	    String selectQuery = "SELECT  * FROM " + TABLE_LIBRARY;
	 
	    SQLiteDatabase db = this.getWritableDatabase();
	    Cursor cursor = db.rawQuery(selectQuery, null);
	 
	    // looping through all rows and adding to list
		    if (cursor.moveToFirst()) {
		        do {
		        	Story story = new Story(cursor.getInt(0), cursor.getString(1),
		    				cursor.getString(2), cursor.getInt(3), cursor.getString(14),
		    				cursor.getString(7), cursor.getString(4), cursor.getString(6),
		    				cursor.getString(5), cursor.getInt(8), cursor.getString(9),
		    				cursor.getString(10), cursor.getString(11), cursor.getLong(13),
		    				cursor.getLong(12));
		        	
		            storyList.add(story);
		        } while (cursor.moveToNext());
		    }
		cursor.close(); 
	    return storyList;
		}
	
	public int getStoryCount() {
	        String countQuery = "SELECT  * FROM " + TABLE_LIBRARY;
	    SQLiteDatabase db = this.getReadableDatabase();
	    Cursor cursor = db.rawQuery(countQuery, null);
	    cursor.close();	 
	    // return count
        return cursor.getCount();
	    }
	
	public int updateStory(Story story) {
		    SQLiteDatabase db = this.getWritableDatabase();
		 
			ContentValues values = new ContentValues();
			values.put(KEY_TITLE, story.getName());
			values.put(KEY_AUTHOR, story.getAuthor());
			values.put(KEY_AUTHOR_ID, story.getAuthor_id());
			values.put(KEY_RATING, story.getRating());
			values.put(KEY_GENRE, story.getGenre());
			values.put(KEY_LANGUAGUE, story.getlanguage());
			values.put(KEY_CATEGORY, story.getCategory());
			values.put(KEY_CHAPTER, story.getChapterLenght());
			values.put(KEY_LENGHT, story.getWordLenght());
			values.put(KEY_FAVORITES, story.getFavorites());
			values.put(KEY_FOLLOWERS, story.getFollows());
			values.put(KEY_PUBLISHED, story.getPublished().getTime());
			values.put(KEY_UPDATED, story.getUpdated().getTime());
			values.put(KEY_SUMMARY, story.getSummary());
		 
		    // updating row
	    return db.update(TABLE_LIBRARY, values, KEY_STORY_ID + " = ?",
		            new String[] { String.valueOf(story.getId())});
	}
	
	public void deleteStory(Story story) {
		    deleteStory(story.getId());
	}
	 
	public void deleteStory(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_LIBRARY, KEY_STORY_ID + " = ?",
		            new String[] { String.valueOf(id) });
	    db.close();
	}
	
	public Cursor getCursor(){
		String countQuery = "SELECT  * FROM " + TABLE_LIBRARY;
	    SQLiteDatabase db = this.getReadableDatabase();
	    return db.rawQuery(countQuery, null);
	}
}
