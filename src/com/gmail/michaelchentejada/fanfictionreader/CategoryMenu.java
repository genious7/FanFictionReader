/**
 * 
 */
package com.gmail.michaelchentejada.fanfictionreader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * The class which shows the main categories in the fan fiction site.
 * @author Michael Chen
 */
public class CategoryMenu extends Activity {
	/*
	 * Variables representing current state
	 */
	private boolean crossover;
	private boolean sort; //true = a-z ; false = views
	private int position2=0;
	
	private Context context;
	private ProgressDialog progress;
	private ListView listView;
	
	private parseSite asynctask = new parseSite();
	private final OnItemClickListener listener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
			if (crossover){
				
			}else{
				Intent i = new Intent(context,StoryMenu.class);
				i.putExtra("URL", list.get((int)id).get(Parser.URL));
				startActivityForResult(i, 1);	
			}
				
		}
	};
	private final OnItemSelectedListener filterListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id2) {	
			if  (position != position2){
				position2 = position;
				asynctask.cancel(true);
				asynctask =  new parseSite();
				asynctask.execute();
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {		
		}
	};

	private final OnCheckedChangeListener sortListener = new OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			Collections.sort(list, new ListComparator(isChecked));
			SimpleAdapter adapter = new SimpleAdapter(context, list, R.layout.category_menu_list_item, new String[] {Parser.TITLE,Parser.VIEWS}, new int[] {R.id.category_menu_title,R.id.category_menu_views});
			listView.setAdapter(adapter);
			sort = isChecked;
		}
	};
	
	private static final String[] FANFIC_URLS = {"/anime/",
		"book/",
		"cartoon/",
		"comic/",
		"game/",
		"misc/",
		"movie/",
		"play/",
		"tv/"}; 
	private ArrayList<HashMap<String, String>> list;
	private int id = 0;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_list_view);
		
		context = this;
		listView = (ListView) findViewById(R.id.menuListView);
		View header = (View)getLayoutInflater().inflate(R.layout.category_menu_header, null);
		listView.addHeaderView(header);
		listView.setOnItemClickListener(listener);
		
		
		Spinner spinner = (Spinner)header.findViewById(R.id.category_menu_header_filter);
		List<String> filterList = new ArrayList<String>();
		filterList.add(getString(R.string.top_200));
		filterList.add("#");
		for (char i = 'A'; i <= 'Z'; i++) {
			filterList.add(Character.toString(i));
		}
		
		position2 = (savedInstanceState == null)? 0 : savedInstanceState.getInt("Positon");
		ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, filterList);
		filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(filterAdapter);
		spinner.setSelection(position2);
		spinner.setOnItemSelectedListener(filterListener);
		
		crossover = getIntent().getBooleanExtra("Crossover", false);
		
		sort = (savedInstanceState==null)? false : savedInstanceState.getBoolean("Sort");
		ToggleButton sortButton = (ToggleButton)findViewById(R.id.category_menu_sort);
		sortButton.setChecked(sort);
		sortButton.setOnCheckedChangeListener(sortListener);	
		
		if (savedInstanceState == null){
			id = getIntent().getIntExtra("Id", 0);	
			if (id >FANFIC_URLS.length){
				end(getResources().getString(R.string.dialog_unspecified));
			}else{
				setResult(RESULT_OK);
				asynctask.execute();
			}
			
		}else {
			list = (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable("List");
			SimpleAdapter adapter = new SimpleAdapter(context, list, R.layout.category_menu_list_item, new String[] {Parser.TITLE,Parser.VIEWS}, new int[] {R.id.category_menu_title,R.id.category_menu_views});
			listView.setAdapter(adapter);	
		}
}
	
	/**
	 * Saves the current list to avoid having to fetch it again upon screen rotation & pause.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("List", list);
		outState.putInt("Positon", position2);
		outState.putBoolean("Crossover", crossover);
		outState.putBoolean("Sort", sort);
		super.onSaveInstanceState(outState);
	}
	
	
	/**
	 * Ends the current intent. Only called when an error occurs. Passes a string containing the error message
	 * to be displayed as a toast on the previous activity.
	 * @param error The error message to display.
	 */
	private void end(String error){
		Intent end = new Intent();
		end.putExtra("Error", error);
		setResult(RESULT_CANCELED, end);
		finish();
	}
	
	/**
	 * The asynchronous class which obtains the list of categories from fanfiction.net. 
	 * @author Michael Chen
	 *
	 */
	private class parseSite extends AsyncTask<Void, Void, ArrayList<HashMap<String, String>>>{

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
		protected ArrayList<HashMap<String, String>> doInBackground(Void... params) {
			String url = "https://m.fanfiction.net/"+(crossover ?"crossovers/":"")+FANFIC_URLS[id] + "?l=" + sortKey();
			return Parser.Categories(url);
		}
		
		@Override
		protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
			// To dismiss the dialog
			progress.dismiss();
			if (result != null) {
				list = result;
				Collections.sort(list, new ListComparator(sort));
				SimpleAdapter adapter = new SimpleAdapter(context, list, R.layout.category_menu_list_item, new String[] {Parser.TITLE,Parser.VIEWS}, new int[] {R.id.category_menu_title,R.id.category_menu_views});
				listView.setAdapter(adapter);				
			}else{
				end(getResources().getString(R.string.dialog_internet));	
			}
			super.onPostExecute(result);
		}
		
	}
	
	/**
	 * Stops the asynchronous task if the screen is closed prematurely. 
	 */
	@Override
	protected void onDestroy() {
		asynctask.cancel(true);
		super.onDestroy();
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && resultCode == RESULT_CANCELED){
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(context, data.getStringExtra("Error"), duration);
			toast.show();
		}
		super.onActivityResult(requestCode, resultCode, data);
		
	}
}
