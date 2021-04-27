/**
 * 
 */
package com.spicymango.fanfictionreader.util;

import com.spicymango.fanfictionreader.R;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * Contains the base uri for the different sites
 * 
 * @author Michael Chen
 */
public enum Sites {
	// Note to self: do not rename enum names. They are used as keys elsewhere
	ARCHIVE_OF_OUR_OWN("http", "archiveofourown.org", R.string.site_archive),
	FANFICTION("https", "m.fanfiction.net", "www.fanfiction.net", R.string.site_fanfiction), 
	FICTIONPRESS("https", "m.fictionpress.com","www.fictionpress.com", R.string.site_fictionpress);

	/**
	 * The base uri for the mobile version of the web site
	 */
	public final Uri BASE_URI;
	/**
	 * The base {@link Uri} for the desktop version of the web site
	 */
	public final Uri DESKTOP_URI;
	/**
	 * The authority for the mobile version of the web site
	 */
	public final String AUTHORITY;
	/**
	 * The authority for the desktop version of the web site
	 */
	public final String AUTHORITY_DESKTOP;
	/**
	 * The site's user friendly name
	 */
	public final int TITLE;

	Sites(@NonNull final String scheme, @NonNull final String authority, @StringRes final int name) {
		this(scheme, authority, null, name);
	}

	/**
	 * Creates a new site instance
	 * 
	 * @param scheme
	 *            The site's scheme
	 * @param authority
	 *            The mobile site authority
	 * @param desktopAuthority
	 *            The desktop site authority
	 * @param name
	 *            The site's user friendly name
	 */
	Sites(@NonNull final String scheme, @NonNull final String authority,
		  @Nullable final String desktopAuthority, @StringRes final int name) {

		// Generates the base Uri
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(scheme);
		builder.authority(authority);
		BASE_URI = builder.build();

		// Sets the authority
		AUTHORITY = authority;

		if (desktopAuthority == null) {
			DESKTOP_URI = null;
			AUTHORITY_DESKTOP = null;
		} else {
			builder = new Uri.Builder();
			builder.scheme(scheme);
			builder.authority(desktopAuthority);
			DESKTOP_URI = builder.build();
			AUTHORITY_DESKTOP = desktopAuthority;
		}

		// Fetches the id's for each web site's name
		TITLE = name;
	}
}
