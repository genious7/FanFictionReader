package com.spicymango.fanfictionreader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.Spanned;

import com.spicymango.fanfictionreader.activity.LibraryMenuActivity;
import com.spicymango.fanfictionreader.util.SqlConstants;
import com.spicymango.fanfictionreader.util.Story;
import com.spicymango.fanfictionreader.util.StoryProvider;

/**
 * Downloads a story into the library. Must pass the story id inside the intent.
 * @author Michael Chen
 */
public class LibraryDownloader extends IntentService{
	
	/**
	 * Key for the last chapter read.
	 */
	public final static String EXTRA_LAST_PAGE = "Last page";
	
	/**
	 * Used for clearing the notification counter
	 */
	public final static String EXTRA_UPDATE_NOT = "Notification";
	
	/**
	 * Key for the story id
	 */
	public final static String EXTRA_STORY_ID = "Story id";
	
	/**
	 * Pattern describing the attributes fields
	 */
	private final static Pattern patternAttrib = Pattern.compile(""
			+ "(?i)\\ARated: Fiction ([KTM]\\+?) - "//Rating
			+ "([^-]+) - "//language
			+ "(?:([^-]+) - )?"//Genre
			+ "(?:(?!(?>Chapters))[^-]+ - )?"//characters (non capturing)
			+ "(?:Chapters: (\\d++) - )?" //Chapters
			+ "Words: ([\\d,]++) - " //Words
			+ "(?:Reviews: [\\d,]++ - )?"//Reviews (non capturing)
			+ "(?:Favs: ([\\d,]++) - )?"//favorites
			+ "(?:Follows: ([\\d,]++))?"); //Follows
	
	/**
	 * Describes the author id Url pattern
	 */
	private final static Pattern patternAuthor = Pattern.compile("/u/(\\d++)/");
	
	/**
	 * ID for the notifications generated.
	 */
	private final static int NOTIFICATION_ID = 1;
	
	private int lastPage;
	
	private long storyId;
	
	/**
	 * Used to keep track for notification purposes
	 */
	private static int storiesDownloaded = 0;

	public LibraryDownloader() {
		super("Story Downloader");
		setIntentRedelivery(true);
	}
	
	/**
	 * Downloads the story if a newer version is available 
	 * @return True if the operation succeeds, false otherwise
	 */
	private boolean download(){
		Story story = new Story();
		
		int totalPages = 1;
		ArrayList<Spanned> list = new ArrayList<Spanned>();
		try {
			for (int currentPage = 1; currentPage <= totalPages; currentPage++) {

				Document document = Jsoup.connect(
						"https://www.fanfiction.net/s/" + storyId + "/"
								+ currentPage + "/").get();

				if (totalPages == 1) { // On first run only
					story = parseDetails(document);

					if (story == null)
						return false;
					
					//If no updates have been made to the story, skip
					if (story.getUpdated().getTime() == lastUpdated()) { 
						return true;
					}

					totalPages = story.getChapterLenght();
					showNotification(story.getName());
				}

				list.add(Html.fromHtml(document.select("div#storytext")
						.html()));
			}
		} catch (IOException e) {
			return false;
		}
		for (int currentPage = 0; currentPage < totalPages; currentPage++) {
			try {
				File file = new File(getFilesDir(), storyId + "_" + (currentPage + 1) + ".txt");
				FileOutputStream fos = new FileOutputStream( file);
				fos.write(list.get(currentPage).toString().getBytes());
				fos.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}

		ContentResolver resolver = this.getContentResolver();
		resolver.insert(StoryProvider.CONTENT_URI, story.toContentValues(lastPage));

		return true;
	}
	
	
	/**
	 * Obtains the last time the story was updated, as a long
	 * 
	 * @return The long corresponding to the date of the last update, or -1
	 *         if the story is not present in the library.
	 */
	private long lastUpdated() {
		ContentResolver resolver = this.getContentResolver();
		Cursor c = resolver.query(StoryProvider.CONTENT_URI,
				new String[] { SqlConstants.KEY_UPDATED },
				SqlConstants.KEY_STORY_ID + " = ?",
				new String[] { String.valueOf(storyId) }, null);
		if (c == null || !c.moveToFirst()) {
			return -1;
		}
		int index = c.getColumnIndex(SqlConstants.KEY_UPDATED);
		return c.getLong(index);
	}

	/**
	 * Obtains the story object from the one available online
	 * @param document The web page
	 * @return The story object
	 */
	private Story parseDetails(Document document){
		
		String category = "";
		Elements categoryElements = document.select("div#pre_story_links span a");
		switch (categoryElements.size()) {
		case 1:		
			category = categoryElements.first().ownText();//Crossover
			break;
		case 2:
			category = categoryElements.get(1).ownText();//Normal
			break;
		default:
			return null;
		}
		
		Element titleElement = document.select("div#profile_top > b").first();
		if (titleElement == null) return null;
		String title = titleElement.ownText();
		
		Element authorElement = document.select("div#profile_top > a").first();
		if (authorElement == null) return null;
		String author = authorElement.ownText();
		Matcher matcher = patternAuthor.matcher(authorElement.attr("href"));
		if (!matcher.find()) return null;
		int authorId = Integer.valueOf(matcher.group(1));
		
		Element summaryElement = document.select("div#profile_top > div").first();
		if (summaryElement == null) return null;
		String summary = summaryElement.ownText();
		
		Element attribs = document.select("div#profile_top > span").last();	
		
		Elements dates = attribs.select("span[data-xutime]");
		long updateDate = 0;
		long publishDate = 0;

		if (dates.size() == 1) {
			updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
			publishDate = updateDate;
		} else if (dates.size() == 2) {
			updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
			publishDate = Long.parseLong(dates.last().attr("data-xutime")) * 1000;
		}else return null;
		
		matcher = patternAttrib.matcher(attribs.text());
		if (!matcher.find()) return null;
		
		return new Story(storyId, title, author, authorId, summary,
				category, matcher.group(1), matcher.group(2),
				matcher.group(3) == null ? "" : matcher.group(3),
				matcher.group(4) == null ? 1 : Integer.valueOf(matcher.group(4)), 
				matcher.group(5),
				matcher.group(6) == null ? "" : matcher.group(6),
				matcher.group(7) == null ? "" : matcher.group(7),
				updateDate, publishDate);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent.getBooleanExtra(EXTRA_UPDATE_NOT, false)) {
			storiesDownloaded = 0;
			return;
		}
		storiesDownloaded++;
		storyId = intent.getLongExtra(EXTRA_STORY_ID, -1);
		lastPage = intent.getIntExtra(EXTRA_LAST_PAGE, 1);

		showNotification("");

		if (download()){
			showCompletetionNotification();
		}else{
			showErrorNotification();
		}
	}
	
	private void showNotification(String storyTitle){
		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(this);
		notBuilder.setContentTitle(getResources().getString(R.string.downloader_downloading));
		notBuilder.setContentText(storyTitle);
		notBuilder.setSmallIcon(R.drawable.ic_action_download);
		notBuilder.setAutoCancel(false);
		
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_ID, notBuilder.build());
	}
	
	private void showErrorNotification(){
		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(this);
		notBuilder.setContentTitle(getResources().getString(R.string.downloader_error));
		notBuilder.setSmallIcon(R.drawable.ic_action_cancel);
		notBuilder.setAutoCancel(false);
		
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_ID, notBuilder.build());
	}
	
	private void showCompletetionNotification(){
		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(this);
		String title = getResources().getQuantityString(R.plurals.downloader_notification, storiesDownloaded, storiesDownloaded);
		notBuilder.setContentTitle(title);
		notBuilder.setSmallIcon(R.drawable.ic_action_accept);
		notBuilder.setAutoCancel(true);
		
		Intent i = new Intent(this, LibraryMenuActivity.class);
		TaskStackBuilder taskBuilder = TaskStackBuilder.create(this);
		taskBuilder.addNextIntentWithParentStack(i);
		
		Intent e = new Intent(this,LibraryDownloader.class);
		e.putExtra(EXTRA_UPDATE_NOT, true);
		PendingIntent cancelIntent = PendingIntent.getService(this, 0, e, 0);
		
		PendingIntent pendingIntent = taskBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		notBuilder.setContentIntent(pendingIntent);
		notBuilder.setDeleteIntent(cancelIntent);
		
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_ID, notBuilder.build());
	}
	
	
}
