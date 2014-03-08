package com.gmail.michaelchentejada.fanfictionreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.gmail.michaelchentejada.fanfictionreader.util.Parser;
import com.gmail.michaelchentejada.fanfictionreader.util.currentState;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class CommunityMenu extends Activity {
	protected final static String URL = "URL";
	private final static String LIST = "List";
	private final static String CURPAGE = "Current Page";
	private final static String FILTERPOSITION = "Filter";
	private final static int[] FILTERVALUES = {3,1,2,4,99}; 
	
	private ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String,String>>();
	private SimpleAdapter adapter;
	private Context context;
	private findCommmunities asyncTask = new findCommmunities();
	private Button addPageButton;
	private int currentPage = 1;
	private int totalPages = 0;
	private int filterPos = 0;
	
	private OnClickListener loadPageListener = new OnClickListener() {	
		@Override
		public void onClick(View v) {
			currentPage++;
			asyncTask.cancel(true);
			asyncTask = new findCommmunities();
			asyncTask.execute();		
		}
	};
	private OnItemClickListener listener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long id) {
			Intent i = new Intent(context,StoryMenu.class);
			i.setData(Uri.parse("https://m.fanfiction.net" + list.get((int)id).get(Parser.URL)));
			i.putExtra(Menu.EXTRA_ACTIVITY_STATE, currentState.COMMUNITIES);
			startActivity(i);
		}
	};
	private OnItemSelectedListener spinnerListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
				long id) {
			if (position != filterPos){
				filterPos = position;
				list.clear();
				currentPage = 1;
				asyncTask.cancel(true);
				asyncTask = new findCommmunities();
				asyncTask.execute();	
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
	};
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		context = this;
		
		
		
		ListView listview = (ListView)findViewById(R.id.list);
		listview.setOnItemClickListener(listener);
		adapter = new SimpleAdapter(context, list, R.layout.community_menu_list_item, 
				new String[]{Parser.TITLE,Parser.SUMMARY,Parser.VIEWS,Parser.AUTHOR}, 
				new int[]{R.id.community_title,R.id.community_summary,R.id.community_stories,R.id.community_author});
		
		View header = (View)getLayoutInflater().inflate(R.layout.community_menu_header, null);
		View footer = (View)getLayoutInflater().inflate(R.layout.story_menu_footer, null);
		listview.addFooterView(footer);
		listview.addHeaderView(header);
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(listener);
		
		Spinner spinner = (Spinner)header.findViewById(R.id.community_spinner);
		spinner.setSelection(savedInstanceState == null? 0 : savedInstanceState.getInt(FILTERPOSITION, 0));
		spinner.setOnItemSelectedListener(spinnerListener);
		
		addPageButton = (Button)findViewById(R.id.story_load_pages);
		addPageButton.setOnClickListener(loadPageListener);
		
		if (savedInstanceState == null) {
			asyncTask.execute();
		} else {
			currentPage = savedInstanceState.getInt(CURPAGE, 1);
			list.addAll((ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable(LIST));
			if (currentPage >= totalPages) {
				addPageButton.setEnabled(false);
			}else{
				addPageButton.setEnabled(true);
			}
		}
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(CURPAGE, currentPage);
		outState.putSerializable(LIST, list);
		outState.putInt(FILTERPOSITION, filterPos);
		super.onSaveInstanceState(outState);
	}
	
	private class findCommmunities extends AsyncTask<Void, Void, ArrayList<HashMap<String, String>>>{
		private ProgressDialog progress;
		
		@Override
		protected void onPreExecute() {
			progress = new ProgressDialog(context);
			progress.setTitle("");
			progress.setMessage(getResources().getString(R.string.dialog_loading));
			progress.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();	
				}
			});
			progress.show();
			super.onPreExecute();
		}
		
		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(
				Void... params) {
			try {
				Document document = Jsoup.connect("https://m.fanfiction.net/"  + getIntent().getStringExtra(URL)+ "0/" + FILTERVALUES[filterPos] + "/" + currentPage + "/").get();
				if (totalPages == 0) {
					totalPages = Parser.Pages(document);
				}
				return Parser.Communities(getString(R.string.Parser_Stories),document);
			} catch (IOException e) {
				return null;
			}	
		}
		
		@Override
		protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
			progress.dismiss();
			if (result != null) {
				list.addAll(result);
				adapter.notifyDataSetChanged();
				addPageButton.setText(String.format(getString(R.string.story_add_page_button), currentPage,totalPages));
				if (currentPage >= totalPages) {
					addPageButton.setEnabled(false);
				}else{
					addPageButton.setEnabled(true);
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
	
}
