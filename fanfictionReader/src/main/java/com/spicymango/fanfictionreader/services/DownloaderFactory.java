package com.spicymango.fanfictionreader.services;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseArray;

import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.activity.Site;
import com.spicymango.fanfictionreader.provider.SqlConstants;
import com.spicymango.fanfictionreader.provider.StoryProvider;
import com.spicymango.fanfictionreader.util.FileHandler;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.Sites;
import com.spicymango.fanfictionreader.util.Story;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple factory that creates instances of the {@code Downloader} for the different sites.
 * Created by Michael Chen on 02/15/2016.
 */
public class DownloaderFactory {
	private static final int FAN_FICTION = 0;
	private static final int FICTION_PRESS = 1;
	private static final int AO3 = 2;

	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

	// Initialize the URI Matcher
	static {
		// FanFiction
		URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "s/#/", FAN_FICTION);
		URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "s/#/#/", FAN_FICTION);
		URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "s/#/#/*", FAN_FICTION);
		URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "s/#/", FAN_FICTION);
		URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "s/#/#/", FAN_FICTION);
		URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "s/#/#/*", FAN_FICTION);

		// FictionPress
		URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "s/#/", FICTION_PRESS);
		URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "s/#/#/", FICTION_PRESS);
		URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "s/#/#/*", FICTION_PRESS);
		URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "s/#/", FICTION_PRESS);
		URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "s/#/#/", FICTION_PRESS);
		URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "s/#/#/*", FICTION_PRESS);

		// Archive of Our Own
		URI_MATCHER.addURI(Sites.ARCHIVE_OF_OUR_OWN.AUTHORITY, "works/#/", AO3);
		URI_MATCHER.addURI(Sites.ARCHIVE_OF_OUR_OWN.AUTHORITY, "works/#/chapters/#", AO3);
	}

	/**
	 * Gets an instance of the {@link com.spicymango.fanfictionreader.services.DownloaderFactory.Downloader}
	 * that can be used to download the stories of a particular site, as determined by the url in
	 * the intent uri.
	 *
	 * @param intent The intent used to open a downloader
	 * @param context The current context
	 * @return The downloader that works for the requested URI
	 */
	public static Downloader getInstance(Intent intent, Context context) {
		switch (URI_MATCHER.match(intent.getData())) {
			case FAN_FICTION:
				return new FanFictionDownloader(intent, context);
			default:
				throw new UnsupportedOperationException("Downloader Factory does not support the Uri: " + intent.getData());
		}
	}

	/**
	 * An object that can be used to download a story from the FanFiction site.
	 */
	public interface Downloader {

		/**
		 * Determines if there is a newer version of the story online.
		 *
		 * @return True if a new version is available, false otherwise.
		 */
		boolean isUpdateNeeded();

		/**
		 * Determines if an additional chapter can be downloaded
		 *
		 * @return True if there are more chapters, false otherwise
		 */
		boolean hasNextChapter();

		/**
		 * Gets the story statistics.
		 * @return The {@link Story}
		 * @throws IOException If an internet connection error occurs
		 * @throws ParseException If an error occurs while parsing
		 * @throws StoryNotFoundException If the story can not be found on the web site
		 */
		Story getStoryState() throws IOException, ParseException, StoryNotFoundException;

		/**
		 * Gets the total number of chapters in the story, as obtained from the online parser.
		 *
		 * @return The total number of chapters
		 */
		int getTotalChapters();

		/**
		 * Gets the chapter that will be downloaded on the next {@link Downloader#downloadChapter} call.
		 * @return The chapter number
		 */
		int getCurrentChapter();

		/**
		 * @return The story's title, as a string
		 */
		String getStoryTitle();

		/**
		 * Downloads the current chapter, as reported by {@link Downloader#getCurrentChapter()}. On
		 * success, the function will automatically advance the current chapter pointer to the next
		 * one.
		 *
		 * @throws IOException            If an Internet Connection error occurs
		 * @throws ParseException         If an error occurs while parsing
		 * @throws StoryNotFoundException If the story cannot be found online
		 */
		void downloadChapter() throws IOException, ParseException, StoryNotFoundException;

		/**
		 * Saves any downloaded chapters and story statistics to the device memory
		 *
		 * @throws IOException If writing to the device memory fails
		 */
		void saveStory() throws IOException;
	}

	public static final class FanFictionDownloader implements Downloader{

		/**
		 * Pattern describing the attributes fields
		 */
		private final static Pattern PATTERN_ATTRIB = Pattern.compile(""
			 + "(?i)\\ARated: Fiction ([KTM]\\+?) - "//Rating
			 + "([^-]+) - "//language
			 + "(?:([^ ]+) - )?"//Genre
			 + "(?:(?!Chapters)((?:(?! - ).)+?(?:(?<=Jenny) - [^-]+)?) - )?"//characters
			 + "(?>Chapters: (\\d+) - )?" //Chapters
			 + "Words: ([\\d,]+) - " //Words
			 + "(?>Reviews: ([\\d,]+) - )?"//Reviews (non capturing)
			 + "(?>Favs: ([\\d,]+) - )?"//favorites
			 + "(?>Follows: ([\\d,]+))?"); //Follows

		/**
		 * Describes the author id and story id Url pattern
		 */
		private final static Pattern PATTERN_FF = Pattern.compile("/[us]/(\\d++)/");

		/**
		 * The mStory details. If the stories have not been loaded, this field is equal to null
		 */
		private Story mStory;

		/**
		 * The last chapter downloaded successfully. This value is changed by hasNextChapter
		 */
		private int mCurrentPage;

		/**
		 * An array of the story chapters downloaded. Each string is an html file.
		 */
		private final SparseArray<String> mText;

		/**
		 * The reader's offset along the story. This field is used to remember the user position so
		 * that the story opens in the same spot the next time.
		 */
		private final int mOffset;

		/**
		 * The reader's currently selected chapter. This field is used to remember the user position
		 * so that the story opens in the same spot the next time.
		 */
		private final int mLastPage;

		private final long mStoryId;

		/**
		 * A handle to the service's context
		 */
		private final Context mContext;

		public FanFictionDownloader(Intent intent, Context context) {
			mContext = context;
			mCurrentPage = 1;

			mText = new SparseArray<>();

			// Get the story id
			final Uri uri = intent.getData();
			if (uri == null) throw new IllegalArgumentException("The uri cannot be null");
			Matcher idMatcher = PATTERN_FF.matcher(uri.toString());
			if (!idMatcher.find()) throw new IllegalArgumentException("The uri " + uri + " is not valid");
			mStoryId = Long.parseLong(idMatcher.group(1));

			// Find the reader's position in the story
			mLastPage = intent.getIntExtra(LibraryDownloader.EXTRA_LAST_PAGE, 1);
			mOffset = intent.getIntExtra(LibraryDownloader.EXTRA_OFFSET, 0);
		}

		@Override
		public boolean isUpdateNeeded() {
			// Verify the class state
			if (mStory == null)
				throw new IllegalStateException("The mStory state must be loaded before determining if an update is required");

			// Get the date the story was last updated
			// Get the cursor
			final long prevUpdate;
			final ContentResolver resolver = mContext.getContentResolver();
			final Cursor c = resolver.query(StoryProvider.FF_CONTENT_URI,
											new String[] { SqlConstants.KEY_UPDATED },
											SqlConstants.KEY_STORY_ID + " = ?",
											new String[] { String.valueOf(mStoryId) }, null);

			if (c == null){
				// Validate the cursor
				prevUpdate = -1;
			} else if (!c.moveToFirst()) {
				// Check that the cursor is not empty
				c.close();
				prevUpdate = -1;
			} else{
				// Determine the last time the story was updated
				final int index = c.getColumnIndex(SqlConstants.KEY_UPDATED);
				prevUpdate = c.getLong(index);
				c.close();
			}

			return mStory.getUpdated().getTime() > prevUpdate;
		}

		@Override
		public boolean hasNextChapter() {
			return mCurrentPage <= getTotalChapters();
		}

		@Override
		public Story getStoryState() throws IOException, ParseException, StoryNotFoundException {
			// The downloadChapter() code automatically fills mStory.
			if (mStory == null) {
				mCurrentPage = 1;
				downloadChapter();
			}
			return mStory;
		}

		@Override
		public int getTotalChapters() {
			// Verify the class state
			if (mStory == null)
				throw new IllegalStateException("The mStory state must be loaded before calling getStoryTitle");
			return mStory.getChapterLength();
		}

		@Override
		public int getCurrentChapter() {
			return mCurrentPage;
		}

		@Override
		public String getStoryTitle() {
			// Verify the class state
			if (mStory == null)
				throw new IllegalStateException("The mStory state must be loaded before calling getStoryTitle");
			return mStory.getName();
		}

		@Override
		public void downloadChapter() throws IOException, ParseException, StoryNotFoundException {

			String url = "https://www.fanfiction.net/s/" + mStoryId + "/"
					+ mCurrentPage + "/";
			Document document = Jsoup.connect(url).timeout(10000).userAgent("Mozilla/5.0").get();

			// On the first run, update the mStory variable
			if (mCurrentPage == 1) {
				mStory = parseDetails(document);

				// If an error occurs while parsing, quit
				// Note that there are two possibilities: either the author deleted the story
				// or there is an issue in the app
				if (mStory == null) {
					if (document.body().text().contains("Story Not Found")) {
						// If the story was deleted from the web site, pass the error to the previous layer
						throw new StoryNotFoundException("Story " + mStoryId + " does not exist");
					} else {
						throw new ParseException("Error parsing story attributes for id: " + mStoryId, 0);
					}
				}
			}

			// Load the chapter itself
			String storyText = document.select("div#storytext").html();
			if (storyText == null || storyText.length() == 0) {
				throw new ParseException("Error reading story text for id: "
												 + mStoryId + " and chapter " + mCurrentPage, 0);
			}

			// Save the chapter on the sparse array
			mText.put(mCurrentPage, storyText);

			// If a download is successful, increment the current page counter. This only needs to
			// be done when the first chapter is downloaded.

			// If incremental updating is selected, determine if there are new chapters
			// over the previous update. This should only be done once.
			if (mCurrentPage == 1 && Settings.isIncrementalUpdatingEnabled(mContext)) {

				//If there are more chapters, assume that the preceding chapters have not been revised
				// and skip them while updating
				int chaptersOnLastUpdate = StoryProvider.numberOfChapters(mContext, Site.FANFICTION, mStoryId);
				if (chaptersOnLastUpdate > 1 && getTotalChapters() > chaptersOnLastUpdate) {
					// Due to the mCurrentPage++ below, the updates will start on the next
					// chapter, as desired.
					mCurrentPage = chaptersOnLastUpdate;
				}
				// Note that if a revision was performed, the total number of chapters before
				// and after the update will match; therefore, no chapters will be skipped
				// while updating.
			}
			mCurrentPage++;
		}

		@Override
		public void saveStory() throws IOException{
			// Write each of the chapters downloaded into the file system
			for (int i = 0; i < mText.size(); i++) {
				final int key = mText.keyAt(i);
				FileHandler.writeFile(mContext, mStoryId, key, mText.get(key));
			}

			// Update the content provider
			final ContentResolver resolver = mContext.getContentResolver();
			final Date added = (mStory.getAdded().getTime() > 0L ? mStory.getAdded() : new Date());
			resolver.insert(StoryProvider.FF_CONTENT_URI, mStory.toContentValues(mLastPage, mOffset, added));
		}

		/**
		 * Obtains the story object from the one available online
		 *
		 * @param document The web page
		 * @return The story object, or null if a parsing error occurs
		 */
		private Story parseDetails(Document document) {

			Elements categoryElements = document.select("div#pre_story_links span a");
			if (categoryElements.isEmpty()) return null;
			String category = categoryElements.last().ownText();

			Element titleElement = document.select("div#profile_top > b").first();
			if (titleElement == null) return null;
			String title = titleElement.ownText();

			Element authorElement = document.select("div#profile_top > a").first();
			if (authorElement == null) return null;
			String author = authorElement.ownText();

			Matcher matcher = PATTERN_FF.matcher(authorElement.attr("href"));
			if (!matcher.find()) return null;
			int authorId = Integer.valueOf(matcher.group(1));

			Element summaryElement = document.select("div#profile_top > div").first();
			if (summaryElement == null) return null;
			String summary = summaryElement.ownText();

			Element attributes = document.select("div#profile_top > span").last();

			Elements dates = attributes.select("span[data-xutime]");
			if (dates.isEmpty()) return null;
			long updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
			long publishDate = Long.parseLong(dates.last().attr("data-xutime")) * 1000;


			matcher = PATTERN_ATTRIB.matcher(attributes.text());
			if (!matcher.find()) return null;

			Story.Builder builder = new Story.Builder();
			builder.setId(mStoryId);
			builder.setName(title);
			builder.setAuthor(author);
			builder.setAuthorId(authorId);
			builder.setSummary(summary);
			builder.setCategory(category);
			builder.setRating(matcher.group(1));
			builder.setLanguage(matcher.group(2));
			if (matcher.group(3) != null) builder.setGenre(matcher.group(3));
			if (matcher.group(4) != null) {
				String[] characterArray = matcher.group(4).split("([,\\[\\]] ?)++");
				for (String character : characterArray) {
					if (!TextUtils.isEmpty(character))
						builder.addCharacter(character);
				}
			}
			if (matcher.group(5) != null)
				builder.setChapterLength(Parser.parseInt(matcher.group(5)));
			builder.setWordLength(Parser.parseInt(matcher.group(6)));
			if (matcher.group(7) != null) builder.setReviews(Parser.parseInt(matcher.group(7)));
			if (matcher.group(8) != null) builder.setFavorites(Parser.parseInt(matcher.group(8)));
			if (matcher.group(9) != null) builder.setFollows(Parser.parseInt(matcher.group(9)));
			builder.setUpdateDate(updateDate);
			builder.setPublishDate(publishDate);
			builder.setCompleted(attributes.text().contains("Complete"));

			return builder.build();
		}

	}
}
