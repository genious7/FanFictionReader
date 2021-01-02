package com.spicymango.fanfictionreader.dialogs.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.slezica.tools.async.ManagedAsyncTask;
import com.slezica.tools.async.TaskManagerFragment;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.util.FileHandler;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * A dialog that backs up all the application data files into a zip file.
 * <p>
 *     If necessary, the dialog will request for the WRITE_EXTERNAL_STORAGE permission.
 * </p>
 */
public class BackUpDialog extends DialogFragment {
	private static final String STATE_REQUEST_PERMISSION = "STATE_Request_permission";

	/** The backup file path. For the moment, the file must be in the root*/
	public static final String FILENAME = "FanFiction_backup.bak";

	/** The progress bar in the back up dialog*/
	private ProgressBar mBar;

	/**
	 * True if the app should request for the permission, false if it has already been requested.
	 */
	private boolean requestPermission;

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Once the backup process starts, it cannot be interrupted.
		setCancelable(false);

		// Add a progress bar to the dialog
		mBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleHorizontal);
		mBar.setId(android.R.id.progress);

		// Create the dialog
		final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.diag_back_up);
		builder.setView(mBar);

		if (isSdCardWritable(getActivity())) {
			builder.setMessage(R.string.diag_back_up_external);
		} else {
			builder.setMessage(R.string.diag_back_up_internal);
		}

		requestPermission = savedInstanceState == null || savedInstanceState.getBoolean(STATE_REQUEST_PERMISSION, true);

		return builder.create();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(STATE_REQUEST_PERMISSION, requestPermission);
		super.onSaveInstanceState(outState);
	}

	private static boolean isSdCardWritable(Context context){

		if (FileHandler.isExternalStorageWritable(context)) {
			// User has an sd card with write permissions
			int currentApiVersion = android.os.Build.VERSION.SDK_INT;
			// Lollipop does not allow for sd card access without using the ACTION_OPEN_DOCUMENT_TREE,
			// even though the sd card permission is set.
			return currentApiVersion < android.os.Build.VERSION_CODES.LOLLIPOP;
		} else {
			return false;
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// In order to save the backup, the storage permission is required.
		final int storagePermissionState = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
		boolean hasStoragePermission = storagePermissionState == PackageManager.PERMISSION_GRANTED;

		if (hasStoragePermission) {
			// If the storage permission is available, start the backup task
			startBackUpTask();
		} else if (requestPermission){
			// If the storage permission is not available and the app has not requested it
			// in the current session, request for the permission
			requestPermission = false;
			requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
							   0);
		} else{
			// If the storage permission is not available and the application already requested for
			// the permission, the user must have denied it. Dismiss the dialog with an error
			// message.
			dismiss();
			Toast.makeText(getContext(),R.string.error_permission_denied, Toast.LENGTH_SHORT).show();
		}
	}

	private void startBackUpTask(){
		// Start the managed async task if it has not been started already.
		Fragment manager = getFragmentManager().findFragmentByTag(TaskManagerFragment.DEFAULT_TAG);
		if (manager == null) {
			new BackUpTask(getActivity()).execute((Void)null);
		}
	}

	/**
	 * A ManagedAsyncTask that will perform a backup.
	 */
	private final static class BackUpTask extends ManagedAsyncTask<Void, Integer, Integer>{
		private int mTotalFiles = 0;
		private int mZippedFiles = 0;

		private final File app_internal[], output;

		private final ArrayList<File> appFiles;

		BackUpTask(FragmentActivity activity) {
			super(activity);
			String s = activity.getApplicationInfo().dataDir;
			app_internal = new File(s).listFiles(new FilesDirFilter());

			appFiles = new ArrayList<>(3);

			// Get the path of all the app files in both the sd card, the emulated memory, and the
			// internal memory
			appFiles.add(activity.getFilesDir());

			if (FileHandler.isExternalStorageWritable(activity)) {
				appFiles.add(FileHandler.getExternalFilesDir(activity));
			}

			if (FileHandler.isEmulatedFilesDirWritable()) {
				final File emulatedDir = FileHandler.getEmulatedFilesDir(activity);
				if (emulatedDir != null)
					appFiles.add(emulatedDir);
			}


			// Get the destination path
			if (isSdCardWritable(activity)) {
				// Only true if there is an sd card and android version is less than 5.0
				output = new File(FileHandler.getExternalStorageDirectory(activity), FILENAME);
			} else if (FileHandler.isEmulatedFilesDirWritable()) {
				output = new File(Environment.getExternalStorageDirectory(), FILENAME);
			} else {
				output = null;
				cancel(true);
			}
		}

		@Override
		protected Integer doInBackground(Void... params) {
			int result = R.string.toast_back_up;

			//Count all non-story files
			for (File f : app_internal) {
				mTotalFiles += countFiles(f);
			}

			//Count all story files
			for (File f : appFiles) {
				mTotalFiles += countFiles(f);
			}

			// Set the maximum possible progress in the progress bar
			publishProgress(mZippedFiles);

			final FileOutputStream fos;
			ZipOutputStream zos = null;

			byte[] buffer = new byte[1024];

			try {
				fos = new FileOutputStream(output);
				zos = new ZipOutputStream(fos);

				// Zip all files
				for (File f : app_internal) {
					zipDir(zos, f, buffer, f.getName());
				}

				for (File f : appFiles) {
					zipDir(zos, f, buffer, f.getName());
				}

			} catch (IOException e) {
				FirebaseCrashlytics.getInstance().recordException(e);
				result = R.string.error_unknown;
			} finally {
				// Note that ZipOutputStream closes the underlying FileOutputStream
				try {
					if (zos != null)
						zos.close();
				} catch (IOException e) {
					FirebaseCrashlytics.getInstance().recordException(e);
					result = R.string.error_unknown;
				}
			}
			return result;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			final FragmentManager manager = getActivity().getSupportFragmentManager();
			BackUpDialog dialog = (BackUpDialog) manager.findFragmentByTag(BackUpDialog.class.getName());

			// On the first progress update, set the progress bar maximum
			if (values[0] == 0) {
				dialog.mBar.setMax(mTotalFiles);
			}

			dialog.mBar.setProgress(values[0]);
		}

		@Override
		protected void onPostExecute(Integer result) {
			Toast toast = Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT);
			toast.show();

			// Fix for android issue 195362, in which files do not show in the MTP file explorer
			// until a device reboot occurs.
			// See https://code.google.com/p/android/issues/detail?id=195362
			MediaScannerConnection.scanFile(getActivity(), new String[]{output.getAbsolutePath()}, null, null);

			FragmentManager manager = getActivity().getSupportFragmentManager();

			DialogFragment dialog = (DialogFragment) manager
					.findFragmentByTag(BackUpDialog.class.getName());

			dialog.dismiss();

			manager.beginTransaction()
					.remove(manager
							.findFragmentByTag(TaskManagerFragment.DEFAULT_TAG))
					.commit();

		}

		/**
		 * Zips all the files and folders present in the supplied directory
		 * @param zos The zipOutputStream
		 * @param dir The parent directory
		 * @param buffer A buffer for the zipping process
		 * @param parent The name of the parent path
		 * @throws IOException If an error occurs while writing the backup file
		 */
		private void zipDir(ZipOutputStream zos, File dir, byte[] buffer, String parent) throws IOException{

			// If the file is not a directory, do not try to zip it.
			if (!dir.isDirectory()) {
				return;
			}

			// In theory, files[] shouldn't be null since the check above should show that dir is a
			// directory. However, some phones (MYPHONE, MID, and ZTE) will return null on listFiles,
			// hence the check.
			final File[] files = dir.listFiles();
			if (files == null) return;

			for (File file : files) {
				if (file.isDirectory()) {
					// Recursively zip directories
					zipDir(zos, file, buffer, parent + '/' + file.getName());
				} else {
					try (FileInputStream in = new FileInputStream(file)) {
						final ZipEntry entry = new ZipEntry(parent + '/' + file.getName());
						zos.putNextEntry(entry);

						// Zip the individual file
						int length;
						while ((length = in.read(buffer)) > 0) {
							zos.write(buffer, 0, length);
						}
						zos.closeEntry();

						// Update the progress bar
						mZippedFiles++;
						publishProgress(mZippedFiles);
					} catch (IOException e) {
						throw new IOException(e.getMessage());
					}
				}
			}
		}

		/**
		 * Counts how many files are contained in a folder
		 * @param folder The parent folder
		 * @return The total number of files inside the folder
		 */
		private static int countFiles(File folder){
			int count = 0;
			File[] files = folder.listFiles();

			if (files == null) return 0;

			for (File file : files) {
				if (file.isDirectory()) {
					count += countFiles(file);
				}else{
					count++;
				}
			}
			return count;
		}

		/**
		 * A simple file filter that separates saved files from the database and
		 * the settings.
		 *
		 * @author Michael Chen
		 */
		private final static class FilesDirFilter implements FilenameFilter{
			@Override
			public boolean accept(File dir, String filename) {
				return !filename.equalsIgnoreCase("Files");
			}
		}
	}
}
