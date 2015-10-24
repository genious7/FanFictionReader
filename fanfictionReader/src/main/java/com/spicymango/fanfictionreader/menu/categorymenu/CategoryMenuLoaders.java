package com.spicymango.fanfictionreader.menu.categorymenu;

import java.lang.Character.UnicodeBlock;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.BaseLoader;
import com.spicymango.fanfictionreader.menu.categorymenu.CategoryMenuActivity.CategoryMenuFragment.Filterable;
import com.spicymango.fanfictionreader.util.Sites;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

final class CategoryMenuLoaders {

	public final static class FanFictionRegularCategoryLoader extends BaseLoader<CategoryMenuItem>
			implements Filterable {
		private static final String STATE_FILTER = "STATE_FILTER";

		private final String mFormatString;
		private final Uri mUri;
		private int mCurrentFilter;

		public FanFictionRegularCategoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);

			Uri.Builder builder = uri.buildUpon();
			builder.authority(Sites.FANFICTION.BASE_URI.getAuthority());
			mUri = builder.build();

			mFormatString = context.getString(R.string.menu_navigation_count_story);

			if (savedInstanceState != null) {
				mCurrentFilter = savedInstanceState.getInt(STATE_FILTER);
			}
		}

		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			savedInstanceState.putInt(STATE_FILTER, mCurrentFilter);
		}

		@Override
		protected int getTotalPages(Document document) {
			return 0;
		}

		@Override
		protected Uri getUri(int currentPage) {
			char query = ' ';
			if (mCurrentFilter == 0) {
				query = ' ';
			} else if (mCurrentFilter == 1) {
				query = '1';
			} else {
				query = (char) ('a' + mCurrentFilter - 2);
			}

			Uri.Builder builder = mUri.buildUpon();
			builder.appendQueryParameter("l", String.valueOf(query));
			return builder.build();
		}

		@Override
		protected boolean load(Document document, List<CategoryMenuItem> list) {
			Elements categories = document.select("div#content > div.bs > a");

			for (Element category : categories) {
				if (category.childNodeSize() == 0) { return false; }

				String title = category.ownText();
				String views = category.child(0).ownText();
				views = String.format(mFormatString, views);
				Uri url = Uri.parse(category.absUrl("href"));
				CategoryMenuItem item = new CategoryMenuItem(title, views, url);
				list.add(item);
			}
			return true;
		}

		@Override
		public String[] getFilterEntries() {
			String[] filterList;
			filterList = new String[28];
			filterList[0] = getContext().getString(R.string.menu_navigation_filter_top_200);
			filterList[1] = "#";
			for (int i = 0; i < 26; i++) {
				filterList[2 + i] = "" + (char) (('A') + i);
			}
			return filterList;
		}

		@Override
		public void onFilterSelected(int position) {
			if (mCurrentFilter != position) {
				mCurrentFilter = position;
				resetState();
				startLoading();
			}
		}
	}

	public final static class FanFictionSubCategoryLoader extends BaseLoader<CategoryMenuItem>implements Filterable {
		private static final String STATE_FILTER = "STATE_FILTER";

		private final String mFormatString;
		private final Uri mUri;
		private int mCurrentFilter;

		public FanFictionSubCategoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);

			Uri.Builder builder = uri.buildUpon();
			builder.authority(Sites.FANFICTION.BASE_URI.getAuthority());
			mUri = builder.build();

			mFormatString = context.getString(R.string.menu_navigation_count_story);

			if (savedInstanceState != null) {
				mCurrentFilter = savedInstanceState.getInt(STATE_FILTER);
			}
		}

		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			savedInstanceState.putInt(STATE_FILTER, mCurrentFilter);
		}

		@Override
		protected int getTotalPages(Document document) {
			return 0;
		}

		@Override
		protected Uri getUri(int currentPage) {
			int query;
			if (mCurrentFilter == 0) {
				query = 0;
			} else if (mCurrentFilter < 5) {
				query = 200 + mCurrentFilter;
			} else if (mCurrentFilter == 5) {
				query = 209;
			} else if (mCurrentFilter == 6) {
				query = 211;
			} else if (mCurrentFilter == 7) {
				query = 205;
			} else if (mCurrentFilter == 8) {
				query = 207;
			} else {
				query = 208;
			}

			Uri.Builder builder = mUri.buildUpon();
			builder.appendQueryParameter("pcategoryid", String.valueOf(query));
			return builder.build();
		}

		@Override
		protected boolean load(Document document, List<CategoryMenuItem> list) {
			Elements categories = document.select("div#content > div.bs > a");

			String allCrossover = getCrossoverName(document);
			Uri allUri = getCrossoverUri(document);
			if (allCrossover == null || allUri == null) { return false; }
			CategoryMenuItem allCrossovers = new CategoryMenuItem(allCrossover, "", allUri);
			list.add(allCrossovers);

			for (Element category : categories) {
				if (category.childNodeSize() == 0) { return false; }

				String title = category.ownText();
				String views = category.child(0).ownText();
				views = String.format(mFormatString, views);
				Uri url = Uri.parse(category.absUrl("href"));
				CategoryMenuItem item = new CategoryMenuItem(title, views, url);
				list.add(item);
			}
			return true;
		}

		/**
		 * Gets the "all crossover" text for the current document
		 * 
		 * @param document The document to fetch the information
		 * @return The requested text, or null if the link does not exist
		 */
		private String getCrossoverName(Document document) {
			Elements url = document.select("div#content > center > a");
			if (url == null || url.first() == null) { return null; }
			return url.first().ownText();
		}

		/**
		 * Gets the "all crossover" url for the current document
		 * 
		 * @param document The document to fetch the information
		 * @return The requested url, or null if the link does not exist
		 */
		private Uri getCrossoverUri(Document document) {
			Elements url = document.select("div#content > center > a");
			if (url == null || url.first() == null) { return null; }
			return Uri.parse(url.first().absUrl("href"));
		}

		@Override
		public String[] getFilterEntries() {
			return getContext().getResources().getStringArray(R.array.menu_navigation_filter_crossover);
		}

		@Override
		public void onFilterSelected(int position) {
			if (mCurrentFilter != position) {
				mCurrentFilter = position;
				resetState();
				startLoading();
			}
		}

	}

	public final static class FanFictionCommunityCategoryLoader extends BaseLoader<CategoryMenuItem>
			implements Filterable {
		private static final String STATE_FILTER = "STATE_FILTER";

		private final String mFormatString;
		private final Uri mUri;
		private int mCurrentFilter;

		public FanFictionCommunityCategoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);

			Uri.Builder builder = uri.buildUpon();
			builder.authority(Sites.FANFICTION.BASE_URI.getAuthority());
			mUri = builder.build();

			mFormatString = context.getString(R.string.menu_navigation_count_community);

			if (savedInstanceState != null) {
				mCurrentFilter = savedInstanceState.getInt(STATE_FILTER);
			}
		}

		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			savedInstanceState.putInt(STATE_FILTER, mCurrentFilter);
		}

		@Override
		protected int getTotalPages(Document document) {
			return 0;
		}

		@Override
		protected Uri getUri(int currentPage) {
			char query = ' ';
			if (mCurrentFilter == 0) {
				query = ' ';
			} else if (mCurrentFilter == 1) {
				query = '1';
			} else {
				query = (char) ('a' + mCurrentFilter - 2);
			}

			Uri.Builder builder = mUri.buildUpon();
			builder.appendQueryParameter("l", String.valueOf(query));
			return builder.build();
		}

		@Override
		protected boolean load(Document document, List<CategoryMenuItem> list) {
			Elements categories = document.select("div#content > div.bs > a");

			for (Element category : categories) {
				if (category.childNodeSize() == 0) { return false; }

				String title = category.ownText();
				String views = category.child(0).ownText();
				views = String.format(mFormatString, views);
				Uri url = Uri.parse(category.absUrl("href"));
				CategoryMenuItem item = new CategoryMenuItem(title, views, url);
				list.add(item);
			}
			return true;
		}

		@Override
		public String[] getFilterEntries() {
			String[] filterList;
			filterList = new String[28];
			filterList[0] = getContext().getString(R.string.menu_navigation_filter_top_200);
			filterList[1] = "#";
			for (int i = 0; i < 26; i++) {
				filterList[2 + i] = "" + (char) (('A') + i);
			}
			return filterList;
		}

		@Override
		public void onFilterSelected(int position) {
			if (mCurrentFilter != position) {
				mCurrentFilter = position;
				resetState();
				startLoading();
			}
		}
	}

	public final static class FictionPressCommunityCategoryLoader extends BaseLoader<CategoryMenuItem> {
		private final String mFormatString;
		private final Uri mUri;
		
		public FictionPressCommunityCategoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);

			Uri.Builder builder = uri.buildUpon();
			builder.authority(Sites.FICTIONPRESS.AUTHORITY);
			mUri = builder.build();

			mFormatString = context.getString(R.string.menu_navigation_count_community);
		}

		@Override
		protected int getTotalPages(Document document) {
			return 0;
		}

		@Override
		protected Uri getUri(int currentPage) {
			return mUri;
		}

		@Override
		protected boolean load(Document document, List<CategoryMenuItem> list) {
			Elements categories = document.select("div#content > div.bs > a");

			for (Element category : categories) {
				if (category.childNodeSize() == 0) { return false; }

				String title = category.ownText();
				String views = category.child(0).ownText();
				views = String.format(mFormatString, views);
				Uri url = Uri.parse(category.absUrl("href"));
				CategoryMenuItem item = new CategoryMenuItem(title, views, url);
				list.add(item);
			}
			return true;
		}
	}

	public final static class ArchiveOfOurOwnCategoryLoader extends BaseLoader<CategoryMenuItem>implements Filterable {
		private static final String STATE_FILTER = "STATE_FILTER";
		private static final String STATE_CACHE = "STATE_CACHE";

		private ArrayList<CategoryMenuItem> mCache;

		private final Uri mUri;
		private final String mFormatString;
		private final NumberFormat mFormatter;
		private final String[] mFilterList;

		private static final CategoryMenuComparator COMPARATOR = new CategoryMenuComparator();
		private static final List<UnicodeBlock> CONSERVE_CHARACTERS = new ArrayList<>();
		private static final Collator COLLATOR = Collator.getInstance(Locale.US);

		static {
			CONSERVE_CHARACTERS.add(Character.UnicodeBlock.BASIC_LATIN);
			CONSERVE_CHARACTERS.add(Character.UnicodeBlock.LATIN_1_SUPPLEMENT);
			CONSERVE_CHARACTERS.add(Character.UnicodeBlock.LATIN_EXTENDED_A);
			CONSERVE_CHARACTERS.add(Character.UnicodeBlock.LATIN_EXTENDED_B);

			COLLATOR.setStrength(Collator.PRIMARY);
		}

		private int mCurrentFilter;

		public ArchiveOfOurOwnCategoryLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);
			mUri = uri;
			mFormatString = context.getString(R.string.menu_navigation_count_story);
			mFormatter = NumberFormat.getInstance(Locale.US);

			// Create the filter entries for the filter dialog
			mFilterList = new String[28];
			mFilterList[0] = getContext().getString(R.string.menu_navigation_filter_top_200);
			mFilterList[1] = "#";
			for (int i = 0; i < 26; i++) {
				mFilterList[2 + i] = "" + (char) (('A') + i);
			}

			// Use the previous filter if available
			if (savedInstanceState != null) {
				mCurrentFilter = savedInstanceState.getInt(STATE_FILTER);
				mCache = savedInstanceState.getParcelableArrayList(STATE_CACHE);
			}
		}

		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			super.onSaveInstanceState(savedInstanceState);
			savedInstanceState.putInt(STATE_FILTER, mCurrentFilter);
			savedInstanceState.putParcelableArrayList(STATE_CACHE, mCache);
		}

		@Override
		public String[] getFilterEntries() {
			return mFilterList;
		}

		@Override
		public void onFilterSelected(int position) {
			if (mCurrentFilter != position) {
				mCurrentFilter = position;
				resetState();
				startLoading();
			}
		}

		@Override
		protected int getTotalPages(Document document) {
			return 0;
		}

		@Override
		protected Uri getUri(int currentPage) {
			// Load only from the web if the cache is empty
			return mCache == null ? mUri : null;
		}

		@Override
		protected boolean load(Document document, List<CategoryMenuItem> list) {

			if (mCache == null) {
				Elements fandoms = document.select("div#main > ol.fandom ul li");
				if (fandoms.isEmpty()) { return false; }
				mCache = new ArrayList<>(fandoms.size());

				for (Element fandom : fandoms) {
					if (fandom.childNodeSize() == 0) { return false; }

					// Remove non Latin characters from the beginning in order
					// to
					// improve section indexing
					String title = fandom.child(0).ownText();
					if (!CONSERVE_CHARACTERS.contains(Character.UnicodeBlock.of(title.charAt(0)))) {
						title = title.contains("| ") ? title.substring(title.lastIndexOf("| ") + 2) : title;
						title = Character.toUpperCase(title.charAt(0)) + title.substring(1);
					}

					String views = fandom.ownText();
					views = views.replaceAll("[\\D]", "");
					final int viewsAsInt = Integer.parseInt(views);
					views = mFormatter.format(viewsAsInt);
					views = String.format(mFormatString, views);

					Uri url = Uri.parse(fandom.child(0).absUrl("href"));

					CategoryMenuItem item = new CategoryMenuItem(title, views, url);
					mCache.add(item);
				}

			}

			list.addAll(mCache);
			filter(list);

			return true;
		}

		/**
		 * Applies the currently selected filter to the list, removing any
		 * entries from the list that should not be displayed.
		 * 
		 * @param list The input list that should be filtered
		 */
		private void filter(List<CategoryMenuItem> list) {
			// Top 200: Sort the items, then remove everything after 200.
			if (mCurrentFilter == 0 && list.size() > 200) {
				Collections.sort(list, COMPARATOR);
				list.subList(200, list.size()).clear();

				// Filter non-letters by removing every item that does starts
				// with a letter.
			} else if (mCurrentFilter == 1) {
				Iterator<CategoryMenuItem> iter = list.iterator();
				while (iter.hasNext()) {
					CategoryMenuItem item = iter.next();
					if (Character.isLetter(item.mTitle.charAt(0))) {
						iter.remove();
					}
				}
				// Use the collator to check if the first character of the two
				// string match. If not, remove.
			} else {
				Iterator<CategoryMenuItem> iter = list.iterator();
				while (iter.hasNext()) {
					CategoryMenuItem item = iter.next();
					Character char1 = item.mTitle.charAt(0);
					Character char2 = (char) ('a' + mCurrentFilter - 2);
					if (COLLATOR.compare(String.valueOf(char1), String.valueOf(char2)) != 0) {
						iter.remove();
					}
				}
			}
		}
	}
}
