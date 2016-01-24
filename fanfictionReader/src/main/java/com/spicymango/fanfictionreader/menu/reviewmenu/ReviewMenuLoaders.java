package com.spicymango.fanfictionreader.menu.reviewmenu;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.spicymango.fanfictionreader.menu.BaseLoader;
import com.spicymango.fanfictionreader.util.Sites;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Michael Chen on 01/17/2016.
 */
final class ReviewMenuLoaders {

	interface TitleLoader{
		@Nullable
		public String getTitle();
	}

	static final class FanFictionReviewLoader extends BaseLoader<ReviewMenuItem> implements  TitleLoader{
		private static final String STATE_ID = "Story ID";
		private static final String STATE_CHAPTER = "Story Chapter";
		private static final String STATE_TITLE = "Story Title";

		/**
		 * The id of the story whose reviews are being displayed
		 */
		private final long mStoryId;

		/**
		 * The chapter whose reviews are being displayed. If no specific chapter is selected, the
		 * field must be set to zero
		 */
		private int mChapter;

		/**
		 * The title of the story
		 */
		private String mTitle;

		private static final Pattern URI_PATTERN = Pattern.compile("/r/(\\d++)/(?:(\\d++)/(\\d++)/)?");

		public FanFictionReviewLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);

			if (savedInstanceState == null){
				// Set the default title to null - this is displayed while loading.
				mTitle = null;

				// Get the chapter and story id from the uri
				Matcher matcher = URI_PATTERN.matcher(uri.toString());
				if (matcher.find()){
					mStoryId = Long.parseLong(matcher.group(1));

					// If there is no chapter id, use 0 as default (all chapters)
					if (matcher.group(2) == null){
						mChapter = 0;
					} else{
						mChapter = Integer.parseInt(matcher.group(2));
					}
				} else{
					throw new IllegalArgumentException("The Uri " + uri + "is not valid");
				}
			} else{
				mStoryId = savedInstanceState.getLong(STATE_ID);
				mChapter = savedInstanceState.getInt(STATE_CHAPTER);
				mTitle = savedInstanceState.getString(STATE_TITLE);
			}
		}

		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			savedInstanceState.putLong(STATE_ID, mStoryId);
			savedInstanceState.putInt(STATE_CHAPTER, mChapter);
			savedInstanceState.putString(STATE_TITLE, mTitle);
		}

		@Override
		protected int getTotalPages(Document document) {
			Elements pageLinks = document.select("div#content div#d_menu a");
			Element lastLink = pageLinks.select("a:contains(last)").last();
			Element nextLink = pageLinks.select("a:contains(next)").last();

			int maxPage;

			if (lastLink != null) {
				// The last link exists. Get the page number from there
				Uri link = Uri.parse(lastLink.absUrl("href"));
				String pageNumber = link.getLastPathSegment();
				maxPage = Integer.valueOf(pageNumber);
			} else if (nextLink != null) {
				// The "next" link exists. Get the page number from there
				Uri link = Uri.parse(nextLink.absUrl("href"));
				String pageNumber = link.getLastPathSegment();
				maxPage = Integer.valueOf(pageNumber);
			} else {
				// The current page is the last page
				maxPage = getCurrentPage();
			}

			return maxPage;
		}

		@Nullable
		@Override
		protected Uri getUri(int currentPage) {
			Uri.Builder builder = Sites.FANFICTION.BASE_URI.buildUpon();
			builder.appendEncodedPath("r");
			builder.appendEncodedPath(Long.toString(mStoryId));
			builder.appendEncodedPath(Integer.toString(mChapter));
			builder.appendEncodedPath(Integer.toString(currentPage));
			builder.appendEncodedPath("");
			return builder.build();
		}

		@Override
		protected boolean load(Document document, List<ReviewMenuItem> list) {
			// Load the story's title
			Element title = document.select("div#content center a").first();
			if (title == null) mTitle = null;
			else mTitle = title.text();

			// Load the reviews
			Elements reviews = document.select("div#content div.bs.brb");
			for (Element review : reviews) {
				final List<TextNode> nodes = review.textNodes();

				// Check that there is at least an author name and one line of review. Note that
				// the program has to account for the possibility of anonymous reviewers.
				final Element author = review.select("a").last();
				final String authorTxt;
				int i;
				if (author == null && nodes.size() > 1){
					// When the reviewer is anonymous, the first line is the author; the review
					// starts on the second line
					authorTxt = nodes.get(0).text();
					i = 1;
				} else if (!nodes.isEmpty()){
					// If the author is known, the review starts directly on the first line
					authorTxt = author.text();
					i = 0;
				}else{
					// Error
					return false;
				}

				// Get the text, adding newlines at <br>.
				final StringBuilder reviewText = new StringBuilder();
				for (; i < nodes.size(); i++) {
					if (reviewText.length() != 0)
						reviewText.append('\n');
					reviewText.append(nodes.get(i).text().trim());
				}

				// Get the chapter number
				final Element data = review.select("span.gray").first();
				if (data == null) return false;
				String chapter = data.ownText();
				int chapterNumber = Integer.parseInt(chapter.replaceAll("\\D", ""));

				// Get the date
				final Element dateElement = data.getElementsByAttribute("data-xutime").first();
				if (dateElement == null) return false;
				Date date = new Date(Long.parseLong(dateElement.attr("data-xutime")) * 1000);

				// Put everything together
				final ReviewMenuItem item = new ReviewMenuItem(authorTxt, reviewText.toString(), date, chapterNumber);
				list.add(item);
			}

			return true;
		}

		@Nullable
		@Override
		public String getTitle() {
			return mTitle;
		}
	}

}
