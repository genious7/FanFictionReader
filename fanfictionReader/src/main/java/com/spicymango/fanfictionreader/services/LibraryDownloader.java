package com.spicymango.fanfictionreader.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.activity.LibraryMenuActivity;
import com.spicymango.fanfictionreader.util.Result;

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
				// already checked stories.
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
				DownloaderFactory.Downloader downloader = DownloaderFactory.getInstance(i, LibraryDownloader.this);

				// Get the story details
				Result detailResult = downloader.getStoryState();

				if (detailResult == Result.ERROR_PARSE){
					hasParsingError = true;	// If a parsing error occurs, go to the next story
					continue;
				} else if (detailResult == Result.ERROR_CONNECTION){
					hasConnectionError = true;	// If a connection error occurs, cancel the whole operation
					break;
				}

				// If an update is required, begin the process
				if (downloader.isUpdateNeeded()){
					final String storyTitle = downloader.getStoryTitle();

					// Download each chapter, updating the notification as required
					while (downloader.hasNextChapter()){
						showUpdateNotification(storyTitle, downloader.currentChapter(), downloader.totalChapters());

						detailResult = downloader.downloadChapter();

						// Exit the inner while loop on any error
						if (detailResult != Result.SUCCESS){
							break;
						}
					}

					if (detailResult == Result.ERROR_PARSE){
						hasParsingError = true;	// If a parsing error occurs, go to the next story
						continue;
					} else if (detailResult == Result.ERROR_CONNECTION){
						hasConnectionError = true;	// If a connection error occurs, cancel the whole operation
						break;
					}

					// Upon success, commit save the results to the disk and add the story to the
					// list of successful updates.
					downloader.saveStory();
					storiesUpdated.add(storyTitle);
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
