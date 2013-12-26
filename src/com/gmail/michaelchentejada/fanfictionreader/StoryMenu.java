package com.gmail.michaelchentejada.fanfictionreader;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class StoryMenu extends Activity {
	
	private ListView listView;
	private Context context;
	private ProgressDialog progressDialog;
	
	private ArrayList<HashMap<String, String>> list;
	private ArrayList<HashMap<String, Integer>> filterList;
	private ParseStories asyncTask = new ParseStories();
	
	private final OnItemClickListener listListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private final OnClickListener filterListener = new OnClickListener() {	
		@Override
		public void onClick(View v) {
			Intent i = new Intent(context,FilterMenu.class);
			i.putExtra("Filter List", filterList);
			startActivityForResult(i, 1);	
			
		}
	};

	protected void onSaveInstanceState(Bundle outState) {
 		outState.putSerializable("List", list);
		outState.putSerializable("Filter List", filterList);
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
		listView.setOnItemClickListener(listListener);
		
		Button filterButton = (Button)findViewById(R.id.story_menu_filter);
		filterButton.setOnClickListener(filterListener);
		
		String url = "https://m.fanfiction.net" + getIntent().getStringExtra("URL");
		
		if (savedInstanceState == null) {
			asyncTask.execute(url);
		} else {
			list = (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable("List");
			filterList = (ArrayList<HashMap<String, Integer>>) savedInstanceState.getSerializable("Filter List");
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
	
	private class ParseStories extends AsyncTask<String, Void, ArrayList<HashMap<String, String>>>{
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
		protected ArrayList<HashMap<String, String>> doInBackground(
				String... url) {
			
			if (filterList==null) {
				filterList=Parser.Filter(url[0]);
			}
			return Parser.Stories(url[0]);
		}
		@Override
		
		protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
			progressDialog.dismiss();
			
			if (result != null) {
				list = result;
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
}
