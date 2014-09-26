package com.spicymango.fanfictionreader.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import com.spicymango.fanfictionreader.Settings;

public class FileHandler {
	
	
	/**
	 * 
	 * @param context
	 * @param storyId
	 * @param currentPage
	 * @param html
	 * @return True if the operation succeeded, false otherwise
	 */
	public static boolean writeFile(Context context, long storyId, int currentPage, String html){
		try {
			File file;
			
			if (Settings.shouldWriteToSD(context)) {
				if (!isExternalStorageWritable()) {
					return false;
				}	
				file = new File(getExternalFilesDir(context), storyId + "_" + currentPage + ".htm");
			} else {
				file = new File(context.getFilesDir(), storyId + "_" + currentPage + ".htm");
			}
			
			if (!file.exists()) {
				deleteFile(context, storyId, currentPage);
			}
			
			FileOutputStream fos = new FileOutputStream( file);
			fos.write(html.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public static Spanned getFile(Context context, long storyId, int currentPage){
		try {
			
			//Internal Memory
			String filename = storyId + "_" + currentPage + ".htm";
			File file = new File(context.getFilesDir(), filename);
			if (file.exists()) {
				BufferedInputStream fin = new BufferedInputStream(
						new FileInputStream(file));
				byte[] buffer = new byte[(int) file.length()];
				fin.read(buffer);
				fin.close();
				return Html.fromHtml(new String(buffer));
			}
			
			//External Memory
			if (isExternalStorageWritable()) {
				file = new File(getExternalFilesDir(context), filename);
				if (file.exists()) {
					BufferedInputStream fin = new BufferedInputStream(
							new FileInputStream(file));
					byte[] buffer = new byte[(int) file.length()];
					fin.read(buffer);
					fin.close();
					return Html.fromHtml(new String(buffer));
				}
			}
			
			//Deprecated format, internal only
			filename = storyId + "_" + currentPage + ".txt";
			file = new File(context.getFilesDir(), filename);
			if (file.exists()) {
				BufferedInputStream fin = new BufferedInputStream(
						new FileInputStream(file));
				byte[] buffer = new byte[(int) file.length()];
				fin.read(buffer);
				fin.close();
				return new SpannedString(new String(buffer));
			}
			return null;
			
		} catch (IOException e) {
			return null;
		}
	}
	
	public static String getRawFile(Context context, long storyId, int currentPage){
		try {
			
			//Internal Memory
			String filename = storyId + "_" + currentPage + ".htm";
			File file = new File(context.getFilesDir(), filename);
			if (file.exists()) {
				BufferedInputStream fin = new BufferedInputStream(
						new FileInputStream(file));
				byte[] buffer = new byte[(int) file.length()];
				fin.read(buffer);
				fin.close();
				return new String(buffer);
			}
			
			//External Memory
			if (isExternalStorageWritable()) {
				file = new File(getExternalFilesDir(context), filename);
				if (file.exists()) {
					BufferedInputStream fin = new BufferedInputStream(
							new FileInputStream(file));
					byte[] buffer = new byte[(int) file.length()];
					fin.read(buffer);
					fin.close();
					return new String(buffer);
				}
			}
			
			//Deprecated format, internal only
			filename = storyId + "_" + currentPage + ".txt";
			file = new File(context.getFilesDir(), filename);
			if (file.exists()) {
				BufferedInputStream fin = new BufferedInputStream(
						new FileInputStream(file));
				byte[] buffer = new byte[(int) file.length()];
				fin.read(buffer);
				fin.close();
				return new String(buffer);
			}
			return null;
			
		} catch (IOException e) {
			return null;
		}
	}
	
	public static void deleteFile(Context context, long storyId, int currentPage){
		
		File file = new File(context.getFilesDir(), storyId + "_" + currentPage + ".htm");
		if (file.exists()) {
			file.delete();
			return;
		}
		
		file = new File(context.getFilesDir(), storyId + "_" + currentPage + ".txt");
		if (file.exists()) {
			file.delete();
			return;
		}
		
		if (isExternalStorageWritable()) {
			file = new File(getExternalFilesDir(context), storyId + "_" + currentPage + ".htm");
			if (file.exists()) {
				file.delete();
				return;
			}
		}
	}
	
	/**
	 * Gets the path of the SD card
	 * @param context The current context
	 * @return A file corresponding to the external storage directory.
	 */
	@TargetApi(Build.VERSION_CODES.FROYO)
	public static File getExternalFilesDir(Context context) {
		File sd;
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.FROYO) {
			sd = context.getExternalFilesDir(null);
		} else {
			String packageName = context.getPackageName();
			sd = new File(Environment.getExternalStorageDirectory()
					.getAbsolutePath()
					+ "/Android/data/"
					+ packageName
					+ "/files");
		}
		if (sd != null && !sd.exists()) {
			sd.mkdirs();
		}
		return sd;
	}

	/**
	 * Checks whether the external storage is writable.
	 * @return True if the external storage is available
	 */
	public static boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
}
