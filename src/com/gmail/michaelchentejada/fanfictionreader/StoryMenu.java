package com.gmail.michaelchentejada.fanfictionreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.jsoup.Jsoup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class StoryMenu extends Activity {
	
	private ListView listView; //Main ListView
	private Button loadPage; //Button to load additional pages
	private Context context; //Current context

	private int[] filter = {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0};//Selected filter, default values applied.
	private int[] selectedPositions = null; //Variable used to restore current filter.
	private int numberOfPages = 1;//Total number of pages
	private int currentPage = 0;//Currently loaded page
	
	protected final static String URL = "URL";
	protected final static String JUST_IN = "JustIn";
	
	private boolean justIn = false;
	private ArrayList<LinkedHashMap<String, Integer>> filterList; //Filter elements
	private ArrayList<HashMap<String, String>> list; //List of stories
	private SimpleAdapter adapter;
	private ParseStories asyncTask = new ParseStories(); //Async Task
	
	/**
	 * A listener that opens a story
	 */
	private final OnItemClickListener listListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			Intent i = new Intent(context,StoryDisplay.class);
			i.putExtra(StoryDisplay.TITLE, list.get((int) arg3).get(Parser.TITLE));
			i.putExtra(StoryDisplay.CHAPTERS, list.get((int) arg3).get(Parser.CHAPTER));
			i.putExtra(StoryDisplay.URL, list.get((int) arg3).get(Parser.URL));
			startActivityForResult(i, 2);
		}
	};
	
	
	private final OnItemLongClickListener openMenu = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			Intent i = new Intent(context,DetailDisplay.class);
			i.putExtra(DetailDisplay.MAP,list.get((int) arg3));
			startActivityForResult(i, 3);
			return false;
		}
	};
	
	/**
	 * A listener that adds a new page to the 
	 */
	private final OnClickListener addPageListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			asyncTask.cancel(true);
			asyncTask = new ParseStories();
			asyncTask.execute();	
		}
	};
	
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
			i.putExtra(FilterMenu.JUST_IN, justIn);
			startActivityForResult(i, 1);		
		}
	};

	protected void onSaveInstanceState(Bundle outState) {
 		outState.putSerializable("List", list);
		outState.putSerializable(FilterMenu.FILTER_LIST, filterList);
		outState.putInt("Pages", numberOfPages);
		outState.putInt("Current Page",currentPage);
		super.onSaveInstanceState(outState);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_list_view);
		
		setResult(RESULT_OK);// Set to OK unless an error occurs
		context = this;
		
		//Set the ListView header and footer
		View header = (View)getLayoutInflater().inflate(R.layout.story_menu_header, null);
		View footer = (View)getLayoutInflater().inflate(R.layout.story_menu_footer, null);
		listView = (ListView)findViewById(R.id.menuListView);
		listView.addHeaderView(header);
		listView.addFooterView(footer);
		listView.setOnItemClickListener(listListener);
		listView.setOnItemLongClickListener(openMenu);
		
		//Set the filtering button
		Button filterButton = (Button)findViewById(R.id.story_menu_filter);
		filterButton.setOnClickListener(filterListener);
		
		//Set the add page button
		loadPage = (Button)findViewById(R.id.story_load_pages);
		loadPage.setOnClickListener(addPageListener);
		
		justIn = getIntent().getBooleanExtra(JUST_IN, false);
		
		if (justIn) {
			loadPage.setVisibility(View.GONE);
		}
		
		if (savedInstanceState == null) {
			asyncTask.execute();
		} else {
			filterList = (ArrayList<LinkedHashMap<String, Integer>>) savedInstanceState.getSerializable(FilterMenu.FILTER_LIST);
			numberOfPages = savedInstanceState.getInt("Pages");
			currentPage = savedInstanceState.getInt("Current Page");
			
			list = (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable("List");
			
			loadPage.setText("Page " + currentPage + " of " + numberOfPages);
			if (currentPage >= numberOfPages) {
				loadPage.setEnabled(false);
			}
			
			adapter = new SimpleAdapter(context, list,
					R.layout.story_menu_list_item, new String[] {
							Parser.TITLE, Parser.SUMMARY, Parser.AUTHOR,
							Parser.LENGHT, Parser.FOLLOWS ,Parser.CHAPTER}, new int[] {
							R.id.story_menu_list_item_title,
							R.id.story_menu_list_item_summary,
							R.id.story_menu_list_item_author,
							R.id.story_menu_list_item_words,
							R.id.story_menu_list_item_follows,
							R.id.story_menu_list_item_chapters});
			listView.setAdapter(adapter);
		}
		
	}
	
	private class ParseStories extends AsyncTask<Void, Void, ArrayList<HashMap<String, String>>>{
		private ProgressDialog progressDialog; //Progress dialogue for the async task.
		
		@Override
		protected void onPreExecute() {
			currentPage++;//Load next page
			
			progressDialog = new ProgressDialog(context);
			progressDialog.setTitle("");
			progressDialog.setMessage(getResources().getString(R.string.dialog_loading));
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();	
				}
			});
			progressDialog.show();
			super.onPreExecute();
		}
		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(Void...voids) {
			
			String url = "https://m.fanfiction.net" + getIntent().getStringExtra("URL");
			
			if (justIn) {
				url = url + "?s=" + filter[12] + "&cid=" + filter[13] + "&l="
						+ filter[14];
			}else {				
				url = url + "?srt=" + filter[0] + "&t=" + filter[1] + "&g1="
						+ filter[2] + "&g2=" + filter[3] + "&r=" + filter[4]
						+ "&lan=" + +filter[5] + "&len=" + filter[6] + "&s="
						+ filter[7] + "&c1=" + filter[8] + "&c2=" + filter[9]
						+ "&c3=" + filter[10] + "&c4=" + filter[11] + "&p="
						+ currentPage;
			}
			
			try {
				org.jsoup.nodes.Document document = Jsoup.connect(url).get();
				
				if (filterList==null) { //Load the filters if they aren't already loaded.
					filterList=Parser.Filter(url,document);
				}
				
				if (!justIn && currentPage == 1 ) {//If the first page is being loaded, then load the total number of pages as well.
						numberOfPages = Parser.Pages(url, document);
					}
				return Parser.Stories(url,document,justIn);//Parse the rest of the info
				
			} catch (IOException e) {
				Log.e("StoryMenu - doInBackground", Log.getStackTraceString(e));
				return null;
			}

		}
		@Override
		
		protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
			progressDialog.dismiss();
			
			if (result != null) {	
				
				if (currentPage == 1) {
					list = result;
					loadPage.setEnabled(true);
					
					adapter = new SimpleAdapter(context, list,
							R.layout.story_menu_list_item, new String[] {
									Parser.TITLE, Parser.SUMMARY, Parser.AUTHOR,
									Parser.LENGHT, Parser.FOLLOWS ,Parser.CHAPTER}, new int[] {
									R.id.story_menu_list_item_title,
									R.id.story_menu_list_item_summary,
									R.id.story_menu_list_item_author,
									R.id.story_menu_list_item_words,
									R.id.story_menu_list_item_follows,
									R.id.story_menu_list_item_chapters});
					
					listView.setAdapter(adapter);
				}else{
					list.addAll(result);
					adapter.notifyDataSetChanged();
				}
					
				loadPage.setText("Page " + currentPage + " of " + numberOfPages);
				
				if (currentPage >= numberOfPages) {
					loadPage.setEnabled(false);
				}
				
				super.onPostExecute(result);
				
			}else{
				end(getResources().getString(R.string.dialog_internet)); //Connection error
			}
		}
	}
	
	private void end(String error){
		Intent end = new Intent();
		end.putExtra("Error", error);
		setResult(RESULT_CANCELED, end);
		finish();
	}
	
	@Override
	protected void onDestroy() {
		asyncTask.cancel(true);
		super.onDestroy();
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
				asyncTask.cancel(true);
				asyncTask = new ParseStories();
				asyncTask.execute();
			}
		}else if (requestCode == 2) { //Read Story
			
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
