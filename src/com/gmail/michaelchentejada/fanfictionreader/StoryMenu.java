package com.gmail.michaelchentejada.fanfictionreader;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.jsoup.nodes.Document;

import com.gmail.michaelchentejada.fanfictionreader.activity.StoryDisplayActivity;
import com.gmail.michaelchentejada.fanfictionreader.util.Parser;
import com.gmail.michaelchentejada.fanfictionreader.util.Story;
import com.gmail.michaelchentejada.fanfictionreader.util.StoryMenuAdapter;
import com.gmail.michaelchentejada.fanfictionreader.util.currentState;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class StoryMenu extends Menu<Story> {

	private int[] filter = {1,0,0,0,10,0,0,0,0,0,0,0,0,0};//Selected filter, default values applied.
	private ArrayList<LinkedHashMap<String, Integer>> filterList; //Filter elements
	private int[] selectedPositions = null; //Variable used to restore current filter.
	private final static String CURRENT_FILTER = "CurrentFilter";
	
	@Override
	protected void addHeaderView(ListView listview) {
		View header = (View)getLayoutInflater().inflate(R.layout.story_menu_header, null);
		listview.addHeaderView(header);		
	}

	@Override
	protected void listListener(int id) {
		Intent i = new Intent(context,StoryDisplayActivity.class);
		i.setData(Uri.parse("https://m.fanfiction.net/s/" + list.get(id).getId() + "/1/"));
		startActivity(i);		
	}

	@Override
	protected void listLongClickListener(int id) {
		Intent i = new Intent(context,DetailDisplay.class);
		i.putExtra(DetailDisplay.MAP,list.get(id));
		startActivity(i);
	}
	
	@Override
	protected void refreshList(ArrayList<Story> list) {
		super.refreshList(list);
	}
	
	/**
	 * Runs the filtering activity.
	 */
	private final OnClickListener filterListener = new OnClickListener() {	
		@Override
		public void onClick(View v) {
			Intent i = new Intent(context,FilterMenu.class);
			
			//Generate the ordered KeySet, as putExtra does not support ordered HashMaps
			ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(); 
			for (int j = 0; j < filterList.size(); j++) {
				keys.add(new ArrayList<String>(filterList.get(j).keySet()));
			}
			
			i.putExtra(FilterMenu.FILTER_LIST, filterList);//HashMap
			i.putExtra(FilterMenu.KEYSET, keys);//Ordered KeySet
			i.putExtra(FilterMenu.SELECTED_KEYS, selectedPositions);//Position selected on previous filter, may equal null 
			startActivityForResult(i, 1);		
		}
	};
	
	@Override
	protected ArrayList<Story> parsePage(Document document) {
		if (filterList==null) { //Load the filters if they aren't already loaded.
			filterList=Parser.Filter(document);
		}
		if (activityState == currentState.JUSTIN || activityState == currentState.COMMUNITIES){
			return Parser.Stories(document);//Parse the rest of the info
		}else{
			return Parser.Stories(document);//Parse the rest of the info
		}
	}

	@Override
	protected String formatUrl(String baseUrl) {
		switch (activityState) {
		case JUSTIN:
			return baseUrl + "?s=" + filter[12] + "&cid=" + filter[13] + "&l="+ filter[5];
		case NORMAL://Fall through
		case CROSSOVER:
			return baseUrl + "?srt=" + filter[0] + "&t=" + filter[1] + "&g1="
					+ filter[2] + "&g2=" + filter[3] + "&r=" + filter[4]
					+ "&lan=" + +filter[5] + "&len=" + filter[6] + "&s="
					+ filter[7] + "&c1=" + filter[8] + "&c2=" + filter[9]
					+ "&c3=" + filter[10] + "&c4=" + filter[11] + "&p="
					+ currentPage;
		case COMMUNITIES:
			return baseUrl + filter[4] + "/" + filter[0] + "/" + currentPage + "/"+ filter[2] + "/" + filter[6] + "/" + filter[7] +  "/" + filter[1] + "/";
		case AUTHOR: default:
			return baseUrl;
		}
	}

	@Override
	protected StoryMenuAdapter setAdapter() {
		return new StoryMenuAdapter(context, R.layout.story_menu_list_item, list);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Button filterButton = (Button)findViewById(R.id.story_menu_filter);
		filterButton.setOnClickListener(filterListener);
		
		if (savedInstanceState == null) {
			if (activityState == currentState.COMMUNITIES)	
					filter = new int[]{0,0,0,0,99,0,0,0,0,0,0,0,0,0};
			setList();;
		} else {
			filterList = (ArrayList<LinkedHashMap<String, Integer>>) savedInstanceState.getSerializable(FilterMenu.FILTER_LIST);
			filter = savedInstanceState.getIntArray(CURRENT_FILTER);
			list.addAll((ArrayList<Story>) savedInstanceState.getSerializable("List"));
			refreshList(list);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(FilterMenu.FILTER_LIST, filterList);
		outState.putIntArray(CURRENT_FILTER, filter);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode==1){//Filter Menu
			if (resultCode==RESULT_CANCELED) {
				//Dialog cancelled
				Toast toast = Toast.makeText(context, getResources().getString(R.string.dialog_cancelled), Toast.LENGTH_SHORT);
				toast.show();
			}else if (resultCode == RESULT_OK) {
				currentPage = 0;
				filter = data.getIntArrayExtra(FilterMenu.FILTER_LIST);
				selectedPositions = data.getIntArrayExtra(FilterMenu.SELECTED_KEYS);
				setList();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
