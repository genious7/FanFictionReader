package com.spicymango.fanfictionreader.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;

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
			
			Story.Builder builder = new Story.Builder();
			builder.setId(Long.parseLong(storyIdMatcher.group(1)));
			builder.setName(title.ownText());
			builder.setAuthor(author.text());
			builder.setAuthorId(Long.parseLong(authorIdMatcher.group(1)));
			builder.setSummary(element.ownText().replaceFirst("(?i)by\\s*", ""));
			builder.setFanFicAttributes(attribs.text());
			builder.setUpdateDate(updateDate);
			builder.setPublishDate(publishDate);
			builder.setCompleted(complete);

			list.add(builder.build());
		}
		return true;
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
	public final static int getPageNumber(Document document){
		Elements elements = document.select("div#content a:matchesOwn(\\A(?i)last\\Z)");
		if (elements.isEmpty()){
			if (document.select("div#content a:matchesOwn(\\A(?i)next)").isEmpty())
				return 1;
			return 2;
		}
		return getPageNumber(elements.last().attr("href"));
		
	}
	
	/**
	 * Gets the page number in the url
	 * @param url The url to parse
	 * @return The current page
	 */
	private final static int getPageNumber(String url){
		Matcher matcher = pattern2.matcher(url);
		if (matcher.find()) {
			for (int i = 1; i < matcher.groupCount(); i++) {
				if (matcher.group(i) != null)
					return Integer.valueOf(matcher.group(i));
			}
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
