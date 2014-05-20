package com.gmail.michaelchentejada.fanfictionreader.util;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper  implements SqlConstants{
	
	private static final int DATABASE_VERSION = 3; //Database version 3
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
	                + KEY_TITLE + " TEXT NOT NULL,"
	                + KEY_AUTHOR + " TEXT NOT NULL,"
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
	                + KEY_SUMMARY + " TEXT NOT NULL,"
	                + KEY_LAST + " INTEGER"
	                + ")";
		 db.execSQL(CREATE_CONTACTS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		arg0.execSQL("DROP TABLE IF EXISTS " + TABLE_LIBRARY);
		onCreate(arg0);
	}

	/**
	 * Adds a new story to the database
	 * @param story The story to add
	 * @param lastChapterRead The last chapter read by user
	 */
	public void addStory(Story story, int lastChapterRead){
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
		values.put(KEY_LAST, lastChapterRead);
		db.insert(TABLE_LIBRARY, null, values);
	    db.close();
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
		if (cursor == null || !cursor.moveToFirst()){
	    	return -1;
		}else{
			int columnIndex = cursor.getColumnIndex(KEY_LAST);
			return cursor.getInt(columnIndex);
		}
			
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
	    
	    if (cursor == null || !cursor.moveToFirst())
	    	return null;
	 
		Story story = new Story(cursor);
		
		cursor.close();
		
	    return story;
	}
	
	/**
	 * Finds if a story is already in the library
	 * @param id The id of the story
	 * @return True if it exists, false otherwise
	 */
	public boolean hasStory(long id) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LIBRARY, new String[] { KEY_STORY_ID },
				KEY_STORY_ID + "=?", new String[] { String.valueOf(id) }, null,
				null, null);
		return cursor.moveToFirst();
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
		cursor.close();
		// return count
		return cursor.getCount();
	}

	/**
	 * Deletes a story from the database
	 * @param story The story to delete
	 */
	public void deleteStory(Story story) {
		    deleteStory(story.getId());
	}
	 
	/**
	 * Deletes a story from the database
	 * @param id The id of the story to delete
	 */
	public void deleteStory(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_LIBRARY, KEY_STORY_ID + " = ?",
		            new String[] { String.valueOf(id) });
	    db.close();
	}
}
