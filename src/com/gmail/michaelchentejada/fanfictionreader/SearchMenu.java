package com.gmail.michaelchentejada.fanfictionreader;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.gmail.michaelchentejada.fanfictionreader.activity.StoryDisplayActivity;
import com.gmail.michaelchentejada.fanfictionreader.util.Parser;
import com.gmail.michaelchentejada.fanfictionreader.util.Story;
import com.gmail.michaelchentejada.fanfictionreader.util.StoryMenuAdapter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;


public class SearchMenu extends Menu<Story> {

	private ArrayList<LinkedHashMap<String, Integer>> filterList; //Filter elements
	private int[] filter = {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0};//Selected filter, default values applied.
	private int[] selectedPositions = null; //Variable used to restore current filter.
	private EditText textBox;
	private final OnClickListener filterListener = new OnClickListener() {	
		@Override
		public void onClick(View v) {
			Intent i = new Intent(context,FilterMenu.class);
			
			//Generate the ordered KeySet, as putExtra does not support ordered HashMaps
			ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(); 
			for (int j = 0; j < filterList.size(); j++) {
				keys.add(new ArrayList<String>(filterList.get(j).keySet()));
			}
			
			i.putExtra(FilterMenu.FILTER_LIST , filterList);//HashMap
			i.putExtra(FilterMenu.KEYSET, keys);//Ordered KeySet
			i.putExtra(FilterMenu.SELECTED_KEYS, selectedPositions);//Position selected on previous filter, may equal null 
			startActivityForResult(i, 1);		
		}
	};
	
	private OnEditorActionListener textChange = new OnEditorActionListener() {		
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			setList();
			return false;
		}
	};
		
	@Override
	protected void addHeaderView(ListView listview) {
		View header = (View)getLayoutInflater().inflate(R.layout.search_menu_header, null);
		listview.addHeaderView(header);	
	}

	@Override
	protected void listListener(int id) {
		Intent i = new Intent(context,StoryDisplayActivity.class);
		i.setData(Uri.parse("https://m.fanfiction.net/s/" + list.get(id).getId() + "/1/"));
		startActivity(i);		
	}

	@Override
	protected ArrayList<Story> parsePage(Document document) {
		Elements bold = document.select("b");
		for (Element element : bold) {
			element.unwrap();
		}
		
		if (filterList==null) { //Load the filters if they aren't already loaded.
			filterList=Parser.SearchFilter(document);
		}
		
		return Parser.Stories(document);//Parse the rest of the info
	}

	@Override
	protected String formatUrl(String baseUrl) {
		return 	"https://m.fanfiction.net/search.php?type="+"story"+"&ready="+"1"+"&keywords="+textBox.getText().toString()
				+"&categoryid="+filter[13]+"&genreid="+filter[2]+"&languageid="+filter[5]+"&censorid=0"+filter[4]
				+"&statusid="+filter[7]+"&ppage="+currentPage+"&words="+filter[6]; 
	}

	@Override
	protected BaseAdapter setAdapter() {
		return new StoryMenuAdapter(context, R.layout.story_menu_list_item, list);
	}
	
	@Override
	protected void listLongClickListener(int id) {
		
		Intent i = new Intent(context,DetailDisplay.class);
		i.putExtra(DetailDisplay.MAP,list.get(id));
		startActivity(i);
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		textBox = (EditText)findViewById(R.id.searchText);
		textBox.setOnEditorActionListener(textChange);
		
		Button filterButton = (Button)findViewById(R.id.filter);
		filterButton.setOnClickListener(filterListener);
		
		if (savedInstanceState == null) {
		} else {
			filterList = (ArrayList<LinkedHashMap<String, Integer>>) savedInstanceState.getSerializable(FilterMenu.FILTER_LIST);		
		}
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(FilterMenu.FILTER_LIST, filterList);
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
