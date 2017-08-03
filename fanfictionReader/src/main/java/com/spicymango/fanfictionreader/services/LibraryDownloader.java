package com.spicymango.fanfictionreader.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.librarymenu.LibraryMenuActivity;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Downloads a story into the library. Must pass the story id inside the intent.
 *
 * @author Michael Chen
 */
public class LibraryDownloader extends IntentService {
	/**
	 * Key for the offset desired
	 */
	final static String EXTRA_OFFSET = "Offset";

	/**
	 * Key for the last chapter read.
	 */
	final static String EXTRA_LAST_PAGE = "Last page";

	/**
	 * IDs for the notifications generated.
	 */
	private final static int NOTIFICATION_UPDATE_ID = 0;
	private final static int NOTIFICATION_DOWNLOAD_ID = 1;

	/**
	 * The number of stories that have been checked for updates
	 */
	private int currentProgress = 0;

	/** Keeps track of errors*/
	private boolean hasParsingError, hasConnectionError, hasIoError;

	/**
	 * Used to show time since library update was started.
	 */
	private long updateStartTime;

	/**
	 * Counts how many more stories need to be parsed before the complete notification is
	 * shown.
	 */
	private AtomicInteger mStoryQueueLength;

	/** Keeps track of story names for update purposes*/
	private final List<String> storiesUpdated = new ArrayList<>();

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
		context.startService(i);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		hasParsingError = false;
		hasConnectionError = false;
		hasIoError = false;
		mStoryQueueLength = new AtomicInteger(0);
		updateStartTime = System.currentTimeMillis();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Add the story to the queue of stories that need to be checked
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
		// If there is a connection error, just end the service.
		if (hasConnectionError){
			onUpdateComplete();
			stopSelf();
			return;
		}

		// The total number of stories in this update sequence. This includes both queued and
		// already checked (regardless of whether they were modified) stories.
		final int totalNumberOfStories = currentProgress + mStoryQueueLength.get();

		// Display the updating notification. If there is more than one story in total the
		// notification should update the progress bar.
		if (totalNumberOfStories > 0) {
			showCheckingNotification(currentProgress, totalNumberOfStories);
		} else {
			showUpdateNotification();
		}

		currentProgress++;
		mStoryQueueLength.decrementAndGet();
		download(intent);

		if (mStoryQueueLength.get() == 0){
			onUpdateComplete();
		}
	}

	/**
	 * Downloads the chapters of a single story.
	 * @param intent An intent with a valid uri
	 */
	private void download(Intent intent){
		// Get the story uri
		final DownloaderFactory.Downloader downloader = DownloaderFactory.getInstance(intent, LibraryDownloader.this);

		// Get the story details
		try {
			downloader.getStoryState();
		} catch (IOException e) {
			hasConnectionError = true;
			return;
		} catch (StoryNotFoundException e) {
			return;
		} catch (ParseException e) {
			// Parsing errors should be logged
			Crashlytics.logException(e);
			hasParsingError = true;
			return;
		}

        removeNotification(NOTIFICATION_DOWNLOAD_ID);

		// If an update is required, begin the process
		if (downloader.isUpdateNeeded()) {
			final String storyTitle = downloader.getStoryTitle();
			final long downloadStartTime = System.currentTimeMillis();

			// Download each chapter, updating the notification as required
			try {
				while (downloader.hasNextChapter()) {
					showUpdateNotification(storyTitle, downloader.getCurrentChapter(), downloader.getTotalChapters(), downloadStartTime);
					downloader.downloadChapter();
				}
			} catch (StoryNotFoundException e) {
				// Disregard missing stories
				return;
			} catch (IOException e) {
				hasConnectionError = true;
				return;
			} catch (ParseException e) {
				// Parsing errors should be logged
				Crashlytics.logException(e);
				hasParsingError = true;
				return;
			}

			// Update the files and the sql database.
			try {
				showUpdateNotification(storyTitle, downloadStartTime);
				downloader.saveStory();

				// Upon success, add the title of the story to the list so that it is displayed
				// in the complete notification
				storiesUpdated.add(storyTitle);
			} catch (IOException e) {
				hasIoError = true;
			}
		} else {
			try {
				downloader.saveStory();
			} catch (IOException e) {
				// This shouldn't happen. Log the exception if it occurs
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
					Crashlytics.logException(new IOException("Exception while saving sql parameters", e));
				} else {
					Crashlytics.logException(new Exception("Exception while saving sql parameters", e));
				}
			}
		}
	}

	/**
	 * Selects the appropriate notification to display at the end of an update cycle.
	 */
	private void onUpdateComplete(){
		// Once every intent has been processed, display a "download complete" notification
		// if a story was updated. If an error occurred, show an error notification. If nothing
		// was done, remove the notification.
		removeNotification(NOTIFICATION_DOWNLOAD_ID);
		if (storiesUpdated.size() > 0) {
			// At least one story was updated. Show the title of the updated stories.
			showUpdateCompleteNotification(storiesUpdated, System.currentTimeMillis());
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
	 */
	private void removeNotification(int notificationId) {
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(notificationId);
	}

	/**
	 * When checking more than one story for updates, shows a progress bar displaying how many
	 * stories have already been checked.
	 *
	 * @param currentStory The story whose progress is currently being checked
	 * @param totalStories The total number of stories in the queue, including previously checked
	 *                     stories.
	 */
	private void showCheckingNotification(int currentStory, int totalStories) {
		// Calculate the percentage of stories checked
		double percent = (((double) currentStory) / totalStories) * 100;

		// Create the notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this);
		builder.setContentTitle(getString(R.string.downloader_checking_updates));
		builder.setContentText(String.format(Locale.US, "%.2f%% (%d/%d)", percent, currentStory + 1, totalStories));
		builder.setProgress(totalStories, currentStory, currentStory == totalStories);
		builder.setWhen(updateStartTime);
		builder.setUsesChronometer(true);
		builder.setSmallIcon(android.R.drawable.stat_notify_sync);
		builder.setAutoCancel(false);

		// Set an empty Pending Intent on the notification
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_UPDATE_ID, builder.build());
	}

	private void showUpdateNotification(){
		// Create the notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this);
		builder.setContentTitle(getString(R.string.downloader_downloading));
		builder.setSmallIcon(android.R.drawable.stat_sys_download);
		builder.setAutoCancel(false);

		// Set an empty Pending Intent on the notification
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
		NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this);
		builder.setContentTitle(getString(R.string.downloader_downloading));
		builder.setContentText(getString(R.string.downloader_context, storyTitle, currentPage, TotalPage));
		builder.setWhen(downloadStartTime);
		builder.setUsesChronometer(true);
		builder.setSmallIcon(android.R.drawable.stat_sys_download);
		builder.setAutoCancel(false);

		// Set an empty Pending Intent on the notification
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_DOWNLOAD_ID, builder.build());
	}

	private void showUpdateNotification(String storyTitle, long downloadStartTime) {
		// Create the notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this);
		builder.setContentTitle(getString(R.string.downloader_saving));
		builder.setContentText(getString(R.string.downloader_context_saving, storyTitle));
		builder.setWhen(downloadStartTime);
		builder.setUsesChronometer(true);
		builder.setSmallIcon(android.R.drawable.stat_sys_download);
		builder.setAutoCancel(false);

		// Set an empty Pending Intent on the notification
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_DOWNLOAD_ID, builder.build());
	}

	/**
	 * Show a notification that displays an error
	 *
	 * @param errorString The error string id
	 */
	private void showErrorNotification(@StringRes int errorString) {
		// Create the notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this);
		builder.setContentTitle(getString(R.string.downloader_error));
		builder.setContentText(getString(errorString));
		builder.setSmallIcon(R.drawable.ic_not_close);
		builder.setAutoCancel(true);

		// Set an empty intent
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_UPDATE_ID, builder.build());
	}

	/**
	 * Shows the notification at the end of an update cycle.
	 * @param storyTitles The titles of the updated stories
	 */
	private void showUpdateCompleteNotification(List<String> storyTitles, long updateCompleteTime) {
		// The title of the notification contains the total number of stories updated
		// TODO: Add "in" to strings.xml; use String.format()
		final String title = getResources().getQuantityString(R.plurals.downloader_notification,
															  storyTitles.size(), storyTitles.size())
				+ " in " + DateUtils.formatElapsedTime((updateCompleteTime - updateStartTime) / 1000l);

		// The content of the notification contains the comma separated list of the titles of the stories updated
		final String text = TextUtils.join(", ", storyTitles);

		// Create the notification
		final NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(LibraryDownloader.this);
		notBuilder.setContentTitle(title);
		notBuilder.setSmallIcon(R.drawable.ic_not_check);
		notBuilder.setAutoCancel(true);
		notBuilder.setContentText(text);

		// If the notification is clicked, open the library
		final Intent i = new Intent(LibraryDownloader.this, LibraryMenuActivity.class);
		final TaskStackBuilder taskBuilder = TaskStackBuilder.create(LibraryDownloader.this);
		taskBuilder.addNextIntentWithParentStack(i);
		PendingIntent pendingIntent = taskBuilder.getPendingIntent(0,
																   PendingIntent.FLAG_UPDATE_CURRENT);
		notBuilder.setContentIntent(pendingIntent);

		// Show or update the notification
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_UPDATE_ID, notBuilder.build());
	}

}
