package com.spicymango.fanfictionreader.services;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.menu.librarymenu.LibraryMenuActivity;
import com.spicymango.fanfictionreader.util.Sites;
import com.spicymango.fanfictionreader.util.Story;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

/**
 * Downloads a story into the library. In order to use it, the story URI must be passed in the
 * intent.
 * <p>
 * The class queues up every intent and downloads a single story at a time. Multiple simultaneous
 * downloads have not been implemented since they could cause FanFiction.net to issue a temporary ip
 * ban due to excessive connections from a single device.
 * <p>
 * This class displays two independent notifications. The first notification shows the progress when
 * an individual story is being updated or downloaded. The second notification displays the lists of
 * downloaded/updated stories during batch updates.
 *
 * @author Michael Chen
 */
public class LibraryDownloader extends IntentService {
	/**
	 * Key for the offset desired. This is used to pass the offset on an intent.
	 * <p>
	 * The offset (an optional parameter) ensures that the user's position along the story is saved
	 * in the database whenever the user downloads a story.
	 */
	final static String EXTRA_OFFSET = "Offset";

	/**
	 * Key for the last chapter read. This is used to pass the current chapter on an intent.
	 * <p>
	 * The offset (an optional parameter) ensures that the user's position along the story is saved
	 * in the database whenever the user downloads a story.
	 */
	final static String EXTRA_LAST_PAGE = "Last page";

	/**
	 * Key for an integrity check flag, which forces the downloader to scan for missing files.
	 */
	final static String EXTRA_INTEGRITY = "Integrity Check";

	/**
	 * IDs for the following notifications
	 * <ul>
	 *     <li>"Checking for updates"</li>
	 *     <li>"Error Notifications"</li>
	 *     <li>"Update Completed"</li>
	 * </ul>
	 */
	private final static int NOTIFICATION_UPDATE_ID = 0;

	/**
	 * IDs for the following notifications
	 * <ul>
	 *     <li>"Downloading Story"</li>
	 *     <li>"Downloading Chapter #/#"</li>
	 *     <li>"Saving Story</li>
	 * </ul>
	 */
	private final static int NOTIFICATION_DOWNLOAD_ID = 1;

	/**
	 * ID For the required foreground notification
	 */
	private final static int NOTIFICATION_FOREGROUND_ID =  3;

	private final static String NOTIFICATION_CHANNEL = "Channel";

	/**
	 * The number of stories that have been already been checked for updates. This variable is used
	 * in order to derive the total number of stories queued, which is used to generate the progress
	 * bar.
	 */
	private int currentProgress = 0;

	/** Keeps track of errors*/
	private boolean hasParsingError, hasConnectionError, hasIoError;
	private int consecutiveConnectionErrors;

	/**
	 * Stores the time at which the update process began. This is used to calculate the time elapsed
	 * displayed in the notification.
	 */
	private long updateStartTime;

	/**
	 * Counts how many more stories need to be parsed before the complete notification is
	 * shown.
	 */
	private AtomicInteger mStoryQueueLength;

	/** Keeps track of story names for update purposes*/
	private final List<String> storiesUpdated = new ArrayList<>();

	private WebView mWebView;

	public LibraryDownloader() {
		super(LibraryDownloader.class.getName());
	}

	/**
	 * Downloads a story into the device. The reader's current location in the story is saved with
	 * the rest of the story properties. If the story already exists, the downloader will update the
	 * story details.
	 *
	 * @param context     The current context
	 * @param uri         The url that points to any chapter in the story
	 * @param currentPage The reader's current page
	 * @param offset      The reader's current scroll offset
	 */
	public static void download(Context context, Uri uri, int currentPage, int offset) {
		Intent i = new Intent(context, LibraryDownloader.class);
		i.setData(uri);
		i.putExtra(EXTRA_LAST_PAGE, currentPage);
		i.putExtra(EXTRA_OFFSET, offset);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(i);
		} else {
			context.startService(i);
		}
	}

	/**
	 * Scans a story for missing chapters and downloads them as required.
	 *
	 * @param context     The current context
	 * @param uri         The url that points to any chapter in the story
	 * @param currentPage The reader's current page
	 * @param offset      The reader's current scroll offset
	 */
	public static void integrityCheck(Context context, Uri uri, int currentPage, int offset) {
		Intent i = new Intent(context, LibraryDownloader.class);
		i.setData(uri);
		i.putExtra(EXTRA_LAST_PAGE, currentPage);
		i.putExtra(EXTRA_OFFSET, offset);
		i.putExtra(EXTRA_INTEGRITY, true);
		context.startService(i);
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate() {
		super.onCreate();

		// Clear error flags when the service is initialized.
		hasParsingError = false;
		hasConnectionError = false;
		hasIoError = false;
		consecutiveConnectionErrors = 0;

		// An atomic integer is used to synchronize incoming requests (which occur on the main
		// thread) with the website downloads, which occur asynchronously.
		mStoryQueueLength = new AtomicInteger(0);

		// The time at which the service starts.
		updateStartTime = System.currentTimeMillis();

		// Create the WebView through which HTTP requests will be performed
		initializeCookies();
		mWebView = new WebView(this);
		mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setDomStorageEnabled(true);


		// Create the Notification Channel
		if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
			final NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL,
																  getString(R.string.app_name),
																  NotificationManager.IMPORTANCE_LOW);
			final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			assert manager != null;
			manager.createNotificationChannel(channel);
			NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this, NOTIFICATION_CHANNEL);
			startForeground(NOTIFICATION_FOREGROUND_ID, builder.build());
		}
	}

	/**
	 * Initializes the cookie storage for the WebView
	 */
	private void initializeCookies(){
		// Set the webView cookies to match the http cookies
		CookieSyncManager.createInstance(this);
		final CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.setAcceptCookie(true);
		final CookieStore cookieStore = ((java.net.CookieManager) CookieHandler.getDefault()).getCookieStore();
		final List<HttpCookie> cookieList = cookieStore.getCookies();
		for (HttpCookie cookie : cookieList){
			URI baseUri;
			try {
				baseUri = new URI(Sites.FANFICTION.DESKTOP_URI.toString());
			} catch (URISyntaxException e) {
				continue;
			}

			// If the HttpCookie has a domain attribute, use that over the provided uri.
			if (cookie.getDomain() != null){
				// Remove the starting dot character of the domain, if exists (e.g: .domain.com -> domain.com)
				String domain = cookie.getDomain();
				if (domain.charAt(0) == '.') {
					domain = domain.substring(1);
				}

				// Create the new URI
				try{
					baseUri = new URI("https",
									  domain,
									  cookie.getPath() == null ? "/" : cookie.getPath(),
									  null);
				} catch (URISyntaxException e) {
					Log.w(this.getClass().getSimpleName(), e);
				}
			}

			String cookieHeader = cookie.toString() + "; domain=" + cookie.getDomain() +
					"; path=" + cookie.getPath();
			cookieManager.setCookie(baseUri.toString(), cookieHeader);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Add the story to the queue of stories that need to be checked for updates and increments
		// the queue length by one.
		mStoryQueueLength.incrementAndGet();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		// By this point, the notification been shown should be the completion notification.
		// If not, something went wrong. Remove the notification in order to avoid leaving a
		// non-cancelable notification.
		if (mStoryQueueLength.get() != 0){
			removeNotification(NOTIFICATION_UPDATE_ID);
			removeNotification(NOTIFICATION_DOWNLOAD_ID);
		}
		Log.d("LibraryDownloader", "Destroyed");

		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// If the connection error flag is true, a connection error occurred during the current
		// execution of the service. It is reasonable to assume that further downloads will fail,
		// which is why the service is cancelled.
		if (hasConnectionError){
			onUpdateComplete();
			stopSelf();
			return;
		}

		// The total number of stories in this update sequence. This includes both queued and
		// already checked (regardless of whether they were updated or not) stories.
		final int totalNumberOfStories = currentProgress + mStoryQueueLength.get();

		// Display the "Checking for updates" notification. If there is more than one story in the
		// queue the notification should update the progress bar accordingly.
		if (totalNumberOfStories > 0) {
			showCheckingNotification(currentProgress, totalNumberOfStories);
		} else {
			showUpdateNotification();
		}

		currentProgress++;

		// Attempt to download the story. Once done, remove the story from the queue.
		mStoryQueueLength.decrementAndGet();
		download(intent);

		// If the queue is empty, display the final notification if appropriate.
		if (mStoryQueueLength.get() == 0){
			onUpdateComplete();
		}
	}

	/**
	 * Downloads the chapters of a single story.
	 * @param intent An intent with a valid uri
	 */
	private void download(Intent intent){
		// Determine the current chapter and the page offset from the intent. These values are used
		// to save the user's location in the database. If not available, assume the user's location
		// is at the beginning of the story.
		final int currPage = intent.getIntExtra(EXTRA_LAST_PAGE, 1);
		final int offset = intent.getIntExtra(EXTRA_OFFSET, 0);

		// Determine if a full integrity check of the story should be performed. An integrity check
		// check for missing files and will download any missing chapters for a particular story.
		final boolean integrityCheck = intent.getBooleanExtra(EXTRA_INTEGRITY, false);

		// The uri, which contains the story site and id.
		final Uri uri = intent.getData();

		// The DownloaderFactory selects the downloader based on the url provided.
		final DownloaderFactory.Downloader downloader = DownloaderFactory.getInstance(uri, LibraryDownloader.this, mWebView);

		// The story variable holds the story's attributes
		Story story;

		// True if the story was updated, false otherwise. This is used to determine if the story's
		// name should be added to the notification.
		boolean updated = false;

		try {
			// First, the story details are obtained in order to determine if a new update is available.
			// Note that should an IOException occur,it should retry as required.
			while (true){
				try {
					story = downloader.getStoryState();
					consecutiveConnectionErrors = 0;
					break;
				} catch (IOException e){
					// Wait 5 seconds and re-download the chapter
					try	{
						Thread.sleep(5000);
					}
					catch(InterruptedException ex){
						Thread.currentThread().interrupt();
					}

					consecutiveConnectionErrors++;
					if (consecutiveConnectionErrors > 3) throw e;
				}
			}


			// The story title can be obtained from the story attributes
			final String storyTitle = story.getName();
			final long downloadStartTime = System.currentTimeMillis();

			if (integrityCheck){
				// If an integrity check is requested, re-download all missing chapters
				// Download each missing chapter, updating the notification as required
				while (downloader.hasNextChapter()) {
					showUpdateNotification(storyTitle, downloader.getCurrentChapter(), downloader.getTotalChapters(), downloadStartTime);

					while (true){
						try {
							downloader.downloadIfMissing();
							consecutiveConnectionErrors = 0;
							break;
						} catch (IOException e){
							// Wait 5 seconds and re-download the chapter
							try	{
								Thread.sleep(5000);
							}
							catch(InterruptedException ex){
								Thread.currentThread().interrupt();
							}

							consecutiveConnectionErrors++;
							if (consecutiveConnectionErrors > 3) throw e;
						}
					}
				}
			} else if (downloader.isUpdateNeeded()) {
				// If an update is required, begin the process
				// Download only new chapters if incremental updating is enabled.
				if (Settings.isIncrementalUpdatingEnabled(this)){
					downloader.EnableIncrementalUpdating();
				}

				// Download each chapter, updating the notification as required
				while (downloader.hasNextChapter()) {
					showUpdateNotification(storyTitle, downloader.getCurrentChapter(), downloader.getTotalChapters(), downloadStartTime);

					while (true){
						try {
							downloader.downloadChapter();
							consecutiveConnectionErrors = 0;
							break;
						} catch (IOException e){
							// Wait 5 seconds and re-download the chapter
							try	{
								Thread.sleep(5000);
							}
							catch(InterruptedException ex){
								Thread.currentThread().interrupt();
							}

							consecutiveConnectionErrors++;
							if (consecutiveConnectionErrors > 3) throw e;
						}
					}
				}
				updated = true;
			}

			// The saveStory method is called regardless of whether an update was done or not since
			// that will update the story attributes such as the number of followers, etc. The
			// notification is only shown if an update took place. This try/catch is separate from
			// the one below in order to distinguish internet connection errors from file IO errors.
			try {
				if (updated){
					showSavingNotification(storyTitle, downloadStartTime);
				}

				downloader.saveStory(currPage,offset, updated);

				// If updated, add the title of the story to the list so that it is displayed
				// in the completed notification
				if (updated){
					storiesUpdated.add(storyTitle);
				}
			} catch (IOException e) {
				// This shouldn't happen. Log the exception if it occurs
				FirebaseCrashlytics.getInstance().recordException(new IOException("Exception while saving sql parameters", e));

				// If no updated were required, fail silently upon error since no significant
				// changes were made. If an update was being performed but failed, set the error
				// flag.
				if (updated){
					hasIoError = true;
				}
			}

		} catch (IOException e) {
			// If a connection error occurs, set the flag and cancel the download by returning.
			hasConnectionError = true;
		} catch (StoryNotFoundException e) {
			// If the story is not found, exit without setting any flags. By not setting an error flag,
			// notifications are avoided for deleted stories during batch updates.
		} catch (ParseException e) {
			// Parsing errors should be logged on Crashlytics for further analysis.
			FirebaseCrashlytics.getInstance().recordException(e);
			hasParsingError = true;
		} finally{
			// Remove the notification after the download stage is completed
			removeNotification(NOTIFICATION_DOWNLOAD_ID);
		}
	}

	/**
	 * Selects the appropriate notification to display at the end of an update cycle.
	 */
	private void onUpdateComplete(){
		// Since this method is only called when no further stories are being updated or downloaded,
		// the download notification should be removed.
		removeNotification(NOTIFICATION_DOWNLOAD_ID);

		// Once every intent has been processed, display a "download complete" notification
		// if a story was updated. If an error occurred, show an error notification. If nothing
		// was done, remove the notification.
		if (storiesUpdated.size() > 0) {
			// At least one story was updated. Show the title of the updated stories.
			showUpdateCompleteNotification(storiesUpdated);
		} else if (hasConnectionError) {
			showErrorNotification(R.string.error_connection);
		} else if (hasParsingError) {
			showErrorNotification(R.string.error_parsing);
		} else if (hasIoError) {
			showErrorNotification(R.string.error_sd);
		} else {
			// The story did not require any updates; no changes were made.
			removeNotification(NOTIFICATION_UPDATE_ID);
		}
	}

	/**
	 * Removes the notification from the screen
	 *
	 * @param notificationId The id of the notification that needs to be removed.
	 */
	private void removeNotification(int notificationId) {
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager != null;
		manager.cancel(notificationId);
	}

	/**
	 * When checking more than one story for updates, shows a progress bar displaying how many
	 * stories have already been checked along with the "Checking for Updates" message.
	 *
	 * @param currentStory The story whose progress is currently being checked
	 * @param totalStories The total number of stories in the queue, including previously checked
	 *                     stories.
	 */
	private void showCheckingNotification(int currentStory, int totalStories) {
		// Calculate the percentage of stories checked
		final double percent = (((double) currentStory) / totalStories) * 100;

		// Create the notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this, NOTIFICATION_CHANNEL);
		builder.setContentTitle(getString(R.string.downloader_checking_updates));
		builder.setContentText(String.format(Locale.US, "%.2f%% (%d/%d)", percent, currentStory + 1, totalStories));
		builder.setProgress(totalStories, currentStory, currentStory == totalStories);
		builder.setWhen(updateStartTime);
		builder.setUsesChronometer(true);
		builder.setSmallIcon(android.R.drawable.ic_popup_sync);
		builder.setAutoCancel(false);

		// Set an empty Pending Intent on the notification
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager != null;
		manager.notify(NOTIFICATION_UPDATE_ID, builder.build());
	}

	private void showUpdateNotification(){
		// Create the notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this, NOTIFICATION_CHANNEL);
		builder.setContentTitle(getString(R.string.downloader_downloading));
		builder.setSmallIcon(android.R.drawable.stat_sys_download);
		builder.setAutoCancel(false);

		// Set an empty Pending Intent on the notification
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager != null;
		manager.notify(NOTIFICATION_DOWNLOAD_ID, builder.build());
	}

	/**
	 * When chapters for a specific story are being downloaded, shows the story title and page
	 * number
	 *
	 * @param storyTitle  The story's title
	 * @param currentPage The chapter being downloaded
	 * @param TotalPage   The total number of chapters
	 */
	private void showUpdateNotification(String storyTitle, int currentPage, int TotalPage, long downloadStartTime) {
		// Create the notification
		final String text = getString(R.string.downloader_context, storyTitle, currentPage, TotalPage);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this, NOTIFICATION_CHANNEL);
		builder.setContentTitle(getString(R.string.downloader_downloading));
		builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
		builder.setContentText(text);
		builder.setWhen(downloadStartTime);
		builder.setUsesChronometer(true);
		builder.setSmallIcon(android.R.drawable.stat_sys_download);
		builder.setAutoCancel(false);

		// Set an empty Pending Intent on the notification
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager != null;
		manager.notify(NOTIFICATION_DOWNLOAD_ID, builder.build());
	}

	private void showSavingNotification(String storyTitle, long downloadStartTime) {
		// Create the notification
		final String text = getString(R.string.downloader_context_saving, storyTitle);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this, NOTIFICATION_CHANNEL);
		builder.setContentTitle(getString(R.string.downloader_saving));
		builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
		builder.setContentText(text);
		builder.setWhen(downloadStartTime);
		builder.setUsesChronometer(true);
		builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
		builder.setAutoCancel(false);

		// Set an empty Pending Intent on the notification
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager != null;
		manager.notify(NOTIFICATION_DOWNLOAD_ID, builder.build());
	}

	/**
	 * Show a notification that displays an error
	 *
	 * @param errorString The error string id
	 */
	private void showErrorNotification(@StringRes int errorString) {
		// Create the notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this, NOTIFICATION_CHANNEL);
		builder.setContentTitle(getString(R.string.downloader_error));
		builder.setContentText(getString(errorString));
		builder.setSmallIcon(R.drawable.ic_not_close);
		builder.setAutoCancel(true);

		// Set an empty intent
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager != null;
		manager.notify(NOTIFICATION_UPDATE_ID, builder.build());
	}

	/**
	 * Shows the notification at the end of an update cycle.
	 * @param storyTitles The titles of the updated stories
	 */
	private void showUpdateCompleteNotification(List<String> storyTitles) {
		// The title of the notification contains the total number of stories updated
		final String title = getResources().getQuantityString(R.plurals.downloader_notification,
				storyTitles.size(), storyTitles.size(),
				DateUtils.formatElapsedTime((System.currentTimeMillis() - updateStartTime) / 1000L));

		// The content of the notification contains the comma separated list of the titles of the
		// stories updated
		final String contentText = TextUtils.join(", ", storyTitles);

		// Create the notification
		final NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(LibraryDownloader.this, NOTIFICATION_CHANNEL);
		notBuilder.setContentTitle(title);
		notBuilder.setSmallIcon(R.drawable.ic_not_check);
		notBuilder.setAutoCancel(true);
		notBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));
		notBuilder.setContentText(contentText);

		// If the notification is clicked, open the library
		final Intent i = new Intent(LibraryDownloader.this, LibraryMenuActivity.class);
		final TaskStackBuilder taskBuilder = TaskStackBuilder.create(LibraryDownloader.this);
		taskBuilder.addNextIntentWithParentStack(i);
		PendingIntent pendingIntent = taskBuilder.getPendingIntent(0,
																   PendingIntent.FLAG_UPDATE_CURRENT);
		notBuilder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager != null;
		manager.notify(NOTIFICATION_UPDATE_ID, notBuilder.build());
	}
}
