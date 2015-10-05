package com.spicymango.fanfictionreader.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.menu.BaseLoader;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;

public abstract class SearchLoader<T extends Parcelable> extends BaseLoader<T> {
	private static final Pattern pattern = Pattern
			.compile("(?:&ppage=)(\\d{1,4}+)");// Search
	private static final String STATE_FILTER = "filter";
	private static final String STATE_FILTER_LIST = "filter list";
	private static final String STATE_QUERY = "query";
	
	
	/**
	 * Gets the number of pages in the document
	 * 
	 * @param document
	 *            The parsed document
	 * @return The number of pages in the document
	 */
	private final static int getpageNumber(Document document) {
		Elements elements = document
				.select("div#content > form > div > a:matchesOwn(\\A(?i)last)");
		if (elements.isEmpty()) {
			if (document.select(
					"div#content > form > div  > a:matchesOwn(\\A(?i)next)")
					.isEmpty())
				return 1;
			return 2;
		}
		return getpageNumber(elements.first().attr("href"));

	}

	/**
	 * Gets the page number from the Url
	 * 
	 * @param url
	 *            The Url to parse
	 * @return The current page
	 */
	private final static int getpageNumber(String url) {
		Matcher matcher = pattern.matcher(url);
		matcher.find();
		for (int i = 1; i < matcher.groupCount() + 1; i++) {
			if (matcher.group(i) != null)
				return Integer.valueOf(matcher.group(i));
		}
		return 1;
	}

	public int[] filter;

	public ArrayList<LinkedHashMap<String, Integer>> mFilterList;

	public String mQuery;
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString(STATE_QUERY, mQuery);
		savedInstanceState.putIntArray(STATE_FILTER, filter);
		savedInstanceState.putSerializable(STATE_FILTER_LIST, mFilterList);
		super.onSaveInstanceState(savedInstanceState);
	}

	@SuppressWarnings("unchecked")
	public SearchLoader(Context context, Bundle savedInstanceState) {
		super(context, savedInstanceState);	
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_FILTER_LIST)) {
			filter = savedInstanceState.getIntArray(STATE_FILTER);
			mFilterList = (ArrayList<LinkedHashMap<String, Integer>>) savedInstanceState.getSerializable(STATE_FILTER_LIST);
			mQuery = savedInstanceState.getString(STATE_QUERY);
		}else{
			resetFilter();
		}
	}

	public void search(String query) {
		if (query != mQuery) {
			resetFilter();
			mFilterList = null;
		}
		mQuery = query;
		resetState();
		startLoading();
	}

	@Override
	protected int getTotalPages(Document document) {
		return getpageNumber(document);
	}
	
	/**
	 * Loads the filters for the search menu.
	 * @param document
	 * @return
	 */
	protected static ArrayList<LinkedHashMap<String, Integer>> SearchFilter(Document document){
		Elements form = document.select("div#content form > div#drop_m > select");
		
		Elements[] filter = {
				form.select("[name=sort result type] > option"),
				form.select("[title=time range options] > option"),
				form.select("[name=genreid] > option"),
				form.select("[title=genre 2 filter] > option"),
				form.select("[name=censorid] > option"),
				form.select("[name=languageid] > option"),
				form.select("[name=words] > option"),
				form.select("[name=statusid] > option"),
				form.select("[title=character 1 filter] > option"),
				form.select("[title=character 2 filter] > option"),
				form.select("[title=character 3 filter] > option"),
				form.select("[title=character 4 filter] > option"),
				form.select("[name=s]:not([title]) > option"),
				form.select("[name=categoryid] > option")};
		
		ArrayList<LinkedHashMap<String, Integer>> list = new ArrayList<LinkedHashMap<String,Integer>>();		
		LinkedHashMap<String, Integer> TempMap = new LinkedHashMap<String, Integer>();
		
		for (Elements j : filter) {
			for (Element k : j) {
				TempMap.put(k.ownText(), Integer.valueOf(k.attr("value")));
			}
			list.add(TempMap);
			TempMap = new LinkedHashMap<String,Integer>();
		}
		return list;
	}

	protected abstract void resetFilter();
}