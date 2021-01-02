package com.spicymango.fanfictionreader.menu.storymenu;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.BaseLoader;
import com.spicymango.fanfictionreader.menu.BaseLoader.Filterable;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog.Builder;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.SpinnerData;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.Sites;
import com.spicymango.fanfictionreader.util.Story;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

final class StoryMenuLoaders {

	// ArchiveOfOurOwnLoaders

	/**
	 * The loader responsible for loading regular Archive of Our Own stories.
	 * 
	 * @author Michael Chen
	 *
	 */
	public final static class AO3RegularStoryLoader extends BaseLoader<Story> {
		private final DateFormat mFormat;
		private final Uri mUri;

		public AO3RegularStoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);
			mUri = uri;
			mFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);
		}

		@Override
		protected int getTotalPages(Document document) {
			Elements pageLists = document.select("ol.pagination");
			if (pageLists.isEmpty()) return 1;

			// Get the first page list set (Each web page has two)
			Element pageList = pageLists.first();
			Elements pageButtons = pageList.children();

			// Get the button corresponding to the last page
			Element pageButton = pageButtons.get(pageButtons.size() - 2);

			return Integer.parseInt(pageButton.text());
		}

		@Override
		protected Uri getUri(int currentPage) {
			Uri.Builder builder = mUri.buildUpon();
			builder.appendQueryParameter("page", String.valueOf(currentPage));
			return builder.build();
		}

		@Override
		protected boolean load(Document document, List<Story> list) {

			Elements stories = document.select("li.work.blurb.group");

			for (Element story : stories) {

				Story.Builder builder = new Story.Builder();

				// Fetch the title, the author, and the story id
				Elements header = story.select("h4.heading a");
				if (header.size() < 2) return false;
				Element title = header.first();
				Element author = header.last();
				String id = title.attr("href").replaceAll("[\\D]", "");

				builder.setName(title.ownText());
				builder.setId(Integer.parseInt(id));
				builder.setAuthor(author.ownText());

				// Fetch the rating
				Element rating = story.select("span.rating").first();
				if (rating == null) return false;
				builder.setRating(rating.text());

				// Fetch the summary
				Element summary = story.select("blockquote.summary").first();
				if (summary != null) {
					builder.setSummary(summary.text());
				}

				// Add characters if they exist
				Elements characters = story.select("li.characters > a");
				for (Element character : characters) {
					builder.addCharacter(character.ownText());
				}

				// Fetch the language
				Element language = story.select("dd.language").first();
				if (language == null) return false;
				builder.setLanguage(language.ownText());

				// Fetch the number of words
				Element words = story.select("dd.words").first();
				if (words == null) return false;
				builder.setWordLength(Parser.parseInt(words.ownText()));

				// Fetch the number of chapters
				Element chapters = story.select("dd.chapters").first();
				if (chapters == null) return false;
				String chapterNo = chapters.text();
				chapterNo = chapterNo.substring(0, chapterNo.indexOf('/'));
				builder.setChapterLength(Integer.parseInt(chapterNo));

				// Fetch the number of hits (follows)
				Element follows = story.select("dd.hits").first();
				if (follows != null) {
					builder.setFollows(Parser.parseInt(follows.ownText()));
				}

				// Fetch the number of kudos (favorites)
				Element favorites = story.select("dd.kudos").first();
				if (favorites != null) {
					builder.setFollows(Parser.parseInt(favorites.text()));
				}

				// Fetch the number of comments (reviews)
				Element comments = story.select("dd.comments").first();
				if (comments != null){
					builder.setReviews(Parser.parseInt(comments.text()));
				}

				// Fetch the update date
				Element updateText = story.select("p.datetime").first();
				if (updateText == null) return false;
				try {
					Date updateDate = mFormat.parse(updateText.text());
					builder.setUpdateDate(updateDate);
				} catch (ParseException e) {
					e.printStackTrace();
				}

				// Find if the work is complete
				Elements complete = story.select("span.complete-yes");
				builder.setCompleted(complete.size() > 0);

				list.add(builder.build());
			}

			return true;
		}

	}

	// FanFiction Loaders

	/**
	 * The loader responsible for loading the just in section for FanFiction
	 * 
	 * @author Michael Chen
	 *
	 */
	public final static class FFJustInStoryLoader extends BaseLoader<Story> implements Filterable {
		private final static String STATE_FILTER = "STATE_FILTER";

		private ArrayList<SpinnerData> mSpinnerData;
		private final Uri mUri;
		
		public FFJustInStoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);

			// Set the authority to the mobile version
			Uri.Builder builder = uri.buildUpon();
			builder.authority(Sites.FANFICTION.AUTHORITY);
			mUri = builder.build();

			// Restore the saved filters
			if (savedInstanceState != null) {
				mSpinnerData = savedInstanceState.getParcelableArrayList(STATE_FILTER);
			}
		}

		@Override
		public void filter(int[] filterSelected) {
			for (int i = 0; i < filterSelected.length; i++) {
				mSpinnerData.get(i).setSelected(filterSelected[i]);
			}
			resetState();
			startLoading();
		}

		@Override
		protected int getTotalPages(Document document) {
			return 0;
		}

		@Override
		protected Uri getUri(int currentPage) {
			Uri.Builder builder = mUri.buildUpon();
			builder.appendQueryParameter("p", currentPage + ""); // Current Page

			// Adds the filter, if available
			if (isFilterAvailable()) {
				for (SpinnerData spinnerData : mSpinnerData) {
					final String key = spinnerData.getName();
					final String value = spinnerData.getCurrentFilter();
					if (key == null) continue;
					builder.appendQueryParameter(key, value);
				}
			}			
			return builder.build();
		}

		@Override
		public boolean isFilterAvailable() {
			return mSpinnerData != null;
		}

		@Override
		protected boolean load(Document document, List<Story> list) {
			// Load the filters if they aren't already loaded.
			if (!isFilterAvailable()) {
				loadFilter(document);
			}
			return Parser.Stories(document, list);
		}

		/**
		 * Loads the just in filter
		 * @param document The web page's {@link Document}
		 */
		private void loadFilter(Document document) {
			Elements form = document.select("div#content div#d_menu form > select");
			Elements[] filter = { form.select("[name=s]"), form.select("[name=cid]"), form.select("[name=l]") };

			mSpinnerData = new ArrayList<>();
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
				mSpinnerData.add(new SpinnerData(name, label, filterKey, 0));
			}
		}

		@Override
		public void onFilterClick(FragmentActivity activity) {
			final FilterDialog.Builder builder = new FilterDialog.Builder();
			builder.addSingleSpinner(activity.getString(R.string.filter_type), mSpinnerData.get(0));
			builder.addSingleSpinner(activity.getString(R.string.filter_category), mSpinnerData.get(1));
			builder.addSingleSpinner(activity.getString(R.string.filter_language), mSpinnerData.get(2));
			builder.show((StoryMenuActivity) activity);
		}
	}

	/**
	 * Creates a new loader that can be used for FanFiction regular stories and
	 * FanFiction crossover stories.
	 * 
	 * @author Michael Chen
	 *
	 */
	public final static class FFRegularStoryLoader extends BaseLoader<Story>implements Filterable {
		private final static String STATE_FILTER = "STATE_FILTER";
		private ArrayList<SpinnerData> mSpinnerData;
		private final Uri mUri;

		/**
		 * Creates a new FanFiction loader
		 * 
		 * @param context
		 *            The current context
		 * @param savedInstanceState
		 *            The savedInstanceState
		 * @param uri
		 *            The {@link Uri} to load
		 */
		public FFRegularStoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);

			// Set the authority to the mobile version
			Uri.Builder builder = uri.buildUpon();
			builder.authority(Sites.FANFICTION.AUTHORITY);
			mUri = builder.build();

			// Restore the saved filters
			if (savedInstanceState != null) {
				mSpinnerData = savedInstanceState.getParcelableArrayList(STATE_FILTER);
			}
		}

		@Override
		public void filter(int[] filterSelected) {
			for (int i = 0; i < filterSelected.length; i++) {
				mSpinnerData.get(i).setSelected(filterSelected[i]);
			}
			resetState();
			startLoading();
		}

		@Override
		protected int getTotalPages(Document document) {
			return Math.max(Parser.getPageNumber(document), getCurrentPage());
		}

		@Override
		protected Uri getUri(int currentPage) {
			Uri.Builder builder = mUri.buildUpon();
			builder.appendQueryParameter("p", currentPage + ""); // Current Page

			// Adds the filter, if available
			if (isFilterAvailable()) {
				for (SpinnerData spinnerData : mSpinnerData) {
					final String key = spinnerData.getName();
					final String value = spinnerData.getCurrentFilter();
					if (key == null) continue;
					builder.appendQueryParameter(key, value);
				}
			} else {
				// Default rating = all
				builder.appendQueryParameter("r", "10");
			}

			return builder.build();
		}

		@Override
		public boolean isFilterAvailable() {
			return mSpinnerData != null;
		}

		@Override
		protected boolean load(Document document, List<Story> list) {
			// Load the filters if they aren't already loaded.
			if (!isFilterAvailable()) {
				loadFilter(document);
			}
			return Parser.Stories(document, list);
		}

		/**
		 * Parses the filter
		 * @param document The HTML document
		 */
		public void loadFilter(Document document){
			Elements form = document.select("div#content div#d_menu form > select");
			Elements[] filter = { form.select("[title=sort options]"),
					form.select("[title=time range options]"),
					form.select("[title=genre 1 filter],[title=genre filter]"),
					form.select("[title=genre 2 filter]"),
					form.select("[title=rating filter]"),
					form.select("[title=language filter],[name=l]"),
					form.select("[title=length in words filter]"),
					form.select("[title=story status]"), 
					form.select("[title=character 1 filter]"),
					form.select("[title=character 2 filter]"),
					form.select("[title=character 3 filter]"),
					form.select("[title=character 4 filter]") };
			
			mSpinnerData = new ArrayList<>();
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
				mSpinnerData.add(new SpinnerData(name, label, filterKey, 0));
			}
		}

		@Override
		public void onFilterClick(FragmentActivity activity) {
			final FilterDialog.Builder builder = new FilterDialog.Builder();
			builder.addSingleSpinner(activity.getString(R.string.filter_sort), mSpinnerData.get(0));
			builder.addSingleSpinner(activity.getString(R.string.filter_date), mSpinnerData.get(1));
			builder.addDoubleSpinner(activity.getString(R.string.filter_genre), mSpinnerData.get(2),
					mSpinnerData.get(3));
			builder.addSingleSpinner(activity.getString(R.string.filter_rating), mSpinnerData.get(4));
			builder.addSingleSpinner(activity.getString(R.string.filter_language), mSpinnerData.get(5));
			builder.addSingleSpinner(activity.getString(R.string.filter_length), mSpinnerData.get(6));
			builder.addSingleSpinner(activity.getString(R.string.filter_status), mSpinnerData.get(7));
			builder.addDoubleSpinner(activity.getString(R.string.filter_character), mSpinnerData.get(8),
					mSpinnerData.get(9));
			builder.addDoubleSpinner(activity.getString(R.string.filter_character), mSpinnerData.get(10),
					mSpinnerData.get(11));
			builder.show((StoryMenuActivity) activity);
		}

		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			savedInstanceState.putParcelableArrayList(STATE_FILTER, mSpinnerData);
		}
	}

	/**
	 * The loader responsible for loading the stories from communities.
	 * @author Michael Chen
	 *
	 */
	public final static class FFCommunityStoryLoader extends BaseLoader<Story>implements Filterable {
		private final static String STATE_FILTER = "STATE_FILTER";
		private ArrayList<SpinnerData> mSpinnerData;
		private final Uri mUri;
		
		public FFCommunityStoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);
			
			// Set the authority to the mobile version
			Uri.Builder builder = uri.buildUpon();
			builder.authority(Sites.FANFICTION.AUTHORITY);
			mUri = builder.build();
			
			if (savedInstanceState != null) {
				mSpinnerData = savedInstanceState.getParcelableArrayList(STATE_FILTER);
			}
		}

		@Override
		public void onFilterClick(FragmentActivity activity) {
			Builder builder = new Builder();
			builder.addSingleSpinner(activity.getString(R.string.filter_sort), mSpinnerData.get(0));
			builder.addSingleSpinner(activity.getString(R.string.filter_rating), mSpinnerData.get(1));
			builder.addSingleSpinner(activity.getString(R.string.filter_date), mSpinnerData.get(2));
			builder.addSingleSpinner(activity.getString(R.string.filter_genre), mSpinnerData.get(3));
			builder.addSingleSpinner(activity.getString(R.string.filter_length), mSpinnerData.get(4));
			builder.addSingleSpinner(activity.getString(R.string.filter_status), mSpinnerData.get(5));
			builder.show((StoryMenuActivity) activity);
		}

		@Override
		public boolean isFilterAvailable() {
			return mSpinnerData != null;
		}

		@Override
		public void filter(int[] filterSelected) {
			for (int i = 0; i < filterSelected.length; i++) {
				mSpinnerData.get(i).setSelected(filterSelected[i]);
			}
			resetState();
			startLoading();
		}

		@Override
		protected int getTotalPages(Document document) {
			return Math.max(Parser.getPageNumber(document), getCurrentPage());
		}

		@Override
		protected Uri getUri(int currentPage) {
			Uri.Builder builder = mUri.buildUpon();

			// Adds the filter, if available
			if (isFilterAvailable()) {
				builder.appendEncodedPath(mSpinnerData.get(1).getCurrentFilter());	//Rating
				builder.appendEncodedPath(mSpinnerData.get(0).getCurrentFilter());	//Sort Options
				builder.appendEncodedPath(String.valueOf(currentPage));				//Current Page
				builder.appendEncodedPath(mSpinnerData.get(3).getCurrentFilter());	//Genre
				builder.appendEncodedPath(mSpinnerData.get(4).getCurrentFilter());	//Length
				builder.appendEncodedPath(mSpinnerData.get(5).getCurrentFilter());	//Status
				builder.appendEncodedPath(mSpinnerData.get(2).getCurrentFilter());	//Time Range
				builder.appendEncodedPath("");
			} else {
				// Default rating = all
				builder.appendEncodedPath("99");//Rating
				builder.appendEncodedPath("0");//Sort Options
				builder.appendEncodedPath(String.valueOf(currentPage));//Current Page
				builder.appendEncodedPath("0");//Genre
				builder.appendEncodedPath("0");//Length
				builder.appendEncodedPath("0");//Status
				builder.appendEncodedPath("0");//Time Range
				builder.appendEncodedPath("");
			}

			return builder.build();
		}

		@Override
		protected boolean load(Document document, List<Story> list) {
			// Load the filters if they aren't already loaded.
			if (!isFilterAvailable()) {
				loadFilter(document);
			}
			return Parser.Stories(document, list);
		}

		/**
		 * Parses the filter
		 * @param document The HTML document
		 */
		private void loadFilter(Document document) {
			Elements form = document.select("div#content div#d_menu form > select");
			Elements[] filter = {
					form.select("[name=s]"),
					form.select("[name=censorid]"),
					form.select("[name=timeid]"),
					form.select("[name=genreid]"),
					form.select("[name=len]"),
					form.select("[name=statusid]") };
			
			mSpinnerData = new ArrayList<>();
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
				mSpinnerData.add(new SpinnerData(name, label, filterKey, 0));
			}
		}
		
		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			savedInstanceState.putParcelableArrayList(STATE_FILTER, mSpinnerData);
		}
	}

	// FictionPress Loaders
	/**
	 * Creates a new loader that can be used for FictionPress regular stories
	 *
	 * @author Michael Chen
	 *
	 */
	public final static class FPRegularStoryLoader extends BaseLoader<Story>implements Filterable {
		private final static String STATE_FILTER = "STATE_FILTER";
		private ArrayList<SpinnerData> mSpinnerData;
		private final Uri mUri;

		/**
		 * Creates a new FanFiction loader
		 *
		 * @param context
		 *            The current context
		 * @param savedInstanceState
		 *            The savedInstanceState
		 * @param uri
		 *            The {@link Uri} to load
		 */
		public FPRegularStoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);

			// Set the authority to the mobile version
			Uri.Builder builder = uri.buildUpon();
			builder.authority(Sites.FICTIONPRESS.AUTHORITY);
			mUri = builder.build();

			// Restore the saved filters
			if (savedInstanceState != null) {
				mSpinnerData = savedInstanceState.getParcelableArrayList(STATE_FILTER);
			}
		}

		@Override
		public void filter(int[] filterSelected) {
			for (int i = 0; i < filterSelected.length; i++) {
				mSpinnerData.get(i).setSelected(filterSelected[i]);
			}
			resetState();
			startLoading();
		}

		@Override
		protected int getTotalPages(Document document) {
			return Math.max(Parser.getPageNumber(document), getCurrentPage());
		}

		@Override
		protected Uri getUri(int currentPage) {
			Uri.Builder builder = mUri.buildUpon();
			builder.appendQueryParameter("p", currentPage + ""); // Current Page

			// Adds the filter, if available
			if (isFilterAvailable()) {
				for (SpinnerData spinnerData : mSpinnerData) {
					final String key = spinnerData.getName();
					final String value = spinnerData.getCurrentFilter();
					if (key == null) continue;
					builder.appendQueryParameter(key, value);
				}
			} else {
				// Default rating = all
				builder.appendQueryParameter("r", "10");
			}

			return builder.build();
		}

		@Override
		public boolean isFilterAvailable() {
			return mSpinnerData != null;
		}

		@Override
		protected boolean load(Document document, List<Story> list) {
			// Load the filters if they aren't already loaded.
			if (!isFilterAvailable()) {
				loadFilter(document);
			}
			return Parser.Stories(document, list);
		}

		/**
		 * Parses the filter
		 * @param document The HTML document
		 */
		public void loadFilter(Document document){
			Elements form = document.select("div#content div#d_menu form > select");
			Elements[] filter = { form.select("[title=sort options]"),
								  form.select("[title=time range options]"),
								  form.select("[title=genre 1 filter],[title=genre filter]"),
								  form.select("[title=genre 2 filter]"),
								  form.select("[title=rating filter]"),
								  form.select("[title=language filter],[name=l]"),
								  form.select("[title=length in words filter]"),
								  form.select("[title=story status]")};

			mSpinnerData = new ArrayList<>();
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
				mSpinnerData.add(new SpinnerData(name, label, filterKey, 0));
			}
		}

		@Override
		public void onFilterClick(FragmentActivity activity) {
			final FilterDialog.Builder builder = new FilterDialog.Builder();
			builder.addSingleSpinner(activity.getString(R.string.filter_sort), mSpinnerData.get(0));
			builder.addSingleSpinner(activity.getString(R.string.filter_date), mSpinnerData.get(1));
			builder.addDoubleSpinner(activity.getString(R.string.filter_genre), mSpinnerData.get(2),
									 mSpinnerData.get(3));
			builder.addSingleSpinner(activity.getString(R.string.filter_rating), mSpinnerData.get(4));
			builder.addSingleSpinner(activity.getString(R.string.filter_language), mSpinnerData.get(5));
			builder.addSingleSpinner(activity.getString(R.string.filter_length), mSpinnerData.get(6));
			builder.addSingleSpinner(activity.getString(R.string.filter_status), mSpinnerData.get(7));
			builder.show((StoryMenuActivity) activity);
		}

		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			savedInstanceState.putParcelableArrayList(STATE_FILTER, mSpinnerData);
		}
	}

}
