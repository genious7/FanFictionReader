/**
 * 
 */
package com.spicymango.fanfictionreader.activity;

import android.net.Uri;

import com.spicymango.fanfictionreader.provider.StoryProvider;

/**
 * @author Michael Chen
 *
 */
@Deprecated
public enum Site {
	FANFICTION ("m.fanfiction.net","www.fanfiction.net", StoryProvider.FF_CONTENT_URI),
	FICTIONPRESS("m.fictionpress.com","www.fictionpress.com", StoryProvider.FP_CONTENT_URI),
	ARCHIVE_OF_OUR_OWN("archiveofourown.org", Uri.EMPTY);
	
	public final String authorityMobile;
	public final String authorityRegular;
	public final Uri content_uri;
	public static final String scheme = "https";
	
	private Site(String authorityMobile, String authorityRegular, Uri contentUri) {
		this.authorityMobile = authorityMobile;
		this.authorityRegular = authorityRegular;
		this.content_uri = contentUri;
	}

	private Site(String authorityMobile, Uri contentUri){
		this(authorityMobile, authorityMobile, contentUri);
	}
	
	/**
	 * Obtains the site with the corresponding authority
	 * @param string The authority of the site
	 * @return The site, or null if no matches are found
	 */
	public static Site fromAuthority(String string){
		for (Site site : Site.values()) {
			if (string.equals(site.authorityMobile) || string.equals(site.authorityRegular)) {
				return site;
			}
		}
		return null;
	}
	
}
