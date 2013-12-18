package com.gmail.michaelchentejada.fanfictionreader;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class StoryMenu extends Activity {
	
	private ListView listView;
	private Context context;
	private ProgressDialog progressDialog;
	private ArrayList<HashMap<String, String>> list;
	private final OnItemClickListener listListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_story_menu);
		
		context = this;
		listView = (ListView)findViewById(R.id.storyMenuListView);
		listView.setOnItemClickListener(listListener);
		
		if (savedInstanceState == null) {
			
		} else {
			SimpleAdapter adapter;
			
		}
		
	}
	
	private class ParseStories extends AsyncTask<String, Void, ArrayList<HashMap<String, String>>>{

		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(
				String... params) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
