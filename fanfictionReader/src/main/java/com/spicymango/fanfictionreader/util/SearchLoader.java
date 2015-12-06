package com.spicymango.fanfictionreader.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.menu.BaseLoader;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.SpinnerData;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

public abstract class SearchLoader<T extends Parcelable> extends BaseLoader<T> {
	private static final Pattern pattern = Pattern
			.compile("(?:&ppage=)(\\d{1,4}+)");// Search
	private static final String STATE_FILTER = "filter";
	private static final String STATE_FILTER_LIST = "filter list";
	private static final String STATE_QUERY = "query";


	/**
	 * Gets the number of pages in the document
	 *
	 * @param document The parsed document
	 * @return The number of pages in the document
	 */
	protected int getTotalPages(Document document) {
		Elements elements = document
				.select("div#content > form > div > a:matchesOwn(\\A(?i)last)");
		if (elements.isEmpty()) {
			if (document.select(
					"div#content > form > div  > a:matchesOwn(\\A(?i)next)")
					.isEmpty())
				return 1;
			return 2;
		}
		return getPageNumber(elements.first().attr("href"));

	}

	/**
	 * Gets the page number from the Url
	 * 
	 * @param url
	 *            The Url to parse
	 * @return The current page
	 */
	private static int getPageNumber(String url) {
		Matcher matcher = pattern.matcher(url);
		if (matcher.find()){
			for (int i = 1; i < matcher.groupCount() + 1; i++) {
				if (matcher.group(i) != null)
					return Integer.valueOf(matcher.group(i));
			}
		}
		return 1;
	}

	public int[] filter;

	public ArrayList<SpinnerData> mFilterData;
	public String mQuery;
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString(STATE_QUERY, mQuery);
		savedInstanceState.putIntArray(STATE_FILTER, filter);
		savedInstanceState.putParcelableArrayList(STATE_FILTER_LIST, mFilterData);
		super.onSaveInstanceState(savedInstanceState);
	}

	public SearchLoader(Context context, Bundle savedInstanceState) {
		super(context, savedInstanceState);	
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_FILTER_LIST)) {
			filter = savedInstanceState.getIntArray(STATE_FILTER);
			mFilterData = savedInstanceState.getParcelableArrayList(STATE_FILTER_LIST);
			mQuery = savedInstanceState.getString(STATE_QUERY);
		}else{
			resetFilter();
		}
	}

	public void search(String query) {
		if (query.equals(mQuery)) {
			// If the query matches the old query, do nothing.
		} else if (TextUtils.isEmpty(query)) {
			// If submitting an empty query, don't remove existing contents from the screen.
		} else {
			// Reset the filter if a different query is being used.
			resetFilter();
			mFilterData = null;

			// Start the loader
			mQuery = query;
			resetState();
			startLoading();
		}
	}

	/**
	 * Loads the filters for the search menu.
	 * @param document The document containing the web site
	 * @return A list of the filter spinner data
	 */
	protected static ArrayList<SpinnerData> SearchFilter(Document document){
		Elements form = document.select("div#content form > div#drop_m > select");
		
		Elements[] filter = {
				form.select("[name=s]:not([title])"),
				form.select("[name=categoryid]"),
				form.select("[name=sort result type]"),
				form.select("[title=time range options]"),
				form.select("[name=genreid]"),
				form.select("[title=genre 2 filter]"),
				form.select("[name=censorid]"),
				form.select("[name=languageid]"),
				form.select("[name=words]"),
				form.select("[name=statusid]"),
				form.select("[title=character 1 filter]"),
				form.select("[title=character 2 filter]"),
				form.select("[title=character 3 filter]"),
				form.select("[title=character 4 filter]")};
		
		ArrayList<SpinnerData> spinnerData = new ArrayList<>();
		
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
			spinnerData.add(new SpinnerData(name, label, filterKey, 0));
		}
		return spinnerData;
	}

	protected abstract void resetFilter();
}