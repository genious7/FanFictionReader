package com.spicymango.fanfictionreader.services;

import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;

import com.spicymango.fanfictionreader.util.Result;
import com.spicymango.fanfictionreader.util.Sites;

/**
 * Created by Michael Chen on 02/15/2016.
 */
public class DownloaderFactory {
	private static final int FAN_FICTION = 0;
	private static final int FICTION_PRESS = 1;
	private static final int AO3 = 2;

	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

	// Initialize the URI Matcher
	static{
		// FanFiction
		URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "s/#/#/", FAN_FICTION);
		URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "s/#/#/*", FAN_FICTION);
		URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "s/#/#/", FAN_FICTION);
		URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "s/#/#/*", FAN_FICTION);

		// FictionPress
		URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "s/#/#/", FICTION_PRESS);
		URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "s/#/#/*", FICTION_PRESS);
		URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "s/#/#/", FICTION_PRESS);
		URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "s/#/#/*", FICTION_PRESS);

		// Archive of Our Own
		URI_MATCHER.addURI(Sites.ARCHIVE_OF_OUR_OWN.AUTHORITY, "works/#/", AO3);
		URI_MATCHER.addURI(Sites.ARCHIVE_OF_OUR_OWN.AUTHORITY, "works/#/chapters/#", AO3);
	}

	public static Downloader getInstance(Intent intent, Context context) {
		switch (URI_MATCHER.match(intent.getData())) {
			case FAN_FICTION:
				return new FanFictionDownloader(intent, context);
			default:
				throw new IllegalArgumentException("Downloader Factory: Invalid Uri: " + intent.getData());
		}
	}

	public interface Downloader{

		boolean isUpdateNeeded();

		boolean hasNextChapter();

		Result getStoryState();

		int totalChapters();

		int currentChapter();

		String getStoryTitle();

		Result downloadChapter();

		Result saveStory();
	}

	public static final class FanFictionDownloader implements Downloader{

		public FanFictionDownloader(Intent intent, Context context) {

		}

		@Override
		public boolean isUpdateNeeded() {
			return false;
		}

		@Override
		public boolean hasNextChapter() {
			return false;
		}

		@Override
		public Result getStoryState() {
			return null;
		}

		@Override
		public int totalChapters() {
			return 0;
		}

		@Override
		public int currentChapter() {
			return 0;
		}

		@Override
		public String getStoryTitle() {
			return null;
		}

		@Override
		public Result downloadChapter() {
			return null;
		}

		@Override
		public Result saveStory() {
			return null;
		}
	}
}
