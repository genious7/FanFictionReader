package com.spicymango.fanfictionreader.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.spicymango.fanfictionreader.provider.SqlConstants;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import android.text.TextUtils;
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
	private final int chapterLength;
	private final int wordLength;
	private final int favorites;
	private final int follows;
	private final boolean completed;// Whether the story is complete or not
	private final Date updated;
	private final Date published;
	private final List<String> characters;
	private final int mReviews;
	private final Date added;
	private final Date lastRead;

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
		builder.setChapterLength(cursor.getInt(8));
		builder.setWordLength(cursor.getInt(9));
		builder.setFavorites(cursor.getInt(10));
		builder.setFollows(cursor.getInt(11));
		builder.setPublishDate(cursor.getLong(12));
		builder.setUpdateDate(cursor.getLong(13));
		builder.setSummary(cursor.getString(14));
		builder.setCompleted(cursor.getShort(16) == 1);

		String characters = cursor.getString(18);
		String[] characterArray = characters.split("\n");
		builder.setCharacters(Arrays.asList(characterArray));

		builder.setReviews(cursor.getInt(19));

		builder.setAdded(cursor.getLong(20));
		builder.setLastRead(cursor.getLong(21));

		return builder.build();
	}

	public ContentValues toContentValues(int lastPage, int offset, Date added, Date lastRead) {
		ContentValues v = new ContentValues();
		v.put(KEY_STORY_ID, id);
		v.put(KEY_TITLE, name);
		v.put(KEY_AUTHOR, author);
		v.put(KEY_AUTHOR_ID, author_id);
		v.put(KEY_SUMMARY, summary);
		v.put(KEY_CATEGORY, category);
		v.put(KEY_RATING, rating);
		v.put(KEY_LANGUAGE, language);
		v.put(KEY_GENRE, genre);
		v.put(KEY_CHAPTER, chapterLength);
		v.put(KEY_LENGTH, wordLength);
		v.put(KEY_FAVORITES, favorites);
		v.put(KEY_FOLLOWERS, follows);
		v.put(KEY_UPDATED, updated.getTime());
		v.put(KEY_PUBLISHED, published.getTime());
		v.put(KEY_LAST, lastPage);
		v.put(KEY_COMPLETE, completed ? 1 : 0);
		v.put(KEY_OFFSET, offset);
		v.put(KEY_CHARACTERS,  TextUtils.join("\n",characters));
		v.put(KEY_REVIEWS, mReviews);
		v.put(KEY_ADDED, added.getTime());
		v.put(KEY_LAST_READ, lastRead.getTime());
		return v;
	}

	/**
	 * Creates a new Story object. This constructor is only used internally by the {@link
	 * Story.Builder} class.
	 *
	 * @param id            The 7 digit story id
	 * @param name          The name of the story
	 * @param author        The name of the author
	 * @param authorId      The 7 digit author code
	 * @param summary       The story's summary
	 * @param category      The story's category
	 * @param rating        The story's rating
	 * @param language      The story's language
	 * @param genre         The story's genre
	 * @param chapterLength The number of chapters
	 * @param wordLength    The number of words
	 * @param favorites     The number of favorites
	 * @param follows       The number of follows
	 * @param updated       The date it was updated
	 * @param published     The date it was published.
	 * @param completed     True if the story has been completed, false otherwise
	 * @param characters    The list of characters that are featured in the story
	 * @param reviews       The number of reviews the story has
	 * @param added			The date the story was added to the library
	 * @param lastRead		The date that the story was last openend
	 */
	private Story(long id, String name, String author, long authorId, String summary,
				  String category, String rating,
				  String language, String genre, int chapterLength, int wordLength, int favorites,
				  int follows, Date updated,
				  Date published, boolean completed, List<String> characters, int reviews,
				  Date added, Date lastRead) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.author_id = authorId;
		this.summary = summary;
		this.category = category;
		this.rating = rating;
		this.language = language;
		this.genre = genre;
		this.chapterLength = chapterLength;
		this.wordLength = wordLength;
		this.favorites = favorites;
		this.follows = follows;
		this.updated = updated;
		this.published = published;
		this.completed = completed;
		this.characters = characters;
		mReviews = reviews;
		this.added = added;
		this.lastRead = lastRead;
	}

	// --------------------------------------Parceling--------------------------------------------------

	/**
	 * Generates a new Story object from the parcel. This constructor is only used internally by the
	 * {@link Story#CREATOR}.
	 *
	 * @param in The parcel containing the object.
	 */
	private static Story fromParcel(Parcel in) {
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
		builder.setChapterLength(in.readInt());
		builder.setWordLength(in.readInt());
		builder.setFavorites(in.readInt());
		builder.setFollows(in.readInt());
		builder.setUpdateDate(new Date(in.readLong()));
		builder.setPublishDate(new Date(in.readLong()));
		builder.setCompleted(in.readByte() != 0);

		List<String> characters = new ArrayList<>();
		in.readStringList(characters);
		builder.setCharacters(characters);

		builder.setReviews(in.readInt());
		builder.setAdded(in.readLong());
		builder.setLastRead(in.readLong());

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
		dest.writeInt(chapterLength);
		dest.writeInt(wordLength);
		dest.writeInt(favorites);
		dest.writeInt(follows);
		dest.writeLong(updated.getTime());
		dest.writeLong(published.getTime());
		dest.writeByte((byte) (completed ? 1 : 0));
		dest.writeStringList(characters);
		dest.writeInt(mReviews);
		dest.writeLong(added.getTime());
		dest.writeLong(lastRead.getTime());
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
	@NonNull
	public String getName() {
		return name;
	}

	/**
	 * @return the author
	 */
	@NonNull
	public String getAuthor() {
		return author;
	}

	/**
	 * @return the author_id
	 */
	public long getAuthorId() {
		return author_id;
	}

	/**
	 * @return the summary
	 */
	@NonNull
	public String getSummary() {
		return summary;
	}

	/**
	 * @return the category
	 */
	@NonNull
	public String getCategory() {
		return category;
	}

	/**
	 * @return the rating
	 */
	@NonNull
	public String getRating() {
		return rating;
	}

	/**
	 * @return the language
	 */
	@NonNull
	public String getLanguage() {
		return language;
	}

	/**
	 * @return the genre
	 */
	@NonNull
	public String getGenre() {
		return genre;
	}

	/**
	 * @return the chapterLength
	 */
	public int getChapterLength() {
		return chapterLength;
	}

	/**
	 * @return the wordLength
	 */
	@NonNull
	public String getWordLength() {
		return Parser.withSuffix(wordLength);
	}

	/**
	 * @return the favorites
	 */
	@NonNull
	public String getFavorites() {
		return Parser.withSuffix(favorites);
	}

	/**
	 * @return the follows
	 */
	@NonNull
	public String getFollows() {
		return Parser.withSuffix(follows);
	}

	/**
	 * @return the updated
	 */
	@NonNull
	public Date getUpdated() {
		return updated;
	}

	/**
	 * @return the published
	 */
	@NonNull
	public Date getPublished() {
		return published;
	}

	public boolean isCompleted() {
		return completed;
	}

	@NonNull
	public List<String> getCharacters() {
		return characters;
	}

	/**
	 * Returns the number of reviews a story has. The default value if no reviews were specified
	 * when building the Story object is zero.
	 *
	 * @return The number of reviews
	 */
	public int getReviews() {
		return mReviews;
	}

	@NonNull
	public Date getAdded() { return added; }

	@NonNull
	public Date getLastRead() { return lastRead; }

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
		private int chapterLength;
		private int wordLength;
		private int favorites;
		private int follows;
		private boolean completed;// Whether the story is complete or not
		private Date updated;
		private Date published;
		private List<String> characters;
		private int reviews;
		private Date added;
		private Date lastRead;

		private final static Pattern ATTRIBUTE_PATTERN = Pattern.compile("(?i)\\A"// At
																				 // the
																				 // beginning
																				 // of
																				 // the
																				 // line
			 + "(?:([^,]++)(?:,[^,]++)*?, )?"            // Category or crossover
			 + "([KTM]\\+?), "                            // Rating
			 + "([^,]++), "                                // Language
			 + "(?:(?!(?>chapter|words))([^,]++), )?"    // Genre
			 + "(?:chapters: (\\d++), )?"                // Chapters
			 + "words: ([^,]++), "                        // Words
			 + "(?:favs: ([^,]++), )?"                    // Favorites
			 + "(?:follows: ([^,]++), )?"                // Follows
			 + "(?:updated: .+? published: )?"            // Updated
			 + "(?:\\w{3} \\d{1,2}(?:, \\d{4})?|(?:[^,]++))"// Published
			 + "(?:, (.*+))?"                            // Characters
		);

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
			chapterLength = 1;
			wordLength = 0;
			favorites = 0;
			follows = 0;
			updated = new Date();
			published = new Date();
			completed = false;
			characters = new ArrayList<>();
			reviews = 0;
			lastRead = new Date(0);
			added = new Date(0);
		}

		public Story build() {
			return new Story(id, name, author, authorId, summary, category, rating, language, genre, chapterLength,
							 wordLength, favorites, follows, updated, published, completed, characters, reviews, added, lastRead);
		}

		public void setId(long id) {
			this.id = id;
		}

		public void setName(@NonNull String name) {
			this.name = name;
		}

		public void setAuthor(@NonNull String author) {
			this.author = author;
		}

		public void setAuthorId(long author_id) {
			this.authorId = author_id;
		}

		public void setSummary(@NonNull String summary) {
			this.summary = summary;
		}

		public void setCategory(@NonNull String category) {
			this.category = category;
		}

		public void setRating(@NonNull String rating) {
			this.rating = rating;
		}

		public void setLanguage(@NonNull String language) {
			this.language = language;
		}

		public void setGenre(@NonNull String genre) {
			this.genre = genre;
		}

		public void setChapterLength(int chapterLength) {
			this.chapterLength = chapterLength;
		}

		public void setWordLength(int wordLength) {
			this.wordLength = wordLength;
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

		public void setUpdateDate(@NonNull Date updated) {
			this.updated = updated;
		}

		public void setUpdateDate(long updated) {
			this.updated = new Date(updated);
		}

		public void setPublishDate(@NonNull Date published) {
			this.published = published;
		}

		public void setPublishDate(long published) {
			this.published = new Date(published);
		}

		public void setCharacters(@NonNull List<String> characters) {
			this.characters = characters;
		}

		public void addCharacter(@NonNull String character) {
			this.characters.add(character);
		}

		public void setReviews(int reviews){
			this.reviews = reviews;
		}

		public void setAdded(@NonNull Date added) { this.added = added; }

		public void setAdded(long added) { this.added = new Date(added); }

		public void setLastRead(@NonNull Date lastRead) { this.lastRead = lastRead; }

		public void setLastRead(long lastRead) { this.lastRead = new Date(lastRead); }

		public void setFanFicAttributes(@NonNull String attributes) {
			Matcher match = ATTRIBUTE_PATTERN.matcher(attributes);
			if (match.find()) {
				if (match.group(1) != null) setCategory(match.group(1));
				setRating(match.group(2));
				setLanguage(match.group(3));
				if (match.group(4) != null) setGenre(match.group(4));
				if (match.group(5) != null) setChapterLength(Integer.valueOf(match.group(5)));
				setWordLength(Parser.parseInt(match.group(6)));
				if (match.group(7) != null) setFavorites(Parser.parseInt(match.group(7)));
				if (match.group(8) != null) setFollows(Parser.parseInt(match.group(8)));

				// Set characters if they exist
				if (match.group(9) != null) {
					String[] characters = match.group(9).split("([,\\[\\]] ?)++");
					for (String character : characters) {
						if (!TextUtils.isEmpty(character))
							addCharacter(character);
					}
				}
			} else {
				Log.d("Story - Parse", attributes);
			}
		}
	}
}
