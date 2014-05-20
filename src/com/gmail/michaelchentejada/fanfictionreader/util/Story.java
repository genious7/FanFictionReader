package com.gmail.michaelchentejada.fanfictionreader.util;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class Story implements Parcelable, SqlConstants{
	
	private long id; //Story id, 7 digit number
	private String name; //The name of the story
	private String author; //The author's name
	private long author_id; //The author's id
	private String summary; //The story's summary
	private String category; //The story's category
	private String rating; //The story's rating
	private String language;
	private String genre; //Both genres combined
	private int chapterLenght;
	private String wordLenght;
	private String favorites;
	private String follows;
	private Date updated;
	private Date published;
	
	private final static Pattern ATTRIBUTE_PATTERN = Pattern.compile(
			"(?i)\\A"//At the beginning of the line
			+ "(?:([^,]+), )?" //Category or crossover
			+ "([KTM]\\+?), "//Rating
			+ "([^,]++), " //Language
			+ "(?:(?!(?>chapter|words))([^,]++), )?" //Genre
			+ "(?:chapters: (\\d++), )?" //Chapters
			+ "words: ([^,]++), " //Words
			+ "(?:favs: ([^,]++), )?" //favorites
			+ "(?:follows: ([^,]++), )?"); //follows 
	
	/**
	 * Default constructor
	 */
	public Story() {
		this(0, "", "", 0, "", "", "", "", "", 1, "", "0", "0", new Date(),
				new Date());
	}
	
	public Story(Cursor cursor){
		this(cursor.getLong(0), cursor.getString(1),
			cursor.getString(2), cursor.getLong(3), cursor.getString(14),
			cursor.getString(7), cursor.getString(4), cursor.getString(6),
			cursor.getString(5), cursor.getInt(8), cursor.getString(9),
			cursor.getString(10), cursor.getString(11), cursor.getLong(13),
			cursor.getLong(12));
	}
	
	public ContentValues toContentValues (int lastPage){
		ContentValues v = new ContentValues();
		v.put(KEY_STORY_ID, id);
		v.put(KEY_TITLE, name);
		v.put(KEY_AUTHOR, author);
		v.put(KEY_AUTHOR_ID, author_id);
		v.put(KEY_SUMMARY, summary);
		v.put(KEY_CATEGORY, category);
		v.put(KEY_RATING, rating);
		v.put(KEY_LANGUAGUE, language);
		v.put(KEY_GENRE, genre);
		v.put(KEY_CHAPTER, chapterLenght);
		v.put(KEY_LENGHT, wordLenght);
		v.put(KEY_FAVORITES, favorites);
		v.put(KEY_FOLLOWERS, follows);
		v.put(KEY_UPDATED, updated.getTime());
		v.put(KEY_PUBLISHED, published.getTime());
		v.put(KEY_LAST, lastPage);
		return v;
	}
	
	/**
	 * Creates a new Story object	
	 * @param id The 7 digit story id
	 * @param name The name of the story
	 * @param author The name of the author
	 * @param author Id The 7 digit author code
	 * @param summary The story's summary
	 * @param category The story's category
	 * @param rating The story's rating
	 * @param language The story's language
	 * @param genre The story's genre
	 * @param chapterLenght The number of chapters
	 * @param wordLenght The number of words
	 * @param favorites The number of favorites
	 * @param follows The number of follows
	 * @param updated The date it was updated
	 * @param published The date it was published.
	 */
	public Story(long id, String name, String author, long authorId,
			String summary, String category, String rating, String language,
			String genre, int chapterLenght, String wordLenght, String favorites,
			String follows, Date updated, Date published) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.author_id = authorId;
		this.summary = summary;
		this.category = category;
		this.rating = rating;
		this.language = language;
		this.genre = genre;
		this.chapterLenght = chapterLenght;
		this.wordLenght = wordLenght;
		this.favorites = favorites;
		this.follows = follows;
		this.updated = updated;
		this.published = published;
	}
	
	/**
	 * Creates a new Story object	
	 * @param id The 7 digit story id
	 * @param name The name of the story
	 * @param author The name of the author
	 * @param author Id The 7 digit author code
	 * @param summary The story's summary
	 * @param category The story's category
	 * @param rating The story's rating
	 * @param language The story's language
	 * @param genre The story's genre
	 * @param chapterLenght The number of chapters
	 * @param wordLenght The number of words
	 * @param favorites The number of favorites
	 * @param follows The number of follows
	 * @param updated The date it was updated
	 * @param published The date it was published.
	 */
	public Story(long id, String name, String author, long authorId,
			String summary, String category, String rating, String language,
			String genre, int chapterLenght, String wordLenght, String favorites,
			String follows, long updated, long published) {
		
		this(id, name, author, authorId, summary, category, rating, language,
				genre, chapterLenght, wordLenght, favorites, follows, new Date(
						updated), new Date(published));
	}
	
	/**
	 * Creates a new Story object
	 * @param id The story's id
	 * @param name The story's name
	 * @param author The story's author
	 * @param authorId The author's id
	 * @param summary The story's summary
	 * @param attribs The attributes element
	 */
	public Story(long id, String name, String author, long authorId, String summary, String attribs, long updated, long published){
		
		this(id,name,author,authorId,summary,"","","","",1,"","0","0",updated,published);
		Matcher match = ATTRIBUTE_PATTERN.matcher(attribs);
		if (match.find()) {
			if (match.group(1) != null)
				category = match.group(1);
			rating = match.group(2);
			language = match.group(3);
			if (match.group(4) != null)
				genre = match.group(4);
			if (match.group(5) != null)
				chapterLenght = Integer.valueOf(match.group(5));
			wordLenght = match.group(6);
			if (match.group(7) != null)
				favorites = match.group(7);
			if (match.group(8) != null)
				follows = match.group(8);
		}
	}
	
	/*-------------------------------------------------------
	 * Parcel Stuff
	 *-----------------------------------------------------*/
	 
	/**
	 * Generates a new Story object from the parcel.
	 * @param in The parcel containing the object.
	 */
	public Story(Parcel in){
		this.id = in.readLong();
		this.name = in.readString();
		this.author = in.readString();
		this.author_id = in.readLong();
		this.summary = in.readString();
		this.category = in.readString();
		this.rating = in.readString();
		this.language = in.readString();
		this.genre = in.readString();
		this.chapterLenght = in.readInt();
		this.wordLenght = in.readString();
		this.favorites = in.readString();
		this.follows = in.readString();
		this.updated = new Date(in.readLong());
		this.published = new Date(in.readLong());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(name);
		dest.writeString(author);
		dest.writeLong(author_id);
		dest.writeString(summary);
		dest.writeString(category);
		dest.writeString(rating);
		dest.writeString(language);
		dest.writeString(genre);
		dest.writeInt(chapterLenght);
		dest.writeString(wordLenght);
		dest.writeString(favorites);
		dest.writeString(follows);
		dest.writeLong(updated.getTime());
		dest.writeLong(published.getTime());
	}
	
	/**
	 * Used for parceling.
	 */
	public static final Parcelable.Creator<Story> CREATOR = new Parcelable.Creator<Story>() {
		
		@Override
		public Story createFromParcel(Parcel source) {
			return new Story(source);
		}

		@Override
		public Story[] newArray(int size) {
			return new Story[size];
		}
	};

	
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @return the author_id
	 */
	public long getAuthor_id() {
		return author_id;
	}

	/**
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @return the rating
	 */
	public String getRating() {
		return rating;
	}

	/**
	 * @return the language
	 */
	public String getlanguage() {
		return language;
	}

	/**
	 * @return the genre
	 */
	public String getGenre() {
		return genre;
	}

	/**
	 * @return the chapterLenght
	 */
	public int getChapterLenght() {
		return chapterLenght;
	}

	/**
	 * @return the wordLenght
	 */
	public String getWordLenght() {
		return wordLenght;
	}

	/**
	 * @return the favorites
	 */
	public String getFavorites() {
		return favorites;
	}

	/**
	 * @return the follows
	 */
	public String getFollows() {
		return follows;
	}

	/**
	 * @return the updated
	 */
	public Date getUpdated() {
		return updated;
	}

	/**
	 * @return the published
	 */
	public Date getPublished() {
		return published;
	}
		
}
