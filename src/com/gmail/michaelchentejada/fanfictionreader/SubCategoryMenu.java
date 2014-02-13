package com.gmail.michaelchentejada.fanfictionreader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.jsoup.nodes.Document;

import com.gmail.michaelchentejada.fanfictionreader.util.ListComparator;
import com.gmail.michaelchentejada.fanfictionreader.util.Parser;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SubCategoryMenu extends Menu<HashMap<String, String>> {

	private String crossoverUrl; 
	private boolean sort; //true = a-z ; false = views
	private int position2=0;
	
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
	

	private final OnClickListener allCrossovers = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent i = new Intent(context,StoryMenu.class);
			i.setData(Uri.parse("https://m.fanfiction.net" + crossoverUrl));
			i.putExtra(Menu.EXTRA_ACTIVITY_STATE, activityState);
			startActivityForResult(i, 1);							
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
	protected void addHeaderView(ListView listview) {
		View header = (View)getLayoutInflater().inflate( R.layout.sub_category_menu_header, null);
		listview.addHeaderView(header);
	}

	@Override
	protected void listListener(int id) {
		Intent i = new Intent(context,StoryMenu.class);
		i.setData(Uri.parse("https://m.fanfiction.net" + list.get((int)id).get(Parser.URL)));
		i.putExtra(Menu.EXTRA_ACTIVITY_STATE, activityState);
		startActivity(i);		
	}

	@Override
	protected ArrayList<HashMap<String, String>> parsePage(Document document) {
		if (crossoverUrl == null) 
			crossoverUrl  = Parser.crossoverUrl(document);
		return Parser.Categories(getString(R.string.Parser_Stories),document);
	}

	@Override
	protected void refreshList(ArrayList<HashMap<String, String>> list) {
		Collections.sort(list, new ListComparator(sort));
		super.refreshList(list);
	}

	@Override
	protected String formatUrl(String baseUrl) {
		return baseUrl + "?pcategoryid=" + sortKey();
	}

	@Override
	protected SimpleAdapter setAdapter() {
		return new SimpleAdapter(context, list,
				R.layout.category_menu_list_item, new String[] { Parser.TITLE,
						Parser.VIEWS }, new int[] { R.id.category_menu_title,
						R.id.category_menu_views });
	}
	
	@Override
	protected int setNumberOfPages(Document document) {
		return 1;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		position2 = (savedInstanceState == null)? 0 : savedInstanceState.getInt("Positon");
		sort = (savedInstanceState==null)? false : savedInstanceState.getBoolean("Sort");
		super.onCreate(savedInstanceState);
		
		Spinner spinner = (Spinner)findViewById(R.id.category_menu_header_filter);
		List<String> filterList = new ArrayList<String>();
		filterList.add(getString(R.string.category_button_all));
		filterList.addAll(Arrays.asList(getResources().getStringArray(R.array.category_button)));
		
		
		ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, filterList);
		filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(filterAdapter);
		spinner.setSelection(position2);
		spinner.setOnItemSelectedListener(filterListener);
		
		ToggleButton sortButton = (ToggleButton)findViewById(R.id.category_menu_sort);
		sortButton.setChecked(sort);
		sortButton.setOnCheckedChangeListener(sortListener);	
		
		Button allcrossovers = (Button)findViewById(R.id.sub_category_menu_all_crossovers);
		allcrossovers.setOnClickListener(allCrossovers);
		
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
	
	private String sortKey (){
		if (position2 == 0) {
			return "0";
		}else if (position2 < 5){
			return "20"+position2;
		}else if (position2 == 5) {
			return "209";
		}else if (position2 == 6) {
			return "211";
		}else if (position2 == 7){
			return "205";
		}else if (position2 == 8){
			return "207";
		}else{
			return "208";
		}
}
}