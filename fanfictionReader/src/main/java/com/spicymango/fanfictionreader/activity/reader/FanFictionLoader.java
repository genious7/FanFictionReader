package com.spicymango.fanfictionreader.activity.reader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.spicymango.fanfictionreader.activity.Site;
import com.spicymango.fanfictionreader.provider.SqlConstants;
import com.spicymango.fanfictionreader.provider.StoryProvider;
import com.spicymango.fanfictionreader.services.LibraryDownloader;
import com.spicymango.fanfictionreader.util.FileHandler;
import com.spicymango.fanfictionreader.util.JsoupUtil;
import com.spicymango.fanfictionreader.util.Sites;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

class FanFictionLoader extends StoryLoader {

	public FanFictionLoader(Context context, Bundle in, long storyId,
							int currentPage) {
		super(context, in, storyId, currentPage);
	}

	@Override
	protected String getStoryFromFile(long storyId, int currentPage) {
		return FileHandler.getFile(getContext(), storyId, currentPage);
	}

	@Override
	@NonNull
	protected Uri getUri() {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(Site.scheme);
		builder.authority(Site.FANFICTION.authorityMobile);
		builder.appendEncodedPath("s");
		builder.appendEncodedPath(Long.toString(getStoryId()));
		builder.appendEncodedPath(Integer.toString(getCurrentPage()));
		builder.appendEncodedPath("");

		return builder.build();
	}

	@Override
	protected String getStoryFromSite(long storyId, int currentPage,
									  StoryChapter data) throws IOException {
		Document document = JsoupUtil.safeGet(getUri().toString());
		return parseHTML(document.outerHtml(), data);
	}

	@Override
	protected String parseHTML(String html, StoryChapter data) {
		final Document document = Jsoup.parse(html,getUri().toString());

		if (data.getTotalChapters() == 0) {
			Element title = document.select("div#content div b").first();
			if (title == null) return null;
			data.setStoryTitle(title.ownText());

			int totalPages;
			Element link = document.select("body#top div[align=center] > a:matches(^\\d++$)").first();
			if (link != null)
				totalPages = Math.max(
						pageNumberOnline(link.attr("href"), 2),
						getCurrentPage());
			else {
				totalPages = 1;
			}
			data.setTotalChapters(totalPages);

			Element authorElement = document.select("input[name=uid]").first();
			long authorId = Long.parseLong(authorElement.attr("value"));
			data.setAuthorId(authorId);
		}

		Elements storyText = document.select("div#storycontent");
		if (storyText.isEmpty()) return null;
		return storyText.html();
	}

	@Override
	protected void reDownload(long storyId, int currentPage) {
		final Uri.Builder storyUri = Sites.FANFICTION.BASE_URI.buildUpon();
		storyUri.appendPath("s");
		storyUri.appendPath(Long.toString(storyId));
		storyUri.appendPath("");
		LibraryDownloader.integrityCheck(getContext(), storyUri.build(), currentPage, 0);
	}

	/**
	 * Extracts the page number or the story id from a url
	 *
	 * @param url   The string containing the url that needs to be parsed
	 * @param group One for the story id, two for the page number.
	 * @return Either the story id or the page number
	 */
	private int pageNumberOnline(String url, int group) {
		final Pattern currentPageNumber = Pattern
				.compile("(?:/s/(\\d{1,10}+)/)(\\d++)(?:/)");
		Matcher matcher = currentPageNumber.matcher(url);
		if (matcher.find()) {
			return Integer.parseInt(Objects.requireNonNull(matcher.group(group)));
		} else {
			return 1;
		}
	}

	@Override
	protected Cursor getFromDatabase(long storyId) {
		return getContext().getContentResolver().query(StoryProvider.FF_CONTENT_URI, null,
													   SqlConstants.KEY_STORY_ID + " = ?",
													   new String[]{Long.toString(storyId)}, null);
	}
}
