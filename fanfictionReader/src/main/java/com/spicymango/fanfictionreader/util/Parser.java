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
			Element attributes = element.select("div.gray").first();
			Elements dates = element.select("span[data-xutime]");

			// Check that all the elements are valid. If at least one of them is invalid, return false
			if (title == null || author == null || attributes == null || dates.isEmpty())
				return false;
			
			storyIdMatcher.reset(title.attr("href"));
			authorIdMatcher.reset(author.attr("href"));
			
			if (!(storyIdMatcher.find() & authorIdMatcher.find()))
				return false;

			long updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
			long publishDate = Long.parseLong(dates.last().attr("data-xutime")) * 1000;

			Elements completeIcon = element.select("img.mm");
			boolean complete = !completeIcon.isEmpty();

			Elements reviewIcon = element.select("a > img.mt");
			final int reviews;
			if (reviewIcon.isEmpty()) {
				reviews = 0;
			} else {
				Element reviewLink = reviewIcon.first().parent();
				reviews = parseInt(reviewLink.ownText());
			}
			
			Story.Builder builder = new Story.Builder();
			builder.setId(Long.parseLong(storyIdMatcher.group(1)));
			builder.setName(title.ownText());
			builder.setAuthor(author.text());
			builder.setAuthorId(Long.parseLong(authorIdMatcher.group(1)));
			builder.setSummary(element.ownText().replaceFirst("(?i)by\\s*", ""));
			builder.setFanFicAttributes(attributes.text());
			builder.setUpdateDate(updateDate);
			builder.setPublishDate(publishDate);
			builder.setCompleted(complete);
			builder.setReviews(reviews);

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
	public static int getPageNumber(Document document){
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
	private static int getPageNumber(String url){
		Matcher matcher = pattern2.matcher(url);
		if (matcher.find()) {
			for (int i = 1; i < matcher.groupCount(); i++) {
				if (matcher.group(i) != null)
					return Integer.valueOf(matcher.group(i));
			}
		}
		return 1;
	}

	/**
	 * Parses an integer into a string. This implementation will recognize when the character 'k' is
	 * used to denote thousands.
	 *
	 * @param string The string that should be parsed
	 * @return The integer representation of the string.
	 */
	public static int parseInt(String string){
		final String tmp = string.replaceAll("[^\\d.]", "");
		if (tmp.length() == 0) {
			return 0;
		} else {
			double digits = Double.parseDouble(tmp);
			if (string.contains("k") || string.contains("K")) {
				digits = digits * 1000;
			}
			return (int) digits;
		}
	}
	
	public static String withSuffix(int count) {
	    if (count < 1000) return "" + count;
	    return count/1000 + "k+";
	}
	
	public static List<Spanned> split(Spanned spanned){
		String[] list = spanned.toString().split("\n");
		
		int i = 0;
		List<Spanned> result = new ArrayList<>(list.length);

		for (String paragraph : list) {
			SpannableString styledParagraph = new SpannableString(paragraph);
			TextUtils.copySpansFrom(spanned, i, i + paragraph.length(), null, styledParagraph, 0);
			i += paragraph.length() + 1;
			result.add(styledParagraph);
		}
		return result;
	}
}
