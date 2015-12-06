package com.spicymango.fanfictionreader.dialogs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.crashlytics.android.Crashlytics;
import com.slezica.tools.async.ManagedAsyncTask;
import com.slezica.tools.async.TaskManagerFragment;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.util.FileHandler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.ProgressBar;
import android.widget.Toast;

public class BackUpDialog extends DialogFragment {
	public static final String filename = "FanFiction_backup.bak";
	private ProgressBar bar;
	
	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		setCancelable(false);
		
		bar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleHorizontal);
		bar.setId(android.R.id.progress);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.diag_back_up);
		builder.setView(bar);
		
		if (FileHandler.isExternalStorageWritable(getActivity())) {
			builder.setMessage(R.string.diag_back_up_external);
		} else{
			builder.setMessage(R.string.diag_back_up_internal);
		}
		
		return builder.create();
	}
		
	@Override
	public void onStart() {
		super.onStart();
		
		Fragment manager = getFragmentManager().findFragmentByTag(TaskManagerFragment.DEFAULT_TAG);
		if (manager == null) {
			new BackUpTask(getActivity()).execute((Void)null);
		}
	}
	
	private final static class BackUpTask extends ManagedAsyncTask<Void, Integer, Integer>{
		private Integer[] progress = {0,0};
		private final File app_internal[], output;
		private final ArrayList<File> appFiles;
		
		public BackUpTask(FragmentActivity activity) {
			super(activity);
			String s = activity.getApplicationInfo().dataDir;
			app_internal = new File(s).listFiles(new FilesDirFilter());
			
			appFiles = new ArrayList<>(3);
			appFiles.add(activity.getFilesDir());
			
			if (FileHandler.isExternalStorageWritable(activity)) {
				appFiles.add(FileHandler.getExternalFilesDir(activity));
			}
			
			if (FileHandler.isEmulatedFilesDirWriteable()) {
				appFiles.add(FileHandler.getEmulatedFilesDir(activity));
			}
			
			if (FileHandler.isExternalStorageWritable(activity)) {
				output = new File(FileHandler.getExternalStorageDirectory(activity),filename);
			}else if(FileHandler.isEmulatedFilesDirWriteable()){
				output = new File(Environment.getExternalStorageDirectory(),filename);
			}else{
				output = null;
				cancel(true);
			}
		}

		@Override
		protected Integer doInBackground(Void... params) {
			int result = R.string.toast_back_up;
			
			FileOutputStream fos = null;
			ZipOutputStream zos = null;
			byte[] buffer = new byte[1024];

			//Count all non-story files
			for (File f : app_internal) {
				progress[1] += countFiles(f);
			}
			
			//Count all story files
			for (File f : appFiles) {
				progress[1] += countFiles(f);
			}
			
			publishProgress(progress);
			
			try {
				fos = new FileOutputStream(output);
				zos = new ZipOutputStream(fos);
				
				for (File f : app_internal) {
					zipDir(zos, f, buffer, f.getName());
				}
				
				for (File f : appFiles) {
					zipDir(zos, f, buffer, f.getName());
				}
				
			} catch (IOException e) {
				Crashlytics.logException(e);
				result = R.string.error_unknown;
			} finally {
				try {
					if (zos != null)
						zos.close();
				} catch (IOException e) {
					Crashlytics.logException(e);
					result = R.string.error_unknown;
				}
				
				try {
					if (fos != null)
						fos.close();
				} catch (IOException e2) {
					Crashlytics.logException(e2);
					result = R.string.error_unknown;
				}
				
			}
			return result;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			
			BackUpDialog diag = (BackUpDialog) getActivity()
					.getSupportFragmentManager().findFragmentByTag(
							BackUpDialog.class.getName());
			
			if (values[0] == 0) {
				diag.bar.setMax(values[1]);
			}
			diag.bar.setProgress(values[0]);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			Toast toast = Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT);
			toast.show();
			
			FragmentManager manager = getActivity().getSupportFragmentManager();

			DialogFragment diag = (DialogFragment) manager
					.findFragmentByTag(BackUpDialog.class.getName());
			
			diag.dismiss();
			
			manager.beginTransaction()
					.remove(manager
							.findFragmentByTag(TaskManagerFragment.DEFAULT_TAG))
					.commit();
			
		}
		
		/**
		 * Zips all the files and folders present in the supplied directory
		 * @param zos The zipOutputStream
		 * @param dir The parent directory
		 * @param buffer A buffer for the zipping proccess
		 * @param parent The name of the parent path
		 * @throws IOException
		 */
		private void zipDir(ZipOutputStream zos, File dir, byte[] buffer, String parent) throws IOException{
			
			if (!dir.isDirectory()) {
				return;
			}
			
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					zipDir(zos, file, buffer, parent + '/' + file.getName());
					continue;
				}
				FileInputStream in = new FileInputStream(file);
				try{				
					ZipEntry entry = new ZipEntry(parent + '/' + file.getName());
					zos.putNextEntry(entry);
					int length;
					while ((length = in.read(buffer)) > 0) {
						zos.write(buffer, 0, length);
					}
					zos.closeEntry();
					progress[0]++;
					publishProgress(progress);
				} catch (IOException e) {
					throw new IOException(e.getMessage());
				} finally {
					in.close();
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
