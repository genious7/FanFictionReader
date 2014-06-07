package com.spicymango.fanfictionreader.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;



public class Parser {
	
	private static final  Pattern pattern = Pattern.compile("/[su]/([\\d]++)/");
	
	
	/**
	 * Parses the stories into a list
	 * @param document The web page to parse
	 * @return The list of stories
	 */
	public static List<Story> Stories(Document document) {

		Elements summaries = document.select("div#content div.bs");
		Elements titles = summaries.select("a[href~=(?i)/s/\\d+/1/.*]");
		Elements authors = summaries.select("a[href^=/u/]");
		Elements attribs = summaries.select("div.gray");
		summaries.select("b").unwrap();

		List<Story> list = new ArrayList<Story>(summaries.size());
		Matcher storyIdMatcher = pattern.matcher("");
		Matcher authorIdMatcher = pattern.matcher("");

		for (int i = 0; i < summaries.size(); i++) {
			storyIdMatcher.reset(titles.get(i).attr("href"));
			authorIdMatcher.reset(authors.get(i).attr("href"));
			storyIdMatcher.find();
			authorIdMatcher.find();

			Elements dates = summaries.get(i).select("span[data-xutime]");
			long updateDate = 0;
			long publishDate = 0;

			if (dates.size() == 1) {
				updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
				publishDate = updateDate;
			} else if (dates.size() == 2) {
				updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
				publishDate = Long.parseLong(dates.last().attr("data-xutime")) * 1000;
			}

			Story TempStory = new Story(
					Long.parseLong(storyIdMatcher.group(1)), titles.get(i)
							.ownText(), authors.get(i).text(),
					Long.parseLong(authorIdMatcher.group(1)), summaries.get(i)
							.ownText().replaceFirst("(?i)by\\s*", ""), attribs
							.get(i).ownText(), updateDate, publishDate);

			list.add(TempStory);
		}
		return list;
	}
		
	
	public static ArrayList<LinkedHashMap<String, Integer>> Filter(Document document){
		Elements form = document.select("div#content div#d_menu form > select");
		
		Elements[] filter = {
				form.select("[title=sort options] > option"),
				form.select("[title=time range options] > option"),
				form.select("[title=genre 1 filter] > option,[title=genre filter] > option"),
				form.select("[title=genre 2 filter] > option"),
				form.select("[title=rating filter] > option"),
				form.select("[title=language filter] > option,[name=l] > option"),
				form.select("[title=length in words filter] > option"),
				form.select("[title=story status] > option"),
				form.select("[title=character 1 filter] > option"),
				form.select("[title=character 2 filter] > option"),
				form.select("[title=character 3 filter] > option"),
				form.select("[title=character 4 filter] > option"),
				form.select("[name=s]:not([title]) > option"),
				form.select("[title=Filter by Category] > option")
				};
		
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
		
	/**
	 * Total number of pages in the story.
	 * @param url The Url of the fanfiction page.
	 * @param document The document representing the page.
	 * @return The total number of pages.
	 */	
	public static int Pages(Document document){
		try {
			Elements number = document.select("div#content center a:contains(last)");
			if (number.size() < 1) {
				if (document.select("div#content center a:contains(next)").isEmpty()) {
					return 1; //For searches with only one page.
				}else{
					return 2;
				}
			}
			String text = number.first().attr("href");
			return Integer.valueOf(text.replaceAll("\\A(/[^/]*){4}/(?=\\d+)|/", ""));
		} catch (NumberFormatException e) {
			Log.e("Parser - PagesCommunity", Log.getStackTraceString(e));
			return 1;		
		}
	}
	
	private static final  Pattern pattern2 = Pattern.compile(
			"(?:&p=)(\\d{1,4}+)"//Normal
			+ "|(?:communit[^/]*+/(?:[^/]*+/){4})(\\d{1,4}+)"//Communities
			+ "|(?:&ppage=)(\\d{1,4}+)");//Search
	
	/**
	 * Gets the number of pages in the document
	 * @param document The parsed document
	 * @return The number of pages in the document
	 */
	public final static int getpageNumber(Document document){
		Elements elements = document.select("div#content a:matchesOwn(\\A(?i)last\\Z)");
		if (elements.isEmpty()){
			if (document.select("div#content a:matchesOwn(\\A(?i)next)").isEmpty())
				return 1;
			return 2;
		}
		return getpageNumber(elements.first().attr("href"));
		
	}
	
	/**
	 * Gets the page number in the url
	 * @param url The url to parse
	 * @return The current page
	 */
	private final static int getpageNumber(String url){
		Matcher matcher = pattern2.matcher(url);
		matcher.find();
		for (int i = 1; i < matcher.groupCount(); i++) {
			if (matcher.group(i) != null)
				return Integer.valueOf(matcher.group(i));
		}
		return 1;
	}

	
}
