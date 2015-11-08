package com.spicymango.fanfictionreader.menu.communitymenu;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.BaseLoader;
import com.spicymango.fanfictionreader.menu.communitymenu.CommunityMenuActivity.CommunityMenuFragment.Sortable;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.util.Log;

final class CommunityMenuLoaders {
	final static class FanFictionCommunityLoader extends BaseLoader<CommunityMenuItem> implements Sortable{
		private final static String STATE_SORT = "STATE SORT";
		private SortBy mSort;
		private final Uri mUri;
		
		public FanFictionCommunityLoader(Context context, Bundle savedInstanceState, Uri uri) {
			super(context, savedInstanceState);
			mUri = uri;
			
			if (savedInstanceState == null) {
				mSort = SortBy.FOLLOWS;
			}else{
				mSort = (SortBy) savedInstanceState.getSerializable(STATE_SORT);
			}
		}
		
		@Override
		protected int getTotalPages(Document document) {
			Elements pageCounters = document.select("div#content center a");
			Element lastLink = pageCounters.select("a:contains(last)").last();
			Element nextLink = pageCounters.select("a:contains(next)").last();

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

		@Override
		protected Uri getUri(int currentPage) {
			Builder uri = mUri.buildUpon();
			uri.appendPath("0");
			uri.appendPath(mSort.getId() + "");
			uri.appendPath(currentPage + "");
			uri.appendPath("");
			return uri.build();
		}

		@Override
		protected boolean load(Document document, List<CommunityMenuItem> list) {
			final Elements communities = document.select("div#content > div.bs");
			final String counterFormat = getContext().getString(R.string.menu_navigation_count_story);
			final DateFormat dateFormat = new SimpleDateFormat("MM-dd-yy", Locale.US);

			for (Element community : communities) {
				// Get the title and the URL
				final Element titleElement = community.select("a").first();
				if (titleElement == null) return false;
				String title = titleElement.ownText();
				Uri url = Uri.parse(titleElement.absUrl("href"));

				// Get the formatted number of views
				final Element viewElement = titleElement.child(0);
				if (viewElement == null) return false;
				String nViews = titleElement.child(0).ownText();
				nViews = String.format(counterFormat, nViews);

				// Get the description
				final Element descElement = community.select("div.z-padtop").first();
				if (descElement == null) return false;
				String desc = descElement.ownText();

				// Get the attributes
				final Element attribElement = descElement.child(0);
				if (attribElement == null) return false;
				// Split the attributes on the dash, removing whitespace at
				// either side
				String[] attribs = attribElement.ownText().split("\\s+-\\s+");

				// Parse the attributes
				String languague = attribs[0];
				// No need to check for NumberFormatException, regex ensures
				// only digits get through
				int staff = Integer.parseInt(attribs[1].replaceAll("[\\D]", ""));
				int follows = Integer.parseInt(attribs[2].replaceAll("[\\D]", ""));

				Date date;
				try {
					date = dateFormat.parse(attribs[3].replaceAll("(?i)since:\\s*", ""));
				} catch (ParseException e) {
					Log.e("FanFicCommunityLoader", e.getMessage());
					return false;
				}

				String author = attribs[4].replaceAll("(?i)founder:\\s*", "");
				list.add(new CommunityMenuItem(title, url, author, desc, nViews, languague, staff, follows, date));
			}
			return true;
		}

		@Override
		protected void onSaveInstanceState(Bundle savedInstanceState) {
			savedInstanceState.putSerializable(STATE_SORT, mSort);
		}

		@Override
		public void sort(SortBy sortKey) {
			if (!mSort.equals(sortKey)) {
				mSort = sortKey;
				resetState();
				startLoading();
			}
		}

	}
}
