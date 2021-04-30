package com.spicymango.fanfictionreader.services;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
import java.net.CookieHandler;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple factory that creates instances of the {@code Downloader} for the different sites.
 * Created by Michael Chen on 02/15/2016.
 */
class DownloaderFactory {
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
	 * that can be used to download the stories of a particular site, as determined by the uri.
	 *
	 * @param uri The uri of the story that needs to be downloaded
	 * @param context The current context
	 * @return The downloader that works for the requested URI
	 */
	static Downloader getInstance(Uri uri, Context context, WebView webView) {
		if (URI_MATCHER.match(uri) == FAN_FICTION) {
			return new FanFictionDownloader(uri, context, webView);
		}
		throw new UnsupportedOperationException("Downloader Factory does not support the Uri: " + uri);
	}

	/**
	 * An object that can be used to download a story from the FanFiction site.
	 */
	interface Downloader {

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
		 * Retrieves the story's attributes, such as the story's last update date, etc.
		 * @return The {@link Story} object, which contains the relevant attributes
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
		 * @param lastPageRead The last chapter read by the user
		 * @param scrollOffset The user's position along the chapter
		 * @param saveChapters True if both chapters and metadata should be saved, false if only
		 *                           metadata should be updated.
		 * @throws IOException If writing to the device memory fails
		 */
		void saveStory(int lastPageRead, int scrollOffset, boolean saveChapters) throws IOException;

		/**
		 * Instructs the downloader to skip pre-existing chapters.
		 */
		void EnableIncrementalUpdating();

		void downloadIfMissing() throws IOException, ParseException, StoryNotFoundException;
	}

	private static final class FanFictionDownloader implements Downloader{

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

		private final long mStoryId;

		/**
		 * A handle to the service's context
		 */
		private final Context mContext;

		/**
		 *  A web view used to bypass Cloudflare's captcha
		 *  */
		private final WebView mWebView;

		private final Object mutex;
		private String mHtmlFromWebView;


		@SuppressLint("AddJavascriptInterface")
		private FanFictionDownloader(Uri uri, Context context, WebView webView) {
			mContext = context;
			mCurrentPage = 1;
			mutex = new Object();
			mHtmlFromWebView = null;

			mText = new SparseArray<>();

			// Get the story id
			if (uri == null) throw new IllegalArgumentException("The uri cannot be null");
			Matcher idMatcher = PATTERN_FF.matcher(uri.toString());
			if (!idMatcher.find()) throw new IllegalArgumentException("The uri " + uri + " is not valid");
			mStoryId = Long.parseLong(idMatcher.group(1));

			// Initialize the webView's callbacks
			mWebView = webView;

			// Try to load the document. This must be done from the main thread
			final Handler mainHandler = new Handler(mContext.getMainLooper());
			final Runnable runnable = () -> {
				mWebView.setWebViewClient(new CustomWebView());
				mWebView.addJavascriptInterface(new JavascriptListener(), "HTMLOUT");
				synchronized (mutex){
					mutex.notify();
				}
			};

			// Wait for the WebView to finish loading
			synchronized (mutex){
				try {
					mainHandler.post(runnable);
					mutex.wait();
				} catch (InterruptedException e){
					Log.e(this.getClass().getSimpleName(), "Thread interrupted", e);
				}
			}
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
			// The downloadChapter() code automatically fills mStory. Since every single story
			// is warrantied to have a chapter 1, attempt to download the chapter.
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
		public void downloadChapter() throws IOException, ParseException, StoryNotFoundException {
			final String url = "https://www.fanfiction.net/s/" + mStoryId + "/"
					+ mCurrentPage + "/";

			// Try to load the document. This must be done from the main thread
			final Handler mainHandler = new Handler(mContext.getMainLooper());
			final Runnable runnable = () -> mWebView.loadUrl(url);
			mHtmlFromWebView = null;

			// Wait for the WebView to finish loading
			synchronized (mutex){
				mainHandler.post(runnable);
				try {
					while (mHtmlFromWebView == null){
						mutex.wait();
					}
				} catch (InterruptedException e){
					throw new IOException();
				}
			}

			if (mHtmlFromWebView.equalsIgnoreCase("404")){
				throw new IOException();
			}

			final Document document = Jsoup.parse(mHtmlFromWebView, url);

			// On the first run, update the mStory variable
			if (mCurrentPage == 1) {
				mStory = parseDetails(document);

				// If an error occurs while parsing, quit
				// Note that there are two possibilities: either the error is in the server (which should fail silently)
				// or there is an issue in the app
				if (mStory == null) {
					if (document.body().text().contains("Story Not Found") || document.body().text().contains("FanFiction.Net Error Type 1")) {
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
				if (document.body().text().contains("FanFiction.Net Error Type 1")) {
					// If a server error occurs, ignore the story
					throw new StoryNotFoundException("Story " + mStoryId + " does not exist");
				} else {
					throw new ParseException("Error reading story text for id: "
													 + mStoryId + " and chapter " + mCurrentPage, 0);
				}
			}

			// Save the chapter on the sparse array
			mText.put(mCurrentPage, storyText);

			// If a download is successful, increment the current page counter. This only needs to
			// be done when the first chapter is downloaded.
			mCurrentPage++;
		}

		@Override
		public void saveStory(int lastPageRead, int scrollOffset, boolean saveChapters) throws IOException{
			// Write each of the chapters downloaded into the file system
			if (saveChapters){
				for (int i = 0; i < mText.size(); i++) {
					final int key = mText.keyAt(i);
					FileHandler.writeFile(mContext, mStoryId, key, mText.get(key));
				}
			}

			// By default, the Story object has a date added equal to 0 upon creation. If this value
			// is found, then the story is newly added and the date added should be set to the current
			// date. Otherwise, the date added should be conserved.
			final Date dateDownloaded, dateLastRead;
			final ContentResolver resolver = mContext.getContentResolver();
			final Cursor c = resolver.query(StoryProvider.FF_CONTENT_URI,
											new String[] { SqlConstants.KEY_ADDED, SqlConstants.KEY_LAST_READ },
											SqlConstants.KEY_STORY_ID + " = ?",
											new String[] { String.valueOf(mStoryId) }, null);

			if (c == null){
				// Validate the cursor
				dateDownloaded = null;
				dateLastRead = null;
			} else if (!c.moveToFirst()) {
				// Check that the cursor is not empty
				c.close();
				dateDownloaded = null;
				dateLastRead = null;
			} else{
				// Determine the last time the story was updated
				dateDownloaded = new Date(c.getLong(c.getColumnIndex(SqlConstants.KEY_ADDED)));
				dateLastRead = new Date(c.getLong(c.getColumnIndex(SqlConstants.KEY_LAST_READ)));
				c.close();
			}
			final Date dateAdded = (dateDownloaded != null ? dateDownloaded : new Date()),
					dateRead = (dateLastRead != null ? dateLastRead : new Date(0));

			// Update the content provider
			resolver.insert(StoryProvider.FF_CONTENT_URI, mStory.toContentValues(lastPageRead, scrollOffset, dateAdded, dateRead));
		}

		@Override
		public void EnableIncrementalUpdating() {
			//If there are more chapters, assume that the preceding chapters have not been revised
			// and skip them while updating
			int chaptersOnLastUpdate = StoryProvider.numberOfChapters(mContext, Site.FANFICTION, mStoryId);
			if (chaptersOnLastUpdate > 1 && getTotalChapters() > chaptersOnLastUpdate) {
				// The first chapter that should be downloaded is the one following the last
				// one from the previous update.
				mCurrentPage = chaptersOnLastUpdate + 1;
			}
			// Note that if a revision was performed, the total number of chapters before
			// and after the update will match; therefore, no chapters will be skipped
			// while updating.
		}

		@Override
		public void downloadIfMissing() throws IOException, ParseException, StoryNotFoundException {
			// First, check if the chapter is missing.
			if (FileHandler.getFile(mContext, mStoryId, mCurrentPage) == null) {
				// If it is missing, download the missing chapter.
				downloadChapter();
			} else {
				mCurrentPage++;
			}
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
			int authorId = Integer.parseInt(matcher.group(1));

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


		private class JavascriptListener{
			@android.webkit.JavascriptInterface
			public void processHTML(String html) {
				mHtmlFromWebView = html;
				synchronized (mutex) {
					mutex.notify();
				}
			}
		}

		private class CustomWebView extends WebViewClient {
			private final Object internalMutex = new Object();
			private boolean mHasLoaded;

			@Override
			public void onReceivedError(WebView view, WebResourceRequest request,
										WebResourceError error) {
				super.onReceivedError(view, request, error);

				mHtmlFromWebView = "404";
				synchronized (mutex) {
					mutex.notify();
				}
			}

			@Override
			public void onReceivedHttpError(WebView view, WebResourceRequest request,
											WebResourceResponse errorResponse) {
				super.onReceivedHttpError(view, request, errorResponse);

				// If an HttpError is received, it may be a "Wait a few seconds" Cloudflare page.
				// Use the internalMutex to wait 5 seconds before assuming an error.
				mHasLoaded = false;
				new Thread(()->{
					synchronized (internalMutex){
						try {
							internalMutex.wait(5*1000);
						} catch (InterruptedException ignored) {}
					}

					if (!mHasLoaded){

						// Even after waiting, the error persists. Return a connection error.
						mHtmlFromWebView = "404";
						synchronized (mutex) {
							mutex.notify();
						}
					}
				}).start();
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				// Notify mutex that the Cloudflare "Wait a few seconds" page has loaded.
				mHasLoaded = true;
				synchronized (internalMutex){
					internalMutex.notify();
				}

				// Pass the cookies from the webView to the cookie storage
				final CookieManager manager = CookieManager.getInstance();
				final String httpCookieHeader = manager.getCookie(url);

				if (httpCookieHeader != null){
					try {
						final URI uri = new URI("https://fanfiction.net/");
						final CookieStore cookieStore = ((java.net.CookieManager) CookieHandler.getDefault()).getCookieStore();

						for (String cookieString : httpCookieHeader.split(";")){
							final String[] splitCookie = cookieString.split("=");
							HttpCookie cookie = new HttpCookie(splitCookie[0], splitCookie[1]);
							cookieStore.add(uri, cookie);
						}

					} catch (URISyntaxException e) {
						Log.d(getClass().getSimpleName(), "Failed to parse URI =" + url, e);
					}
				}

				// Retrieve the html code.
				view.loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");

				super.onPageFinished(view, url);
			}
		}

	}
}
