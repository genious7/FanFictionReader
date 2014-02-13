/**
 * 
 */
package com.gmail.michaelchentejada.fanfictionreader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.jsoup.nodes.Document;

import com.gmail.michaelchentejada.fanfictionreader.util.ListComparator;
import com.gmail.michaelchentejada.fanfictionreader.util.Parser;
import com.gmail.michaelchentejada.fanfictionreader.util.currentState;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

/**
 * The class which shows the main categories in the fan fiction site.
 * @author Michael Chen
 */
public class CategoryMenu extends Menu<HashMap<String, String>> {

	private boolean sort; //true = a-z ; false = views
	private int position2=0;
	
	@Override
	protected void addHeaderView(ListView listview) {
		View header = (View)getLayoutInflater().inflate(R.layout.category_menu_header, null);
		listview.addHeaderView(header);	
	}

	@Override
	protected void listListener(int id) {
		Intent i;
		switch (activityState) {
		case CROSSOVER:
			i = new Intent(context,SubCategoryMenu.class);
			i.setData(Uri.parse("https://m.fanfiction.net"+ list.get((int)id).get(Parser.URL)));
			i.putExtra(Menu.EXTRA_ACTIVITY_STATE, activityState);
			break;
		case NORMAL:
			i = new Intent(context,StoryMenu.class);
			i.setData(Uri.parse("https://m.fanfiction.net"+ list.get((int)id).get(Parser.URL)));
			i.putExtra(Menu.EXTRA_ACTIVITY_STATE, currentState.NORMAL);
			break;
		case COMMUNITIES: default:
			i = new Intent(context,CommunityMenu.class);
			i.putExtra(CommunityMenu.URL, list.get((int)id).get(Parser.URL));
			break;
		}
		startActivity(i);	
	}

	private final OnItemSelectedListener filterListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id2) {	
			if  (position != position2){
				position2 = position;
				setList();
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {		
		}
	};
	
	private final OnCheckedChangeListener sortListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			sort = isChecked;
			refreshList(list);
		}
	};
	

	@Override
	protected void refreshList(java.util.ArrayList<java.util.HashMap<String,String>> list) {
		Collections.sort(list, new ListComparator(sort));
		super.refreshList(list);
	}
	
	@Override
	protected ArrayList<HashMap<String, String>> parsePage(Document document) {
		return Parser.Categories(getString(R.string.Parser_Stories),document);
	}

	@Override
	protected String formatUrl(String baseUrl) {
		return baseUrl + "?l=" + sortKey();
	}

	@Override
	protected SimpleAdapter setAdapter() {
		return new SimpleAdapter(context, list,
				R.layout.category_menu_list_item, new String[] { Parser.TITLE,
						Parser.VIEWS }, new int[] { R.id.category_menu_title,
						R.id.category_menu_views });
	}
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		sort = (savedInstanceState==null)? false : savedInstanceState.getBoolean("Sort");	
		position2 = (savedInstanceState == null)? 0 : savedInstanceState.getInt("Positon");
		
		super.onCreate(savedInstanceState);
		
		Spinner spinner = (Spinner)findViewById(R.id.category_menu_header_filter);
		List<String> filterList = new ArrayList<String>();
		filterList.add(getString(R.string.top_200));
		filterList.add("#");
		for (char i = 'A'; i <= 'Z'; i++) {
			filterList.add(Character.toString(i));
		}
		
		
		ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, filterList);
		filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(filterAdapter);
		spinner.setSelection(position2);
		spinner.setOnItemSelectedListener(filterListener);
		
		ToggleButton sortButton = (ToggleButton)findViewById(R.id.category_menu_sort);
		sortButton.setChecked(sort);
		sortButton.setOnCheckedChangeListener(sortListener);	
		
		if (savedInstanceState == null) {
			setList();
		}
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt("Positon", position2);
		outState.putBoolean("Sort", sort);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected int setNumberOfPages(Document document) {
		return 1;
	}
	
	private String sortKey (){
		char selector = ' ';
		if (position2 == 0) {
			selector = ' ';
		}else if (position2 == 1){
			selector = '1';
		}else{
			selector = (char) ('a'+position2-2);
		}
		return Character.toString(selector);
	}
}




