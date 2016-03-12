package com.spicymango.fanfictionreader.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.activity.LibraryMenuActivity;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Downloads a story into the library. Must pass the story id inside the intent.
 *
 * @author Michael Chen
 */
public class LibraryDownloader extends Service {
	/**
	 * Key for the offset desired
	 */
	final static String EXTRA_OFFSET = "Offset";

	/**
	 * Key for the last chapter read.
	 */
	final static String EXTRA_LAST_PAGE = "Last page";

	/**
	 * ID for the notifications generated.
	 */
	private final static int NOTIFICATION_ID = 0;

	/**
	 * A unique Id used to identify this particular service. This is used to start and stop the
	 * service
	 */
	private int mStartId;

	/**
	 * A queue containing the intents of the stories that need to be updated
	 */
	private final BlockingQueue<Intent> mTaskQueue;

	/**
	 * The thread in which the different stories are downloaded, one by one
	 */
	private final Thread mThread = new DownloaderThread();

	/**
	 * Creates a new LibraryDownloader service.
	 */
	public LibraryDownloader() {
		super();
		mTaskQueue = new LinkedBlockingQueue<>();
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

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Save the start id in order to stop the service later on
		mStartId = startId;

		// Add the story to the queue of stories that need to be checked
		mTaskQueue.add(intent);

		// If the downloader thread is inactive, start it
		if (!mThread.isAlive()){
			mThread.start();
		}

		return Service.START_REDELIVER_INTENT;
	}

	/**
	 * A thread that sequentially downloads each story
	 */
	private class DownloaderThread extends Thread{

		@Override
		public void run() {
			final List<String> storiesUpdated = new ArrayList<>();
			boolean hasParsingError = false;
			boolean hasConnectionError = false;
			boolean hasSdError = false;

			// Used to keep track of how many stories have been updated
			int currentProgress = 0;

			// Process each update request one by one
			for (; !mTaskQueue.isEmpty(); currentProgress++) {

				// The total number of stories in this update sequence. This includes both queued and
				// already checked (regardless of whether they were modified) stories.
				final int totalNumberOfStories = currentProgress + mTaskQueue.size();

				// Display the updating notification. If there is more than one story in total the
				// notification should update the progress bar.
				if (totalNumberOfStories > 0) {
					showCheckingNotification(currentProgress, totalNumberOfStories);
				} else {
					showUpdateNotification();
				}

				// Get the story uri
				final Intent i = mTaskQueue.remove();
				final DownloaderFactory.Downloader downloader = DownloaderFactory.getInstance(i, LibraryDownloader.this);

				// Get the story details
				try {
					downloader.getStoryState();
				} catch (IOException e) {
					hasConnectionError = true;    // If a connection error occurs, cancel the whole operation
					break;
				} catch (StoryNotFoundException e) {
					continue;                    // Disregard missing stories
				} catch (ParseException e) {
					// Parsing errors should be logged
					Crashlytics.logException(e);
					hasParsingError = true;    // If a parsing error occurs, go to the next story
					continue;
				}

				// If an update is required, begin the process
				if (downloader.isUpdateNeeded()) {
					final String storyTitle = downloader.getStoryTitle();

					// Download each chapter, updating the notification as required
					try {
						while (downloader.hasNextChapter()) {
							showUpdateNotification(storyTitle, downloader.getCurrentChapter(), downloader.getTotalChapters());
							downloader.downloadChapter();
						}
					} catch (StoryNotFoundException e) {
						// Disregard missing stories
						continue;
					} catch (IOException e) {
						// If a connection error occurs, cancel the whole operation
						hasConnectionError = true;
						break;
					} catch (ParseException e) {
						// If a parsing error occurs, go to the next story
						// Parsing errors should be logged
						Crashlytics.logException(e);
						hasParsingError = true;
						continue;
					}

					// Update the files and the sql database.
					try {
						downloader.saveStory();

						// Upon success, add the title of the story to the list so that it is displayed
						// in the complete notification
						storiesUpdated.add(storyTitle);
					} catch (IOException e) {
						hasSdError = true;
					}
				} else {
					try {
						downloader.saveStory();
					} catch (IOException e) {
						// This shouldn't happen. Log the exception if it occurs
						Crashlytics.logException(new IOException("Exception while saving sql parameters", e));
					}
				}
			}

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
			} else if (hasSdError) {
				showErrorNotification(R.string.error_sd);
			} else {
				// The story did not require any updates; no changes were made.
				removeNotification();
			}

			// Stop the service
			stopSelf(mStartId);
		}

		/**
		 * Removes the notification from the screen
		 */
		private void removeNotification() {
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.cancel(NOTIFICATION_ID);
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
			builder.setContentText(String.format(Locale.US, "%.2f", percent) + "%");
			builder.setProgress(totalStories, currentStory, currentStory == totalStories);
			builder.setSmallIcon(android.R.drawable.stat_sys_download);
			builder.setAutoCancel(false);

			// Set an empty Pending Intent on the notification
			PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setContentIntent(pendingIntent);

			// Show or update the notification
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.notify(NOTIFICATION_ID, builder.build());
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
			manager.notify(NOTIFICATION_ID, builder.build());
		}

		/**
		 * When chapters for a specific story are being downloaded, shows the story title and page
		 * number
		 *
		 * @param storyTitle  The story's title
		 * @param currentPage The chapter being downloaded
		 * @param TotalPage   The total number of chapters
		 */
		private void showUpdateNotification(String storyTitle, int currentPage, int TotalPage) {
			// Create the notification
			NotificationCompat.Builder builder = new NotificationCompat.Builder(LibraryDownloader.this);
			builder.setContentTitle(getString(R.string.downloader_downloading));
			builder.setContentText(getString(R.string.downloader_context, storyTitle, currentPage, TotalPage));
			builder.setSmallIcon(android.R.drawable.stat_sys_download);
			builder.setAutoCancel(false);

			// Set an empty Pending Intent on the notification
			PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setContentIntent(pendingIntent);

			// Show or update the notification
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.notify(NOTIFICATION_ID, builder.build());
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
			builder.setSmallIcon(R.drawable.ic_action_cancel);
			builder.setAutoCancel(true);

			// Set an empty intent
			PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setContentIntent(pendingIntent);

			// Show or update the notification
			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			manager.notify(NOTIFICATION_ID, builder.build());
		}

		/**
		 * Shows the notification at the end of an update cycle.
		 * @param storyTitles The titles of the updated stories
		 */
		private void showUpdateCompleteNotification(List<String> storyTitles) {
			// The title of the notification contains the total number of stories updated
			final String title = getResources().getQuantityString(R.plurals.downloader_notification,
																  storyTitles.size(), storyTitles.size());

			// The content of the notification contains the comma separated list of the titles of the stories updated
			final String text = TextUtils.join(", ", storyTitles);

			// Create the notification
			final NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(LibraryDownloader.this);
			notBuilder.setContentTitle(title);
			notBuilder.setSmallIcon(R.drawable.ic_action_accept);
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
			manager.notify(NOTIFICATION_ID, notBuilder.build());
		}
	}
}
