package com.gmail.michaelchentejada.fanfictionreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.jsoup.Jsoup;
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
	
	/**
	 * Obtains the list of sub-categories inside one of the main categories.
	 * @author Michael Chen
	 * @param url The url of the selected category
	 * @return A hashmap containing target url's, titles, and views if the Internet connection is valid, null otherwise
	 */
	public static ArrayList<HashMap<String, String>> Categories(String url, Document document){	

		Elements titles = document.select("div#content > div.bs > a");
		
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String,String>>();
		
		for (int i = 0; i < titles.size(); i++) {
			HashMap<String, String> TempMap = new HashMap<String, String>();
			TempMap.put(TITLE, titles.get(i).ownText());
			TempMap.put(URL, titles.get(i).attr("href"));

			String n_views = titles.get(i).child(0).ownText().replaceAll("[() ]", "");
			TempMap.put(VIEWS,n_views + " Stories");
			
			n_views = (n_views.contains("K")||n_views.contains("k"))? Integer.toString((int)(Double.parseDouble(n_views.replaceAll("[^\\d[.]]", ""))*1000)) :n_views.replaceAll("[^\\d[.]]", "");
			TempMap.put(VIEWS_INT, n_views );
			list.add(TempMap);
		}
		return list;

	}
	
	public static ArrayList<HashMap<String, String>> Stories(String url, Document document, boolean justIn){	
		Elements summaries = document.select("div#content > div.bs");
		Elements titles = summaries.select("a[href~=(?i)/s/\\d+/1/.*]");
		Elements authors = summaries.select("a[href^=/u/]");
		
		Elements attribs = summaries.select("div.gray");
		
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String,String>>();
		
		for (int i = 0; i < titles.size(); i++) {
			HashMap<String, String> TempMap = new HashMap<String, String>();
			
			TempMap.put(TITLE, titles.get(i).ownText());
			
			TempMap.put(URL, titles.get(i).attr("href").replaceAll("(?<=/s/\\d{1,10}/).+", ""));
			
			TempMap.put(AUTHOR, authors.get(i).ownText());
			
			TempMap.put(AUTHOR_URL, authors.get(i).attr("href"));
			
			TempMap.put(SUMMARY, summaries.get(i).ownText().replaceFirst("(?i)by\\s*", ""));

			String attrib[] = attribs.get(i).text().split("\\s*,\\s*");
			
			int j = 0;
			
			if (attrib[j].contains("&")) {
				TempMap.put(CROSSOVER,attrib[j++]);
				TempMap.put(CATEGORY,"0");
			}else{
				TempMap.put(CROSSOVER,"0");
				if (justIn) {
					TempMap.put(CATEGORY,attrib[j++]);
				}else{
					TempMap.put(CATEGORY,"0");
				}		
			}
			TempMap.put(RATING, attrib[j++]);
			TempMap.put(LANGUAGUE,attrib[j++]);
			
			if (!(attrib[j].contains("chapter")||attrib[j].contains("words"))) {
				TempMap.put(GENRE,attrib[j++]);
			}else{
				TempMap.put(GENRE,"0");
			}
			
			if (attrib[j].contains("chapter")) {
				TempMap.put(CHAPTER,attrib[j++].replaceFirst("(?i)chapters:\\s*", ""));
			}else{
				TempMap.put(CHAPTER,"1");
			}
			
			TempMap.put(LENGHT,attrib[j++].replaceFirst("(?i)words:\\s*", ""));
			
			if (attrib[j].contains("favs")) {
				TempMap.put(FAVORITES,attrib[j++].replaceFirst("(?i)favs:\\s*", ""));
			}else{
				TempMap.put(FAVORITES,"0");
			}
			if (attrib[j].contains("follows")) {
				TempMap.put(FOLLOWS,attrib[j++].replaceFirst("(?i)follows:\\s*", ""));
			}else{
				TempMap.put(FOLLOWS,"0");
			}
			if (attrib[j].contains("updated")) {
				attrib = attrib[j].replaceFirst("(?i)updated:\\s*", "").split("(?i)published:\\s*");
				TempMap.put(UPDATED,attrib[0]);
				TempMap.put(PUBLISHED,attrib[1]);
			}else{
				TempMap.put(UPDATED,attrib[j].replaceFirst("\\s*", ""));
				TempMap.put(PUBLISHED,attrib[j].replaceFirst("\\s*", ""));
			}
			list.add(TempMap);
		}
		return list;
	}
	
	public static ArrayList<LinkedHashMap<String, Integer>> Filter(String url, Document document){
		Elements form = document.select("form#myform > select");
		Elements form2 = document.select("form#justin > select");
		
		Elements[] filter = {
				form.select("[title=sort options] > option"),
				form.select("[title=time range options] > option"),
				form.select("[title=genre 1 filter] > option"),
				form.select("[title=genre 2 filter] > option"),
				form.select("[title=rating filter] > option"),
				form.select("[title=language filter] > option"),
				form.select("[title=length in words filter] > option"),
				form.select("[title=story status] > option"),
				form.select("[title=character 1 filter] > option"),
				form.select("[title=character 2 filter] > option"),
				form.select("[title=character 3 filter] > option"),
				form.select("[title=character 4 filter] > option"),
				form2.select("[name=s] > option"),
				form2.select("[title=Filter by Category] > option"),
				form2.select("[name=l] > option"),};
		
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
	 * @param url The url of the fanfiction page.
	 * @param document The document representing the page.
	 * @return The total number of pages.
	 */
	public static int Pages(String url, Document document){
		try {
			Elements number = document.select("div#content > center > a:contains(last)");
			if (number.size() < 1) {
				return 1; //For searches with only one page.
			}
			String text = number.first().attr("href");
			return Integer.valueOf(text.replaceAll(".+&p=", ""));
		} catch (NumberFormatException e) {
			Log.e("Parser - Number of pages", Log.getStackTraceString(e));
			return 1;		
		}
	}
	
	public static String storyHTML(String url) throws IOException{
		org.jsoup.nodes.Document document = Jsoup.connect(url).get();
		Elements titles = document.select("div#storycontent");
		return titles.html();
	}
	
	public static String crossoverUrl (Document document){
		Elements url = document.select("div#content > center > a");
		return url.first().attr("href");
	}
}
