package com.gmail.michaelchentejada.fanfictionreader;

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

import com.gmail.michaelchentejada.fanfictionreader.R;
import com.gmail.michaelchentejada.fanfictionreader.util.SqlConstants;
import com.gmail.michaelchentejada.fanfictionreader.util.Story;
import com.gmail.michaelchentejada.fanfictionreader.util.DatabaseHelper;
import com.gmail.michaelchentejada.fanfictionreader.util.StoryProvider;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.Html;
import android.text.Spanned;
import android.widget.Toast;

/**
 * Downloads a story into the library. Must pass the story id inside the intent.
 * @author Michael Chen
 */
public class LibraryDownloader extends Service {
	/**
	 * The startID; used to identify whether more than one story are being downloaded.
	 */
	private int startId;
	private long storyId;	
	private int lastPage;
	private Context context;
	public final static String EXTRA_STORY_ID = "Story id";
	public final static String EXTRA_LAST_PAGE = "Last page";
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.startId = startId;
		this.storyId = intent.getLongExtra(EXTRA_STORY_ID, -1);
		this.lastPage = intent.getIntExtra(EXTRA_LAST_PAGE, 1);
		
		context = this;
		
		if (storyId == -1) {
			stopSelf(startId);
			return START_REDELIVER_INTENT;
		}
		
		DownloadStory asyncTask = new DownloadStory();
		asyncTask.execute();
		
		return START_REDELIVER_INTENT;
	}
	
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
	private final static Pattern patternAuthor = Pattern.compile("/u/(\\d++)/");
	
	private class DownloadStory extends AsyncTask<Void, Void, Boolean>{
		
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
		 * Obtains the last time the story was updated, as a long
		 * 
		 * @return The long corresponding to the date of the last update, or -1
		 *         if the story is not present in the library.
		 */
		private long lastUpdated() {
			ContentResolver resolver = context.getContentResolver();
			Cursor c = resolver.query(StoryProvider.CONTENT_URI,
					new String[] { SqlConstants.KEY_UPDATED },
					SqlConstants.KEY_STORY_ID + " = ?",
					new String[] { String.valueOf(storyId) }, null);
			if (!c.moveToFirst()) {
				return -1;
			}
			int index = c.getColumnIndex(SqlConstants.KEY_UPDATED);
			return c.getLong(index);
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			
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
					}

					list.add(Html.fromHtml(document.select("div#storytext")
							.html()));

					if (isCancelled()) {
						return false;
					}

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

			ContentResolver resolver = context.getContentResolver();
			resolver.insert(StoryProvider.CONTENT_URI, story.toContentValues(lastPage));

			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result == false){
				Toast toast = Toast.makeText(context,
						getString(R.string.dialog_internet), 
						Toast.LENGTH_SHORT);							//Internet access error toast
				
				toast.show();
			}else{
				Toast toast = Toast.makeText(context,
						getString(R.string.library_success), 
						Toast.LENGTH_SHORT);							//Internet access error toast
				toast.show();
			}
			
			stopSelf(startId);
		}	
	}
}
