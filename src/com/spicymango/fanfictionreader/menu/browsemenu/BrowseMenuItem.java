package com.spicymango.fanfictionreader.menu.browsemenu;

import android.net.Uri;


/**
 * Represents a String and its associated destination URI's
 * @author Michael Chen
 */
public class BrowseMenuItem {

	private final Uri mUriNormal;
	private final Uri mUriCrossover;
	private final String mTitle;

	/**
	 * Creates a new menu item entry
	 * @param uriNormal The regular link
	 * @param uriCrossover The cross over link, or null
	 * @param title The title to display
	 */
	public BrowseMenuItem(Uri uriNormal, Uri uriCrossover, String title) {
		super();
		mUriNormal = uriNormal;
		mUriCrossover = uriCrossover;
		mTitle = title;
	}

	public Uri getUriNormal() {
		return mUriNormal;
	}

	public Uri getUriCrossover() {
		return mUriCrossover;
	}

	public String getTitle() {
		return mTitle;
	}
}
