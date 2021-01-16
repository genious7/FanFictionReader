package com.spicymango.fanfictionreader.menu;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A fragment used to store the data obtained from the server during state changes. This is used
 * instead of the savedInstanceState bundle in order to prevent an
 * android.os.android.os.TransactionTooLargeException on android Nougat.
 * <p>
 * Whenever the app is closed, any saved data is written to a cached file in order to recover it
 * upon reopening the app.
 * </p>
 * Created by Michael Chen on 15/09/2017.
 */

public final class DataFragment extends Fragment {
	/**
	 * Any cache files that represent a saved instance state are stored in this folder.
	 */
	public final static String SAVED_INSTANCE_STATE_CACHE_PATH = "Saved Instance State";

	/**
	 * A simple variable to indicate that data has been cached. This is required in order to
	 * determine if a cached file is relevant to the current session.
	 */
	private final static String KEY_FLAG = "KEY_FLAG";

	/** A bundle containing the retained data during orientation changes */
	private Bundle mSavedData;

	/** The cache file*/
	private File mCacheFile;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create the filename. The filename is composed of the class name (to easily identify it)
		// the intent uri hashcode (to distinguish the Category and Sub-Category Menus and the
		// .tmp extension to clearly mark the file as a temporary file.
		final File cacheFolder = new File(requireActivity().getCacheDir(), SAVED_INSTANCE_STATE_CACHE_PATH);
		if (!cacheFolder.exists()) {
			// If the directory does not exist, create it
			cacheFolder.mkdir();
		}

		String filename = requireActivity().getClass().getName();
		final Uri uri = requireActivity().getIntent().getData();
		if (uri != null){
			filename += uri.hashCode();
		}
		filename += ".tmp";
		mCacheFile = new File(cacheFolder, filename);

		// Prevent the fragment from being recreated on configuration changes.
		setRetainInstance(true);

		// If the fragment is being recreated, read the cache file
		if (savedInstanceState != null && savedInstanceState.containsKey(KEY_FLAG)) {
			// Read the cache file
			final Parcel bundledParcel = Parcel.obtain();

			// The cache file may not exist if an error occurred while saving the file
			if (mCacheFile.exists()) {
				try (FileInputStream fin = new FileInputStream(mCacheFile)) {

					byte[] buffer = new byte[(int) fin.getChannel().size()];
					fin.read(buffer, 0, buffer.length);
					fin.close();

					bundledParcel.unmarshall(buffer, 0, buffer.length);
					bundledParcel.setDataPosition(0);
					mSavedData = bundledParcel.readBundle(getClass().getClassLoader());

				} catch (IOException e) {
					FirebaseCrashlytics.getInstance().recordException(e);
				} finally {
					bundledParcel.recycle();
				}
			}
		}
	}

	public void saveData(Bundle outState) {
		mSavedData = outState;
	}

	public Bundle getData() {
		return mSavedData;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		// On android Honeycomb or greater, the fragment will be retained on configuration changes
		// and therefore, the data can be retrieved from the fragment directly. The cache file only
		// needs to be used on Api versions lower than Honeycomb or when a configuration change is
		// not occurring.
		if (!requireActivity().isChangingConfigurations() && mSavedData != null) {

			// Write the cache file
			final Parcel bundledParcel = Parcel.obtain();

			try (FileOutputStream fos = new FileOutputStream(mCacheFile);){
				// Convert the bundle to a parcel and save it on the cache file
				mSavedData.writeToParcel(bundledParcel, 0);
				fos.write(bundledParcel.marshall());

				// Data has been saved. Mark a flag.
				outState.putBoolean(KEY_FLAG, true);

				fos.flush();
			} catch (IOException e) {
				FirebaseCrashlytics.getInstance().recordException(e);
			} finally {
				bundledParcel.recycle();
			}
		}

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPause() {
		super.onPause();

		// If the activity is being closed, delete the cache file
		if (requireActivity().isFinishing()) {
			if (mCacheFile.exists()) {
				mCacheFile.delete();
			}
		}
	}
}
