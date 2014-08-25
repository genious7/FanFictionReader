package com.spicymango.fanfictionreader.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;



public class Parser {
	
	private static final  Pattern pattern = Pattern.compile("/[su]/([\\d]++)/");
		
	/**
	 * Parses the stories into a list
	 * @param document The web page to parse
	 * @param list The list to add the stories to
	 * @return True if the operation succeeded, false otherwise
	 */
	public static boolean Stories(Document document, List<Story> list) {
		
		Elements summaries = document.select("div#content div.bs");
		
		Matcher storyIdMatcher = pattern.matcher("");
		Matcher authorIdMatcher = pattern.matcher("");
		
		for (Element element : summaries) {
			element.select("b").unwrap();
			Element title = element.select("a[href~=(?i)/s/\\d+/1/.*]").first();
			Element author = element.select("a[href^=/u/]").first();
			Element attribs = element.select("div.gray").first();
			Elements dates = element.select("span[data-xutime]");
			
			storyIdMatcher.reset(title.attr("href"));
			authorIdMatcher.reset(author.attr("href"));
			
			storyIdMatcher.find();
			authorIdMatcher.find();
				
			long updateDate = 0;
			long publishDate = 0;

			updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
			publishDate = Long.parseLong(dates.last().attr("data-xutime")) * 1000;
			
			boolean complete;	
			Elements imgs = element.select("img.mm");
			complete = !imgs.isEmpty();
			
			Story TempStory = new Story(
					Long.parseLong(storyIdMatcher.group(1)), title.ownText(),
					author.text(), Long.parseLong(authorIdMatcher.group(1)),
					element.ownText().replaceFirst("(?i)by\\s*", ""),
					attribs.text(), updateDate, publishDate, complete);

			list.add(TempStory);
		}
		return true;
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
	
	public final static int parseInt(String string){
		if (string.length() == 0) {
			return 0;
		} else {
			double digits = Double.parseDouble(string.replaceAll("[^\\d.]", ""));
			if (string.contains("k") || string.contains("K")) {
				digits = digits * 1000;
			}
			return (int) digits;
		}
	}
	
	public static final String withSuffix(int count) {
	    if (count < 1000) return "" + count;
	    return count/1000 + "k+";
	}
	
	public static final List<Spanned> split(Spanned spanned){
		String[] list = spanned.toString().split("\n");
		
		int i = 0;
		List<Spanned> result = new ArrayList<Spanned>(list.length);
		
		for (int j = 0; j < list.length; j++) {
			SpannableString line = new SpannableString(list[j]);
			TextUtils.copySpansFrom(spanned, i, i + list[j].length(), null, line, 0);
			i += list[j].length() + 1;
			result.add(line);
		}
		return result;
	}
}
