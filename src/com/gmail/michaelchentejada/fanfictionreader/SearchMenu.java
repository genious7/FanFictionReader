package com.gmail.michaelchentejada.fanfictionreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView.OnEditorActionListener;


public class SearchMenu extends Activity {

	private Context context;
	private SimpleAdapter adapter;
	private ArrayList<LinkedHashMap<String, Integer>> filterList; //Filter elements
	private ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String,String>>();
	private Button loadPage;
	private EditText textBox;
	private ParseStories parseStory = new ParseStories();
	
	private int[] filter = {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0};//Selected filter, default values applied.
	private int[] selectedPositions = null; //Variable used to restore current filter.
	private int numberOfPages = 1;//Total number of pages
	private int currentPage = 0;//Currently loaded page
	
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
	
	private OnEditorActionListener textChange = new OnEditorActionListener() {
		
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			list.clear();
			parseStory.cancel(true);
			parseStory = new ParseStories();
			parseStory.execute();
			return false;
		}
	};
	
	private final OnItemClickListener listListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			Intent i = new Intent(context,StoryDisplay.class);
			i.putExtra(StoryDisplay.TITLE, list.get((int) arg3).get(Parser.TITLE));
			i.putExtra(StoryDisplay.CHAPTERS, list.get((int) arg3).get(Parser.CHAPTER));
			i.putExtra(StoryDisplay.URL, list.get((int) arg3).get(Parser.URL));
			startActivity(i);
		}
	};
	
	
	private final OnItemLongClickListener openMenu = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			Intent i = new Intent(context,DetailDisplay.class);
			i.putExtra(DetailDisplay.MAP,list.get((int) arg3));
			startActivity(i);
			return false;
		}
	};
	
	/**
	 * A listener that adds a new page to the 
	 */
	private final OnClickListener addPageListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			parseStory.cancel(true);
			parseStory = new ParseStories();
			parseStory.execute();	
		}
	};
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		context = this;
		
		ListView listview = (ListView) findViewById(R.id.menuListView);
		View header = (View)getLayoutInflater().inflate(R.layout.search_menu_header, null);
		View footer = (View)getLayoutInflater().inflate(R.layout.story_menu_footer, null);
		listview.addHeaderView(header);
		listview.addFooterView(footer);
		
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
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(listListener);
		listview.setOnItemLongClickListener(openMenu);
		
		textBox = (EditText)findViewById(R.id.searchText);
		textBox.setOnEditorActionListener(textChange);
		loadPage = (Button)findViewById(R.id.story_load_pages);
		loadPage.setOnClickListener(addPageListener);
		
		Button filterButton = (Button)findViewById(R.id.filter);
		filterButton.setOnClickListener(filterListener);
		
		if (savedInstanceState == null) {
			loadPage.setEnabled(false);
		} else {
			filterList = (ArrayList<LinkedHashMap<String, Integer>>) savedInstanceState.getSerializable(FilterMenu.FILTER_LIST);
			numberOfPages = savedInstanceState.getInt("Pages");
			currentPage = savedInstanceState.getInt("Current Page");
			list.addAll((ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable("List"));			
			loadPage.setText(String.format(getResources().getString(R.string.story_add_page_button), currentPage, numberOfPages));
			if (currentPage >= numberOfPages) {
				loadPage.setEnabled(false);
			}
			
		}
		
	}
	
	protected void onSaveInstanceState(Bundle outState) {
 		outState.putSerializable("List", list);
		outState.putSerializable(FilterMenu.FILTER_LIST, filterList);
		outState.putInt("Pages", numberOfPages);
		outState.putInt("Current Page",currentPage);
		super.onSaveInstanceState(outState);
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
			
			String url = "https://m.fanfiction.net/search.php?type="+"story"+"&ready="+"1"+"&keywords="+textBox.getText().toString()
					+"&categoryid="+filter[13]+"&genreid="+filter[2]+"&languageid="+filter[5]+"&censorid=0"+filter[4]
					+"&statusid="+filter[7]+"&ppage="+currentPage+"&words="+filter[6];
			
						
			try {
				org.jsoup.nodes.Document document = Jsoup.connect(url).get();
				Elements bold = document.select("b");
				for (Element element : bold) {
					element.unwrap();
				}
				
				if (filterList==null) { //Load the filters if they aren't already loaded.
					filterList=Parser.SearchFilter(document);
				}
				
				if (currentPage == 1 ) {//If the first page is being loaded, then load the total number of pages as well.
						numberOfPages = Parser.PagesSearch(document);
				}
				
				return Parser.Stories(url,document,true);//Parse the rest of the info

			} catch (IOException e) {
				Log.e("StoryMenu - doInBackground", Log.getStackTraceString(e));
				return null;
			}

		}
		@Override
		
		protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
			progressDialog.dismiss();
			
			if (result != null) {	
				list.addAll(result);
				adapter.notifyDataSetChanged();				
				loadPage.setText(String.format(getResources().getString(R.string.story_add_page_button), currentPage, numberOfPages));
				if (currentPage >= numberOfPages) {
					loadPage.setEnabled(false);
				}else{
					loadPage.setEnabled(true);
				}				
			}else{
				Toast toast = Toast.makeText(context, getString(R.string.dialog_internet), Toast.LENGTH_SHORT);
				toast.show();
				if (currentPage == 1) {
					finish();
				}else{
					currentPage--;
				}
			}
			super.onPostExecute(result);	
		}
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
				list.clear();
				parseStory.cancel(true);
				parseStory = new ParseStories();
				parseStory.execute();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
