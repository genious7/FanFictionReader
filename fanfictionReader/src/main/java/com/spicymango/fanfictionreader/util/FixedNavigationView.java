package com.spicymango.fanfictionreader.util;

import android.content.Context;
import android.os.Parcelable;
import com.google.android.material.navigation.NavigationView;
import android.util.AttributeSet;

/**
 * Fixes BadParcelableException on Android versions 2.3-3.0
 * See https://code.google.com/p/android/issues/detail?id=196430
 */
public class FixedNavigationView extends NavigationView{
	public FixedNavigationView(Context context) {
		super(context);
	}

	public FixedNavigationView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FixedNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	final protected void onRestoreInstanceState(Parcelable savedState) {
		if (savedState != null) {
			SavedState state = (SavedState) savedState;

			if (state.menuState != null)
				state.menuState.setClassLoader(getContext().getClass().getClassLoader());
		}

		super.onRestoreInstanceState(savedState);
	}
}
