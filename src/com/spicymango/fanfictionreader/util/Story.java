package com.spicymango.fanfictionreader.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.spicymango.fanfictionreader.provider.SqlConstants;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Story implements Parcelable, SqlConstants {

	private final long id; // Story id, 7 digit number
	private final String name; // The name of the story
	private final String author; // The author's name
	private final long author_id; // The author's id
	private final String summary; // The story's summary
	private final String category; // The story's category
	private final String rating; // The story's rating
	private final String language;
	private final String genre; // Both genres combined
	private final int chapterLenght;
	private final int wordLenght;
	private final int favorites;
	private final int follows;
	private final boolean completed;// Whether the story is complete or not
	private final Date updated;
	private final Date published;
	private final List<String> characters;

	public static Story fromCursor(Cursor cursor) {
		Builder builder = new Builder();
		builder.setId(cursor.getLong(0));
		builder.setName(cursor.getString(1));
		builder.setAuthor(cursor.getString(2));
		builder.setAuthorId(cursor.getLong(3));
		builder.setRating(cursor.getString(4));
		builder.setGenre(cursor.getString(5));
		builder.setLanguage(cursor.getString(6));
		builder.setCategory(cursor.getString(7));
		builder.setChapterLenght(cursor.getInt(8));
		builder.setWordLenght(cursor.getInt(9));
		builder.setFavorites(cursor.getInt(10));
		builder.setFollows(cursor.getInt(11));
		builder.setPublishDate(cursor.getLong(12));
		builder.setUpdateDate(cursor.getLong(13));
		builder.setSummary(cursor.getString(14));
		builder.setCompleted(cursor.getShort(16) == 1);
		return builder.build();
	}

	public ContentValues toContentValues(int lastPage, int offset) {
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
		v.put(KEY_LENGTH, wordLenght);
		v.put(KEY_FAVORITES, favorites);
		v.put(KEY_FOLLOWERS, follows);
		v.put(KEY_UPDATED, updated.getTime());
		v.put(KEY_PUBLISHED, published.getTime());
		v.put(KEY_LAST, lastPage);
		v.put(KEY_COMPLETE, completed ? 1 : 0);
		v.put(KEY_OFFSET, offset);
		return v;
	}

	/**
	 * Creates a new Story object
	 * 
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
	private Story(long id, String name, String author, long authorId, String summary, String category, String rating,
			String language, String genre, int chapterLenght, int wordLenght, int favorites, int follows, Date updated,
			Date published, boolean completed) {
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
		this.completed = completed;
		characters = new ArrayList<>();
	}

	// --------------------------------------Parceling--------------------------------------------------

	/**
	 * Generates a new Story object from the parcel.
	 * 
	 * @param in The parcel containing the object.
	 */
	public static Story fromParcel(Parcel in) { // NO_UCD (use private)
		Builder builder = new Builder();
		builder.setId(in.readLong());
		builder.setName(in.readString());
		builder.setAuthor(in.readString());
		builder.setAuthorId(in.readLong());
		builder.setSummary(in.readString());
		builder.setCategory(in.readString());
		builder.setRating(in.readString());
		builder.setLanguage(in.readString());
		builder.setGenre(in.readString());
		builder.setChapterLenght(in.readInt());
		builder.setWordLenght(in.readInt());
		builder.setFavorites(in.readInt());
		builder.setFollows(in.readInt());
		builder.setUpdateDate(new Date(in.readLong()));
		builder.setPublishDate(new Date(in.readLong()));
		builder.setCompleted(in.readByte() != 0);

		List<String> characters = new ArrayList<>();
		in.readStringList(characters);
		builder.setCharacters(characters);

		return builder.build();
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
		dest.writeInt(wordLenght);
		dest.writeInt(favorites);
		dest.writeInt(follows);
		dest.writeLong(updated.getTime());
		dest.writeLong(published.getTime());
		dest.writeByte((byte) (completed ? 1 : 0));
		dest.writeStringList(characters);
	}

	/**
	 * Used for parceling.
	 */
	public static final Parcelable.Creator<Story> CREATOR = new Parcelable.Creator<Story>() { // NO_UCD

		@Override
		public Story createFromParcel(Parcel source) {
			return Story.fromParcel(source);
		}

		@Override
		public Story[] newArray(int size) {
			return new Story[size];
		}
	};

	// --------------------------------------Getters-------------------------------------------------

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
		return Parser.withSuffix(wordLenght);
	}

	/**
	 * @return the favorites
	 */
	public String getFavorites() {
		return Parser.withSuffix(favorites);
	}

	/**
	 * @return the follows
	 */
	public String getFollows() {
		return Parser.withSuffix(follows);
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

	public boolean isCompleted() {
		return completed;
	}

	public final static class Builder {
		private long id; // Story id, 7 digit number
		private String name; // The name of the story
		private String author; // The author's name
		private long authorId; // The author's id
		private String summary; // The story's summary
		private String category; // The story's category
		private String rating; // The story's rating
		private String language;
		private String genre; // Both genres combined
		private int chapterLenght;
		private int wordLenght;
		private int favorites;
		private int follows;
		private boolean completed;// Whether the story is complete or not
		private Date updated;
		private Date published;
		private List<String> characters;

		private final static Pattern ATTRIBUTE_PATTERN = Pattern.compile("(?i)\\A"// At
				// the
				// beginning
				// of
				// the
				// line
				+ "(?:([^,]++)(?:,[^,]++)*?, )?" // Category or crossover
				+ "([KTM]\\+?), "// Rating
				+ "([^,]++), " // Language
				+ "(?:(?!(?>chapter|words))([^,]++), )?" // Genre
				+ "(?:chapters: (\\d++), )?" // Chapters
				+ "words: ([^,]++), " // Words
				+ "(?:favs: ([^,]++), )?" // favorites
				+ "(?:follows: ([^,]++), )?"); // follows

		public Builder() {
			//Initialize to default values
			id = 0;
			name = "";
			author = "";
			authorId = 0;
			summary = "";
			category = "";
			rating = "";
			language = "";
			genre = "";
			chapterLenght = 1;
			wordLenght = 0;
			favorites = 0;
			follows = 0;
			updated = new Date();
			published = new Date();
			completed = false;
			characters = new ArrayList<>();
		}

		public Story build() {
			return new Story(id, name, author, authorId, summary, category, rating, language, genre, chapterLenght,
					wordLenght, favorites, follows, updated, published, completed);
		}

		public void setId(long id) {
			this.id = id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public void setAuthorId(long author_id) {
			this.authorId = author_id;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public void setRating(String rating) {
			this.rating = rating;
		}

		public void setLanguage(String language) {
			this.language = language;
		}

		public void setGenre(String genre) {
			this.genre = genre;
		}

		public void setChapterLenght(int chapterLenght) {
			this.chapterLenght = chapterLenght;
		}

		public void setWordLenght(int wordLenght) {
			this.wordLenght = wordLenght;
		}

		public void setFavorites(int favorites) {
			this.favorites = favorites;
		}

		public void setFollows(int follows) {
			this.follows = follows;
		}

		public void setCompleted(boolean completed) {
			this.completed = completed;
		}

		public void setUpdateDate(Date updated) {
			this.updated = updated;
		}

		public void setUpdateDate(long updated) {
			this.updated = new Date(updated);
		}

		public void setPublishDate(Date published) {
			this.published = published;
		}

		public void setPublishDate(long published) {
			this.published = new Date(published);
		}

		public void setCharacters(List<String> characters) {
			this.characters = characters;
		}

		public void addCharacters(String character) {
			this.characters.add(character);
		}

		public void setFanFicAttribs(String attribs) {
			Matcher match = ATTRIBUTE_PATTERN.matcher(attribs);
			if (match.find()) {
				if (match.group(1) != null) setCategory(match.group(1));
				setRating(match.group(2));
				setLanguage(match.group(3));
				if (match.group(4) != null) setGenre(match.group(4));
				if (match.group(5) != null) setChapterLenght(Integer.valueOf(match.group(5)));
				setWordLenght(Parser.parseInt(match.group(6)));
				if (match.group(7) != null) setFavorites(Parser.parseInt(match.group(7)));
				if (match.group(8) != null) setFollows(Parser.parseInt(match.group(8)));
			} else {
				Log.d("Story - Parse", attribs);
			}
		}
	}
}
