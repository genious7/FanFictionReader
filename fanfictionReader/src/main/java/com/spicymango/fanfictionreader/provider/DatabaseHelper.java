package com.spicymango.fanfictionreader.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseHelper extends SQLiteOpenHelper implements SqlConstants {

	private static final int DATABASE_VERSION = 12; //Database version 11
	private static final String DATABASE_NAME = "library.db";

	//The name of the FanFiction table and the full text search virtual table
	protected static final String FANFICTION_TABLE = "library";
	protected static final String FANFICTION_TABLE_FTS = "fanfiction_library_fts";

	protected static final String FICTIONPRESS_TABLE = "fictionpress_library";

	// Define the trigger suffixes
	private static final String DELETE_TRIGGER = "_DEL";
	private static final String INSERT_TRIGGER = "_INS";
	private static final String UPDATE_TRIGGER = "_UPD";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	private static final String TABLE_DEF = "("
            + KEY_STORY_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE,"
            + KEY_TITLE + " TEXT NOT NULL,"
            + KEY_AUTHOR + " TEXT NOT NULL,"
            + KEY_AUTHOR_ID + " TEXT,"
            + KEY_RATING + " TEXT,"
            + KEY_GENRE + " TEXT,"
            + KEY_LANGUAGE + " TEXT,"
            + KEY_CATEGORY + " TEXT,"
            + KEY_CHAPTER + " INTEGER,"
            + KEY_LENGTH + " INTEGER,"
            + KEY_FAVORITES + " INTEGER,"
            + KEY_FOLLOWERS + " INTEGER,"
            + KEY_PUBLISHED + " INTEGER,"
            + KEY_UPDATED + " INTEGER,"
            + KEY_SUMMARY + " TEXT NOT NULL,"
			+ KEY_LAST + " INTEGER,"
			+ KEY_COMPLETE + " BOOLEAN,"
			+ KEY_OFFSET + " INTEGER,"
			+ KEY_CHARACTERS + " TEXT,"
			+ KEY_REVIEWS + " INTEGER,"
			+ KEY_ADDED + " INTEGER,"
			+ KEY_LAST_READ + " INTEGER"
			+ ")";

	private static final String FTS_TABLE_DEF =
			" USING fts3("
					+ KEY_FTS_TITLE + ", "
					+ KEY_FTS_AUTHOR + ", "
					+ KEY_FTS_CATEGORY + ", "
					+ KEY_FTS_SUMMARY + ", "
					+ KEY_FTS_CHARACTERS + ", "
					+ "tokenize=porter );";
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// Create the data tables for each web site
		final String fanFicTable = "CREATE TABLE " + FANFICTION_TABLE + TABLE_DEF;
		final String fictionPressTable = "CREATE TABLE " + FICTIONPRESS_TABLE + TABLE_DEF;
		db.execSQL(fanFicTable);
		db.execSQL(fictionPressTable);

		// Create the full text search virtual tables
		final String fanFicFTS = "CREATE VIRTUAL TABLE " + FANFICTION_TABLE_FTS + FTS_TABLE_DEF;
		db.execSQL(fanFicFTS);

		// Create the trigger that will delete items from the full text search tables whenever
		// they're deleted from the main table.
		final String fanFicDelTrigger = "CREATE TRIGGER " + FANFICTION_TABLE + DELETE_TRIGGER
				+ " AFTER DELETE ON " + FANFICTION_TABLE + " FOR EACH ROW BEGIN DELETE FROM "
				+ FANFICTION_TABLE_FTS + " WHERE " + KEY_FTS_ID + " = " + "OLD." + KEY_STORY_ID
				+ "; END";
		db.execSQL(fanFicDelTrigger);

		// Create the trigger that will insert items from the full text search tables whenever
		// they're inserted into the main table.
		final String fanFicInsTrigger = "CREATE TRIGGER " + FANFICTION_TABLE + INSERT_TRIGGER
				+ " AFTER INSERT ON " + FANFICTION_TABLE + " FOR EACH ROW BEGIN INSERT OR REPLACE INTO "
				+ FANFICTION_TABLE_FTS
				+ " (" + KEY_FTS_ID + ", "
				+ KEY_FTS_TITLE + ", "
				+ KEY_FTS_AUTHOR + ", "
				+ KEY_FTS_CATEGORY + ", "
				+ KEY_FTS_SUMMARY + ", "
				+ KEY_FTS_CHARACTERS + ") "
				+ " VALUES("
				+ "NEW." + KEY_STORY_ID + ", "
				+ "NEW." + KEY_TITLE + ", "
				+ "NEW." + KEY_AUTHOR + ", "
				+ "NEW." + KEY_CATEGORY + ", "
				+ "NEW." + KEY_SUMMARY + ", "
				+ "NEW." + KEY_CHARACTERS + ") "
				+ "; END";
		db.execSQL(fanFicInsTrigger);

		// Create the trigger that will update items from the full text search tables whenever
		// they're updated in the main table.
		final String fanFicUpdTrigger = "CREATE TRIGGER " + FANFICTION_TABLE + UPDATE_TRIGGER
				+ " AFTER UPDATE ON " + FANFICTION_TABLE + " FOR EACH ROW BEGIN UPDATE "
				+ FANFICTION_TABLE_FTS + " SET "
				+ KEY_FTS_TITLE+ "=" + "NEW." + KEY_TITLE + ", "
				+ KEY_FTS_AUTHOR + "=" + "NEW." + KEY_AUTHOR + ", "
				+ KEY_FTS_CATEGORY+ "=" + "NEW." + KEY_CATEGORY + ", "
				+ KEY_FTS_SUMMARY + "=" + "NEW." + KEY_SUMMARY + ", "
				+ KEY_FTS_CHARACTERS+ "=" + "NEW." + KEY_CHARACTERS
				+ " WHERE " + KEY_FTS_ID + " = " + "NEW." + KEY_STORY_ID
				+ "; END";
		db.execSQL(fanFicUpdTrigger);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int arg2) {
		if (oldVersion < 3) {
			db.execSQL("DROP TABLE IF EXISTS " + FANFICTION_TABLE);
			onCreate(db);
		}
		if (oldVersion < 4) {
			// In this version, the internal representation of lengths, favorites, and follows was
			// changed from text entries to integer entries
			db.execSQL("UPDATE " + FANFICTION_TABLE + " SET " + KEY_LENGTH + " = REPLACE(" + KEY_LENGTH+ ",',','')");
			db.execSQL("UPDATE " + FANFICTION_TABLE + " SET " + KEY_FAVORITES + " = REPLACE(" + KEY_FAVORITES+ ",',','')");
			db.execSQL("UPDATE " + FANFICTION_TABLE + " SET " + KEY_FOLLOWERS + " = REPLACE(" + KEY_FOLLOWERS+ ",',','')");
			
			db.execSQL("CREATE TABLE IF NOT EXISTS " +"tmp" + "("
	                + KEY_STORY_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE,"
	                + KEY_TITLE + " TEXT NOT NULL,"
	                + KEY_AUTHOR + " TEXT NOT NULL,"
	                + KEY_AUTHOR_ID + " TEXT,"
	                + KEY_RATING + " TEXT,"
	                + KEY_GENRE + " TEXT,"
	                + KEY_LANGUAGE + " TEXT,"
	                + KEY_CATEGORY + " TEXT,"
	                + KEY_CHAPTER + " INTEGER,"
	                + KEY_LENGTH + " INTEGER,"
	                + KEY_FAVORITES + " INTEGER,"
	                + KEY_FOLLOWERS + " INTEGER,"
	                + KEY_PUBLISHED + " INTEGER,"
	                + KEY_UPDATED + " INTEGER,"
	                + KEY_SUMMARY + " TEXT NOT NULL,"
	                + KEY_LAST + " INTEGER"
					+ ")");
			db.execSQL("INSERT INTO tmp SELECT * FROM " + FANFICTION_TABLE);
			db.execSQL("DROP TABLE " + FANFICTION_TABLE);
			db.execSQL("ALTER TABLE tmp RENAME TO " + FANFICTION_TABLE);
		}
		if (oldVersion < 5) {
			// A column was added in order to remember if a story was completed or not
			db.execSQL("ALTER TABLE " + FANFICTION_TABLE + " ADD COLUMN " + KEY_COMPLETE + " BOOLEAN DEFAULT 0");
		}
		if (oldVersion < 6) {
			// A column was added in order to remember the position of the scroll bar in the story
			db.execSQL("ALTER TABLE " + FANFICTION_TABLE + " ADD COLUMN " + KEY_OFFSET + " TEXT DEFAULT ''");
		}
		if (oldVersion < 7) {
			// A FictionPress table was defined.
			db.execSQL("CREATE TABLE " + FICTIONPRESS_TABLE + TABLE_DEF);
		}
		if (oldVersion < 8){
			// A column for the different characters was added
			db.execSQL("ALTER TABLE " + FANFICTION_TABLE + " ADD COLUMN " + KEY_CHARACTERS + " TEXT DEFAULT ''");
		}
		if (oldVersion < 9) {
			// A column for the different characters was added
			db.execSQL("ALTER TABLE " + FANFICTION_TABLE + " ADD COLUMN " + KEY_REVIEWS + " INTEGER DEFAULT 0");
		}
		if (oldVersion < 10) {
			// Create the full text search virtual tables
			final String fanFicFTS = "CREATE VIRTUAL TABLE " + FANFICTION_TABLE_FTS
					+ " USING fts3("
					+ KEY_FTS_TITLE + ", "
					+ KEY_FTS_AUTHOR + ", "
					+ KEY_FTS_CATEGORY + ", "
					+ KEY_FTS_SUMMARY + ", "
					+ KEY_FTS_CHARACTERS + ", "
					+ "tokenize=porter );";
			db.execSQL(fanFicFTS);

			// Fill the full text search table with the data from the regular table
			db.execSQL("INSERT INTO "
							   + FANFICTION_TABLE_FTS + " ("
							   + KEY_FTS_ID + ", "
							   + KEY_FTS_TITLE + ", "
							   + KEY_FTS_AUTHOR + ", "
							   + KEY_FTS_CATEGORY + ", "
							   + KEY_FTS_SUMMARY + ", "
							   + KEY_FTS_CHARACTERS + ") "

							   + " SELECT "
							   + KEY_STORY_ID + ", "
							   + KEY_TITLE + ", "
							   + KEY_AUTHOR + ", "
							   + KEY_CATEGORY + ", "
							   + KEY_SUMMARY + ", "
							   + KEY_CHARACTERS
							   + " FROM " + FANFICTION_TABLE);
		}

		if (oldVersion < 11){
			// Create the trigger that will delete items from the full text search tables whenever
			// they're deleted from the main table.
			final String fanFicDelTrigger = "CREATE TRIGGER " + FANFICTION_TABLE + DELETE_TRIGGER
					+ " AFTER DELETE ON " + FANFICTION_TABLE + " FOR EACH ROW BEGIN DELETE FROM "
					+ FANFICTION_TABLE_FTS + " WHERE " + KEY_FTS_ID + " = " + "OLD." + KEY_STORY_ID
					+ "; END";
			db.execSQL(fanFicDelTrigger);

			// Create the trigger that will insert items from the full text search tables whenever
			// they're inserted into the main table.
			final String fanFicInsTrigger = "CREATE TRIGGER " + FANFICTION_TABLE + INSERT_TRIGGER
					+ " AFTER INSERT ON " + FANFICTION_TABLE + " FOR EACH ROW BEGIN INSERT OR REPLACE INTO "
					+ FANFICTION_TABLE_FTS
					+ " (" + KEY_FTS_ID + ", "
					+ KEY_FTS_TITLE + ", "
					+ KEY_FTS_AUTHOR + ", "
					+ KEY_FTS_CATEGORY + ", "
					+ KEY_FTS_SUMMARY + ", "
					+ KEY_FTS_CHARACTERS + ") "
					+ " VALUES("
					+ "NEW." + KEY_STORY_ID + ", "
					+ "NEW." + KEY_TITLE + ", "
					+ "NEW." + KEY_AUTHOR + ", "
					+ "NEW." + KEY_CATEGORY + ", "
					+ "NEW." + KEY_SUMMARY + ", "
					+ "NEW." + KEY_CHARACTERS + ") "
					+ "; END";
			db.execSQL(fanFicInsTrigger);

			// Create the trigger that will update items from the full text search tables whenever
			// they're updated in the main table.
			final String fanFicUpdTrigger = "CREATE TRIGGER " + FANFICTION_TABLE + UPDATE_TRIGGER
					+ " AFTER UPDATE ON " + FANFICTION_TABLE + " FOR EACH ROW BEGIN UPDATE "
					+ FANFICTION_TABLE_FTS + " SET "
					+ KEY_FTS_TITLE+ "=" + "NEW." + KEY_TITLE + ", "
					+ KEY_FTS_AUTHOR + "=" + "NEW." + KEY_AUTHOR + ", "
					+ KEY_FTS_CATEGORY+ "=" + "NEW." + KEY_CATEGORY + ", "
					+ KEY_FTS_SUMMARY + "=" + "NEW." + KEY_SUMMARY + ", "
					+ KEY_FTS_CHARACTERS+ "=" + "NEW." + KEY_CHARACTERS
					+ " WHERE " + KEY_FTS_ID + " = " + "NEW." + KEY_STORY_ID
					+ "; END";
			db.execSQL(fanFicUpdTrigger);
		}
		if (oldVersion < 12) {
			// Columns for last read and time added to library were added
            db.execSQL("ALTER TABLE " + FANFICTION_TABLE + " ADD COLUMN " + KEY_ADDED + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + FANFICTION_TABLE + " ADD COLUMN " + KEY_LAST_READ + " INTEGER DEFAULT 0");
		}
	}
}
