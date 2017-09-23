package com.spicymango.fanfictionreader.menu;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * A fragment used to store the data obtained from the server during state changes. This is used
 * instead of the savedInstanceState bundle in order to prevent an
 * android.os.android.os.TransactionTooLargeException on android Nougat.
 * </p>
 * Created by Michael Chen on 15/09/2017.
 */

public final class DataFragment extends Fragment {
	private final static String KEY_DATA = "KEY_DATA";
	private Bundle mSavedData;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// In order to retain data, the fragment must not be recreated on instance changes.
		// SetRetainInstance not available on api 11, which is why we'll resort to using
		// savedInstanceState in there anyways. In api 11, large bundles fail silently
		// so it isn't a problem.
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setRetainInstance(true);
		} else if (savedInstanceState != null) {
			// If api is less than 11, the fragment is not saved but the os wont throw a
			// TransactionTooLargeException.
			mSavedData = savedInstanceState.getBundle(KEY_DATA);
		}
	}

	public void saveData(Bundle outState) {
		mSavedData = outState;
	}

	public Bundle getData() {
		return mSavedData;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Save the bundle on api less than 11.
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			outState.putBundle(KEY_DATA, mSavedData);
		}
	}

}
