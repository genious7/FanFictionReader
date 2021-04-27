package com.spicymango.fanfictionreader.menu.authormenu;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.BaseLoader;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.SpinnerData;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.Sites;
import com.spicymango.fanfictionreader.util.Story;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class containing the story loaders for the author activity Created by Michael Chen on
 * 01/30/2016.
 */
class AuthorStoryLoader {

	/*
	FanFiction Loaders
	 */

	static abstract class BaseFanFictionAuthorStoryLoader extends BaseLoader<Story> implements BaseLoader.Filterable, AuthorStoryFragment.SubTitleGetter {
		private static final String STATE_FILTER = "Filters";
		private static final String STATE_AUTHOR = "Author";

		/**
		 * The author's name
		 */
		protected String mAuthor;

		/**
		 * The author's id.
		 */
		protected final long mAuthorId;

		protected ArrayList<SpinnerData> mFilter;

		/**
		 * Creates a new loader to obtain an author's stories
		 * @param context The current context
		 * @param savedInstanceState The SavedInstanceState, or null if the activity is not being recreated
		 * @param uri The uri of the author that should be loaded
		 */
		public BaseFanFictionAuthorStoryLoader(Context context, Bundle savedInstanceState,
											   Uri uri) {
			super(context, savedInstanceState);

			// Get the author id
			String segment = uri.getPathSegments().get(1);
			mAuthorId = Long.parseLong(segment);

			if (savedInstanceState != null) {
				mFilter = savedInstanceState.getParcelableArrayList(STATE_FILTER);
				mAuthor = savedInstanceState.getString(STATE_AUTHOR);
			}
		}

		@Override
		public String getSubTitle() {
			return mAuthor;
		}

		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			savedInstanceState.putParcelableArrayList(STATE_FILTER, mFilter);
			savedInstanceState.putString(STATE_AUTHOR, mAuthor);
		}

		@Override
		public void onFilterClick(FragmentActivity activity) {
			final FilterDialog.Builder builder = new FilterDialog.Builder();
			builder.addSingleSpinner(activity.getString(R.string.filter_sort), mFilter.get(0));
			builder.addSingleSpinner(activity.getString(R.string.filter_category), mFilter.get(1));
			builder.show((AuthorMenuActivity) activity);
		}

		@Override
		public boolean isFilterAvailable() {
			return mFilter != null;
		}

		@Override
		public void filter(int[] filterSelected) {
			for (int i = 0; i < filterSelected.length; i++) {
				mFilter.get(i).setSelected(filterSelected[i]);
			}
			resetState();
			startLoading();
		}

		@Override
		protected int getTotalPages(Document document) {
			return Parser.getPageNumber(document);
		}

		protected static final Pattern PATTERN_STORY_ID = Pattern.compile("/s/([\\d]++)/");

		@Override
		protected boolean load(Document document, List<Story> list) {
			// Get the author's name
			if (mAuthor == null) {
				mAuthor = getAuthor(document);
			}

			// Load the filter
			if (mFilter == null) {
				Elements form = document.select("div#content div#d_menu form > select");
				Elements[] filter = {form.select("[name=s]"), form.select("[name=cid]")};

				mFilter = new ArrayList<>();
				for (Elements j : filter) {
					final ArrayList<String> label = new ArrayList<>();
					final ArrayList<String> filterKey = new ArrayList<>();

					String name = null;
					if (!j.isEmpty()) {
						name = j.attr("name");
						Element item = j.first();
						Elements options = item.children();
						for (Element k : options) {
							label.add(k.ownText());
							filterKey.add(k.attr("value"));
						}
					}
					mFilter.add(new SpinnerData(name, label, filterKey, 0));
				}
			}

			final Matcher storyIdMatcher = PATTERN_STORY_ID.matcher("");
			final Elements summaries = document.select("div#content div.bs");

			// For each story in the list
			for (Element element : summaries) {
				element.select("b").unwrap();    // Fixes a bug that occurs if the text is bold. Occurs in a few authors

				final Elements links = element.select("a");

				// Get the story title and id
				final Element titleElement = links.select("a[href~=(?i)/s/\\d+/1/.*]").first();
				if (titleElement == null) return false;
				storyIdMatcher.reset(titleElement.attr("href"));
				if (!storyIdMatcher.find()) return false;
				final String storyTitle = titleElement.ownText();

				// Get the story attributes
				final Element attributes = element.select("div.gray").first();
				if (attributes == null) return false;

				// Get the story publish and update date
				final Elements dates = element.select("span[data-xutime]");
				if (dates.isEmpty()) return false;
				final long updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
				final long publishDate = Long.parseLong(dates.last().attr("data-xutime")) * 1000;

				// Determine if the story is complete
				final Elements completeImageElements = element.select("img.mm");
				final boolean complete = !completeImageElements.isEmpty();

				// Determine the number of reviews of the story
				final Elements reviewIcon = element.select("a > img.mt");
				final int reviews;
				if (reviewIcon.isEmpty()) {
					reviews = 0;
				} else {
					final Element reviewLink = reviewIcon.first().parent();
					reviews = Parser.parseInt(reviewLink.ownText());
				}

				Story.Builder builder = new Story.Builder();
				builder.setId(Long.parseLong(storyIdMatcher.group(1)));
				builder.setName(storyTitle);
				builder.setAuthor(mAuthor);
				builder.setAuthorId(mAuthorId);
				builder.setSummary(element.ownText().replaceFirst("^(?i)by\\s*", ""));
				builder.setFanFicAttributes(attributes.text());
				builder.setUpdateDate(updateDate);
				builder.setPublishDate(publishDate);
				builder.setCompleted(complete);
				builder.setReviews(reviews);

				list.add(builder.build());
			}
			return true;
		}

		/**
		 * Gets the author's name.
		 *
		 * @param document The FanFiction web site
		 * @return The author's name on success; otherwise, an empty string is returned.
		 */
		@NonNull
		protected String getAuthor(Document document) {
			Elements author = document.select("div#content div b");
			if (author.isEmpty()) {
				return "";
			} else {
				return author.first().ownText();
			}
		}
	}

	/**
	 * A loader used to obtain the stories written by a certain author
	 */
	static class FanFictionAuthorStoryLoader extends BaseFanFictionAuthorStoryLoader {

		/**
		 * Creates a new loader to obtain an author's stories
		 * @param context The current context
		 * @param savedInstanceState The SavedInstanceState, or null if the activity is not being recreated
		 * @param uri The uri of the author that should be loaded
		 */
		public FanFictionAuthorStoryLoader(Context context, Bundle savedInstanceState,
										   Uri uri) {
			super(context, savedInstanceState, uri);
		}

		@Nullable
		@Override
		protected Uri getUri(int currentPage) {
			// Create the link based on the author id
			Uri.Builder builder = Sites.FANFICTION.BASE_URI.buildUpon();
			builder.appendPath("u")                            // Author
					.appendPath(Long.toString(mAuthorId))    // Author ID
					.appendPath("");                        // The slash at the end of the path.

			builder.appendQueryParameter("a", "s")            // Show the author's stories
					.appendQueryParameter("p", Integer.toString(currentPage));    // Page Number

			if (isFilterAvailable()) {
				builder.appendQueryParameter("s", mFilter.get(0).getCurrentFilter())    // Sort order
						.appendQueryParameter("cid", mFilter.get(1).getCurrentFilter());    // The filtered category
			}

			return builder.build();
		}
	}

	/**
	 * A loader used to obtain the stories that have been added to a certain author's favorites
	 */
	static class FanFictionAuthorFavoriteLoader extends BaseFanFictionAuthorStoryLoader {

		/**
		 * Creates a new loader to obtain an author's favorite stories
		 * @param context The current context
		 * @param savedInstanceState The SavedInstanceState, or null if the activity is not being recreated
		 * @param uri The uri of the author that should be loaded
		 */
		public FanFictionAuthorFavoriteLoader(Context context, Bundle savedInstanceState,
										   Uri uri) {
			super(context, savedInstanceState, uri);
		}

		@Nullable
		@Override
		protected Uri getUri(int currentPage) {
			// Create the link based on the author id
			Uri.Builder builder = Sites.FANFICTION.BASE_URI.buildUpon();
			builder.appendPath("u")                            // Author
					.appendPath(Long.toString(mAuthorId))    // Author ID
					.appendPath("");                        // The slash at the end of the path.

			builder.appendQueryParameter("a", "fs")            // Show the author's stories
					.appendQueryParameter("p", Integer.toString(currentPage));    // Page Number

			if (isFilterAvailable()) {
				builder.appendQueryParameter("s", mFilter.get(0).getCurrentFilter())    // Sort order
						.appendQueryParameter("cid", mFilter.get(1).getCurrentFilter());    // The filtered category
			}

			return builder.build();
		}

		private static final Pattern PATTERN_AUTHOR_ID = Pattern.compile("/u/([\\d]++)/");

		@Override
		protected boolean load(Document document, List<Story> list) {
			// Get the author's name
			if (mAuthor == null) {
				mAuthor = getAuthor(document);
			}

			// Load the filter
			if (mFilter == null) {
				Elements form = document.select("div#content div#d_menu form > select");
				Elements[] filter = {form.select("[name=s]"), form.select("[name=cid]")};

				mFilter = new ArrayList<>();
				for (Elements j : filter) {
					final ArrayList<String> label = new ArrayList<>();
					final ArrayList<String> filterKey = new ArrayList<>();

					String name = null;
					if (!j.isEmpty()) {
						name = j.attr("name");
						Element item = j.first();
						Elements options = item.children();
						for (Element k : options) {
							label.add(k.ownText());
							filterKey.add(k.attr("value"));
						}
					}
					mFilter.add(new SpinnerData(name, label, filterKey, 0));
				}
			}

			final Matcher storyIdMatcher = PATTERN_STORY_ID.matcher("");
			final Matcher authorIdMatcher = PATTERN_AUTHOR_ID.matcher("");
			final Elements summaries = document.select("div#content div.bs");

			// For each story in the list
			for (Element element : summaries) {
				element.select("b").unwrap();    // Fixes a bug that occurs if the text is bold. Occurs in a few authors

				final Elements links = element.select("a");

				// Get the story title and id
				final Element titleElement = links.select("a[href~=(?i)/s/\\d+/1/.*]").first();
				if (titleElement == null) return false;
				storyIdMatcher.reset(titleElement.attr("href"));
				if (!storyIdMatcher.find()) return false;
				final String storyTitle = titleElement.ownText();

				// Get the story author
				final Element authorElement = links.last();
				if (authorElement == null) return false;
				authorIdMatcher.reset(authorElement.attr("href"));
				if (!authorIdMatcher.find()) return false;
				final String authorName = authorElement.ownText();

				// Get the story attributes
				final Element attributes = element.select("div.gray").first();
				if (attributes == null) return false;

				// Get the story publish and update date
				final Elements dates = element.select("span[data-xutime]");
				if (dates.isEmpty()) return false;
				final long updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
				final long publishDate = Long.parseLong(dates.last().attr("data-xutime")) * 1000;

				// Determine if the story is complete
				final Elements completeImageElements = element.select("img.mm");
				final boolean complete = !completeImageElements.isEmpty();

				// Determine the number of reviews of the story
				final Elements reviewIcon = element.select("a > img.mt");
				final int reviews;
				if (reviewIcon.isEmpty()) {
					reviews = 0;
				} else {
					final Element reviewLink = reviewIcon.first().parent();
					reviews = Parser.parseInt(reviewLink.ownText());
				}

				Story.Builder builder = new Story.Builder();
				builder.setId(Long.parseLong(storyIdMatcher.group(1)));
				builder.setName(storyTitle);
				builder.setAuthor(authorName);
				builder.setAuthorId(Long.parseLong(authorIdMatcher.group(1)));
				builder.setSummary(element.ownText().replaceFirst("^(?i)by\\s*", ""));
				builder.setFanFicAttributes(attributes.text());
				builder.setUpdateDate(updateDate);
				builder.setPublishDate(publishDate);
				builder.setCompleted(complete);
				builder.setReviews(reviews);

				list.add(builder.build());
			}
			return true;
		}

	}
}
