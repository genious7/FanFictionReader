package com.spicymango.fanfictionreader.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper  implements SqlConstants{
	
	private static final int DATABASE_VERSION = 6; //Database version 6
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
	                + KEY_LAST + " INTEGER,"
	                + KEY_COMPLETE + " BOOLEAN,"
	                + KEY_OFFSET + " INTEGER"
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
		if (oldVersion < 5) {
			db.execSQL("ALTER TABLE " + TABLE_LIBRARY + " ADD COLUMN " + KEY_COMPLETE + " BOOLEAN DEFAULT 0");
		}
		if (oldVersion < 6) {
			db.execSQL("ALTER TABLE " + TABLE_LIBRARY + " ADD COLUMN " + KEY_OFFSET + " INTEGER DEFAULT 0");
		}	
	}

}
