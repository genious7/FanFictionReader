package com.gmail.michaelchentejada.fanfictionreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import android.util.Log;

public class Parser {
	
	public static final String TITLE = "Title";
	public static final String VIEWS = "Views";
	public static final String VIEWS_INT = "Views Int";
	public static final String URL = "Url";
	public static final String SUMMARY = "Summary";
	public static final String FOLLOWS = "Follows";
	public static final String AUTHOR = "Author";
	public static final String LENGHT = "Lenght";
	
	/**
	 * Obtains the list of sub-categories inside one of the main categories.
	 * @author Michael Chen
	 * @param url The url of the selected category
	 * @return A hashmap containing target url's, titles, and views if the Internet connection is valid, null otherwise
	 */
	public static ArrayList<HashMap<String, String>> Categories(String url){	
		try {
			org.jsoup.nodes.Document document = Jsoup.connect(url).get();
			Elements titles = document.select("#list_output div > a");
			Elements views = document.select("#list_output div > span");
			
			ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String,String>>();
			
			for (int i = 0; i < (titles.size()<views.size()?titles.size():views.size()); i++) {
				HashMap<String, String> TempMap = new HashMap<String, String>();
				TempMap.put(TITLE, titles.get(i).ownText());
				TempMap.put(URL, titles.get(i).attr("href"));
	
				String n_views = views.get(i).ownText().replaceAll("[() ]", "");
				TempMap.put(VIEWS,n_views + " Stories");
				
				n_views = (views.get(i).ownText().contains("K")||views.get(i).ownText().contains("k"))? Integer.toString((int)(Double.parseDouble(n_views.replaceAll("[^\\d[.]]", ""))*1000)) :n_views.replaceAll("[^\\d[.]]", "");
				TempMap.put(VIEWS_INT, n_views );
				list.add(TempMap);
			}
			return list;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	public static ArrayList<HashMap<String, String>> Stories(String url){	
		try {
			org.jsoup.nodes.Document document = Jsoup.connect(url).get();
			Elements titles = document.select("div.z-list  > a.stitle");
			Elements authors = document.select("div.z-list  > a[href^=/u/]");
			Elements summaries = document.select("div.z-list  > div.z-padtop");
			Elements attribs = document.select("div.z-list > div.z-padtop > div.z-padtop2");
			
			ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String,String>>();
			
			for (int i = 0; i < titles.size(); i++) {
				HashMap<String, String> TempMap = new HashMap<String, String>();
				TempMap.put(TITLE, titles.get(i).ownText());
				TempMap.put(URL, titles.get(i).attr("href"));
				
				Log.d("Parse", titles.get(i).ownText());
				Log.d("Parse", authors.get(i).ownText());
				Log.d("Parse", summaries.get(i).ownText());
				Log.d("Parse", attribs.get(i).ownText());
				
				list.add(TempMap);
			}
			return list;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}
