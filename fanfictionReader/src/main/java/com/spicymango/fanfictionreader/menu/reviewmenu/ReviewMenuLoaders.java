package com.spicymango.fanfictionreader.menu.reviewmenu;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.BaseLoader;
import com.spicymango.fanfictionreader.util.Sites;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Michael Chen on 01/17/2016.
 */
final class ReviewMenuLoaders {

	interface TitleLoader{
		@Nullable
		String getTitle();
	}

	static final class FanFictionReviewLoader extends BaseLoader<ReviewMenuItem> implements  TitleLoader, BaseLoader.Filterable{
		private static final String STATE_ID = "Story ID";
		private static final String STATE_CHAPTER = "Story Chapter";
		private static final String STATE_TITLE = "Story Title";
		private static final String STATE_TOTAL_CHAPTERS = "Total Chapters";

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

		/**
		 * The total number of chapters in the story. This is used in the Filter Dialog Creation
		 */
		private int mTotalChapters;

		private static final Pattern URI_PATTERN = Pattern.compile("/r/(\\d++)/(?:(\\d++)/(\\d++)/)?");

		public FanFictionReviewLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);

			if (savedInstanceState == null){
				// Set the default title to null - this is displayed while loading.
				mTitle = null;

				// Get the chapter and story id from the uri
				Matcher matcher = URI_PATTERN.matcher(uri.toString());
				if (matcher.find()){
					mStoryId = Long.parseLong(Objects.requireNonNull(matcher.group(1)));

					// If there is no chapter id, use 0 as default (all chapters)
					if (matcher.group(2) == null){
						mChapter = 0;
						mTotalChapters = 0;
					} else{
						mChapter = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));
						mTotalChapters = mChapter;
					}
				} else{
					throw new IllegalArgumentException("The Uri " + uri + "is not valid");
				}
			} else{
				mStoryId = savedInstanceState.getLong(STATE_ID);
				mChapter = savedInstanceState.getInt(STATE_CHAPTER);
				mTitle = savedInstanceState.getString(STATE_TITLE);
				mTotalChapters = savedInstanceState.getInt(STATE_TOTAL_CHAPTERS);
			}
		}

		@Override
		public void onFilterClick(final FragmentActivity activity) {
			// Create a new alert dialog
			AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

			// Create an entry for each chapter, plus an "all" entry
			String[] entries = new String[mTotalChapters + 1];
			entries[0] = activity.getString(R.string.menu_library_filter_all);
			for (int i = 1; i <= mTotalChapters; i++){
				entries[i] = activity.getString(R.string.menu_reviews_chapter_posted, i);
			}

			// Show the dialog
			dialog.setItems(entries, (dialog1, position) -> {
				// Since this is a much simpler dialog than filters on other sections of the app,
				// the app won't remember if the dialog is open on orientation changes.
				filter(new int[]{position});
			});
			dialog.show();
		}

		@Override
		public boolean isFilterAvailable() {
			// If there is only one chapter, a filter is not needed.
			// If the value is zero, the available number of chapters hasn't been loaded yet.
			return mTotalChapters > 1;
		}

		@Override
		public void filter(int[] filterSelected) {
			mChapter = filterSelected[0];
			resetState();
			startLoading();
		}

		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			savedInstanceState.putLong(STATE_ID, mStoryId);
			savedInstanceState.putInt(STATE_CHAPTER, mChapter);
			savedInstanceState.putString(STATE_TITLE, mTitle);
			savedInstanceState.putInt(STATE_TOTAL_CHAPTERS, mTotalChapters);
		}

		@Override
		protected int getTotalPages(Document document) {
			Elements pageLinks = document.select("div#content_wrapper_inner > center > a");
			Element lastLink = pageLinks.select("a:contains(last)").last();
			Element nextLink = pageLinks.select("a:contains(next)").last();

			int maxPage;

			if (lastLink != null) {
				// The last link exists. Get the page number from there
				Uri link = Uri.parse(lastLink.absUrl("href"));
				String pageNumber = link.getLastPathSegment();
				maxPage = Integer.parseInt(pageNumber);
			} else if (nextLink != null) {
				// The "next" link exists. Get the page number from there
				Uri link = Uri.parse(nextLink.absUrl("href"));
				String pageNumber = link.getLastPathSegment();
				maxPage = Integer.parseInt(pageNumber);
			} else {
				// The current page is the last page
				maxPage = getCurrentPage();
			}

			return maxPage;
		}

		@Nullable
		@Override
		protected Uri getUri(int currentPage) {
			Uri.Builder builder = Sites.FANFICTION.DESKTOP_URI.buildUpon();
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
			Element title = document.select("table#gui_table1i thead a").first();
			if (title == null) mTitle = null;
			else mTitle = title.text();

			// Get the total number of chapters, if possible
			final Element lastChapterElement = document.select("table#gui_table1i thead option").last();
			if (lastChapterElement == null) mTotalChapters = 1;
			else mTotalChapters = Integer.parseInt(lastChapterElement.attr("value"));

			// Load the reviews
			Elements reviews = document.select("table#gui_table1i tbody td");
			for (Element review : reviews) {

				// Get the reviewer name, which is either the element's own text or the only link
				final Element authorLink = review.select(":root > a").last();
				final String authorTxt;
				if (authorLink == null){
					authorTxt = review.ownText().trim();
				}else{
					authorTxt = authorLink.ownText().trim();
				}

				// Get the text, adding newlines at <br>.
				final Element reviewElement = review.select("div").first();

				// At this point, a null may indicate that there are no reviews available; it is not
				// necessarily an error.
				if (reviewElement == null) continue;
				final List<TextNode> nodes = reviewElement.textNodes();
				final StringBuilder reviewText = new StringBuilder();
				for (TextNode paragraph : nodes) {
					if (reviewText.length() != 0)
						reviewText.append('\n');
					reviewText.append(paragraph.text().trim());
				}

				// Get the chapter number
				final Element data = review.select("small").first();
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
