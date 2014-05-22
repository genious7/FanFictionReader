package com.spicymango.fanfictionreader.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;



public class Parser {
	
	public static final String TITLE = "Title";
	public static final String URL = "Url";
	public static final String VIEWS = "Views";
	public static final String VIEWS_INT = "Views Int";
	
	public static final String AUTHOR = "Author";
	public static final String AUTHOR_URL = "Author Url";
	public static final String SUMMARY = "Summary";
	public static final String CROSSOVER = "Crossover Category";
	public static final String CATEGORY = "Category";
	public static final String RATING = "Rating";
	public static final String LANGUAGUE = "Languague";
	public static final String GENRE = "Genre";	
	public static final String CHAPTER = "Chapter";
	public static final String LENGHT = "Lenght";
	public static final String FAVORITES = "Favorites";
	public static final String FOLLOWS = "Follows";
	public static final String UPDATED = "Updated";
	public static final String PUBLISHED = "Published";
	public static final String PAGES = "Pages";
	public static final String STAFF = "Staff";
	
	/**
	 * Obtains the list of sub-categories inside one of the main categories.
	 * @author Michael Chen
	 * @param storyString Story Resource
	 * @return A hashmap containing target url's, titles, and views if the Internet connection is valid, null otherwise
	 */
	public static ArrayList<HashMap<String, String>> Categories(String storyString, Document document){	

		Elements titles = document.select("div#content > div.bs > a");
		
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String,String>>();
		
		for (int i = 0; i < titles.size(); i++) {
			HashMap<String, String> TempMap = new HashMap<String, String>();
			TempMap.put(TITLE, titles.get(i).ownText());
			TempMap.put(URL, titles.get(i).attr("href"));

			String n_views = titles.get(i).child(0).ownText().replaceAll("[() ]", "");
			TempMap.put(VIEWS,String.format(storyString, n_views));
			
			n_views = (n_views.contains("K")||n_views.contains("k"))? Integer.toString((int)(Double.parseDouble(n_views.replaceAll("[^\\d[.]]", ""))*1000)) :n_views.replaceAll("[^\\d[.]]", "");
			TempMap.put(VIEWS_INT, n_views );
			list.add(TempMap);
		}
		return list;

	}
	
	public static ArrayList<HashMap<String, String>> Communities(String StoryResource, Document document){	

		Elements base = document.select("div#content > div.bs");
		Elements title = base.select("a");
		Elements summary = base.select("div.z-padtop");
		
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String,String>>();
		
		for (int i = 0; i < base.size(); i++) {
			HashMap<String, String> TempMap = new HashMap<String, String>();
			TempMap.put(TITLE, title.get(i).ownText());
			TempMap.put(URL, title.get(i).attr("href"));

			String n_views = title.get(i).child(0).ownText().replaceAll("[() ]", "");
			TempMap.put(VIEWS,String.format(StoryResource, n_views));
			
			TempMap.put(SUMMARY,summary.get(i).ownText());
			
			String attrib[] = summary.get(i).child(0).ownText().toString().split("\\s+-\\s+");
			TempMap.put(LANGUAGUE,attrib[0]);
			TempMap.put(STAFF,attrib[1].replaceAll("(?i)staff:\\s*", ""));
			TempMap.put(FOLLOWS,attrib[2].replaceAll("(?i)followers:\\s*", ""));
			TempMap.put(PUBLISHED,attrib[3].replaceAll("(?i)since:\\s*", ""));
			TempMap.put(AUTHOR,attrib[4].replaceAll("(?i)founder:\\s*", ""));
			
			list.add(TempMap);
		}
		return list;

	}
	
	
	private static final  Pattern pattern = Pattern.compile(
			"/s/([\\d]++)/");
	
	public static ArrayList<Story> Stories(Document document) {

		Elements summaries = document.select("div#content div.bs");
		summaries.select("b").unwrap();
		
		Elements titles = summaries.select("a[href~=(?i)/s/\\d+/1/.*]");
		Elements authors = summaries.select("a[href^=/u/]");

		Elements attribs = summaries.select("div.gray");

		ArrayList<Story> list = new ArrayList<Story>();
		Matcher matcher = pattern.matcher("");

		for (int i = 0; i < titles.size(); i++) {
			matcher.reset(titles.get(i).attr("href"));
			matcher.find();

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
			
			

			Story TempStory = new Story(Integer.parseInt(matcher.group(1)),
					titles.get(i).ownText(), authors.get(i).text(), 0,
					summaries.get(i).ownText().replaceFirst("(?i)by\\s*", ""),
					attribs.get(i).ownText(), updateDate, publishDate);

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
	
	public static ArrayList<LinkedHashMap<String, Integer>> SearchFilter(Document document){
		Elements form = document.select("div#content form > div#drop_m > select");
		
		Elements[] filter = {
				form.select("[title=sort options] > option"),
				form.select("[title=time range options] > option"),
				form.select("[name=genreid] > option"),
				form.select("[title=genre 2 filter] > option"),
				form.select("[name=censorid] > option"),
				form.select("[title=language filter] > option"),
				form.select("[name=words] > option"),
				form.select("[name=statusid] > option"),
				form.select("[title=character 1 filter] > option"),
				form.select("[title=character 2 filter] > option"),
				form.select("[title=character 3 filter] > option"),
				form.select("[title=character 4 filter] > option"),
				form.select("[name=s]:not([title]) > option"),
				form.select("[name=categoryid] > option"),
				form.select("[name=l] > option"),};
		
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
	
	public static int PagesSearch(Document document){
		try {
			Elements number = document.select("div#content form div a:contains(last)");
			if (number.size() < 1) {
				if (document.select("div#content form div a:contains(next)").isEmpty()) {
					return 1; //For searches with only one page.
				}else{
					return 2;
				}
			}
			String text = number.first().attr("href");
			return Integer.valueOf(text.replaceAll(".+&ppage=|(?<=\\d{0,4})&.*", ""));
		} catch (NumberFormatException e) {
			Log.e("Parser - PagesCommunity", Log.getStackTraceString(e));
			return 1;		
		}
	}
	
}
