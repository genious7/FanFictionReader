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
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class StoryMenu extends Activity {
	
	private ListView listView;
	private Button loadPage;
	private Context context;
	private ProgressDialog progressDialog;
	private int[] filter = {1,0,0,0,0,0,0,0,0,0,0,0};
	private int numberOfPages = 1;
	private int currentPages = 1;
	
	private ArrayList<HashMap<String, String>> list;
	private ArrayList<LinkedHashMap<String, Integer>> filterList;
	private ParseStories asyncTask = new ParseStories();
	
	private final OnItemClickListener listListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			
		}
	};
	
	private final OnClickListener addPageListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			asyncTask.cancel(true);
			asyncTask = new ParseStories();
			asyncTask.execute();	
		}
	};
	
	private final OnClickListener filterListener = new OnClickListener() {	
		@Override
		public void onClick(View v) {
			Intent i = new Intent(context,FilterMenu.class);
			ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>();
			
			for (int j = 0; j < filterList.size(); j++) {
				keys.add(new ArrayList<String>(filterList.get(j).keySet()));
			}
			
			i.putExtra("Filter List", filterList);
			i.putExtra("Keyset", keys);
			startActivityForResult(i, 1);	
			
		}
	};

	protected void onSaveInstanceState(Bundle outState) {
 		outState.putSerializable("List", list);
		outState.putSerializable("Filter List", filterList);
		outState.putInt("Pages", numberOfPages);
		outState.putInt("Current Page",currentPages);
		super.onSaveInstanceState(outState);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_list_view);
		
		setResult(RESULT_OK);
		context = this;
		listView = (ListView)findViewById(R.id.menuListView);
		View header = (View)getLayoutInflater().inflate(R.layout.story_menu_header, null);
		listView.addHeaderView(header);
		View footer = (View)getLayoutInflater().inflate(R.layout.story_menu_footer, null);
		listView.addFooterView(footer);
		listView.setOnItemClickListener(listListener);
		
		Button filterButton = (Button)findViewById(R.id.story_menu_filter);
		filterButton.setOnClickListener(filterListener);
		
		loadPage = (Button)findViewById(R.id.story_load_pages);
		loadPage.setOnClickListener(addPageListener);
		
		if (savedInstanceState == null) {
			asyncTask.execute();
		} else {
			list = (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable("List");
			filterList = (ArrayList<LinkedHashMap<String, Integer>>) savedInstanceState.getSerializable("Filter List");
			numberOfPages = savedInstanceState.getInt("Pages");
			currentPages = savedInstanceState.getInt("Current Page");
			SimpleAdapter adapter = new SimpleAdapter(context, list,
					R.layout.story_menu_list_item, new String[] {
							Parser.TITLE, Parser.SUMMARY, Parser.AUTHOR,
							Parser.LENGHT, Parser.FOLLOWS }, new int[] {
							R.id.story_menu_list_item_title,
							R.id.story_menu_list_item_summary,
							R.id.story_menu_list_item_author,
							R.id.story_menu_list_item_words,
							R.id.story_menu_list_item_follows });
			listView.setAdapter(adapter);
		}
		
	}
	
	private class ParseStories extends AsyncTask<Void, Void, ArrayList<HashMap<String, String>>>{
		@Override
		protected void onPreExecute() {
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
			url = url + "?srt=" + filter[0] + "&t=" + filter[1] + "&g1=" + filter[2] + "&g2=" + filter[3] + "&r=" + filter[4] + "&lan=" + + filter[5] + "&len=" + filter[6] + "&s=" + filter[7] + "&c1=" + filter[8] + "&c2=" + filter[9] + "&c3="+ filter[10] + "&c4=" + filter[11];
			try {
				org.jsoup.nodes.Document document = Jsoup.connect(url).get();
				if (filterList==null) {
					filterList=Parser.Filter(url,document);
				}
				if (currentPages == 1) {
					numberOfPages = Parser.Pages(url, document);
				}
				return Parser.Stories(url,document);	
				
			} catch (IOException e) {
				Log.e("StoryMenu - doInBackground", Log.getStackTraceString(e));
				return null;
			}

		}
		@Override
		
		protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
			progressDialog.dismiss();
			
			if (result != null) {
				
				if (currentPages == 1) {
					list = result;
					loadPage.setEnabled(true);
				}else{
					list.addAll(result);
				}
					
				SimpleAdapter adapter = new SimpleAdapter(context, list,
						R.layout.story_menu_list_item, new String[] {
								Parser.TITLE, Parser.SUMMARY, Parser.AUTHOR,
								Parser.LENGHT, Parser.FOLLOWS }, new int[] {
								R.id.story_menu_list_item_title,
								R.id.story_menu_list_item_summary,
								R.id.story_menu_list_item_author,
								R.id.story_menu_list_item_words,
								R.id.story_menu_list_item_follows });
				listView.setAdapter(adapter);
				
				loadPage.setText("Page " + currentPages + " of " + numberOfPages);
				currentPages++;
				if (currentPages > numberOfPages) {
					loadPage.setEnabled(false);
				}
				super.onPostExecute(result);
				
			}else{
				end(getResources().getString(R.string.dialog_internet));
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
				Toast toast = Toast.makeText(context, getResources().getString(R.string.dialog_cancelled), Toast.LENGTH_SHORT);
				toast.show();
			}else if (resultCode == RESULT_OK) {
				currentPages = 1;
				filter = data.getIntArrayExtra("Filter");
				asyncTask.cancel(true);
				asyncTask = new ParseStories();
				asyncTask.execute();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
